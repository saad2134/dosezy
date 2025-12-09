package com.example.dosezy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.ScheduleWithMedicine
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    // Get ALL schedule entries for a user (manual filter)
    @Query("SELECT * FROM schedule_entries WHERE userId = :userId")
    fun getScheduleForUser(userId: String): Flow<List<ScheduleEntry>>

    @Query("SELECT * FROM schedule_entries WHERE userId = :userId AND scheduledDateTime BETWEEN :startDate AND :endDate")
    fun getScheduleForDateRange(userId: String, startDate: String, endDate: String): Flow<List<ScheduleEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleEntry(entry: ScheduleEntry)

    @Update
    suspend fun updateScheduleEntry(entry: ScheduleEntry)

    @Query("UPDATE schedule_entries SET status = :status, takenAt = :takenAt WHERE entryId = :entryId")
    suspend fun updateMedicationStatus(entryId: String, status: String, takenAt: String?)

    @Query("DELETE FROM schedule_entries WHERE userId = :userId")
    suspend fun deleteScheduleByUser(userId: String)

    // Get ALL schedule entries with medicine for a user (manual filter)
    @Transaction
    @Query("SELECT * FROM schedule_entries WHERE userId = :userId")
    fun getScheduleWithMedicineForUser(userId: String): Flow<List<ScheduleWithMedicine>>

    // ADD THIS METHOD: Get schedule entries by medicine ID
    @Query("SELECT * FROM schedule_entries WHERE medicineId = :medicineId")
    suspend fun getScheduleEntriesByMedicine(medicineId: String): List<ScheduleEntry>

    // ADD THIS METHOD: Delete a specific schedule entry by ID
    @Query("DELETE FROM schedule_entries WHERE entryId = :entryId")
    suspend fun deleteScheduleEntry(entryId: String)

    // ADD THIS METHOD: Bulk delete all schedule entries for a medicine
    @Query("DELETE FROM schedule_entries WHERE medicineId = :medicineId")
    suspend fun deleteScheduleEntriesByMedicine(medicineId: String)

    // ADD THIS FOR DEBUGGING: Get all schedule entries
    @Query("SELECT * FROM schedule_entries WHERE userId = :userId")
    suspend fun getAllScheduleEntries(userId: String): List<ScheduleEntry>
}