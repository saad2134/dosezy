package com.example.dosezy.data.repository

import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.ScheduleEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class ScheduleRepository(private val database: DosezyDatabase) {
    fun getScheduleForDate(userId: String, date: LocalDate): Flow<List<ScheduleEntry>> =
        database.scheduleDao().getScheduleForDate(userId, date)

    suspend fun insertScheduleEntry(entry: ScheduleEntry) =
        database.scheduleDao().insertScheduleEntry(entry)

    suspend fun updateMedicationStatus(entryId: String, status: String, takenAt: String?) =
        database.scheduleDao().updateMedicationStatus(entryId, status, takenAt)
}