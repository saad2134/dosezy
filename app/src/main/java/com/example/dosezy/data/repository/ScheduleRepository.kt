package com.example.dosezy.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.ScheduleWithMedicine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

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

    fun getScheduleForDateRange(userId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<ScheduleEntry>> =
        database.scheduleDao().getScheduleForDateRange(userId, startDate.toString(), endDate.toString())

    // Add this method to get schedules as list (not Flow) - FIXED NAME
    suspend fun getSchedulesByUserSync(userId: String): List<ScheduleEntry> =
        database.scheduleDao().getScheduleForUser(userId).first()

    suspend fun insertScheduleEntry(entry: ScheduleEntry) =
        database.scheduleDao().insertScheduleEntry(entry)

    suspend fun updateMedicationStatus(entryId: String, status: String, takenAt: String?) =
        database.scheduleDao().updateMedicationStatus(entryId, status, takenAt)
}