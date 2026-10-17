/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.domain.usecases

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.domain.ScheduleRecurrenceEngine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Use case coordinating schedule generation and auto-extension for medicines.
 * Delegates recurrence calculation to ScheduleRecurrenceEngine and persists generated entries.
 */
@RequiresApi(Build.VERSION_CODES.O)
class GenerateSchedulesUseCase @Inject constructor(
    private val database: DosezyDatabase
) {
    companion object {
        private const val TAG = "GenerateSchedulesUseCase"
    }

    suspend fun autoExtendSchedules(userId: String) {
        try {
            // Guard: Use getActiveMedicinesByUserDirect to avoid Flow InvalidationTracker observer overhead in autoExtendSchedules
            val medicines = database.medicineDao().getActiveMedicinesByUserDirect(userId)
            medicines.forEach { medicine ->
                // Guard: Skip expired finite medications whose endDate is in the past to prevent redundant SQLite queries and recalculations
                if (medicine.endDate != null && medicine.endDate.isBefore(LocalDate.now())) {
                    return@forEach
                }

                if (!medicine.isArchived) {
                    val scheduleEntries = database.scheduleDao().getScheduleEntriesByMedicine(medicine.medicineId)
                    val latestEntry = scheduleEntries.maxByOrNull { it.scheduledDateTime }

                    // Guard: Skip if schedule already reached the medication finite endDate to prevent repeated recurrence scans
                    if (medicine.endDate != null && latestEntry != null && !latestEntry.scheduledDateTime.toLocalDate().isBefore(medicine.endDate)) {
                        return@forEach
                    }

                    if (latestEntry == null || latestEntry.scheduledDateTime.isBefore(LocalDateTime.now().plusDays(15))) {
                        val today = LocalDate.now()
                        val latestDate = latestEntry?.scheduledDateTime?.toLocalDate()
                        // Guard: Anchor to latestDate rather than plusDays(1) and filter by latestEntry.scheduledDateTime to prevent dropping evening or nocturnal doses
                        val startGenerateFrom = if (latestDate == null || latestDate.isBefore(today)) today else latestDate
                        val newEntries = ScheduleRecurrenceEngine.generateScheduleEntries(medicine, startGenerateFrom, 30)
                            .filter { latestEntry == null || it.scheduledDateTime.isAfter(latestEntry.scheduledDateTime) }
                        if (newEntries.isNotEmpty()) {
                            database.scheduleDao().insertScheduleEntries(newEntries)
                            Log.d(TAG, "Auto-extended schedule for medicine: ${medicine.medicationName} by 30 days starting from $startGenerateFrom")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error auto-extending schedules for user: $userId", e)
        }
    }

    suspend fun generateInitialSchedules(medicine: Medicine, daysCount: Int = 30): List<ScheduleEntry> {
        val today = LocalDate.now()
        // Guard: Anchor initial schedule generation to today if startDate is backdated to prevent generating stale historical PENDING entries
        val start = if (medicine.startDate != null && medicine.startDate.isBefore(today)) today else (medicine.startDate ?: today)
        val entries = ScheduleRecurrenceEngine.generateScheduleEntries(medicine, start, daysCount)
        if (entries.isNotEmpty()) {
            database.scheduleDao().insertScheduleEntries(entries)
        }
        return entries
    }
}
