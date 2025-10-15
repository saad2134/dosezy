package com.example.dosezy.data.repository

import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.Medicine
import kotlinx.coroutines.flow.Flow

class MedicineRepository(private val database: DosezyDatabase) {
    fun getMedicinesByUser(userId: String): Flow<List<Medicine>> =
        database.medicineDao().getMedicinesByUser(userId)

    suspend fun insertMedicine(medicine: Medicine) = database.medicineDao().insertMedicine(medicine)

    suspend fun updateMedicine(medicine: Medicine) = database.medicineDao().updateMedicine(medicine)

    suspend fun deleteMedicine(medicine: Medicine) = database.medicineDao().deleteMedicine(medicine)
}