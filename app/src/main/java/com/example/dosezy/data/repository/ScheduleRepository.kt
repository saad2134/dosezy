// ScheduleRepository.kt
package com.example.dosezy.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.ScheduleWithMedicine
import com.example.dosezy.notifications.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime

class ScheduleRepository(private val database: DosezyDatabase) {

    companion object {
        private const val TAG = "ScheduleRepository"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getScheduleForDate(userId: String, date: LocalDate): Flow<List<ScheduleEntry>> {
        Log.d(TAG, "Getting schedule for date: $date")
        return database.scheduleDao().getScheduleForUser(userId).map { entries ->
            // Manual filtering by date
            val filteredEntries = entries.filter { entry ->
                try {
                    val entryDate = entry.scheduledDateTime.toLocalDate()
                    entryDate == date
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing date for entry: ${entry.scheduledDateTime}", e)
                    false
                }
            }
            Log.d(TAG, "Filtered ${filteredEntries.size} entries for date $date from ${entries.size} total entries")
            filteredEntries
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getScheduleWithMedicineForDate(userId: String, date: LocalDate): Flow<List<ScheduleWithMedicine>> {
        Log.d(TAG, "Getting schedule with medicine for date: $date")
        return database.scheduleDao().getScheduleWithMedicineForUser(userId).map { scheduleWithMedicine ->
            // Manual filtering by date
            val filteredEntries = scheduleWithMedicine.filter { item ->
                try {
                    val entryDate = item.scheduleEntry.scheduledDateTime.toLocalDate()
                    entryDate == date
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing date for entry: ${item.scheduleEntry.scheduledDateTime}", e)
                    false
                }
            }
            Log.d(TAG, "Filtered ${filteredEntries.size} schedule with medicine entries for date $date from ${scheduleWithMedicine.size} total entries")
            filteredEntries.forEach { item ->
                Log.d(TAG, "Found: ${item.medicine?.medicationName} at ${item.scheduleEntry.scheduledDateTime}")
            }
            filteredEntries
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun autoExtendSchedules(userId: String) {
        try {
            val medicines = database.medicineDao().getMedicinesByUser(userId).first()
            medicines.forEach { medicine ->
                val scheduleEntries = database.scheduleDao().getScheduleEntriesByMedicine(medicine.medicineId)
                val latestEntry = scheduleEntries.maxByOrNull { it.scheduledDateTime }

                // If no entries exist, or the latest entry is less than 15 days in the future,
                // auto-generate/append next 30 days of schedules
                if (latestEntry == null || latestEntry.scheduledDateTime.isBefore(LocalDateTime.now().plusDays(15))) {
                    val startGenerateFrom = latestEntry?.scheduledDateTime?.toLocalDate()?.plusDays(1) ?: LocalDate.now()
                    val newEntries = medicine.generateScheduleEntries(startGenerateFrom, 30)
                    if (newEntries.isNotEmpty()) {
                        database.scheduleDao().insertScheduleEntries(newEntries)
                        Log.d(TAG, "Auto-extended schedule for medicine: ${medicine.medicationName} by 30 days starting from $startGenerateFrom")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error auto-extending schedules for user: $userId", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun scheduleAlarmsForMedicine(medicineId: String, context: Context) {
        val alarmScheduler = AlarmScheduler(context)
        val medicine = database.medicineDao().getMedicineById(medicineId).first()

        if (medicine != null) {
            // First check and extend schedules for this user
            autoExtendSchedules(medicine.userId)

            val scheduleEntries = database.scheduleDao().getScheduleEntriesByMedicine(medicineId)
            var scheduledCount = 0
            val limitTime = LocalDateTime.now().plusDays(7)

            scheduleEntries.forEach { entry ->
                if (entry.status == MedicationStatus.PENDING &&
                    entry.scheduledDateTime.isAfter(LocalDateTime.now()) &&
                    entry.scheduledDateTime.isBefore(limitTime)) {

                    alarmScheduler.scheduleMedicineAlarm(entry, medicine.medicationName)
                    scheduledCount++
                } else {
                    // Cancel alarms that are further in the future or no longer pending
                    alarmScheduler.cancelAlarm(entry.entryId)
                    alarmScheduler.cancelSnooze(entry.entryId)
                }
            }
            Log.d(TAG, "Scheduled alarms for medicine: ${medicine.medicationName} ($scheduledCount/${scheduleEntries.size} entries, limit 7 days)")
        } else {
            Log.w(TAG, "Medicine not found for ID: $medicineId - cannot schedule alarms")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun cancelAlarmsForMedicine(medicineId: String, context: Context) {
        val alarmScheduler = AlarmScheduler(context)
        val scheduleEntries = database.scheduleDao().getScheduleEntriesByMedicine(medicineId)
        scheduleEntries.forEach { entry ->
            alarmScheduler.cancelAlarm(entry.entryId)
            alarmScheduler.cancelSnooze(entry.entryId)
        }
        Log.d(TAG, "Cancelled alarms for medicine ID: $medicineId (${scheduleEntries.size} entries)")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun rescheduleAllAlarms(userId: String, context: Context) {
        val alarmScheduler = AlarmScheduler(context)

        try {
            // First check and extend schedules
            autoExtendSchedules(userId)

            // Get all schedule entries for the user
            val allEntries = database.scheduleDao().getAllScheduleEntries(userId)

            // Cancel all existing alarms first
            allEntries.forEach { entry ->
                alarmScheduler.cancelAlarm(entry.entryId)
                alarmScheduler.cancelSnooze(entry.entryId)
            }

            // Schedule new alarms for pending future entries within the 7-day window
            var scheduledCount = 0
            val limitTime = LocalDateTime.now().plusDays(7)
            allEntries.forEach { entry ->
                if (entry.status == MedicationStatus.PENDING &&
                    entry.scheduledDateTime.isAfter(LocalDateTime.now()) &&
                    entry.scheduledDateTime.isBefore(limitTime)) {

                    val medicine = database.medicineDao().getMedicineById(entry.medicineId).first()
                    medicine?.let {
                        alarmScheduler.scheduleMedicineAlarm(entry, it.medicationName)
                        scheduledCount++
                    }
                }
            }

            Log.d(TAG, "Rescheduled $scheduledCount alarms for user: $userId (from ${allEntries.size} total entries, limit 7 days)")
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling alarms for user: $userId", e)
            throw e
        }
    }

    // method to schedule alarms for a specific date range
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun scheduleAlarmsForDateRange(userId: String, startDate: LocalDate, endDate: LocalDate, context: Context) {
        val alarmScheduler = AlarmScheduler(context)

        try {
            val entries = database.scheduleDao().getScheduleForDateRange(userId, startDate.toString(), endDate.toString()).first()

            var scheduledCount = 0
            entries.forEach { entry ->
                if (entry.status == MedicationStatus.PENDING &&
                    entry.scheduledDateTime.isAfter(LocalDateTime.now())) {

                    val medicine = database.medicineDao().getMedicineById(entry.medicineId).first()
                    medicine?.let {
                        alarmScheduler.scheduleMedicineAlarm(entry, it.medicationName)
                        scheduledCount++
                    }
                }
            }

            Log.d(TAG, "Scheduled $scheduledCount alarms for date range $startDate to $endDate")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarms for date range", e)
        }
    }

    fun getScheduleForDateRange(userId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<ScheduleEntry>> =
        database.scheduleDao().getScheduleForDateRange(userId, startDate.toString(), endDate.toString())

    // method to get schedules as list (not Flow)
    suspend fun getSchedulesByUserSync(userId: String): List<ScheduleEntry> =
        database.scheduleDao().getScheduleForUser(userId).first()

    suspend fun insertScheduleEntry(entry: ScheduleEntry) =
        database.scheduleDao().insertScheduleEntry(entry)

    suspend fun updateMedicationStatus(entryId: String, status: String, takenAt: String?) =
        database.scheduleDao().updateMedicationStatus(entryId, status, takenAt)
}