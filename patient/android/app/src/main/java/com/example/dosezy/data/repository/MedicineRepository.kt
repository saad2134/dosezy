// MedicineRepository.kt
package com.example.dosezy.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.Medicine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

import dagger.hilt.android.qualifiers.ApplicationContext

class MedicineRepository @Inject constructor(
    private val database: DosezyDatabase,
    private val scheduleRepository: ScheduleRepository,
    @ApplicationContext val context: Context
) {

    companion object {
        private const val TAG = "MedicineRepository"
    }

    // Active medicines only (returns Flow)
    fun getMedicinesByUser(userId: String): Flow<List<Medicine>> =
        database.medicineDao().getActiveMedicinesByUser(userId)

    // All medicines including archived (returns Flow)
    fun getAllMedicinesByUser(userId: String): Flow<List<Medicine>> =
        database.medicineDao().getMedicinesByUser(userId)

    // Archived / discontinued medicines (returns Flow)
    fun getArchivedMedicinesByUser(userId: String): Flow<List<Medicine>> =
        database.medicineDao().getArchivedMedicinesByUser(userId)

    // Get active medicines as list (not Flow)
    suspend fun getMedicinesByUserSync(userId: String): List<Medicine> =
        database.medicineDao().getActiveMedicinesByUser(userId).first()

    // Get all medicines (active + archived) directly for cleanup / export
    suspend fun getMedicinesByUserDirect(userId: String): List<Medicine> =
        database.medicineDao().getMedicinesByUserDirect(userId)

    @RequiresApi(Build.VERSION_CODES.O)
    @Suppress("UNUSED_PARAMETER")
    suspend fun insertMedicine(medicine: Medicine, context: Context? = null) {
        Log.d(TAG, "Inserting medicine: ${medicine.medicationName} for user: ${medicine.userId}")

        // First insert the medicine
        database.medicineDao().insertMedicine(medicine)
        Log.d(TAG, "Medicine inserted successfully")

        // Then generate and insert schedule entries for the next 30 days
        val scheduleEntries = medicine.generateScheduleEntries(LocalDate.now(), 30)
        Log.d(TAG, "Generated ${scheduleEntries.size} schedule entries")

        // Guard: Batch insert schedule entries in a single Room transaction to eliminate 120+ sequential SQLite fsync operations and UI frame drops
        database.scheduleDao().insertScheduleEntries(scheduleEntries)
        Log.d(TAG, "All ${scheduleEntries.size} schedule entries inserted in batch")

        // SCHEDULE ALARMS IMMEDIATELY
        scheduleRepository.rescheduleAllAlarms(medicine.userId, this.context)
        Log.d(TAG, "Immediately scheduled alarms for new medicine")

        // Update home screen widget
        try {
            com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(this.context)
        } catch (_: Exception) {}

        // Debug
        val allEntries = database.scheduleDao().getAllScheduleEntries(medicine.userId)
        Log.d(TAG, "Total schedule entries in DB: ${allEntries.size}")
        allEntries.take(5).forEach { entry ->
            Log.d(TAG, "Sample entry: ${entry.scheduledDateTime} - ${entry.medicineId}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun updateMedicine(medicine: Medicine) {
        val oldMedicine = database.medicineDao().getMedicineByIdDirect(medicine.medicineId)

        // Update the medicine metadata
        database.medicineDao().updateMedicine(medicine)

        if (oldMedicine != null) {
            val scheduleChanged = oldMedicine.scheduledTimes != medicine.scheduledTimes ||
                    oldMedicine.frequency != medicine.frequency ||
                    oldMedicine.timesPerDay != medicine.timesPerDay ||
                    oldMedicine.dosage != medicine.dosage ||
                    oldMedicine.customDosages != medicine.customDosages ||
                    oldMedicine.startDate != medicine.startDate ||
                    oldMedicine.endDate != medicine.endDate ||
                    oldMedicine.durationDays != medicine.durationDays

            if (scheduleChanged) {
                // Cancel existing alarms for this medicine BEFORE modifying or deleting schedule entries
                scheduleRepository.cancelAlarmsForMedicine(medicine.medicineId, this.context)

                // Guard: Align deletion cutoff with generateScheduleEntries (now - 15m) so untaken doses from earlier today are not deleted and lost from adherence history
                val cutoff = LocalDateTime.now().minusMinutes(15)
                val cutoffEpochMillis = cutoff.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()

                // Delete untaken schedule entries from cutoff onwards for this medicine
                database.scheduleDao().deleteUntakenScheduleEntriesFrom(medicine.medicineId, cutoffEpochMillis)

                // Generate new schedule entries starting from today for 30 days
                val newEntries = medicine.generateScheduleEntries(LocalDate.now(), 30)

                // Insert the new entries (already taken entries are preserved by OnConflictStrategy.IGNORE)
                database.scheduleDao().insertScheduleEntries(newEntries)
            }
        } else {
            // Fallback if old medicine wasn't in DB: cancel existing alarms first
            scheduleRepository.cancelAlarmsForMedicine(medicine.medicineId, this.context)
            val scheduleEntries = medicine.generateScheduleEntries(LocalDate.now(), 30)
            database.scheduleDao().deleteScheduleEntriesByMedicine(medicine.medicineId)
            database.scheduleDao().insertScheduleEntries(scheduleEntries)
        }

        // Reschedule alarms for user (reconstructs grouped alarms for all active medications)
        scheduleRepository.rescheduleAllAlarms(medicine.userId, this.context)

        // Update home screen widget
        try {
            com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(this.context)
        } catch (_: Exception) {}
    }


    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun archiveMedicine(medicine: Medicine) {
        // 1. Cancel future alarms for this medicine
        scheduleRepository.cancelAlarmsForMedicine(medicine.medicineId, this.context)
        // 2. Delete only future pending schedule entries (preserves all past taken/missed records!)
        val nowMillis = java.time.LocalDateTime.now().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        database.scheduleDao().deleteFuturePendingScheduleEntries(medicine.medicineId, nowMillis)
        // 3. Mark medicine as archived
        database.medicineDao().setArchivedStatus(medicine.medicineId, true)
        // 4. Reschedule remaining active alarms for user to ensure sibling medicines in slots are preserved
        scheduleRepository.rescheduleAllAlarms(medicine.userId, this.context)
        // 5. Update home widget
        try {
            com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(this.context)
        } catch (_: Exception) {}
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun unarchiveMedicine(medicine: Medicine) {
        // 1. Mark medicine as active (unarchived)
        database.medicineDao().setArchivedStatus(medicine.medicineId, false)
        // 2. Generate new schedule entries starting from today for 30 days
        val newEntries = medicine.copy(isArchived = false).generateScheduleEntries(LocalDate.now(), 30)
        database.scheduleDao().insertScheduleEntries(newEntries)
        // 3. Reschedule all alarms for user so grouped slots are safely reconstructed
        scheduleRepository.rescheduleAllAlarms(medicine.userId, this.context)
        // 4. Update home widget
        try {
            com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(this.context)
        } catch (_: Exception) {}
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun deleteMedicinePermanently(medicine: Medicine) {
        // Cancel alarms first
        scheduleRepository.cancelAlarmsForMedicine(medicine.medicineId, this.context)
        // Delete all schedule entries
        database.scheduleDao().deleteScheduleEntriesByMedicine(medicine.medicineId)
        // Then delete the medicine
        database.medicineDao().deleteMedicine(medicine)

        // Guard: Delete internal storage medicine photo and verify canonicalPath to prevent storage leaks and path traversal
        medicine.imageUri?.let { uriStr ->
            try {
                val cleanPath = uriStr.removePrefix("file://")
                val file = java.io.File(cleanPath)
                val filesDir = this.context.filesDir
                if (file.exists() && (file.parentFile?.canonicalPath == filesDir.canonicalPath || file.canonicalPath.startsWith(filesDir.canonicalPath + java.io.File.separator))) {
                    file.delete()
                }
            } catch (_: Exception) {}
        }

        // Reschedule remaining active alarms for user to preserve sibling medicines
        scheduleRepository.rescheduleAllAlarms(medicine.userId, this.context)

        // Update home screen widget
        try {
            com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(this.context)
        } catch (_: Exception) {}
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun deleteMedicine(medicine: Medicine) {
        deleteMedicinePermanently(medicine)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun deleteMedicine(medicineId: String) {
        val med = database.medicineDao().getMedicineByIdDirect(medicineId)
        if (med != null) {
            deleteMedicinePermanently(med)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addMedicineWithAlarms(medicine: Medicine, context: Context) {
        // Insert the medicine
        insertMedicine(medicine)

        // Schedule alarms for this medicine
        scheduleRepository.scheduleAlarmsForMedicine(medicine.medicineId, context)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun updateMedicineWithAlarms(medicine: Medicine, context: Context) {
        // Update the medicine
        updateMedicine(medicine)

        // Cancel existing alarms and reschedule
        scheduleRepository.cancelAlarmsForMedicine(medicine.medicineId, context)
        scheduleRepository.scheduleAlarmsForMedicine(medicine.medicineId, context)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun deleteMedicineWithAlarms(medicineId: String, context: Context) {
        // Cancel alarms first
        scheduleRepository.cancelAlarmsForMedicine(medicineId, context)

        // Then delete the medicine (which will cascade delete schedule entries)
        deleteMedicine(medicineId) // uses the overloaded method
    }
}