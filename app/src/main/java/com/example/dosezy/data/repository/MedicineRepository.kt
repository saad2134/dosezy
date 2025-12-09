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
import javax.inject.Inject

class MedicineRepository @Inject constructor(
    private val database: DosezyDatabase,
    private val scheduleRepository: ScheduleRepository
) {

    companion object {
        private const val TAG = "MedicineRepository"
    }

    // Method (returns Flow)
    fun getMedicinesByUser(userId: String): Flow<List<Medicine>> =
        database.medicineDao().getMedicinesByUser(userId)

    // Get medicines as list (not Flow)
    suspend fun getMedicinesByUserSync(userId: String): List<Medicine> =
        database.medicineDao().getMedicinesByUser(userId).first()

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun insertMedicine(medicine: Medicine, context: Context? = null) {
        Log.d(TAG, "Inserting medicine: ${medicine.medicationName} for user: ${medicine.userId}")

        // First insert the medicine
        database.medicineDao().insertMedicine(medicine)
        Log.d(TAG, "Medicine inserted successfully")

        // Then generate and insert schedule entries for the next 30 days
        val scheduleEntries = medicine.generateScheduleEntries(LocalDate.now(), 30)
        Log.d(TAG, "Generated ${scheduleEntries.size} schedule entries")

        scheduleEntries.forEach { entry ->
            database.scheduleDao().insertScheduleEntry(entry)
            Log.d(TAG, "Inserted schedule entry for: ${entry.scheduledDateTime}")
        }
        Log.d(TAG, "All schedule entries inserted")

        // SCHEDULE ALARMS IMMEDIATELY
        context?.let {
            scheduleRepository.scheduleAlarmsForMedicine(medicine.medicineId, it)
            Log.d(TAG, "Immediately scheduled alarms for new medicine")
        }

        // Debug
        val allEntries = database.scheduleDao().getAllScheduleEntries(medicine.userId)
        Log.d(TAG, "Total schedule entries in DB: ${allEntries.size}")
        allEntries.take(5).forEach { entry ->
            Log.d(TAG, "Sample entry: ${entry.scheduledDateTime} - ${entry.medicineId}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun updateMedicine(medicine: Medicine) {
        // Update the medicine
        database.medicineDao().updateMedicine(medicine)

        // Delete existing schedule entries for this medicine and regenerate
        val scheduleEntries = medicine.generateScheduleEntries(LocalDate.now(), 30)

        // Remove old entries for this medicine
        database.scheduleDao().deleteScheduleEntriesByMedicine(medicine.medicineId)

        // Insert new entries
        scheduleEntries.forEach { entry ->
            database.scheduleDao().insertScheduleEntry(entry)
        }
    }


    suspend fun deleteMedicine(medicine: Medicine) {
        // Delete schedule entries first
        database.scheduleDao().deleteScheduleEntriesByMedicine(medicine.medicineId)
        // Then delete the medicine
        database.medicineDao().deleteMedicine(medicine)
    }


    suspend fun deleteMedicine(medicineId: String) {
        // Delete schedule entries first
        database.scheduleDao().deleteScheduleEntriesByMedicine(medicineId)
        // Then delete the medicine using the new method
        database.medicineDao().deleteMedicineById(medicineId)
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