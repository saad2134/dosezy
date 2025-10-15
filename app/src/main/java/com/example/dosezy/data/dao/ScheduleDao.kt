package com.example.dosezy.data.dao

import androidx.room.*
import com.example.dosezy.data.model.ScheduleEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_entries WHERE userId = :userId AND date(scheduledDateTime) = :date")
    fun getScheduleForDate(userId: String, date: LocalDate): Flow<List<ScheduleEntry>>

    @Query("SELECT * FROM schedule_entries WHERE userId = :userId AND scheduledDateTime BETWEEN :startDate AND :endDate")
    fun getScheduleForDateRange(userId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<ScheduleEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleEntry(entry: ScheduleEntry)

    @Update
    suspend fun updateScheduleEntry(entry: ScheduleEntry)

    @Query("UPDATE schedule_entries SET status = :status, takenAt = :takenAt WHERE entryId = :entryId")
    suspend fun updateMedicationStatus(entryId: String, status: String, takenAt: String?)

    @Query("DELETE FROM schedule_entries WHERE userId = :userId")
    suspend fun deleteScheduleByUser(userId: String)
}