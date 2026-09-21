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

    // Get ALL schedule entries for a user (ordered chronologically descending)
    @Query("SELECT * FROM schedule_entries WHERE userId = :userId ORDER BY scheduledDateTime DESC")
    fun getScheduleForUser(userId: String): Flow<List<ScheduleEntry>>

    @Query("SELECT * FROM schedule_entries WHERE userId = :userId AND scheduledDateTime BETWEEN :startDate AND :endDate ORDER BY scheduledDateTime ASC")
    fun getScheduleForDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<ScheduleEntry>>

    @Query("SELECT * FROM schedule_entries WHERE userId = :userId AND scheduledDateTime BETWEEN :startDate AND :endDate ORDER BY scheduledDateTime ASC")
    suspend fun getScheduleForDateRangeDirect(userId: String, startDate: Long, endDate: Long): List<ScheduleEntry>

    @Transaction
    @Query("SELECT * FROM schedule_entries WHERE userId = :userId AND scheduledDateTime BETWEEN :startDate AND :endDate ORDER BY scheduledDateTime ASC")
    fun getScheduleWithMedicineForDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<ScheduleWithMedicine>>

    @Query("SELECT * FROM schedule_entries WHERE entryId = :entryId")
    suspend fun getScheduleEntryById(entryId: String): ScheduleEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleEntry(entry: ScheduleEntry)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertScheduleEntries(entries: List<ScheduleEntry>)

    @Update
    suspend fun updateScheduleEntry(entry: ScheduleEntry)

    @Query("UPDATE schedule_entries SET status = :status, takenAt = :takenAt WHERE entryId = :entryId")
    suspend fun updateMedicationStatus(entryId: String, status: String, takenAt: Long?)

    @Query("UPDATE schedule_entries SET status = :status, takenAt = :takenAt, skipReason = :skipReason WHERE entryId = :entryId")
    suspend fun updateMedicationStatusWithReason(entryId: String, status: String, takenAt: Long?, skipReason: String?)

    @Query("DELETE FROM schedule_entries WHERE userId = :userId")
    suspend fun deleteScheduleByUser(userId: String)

    // Get ALL schedule entries with medicine for a user (ordered chronologically descending)
    @Transaction
    @Query("SELECT * FROM schedule_entries WHERE userId = :userId ORDER BY scheduledDateTime DESC")
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

    // Delete only future pending schedule entries for a specific medicine
    @Query("DELETE FROM schedule_entries WHERE medicineId = :medicineId AND status = 'PENDING' AND scheduledDateTime >= :fromEpochMillis")
    suspend fun deleteFuturePendingScheduleEntries(medicineId: String, fromEpochMillis: Long)

    // Delete all untaken (pending or untaken missed) schedule entries for a specific medicine from a timestamp onwards
    @Query("DELETE FROM schedule_entries WHERE medicineId = :medicineId AND (status = 'PENDING' OR (status = 'MISSED' AND takenAt IS NULL)) AND scheduledDateTime >= :fromEpochMillis")
    suspend fun deleteUntakenScheduleEntriesFrom(medicineId: String, fromEpochMillis: Long)

    // Query only pending entries that have passed the cutoff time to avoid scanning all history
    @Query("SELECT * FROM schedule_entries WHERE userId = :userId AND status = 'PENDING' AND scheduledDateTime <= :cutoffMillis")
    suspend fun getPendingEntriesBefore(userId: String, cutoffMillis: Long): List<ScheduleEntry>
}