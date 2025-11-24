// MedicineDao.kt
package com.example.dosezy.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dosezy.data.model.Medicine
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines WHERE userId = :userId")
    fun getMedicinesByUser(userId: String): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE medicineId = :medicineId")
    fun getMedicineById(medicineId: String): Flow<Medicine?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: Medicine)

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    @Query("DELETE FROM medicines WHERE userId = :userId")
    suspend fun deleteMedicinesByUser(userId: String)

    // ADD THIS METHOD:
    @Query("DELETE FROM medicines WHERE medicineId = :medicineId")
    suspend fun deleteMedicineById(medicineId: String)
}