package com.example.dosezy.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.Medicine
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class MedicineRepository(private val database: DosezyDatabase) {

    companion object {
        private const val TAG = "MedicineRepository"
    }

    fun getMedicinesByUser(userId: String): Flow<List<Medicine>> =
        database.medicineDao().getMedicinesByUser(userId)

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun insertMedicine(medicine: Medicine) {
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

        // Debug: Check what's in the database
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
}