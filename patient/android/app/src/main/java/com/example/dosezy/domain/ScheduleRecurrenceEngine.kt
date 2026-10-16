/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.domain

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Pure domain engine responsible for computing recurring medication dose schedules,
 * interval timelines across midnight boundaries, and frequency evaluations.
 */
object ScheduleRecurrenceEngine {

    /**
     * Generates schedule entries for a medicine across a given date range.
     *
     * @param medicine The target medicine entity.
     * @param startDateRange The start date for generating schedule entries.
     * @param days Number of days to generate entries for (default: 30 days).
     * @return List of computed [ScheduleEntry] objects.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun generateScheduleEntries(
        medicine: Medicine,
        startDateRange: LocalDate,
        days: Int = 30
    ): List<ScheduleEntry> {
        // As-needed (PRN) and archived medications do not generate automated scheduled reminder slots
        if (medicine.frequency.pattern == FrequencyPattern.AS_NEEDED || medicine.isArchived) {
            return emptyList()
        }

        val entries = mutableListOf<ScheduleEntry>()
        val effectiveStart = if (medicine.startDate != null && medicine.startDate.isAfter(startDateRange)) {
            medicine.startDate
        } else {
            startDateRange
        }
        val effectiveEnd = if (medicine.endDate != null && medicine.endDate.isBefore(startDateRange.plusDays(days.toLong()))) {
            medicine.endDate
        } else {
            startDateRange.plusDays(days.toLong())
        }

        if (effectiveStart.isAfter(effectiveEnd)) return emptyList()

        // Guard: Generate EVERY_X_HOURS along a continuous timeline across midnight boundaries rather than day-by-day truncation to prevent missing nocturnal doses and schedule drift
        if (medicine.frequency.pattern == FrequencyPattern.EVERY_X_HOURS) {
            val interval = (medicine.frequency.intervalHours ?: 4).coerceIn(1, 23)
            val startTime = (medicine.scheduledTimes.firstOrNull() ?: LocalTime.of(8, 0)).withSecond(0).withNano(0)
            // Guard: Anchor to stable epoch if startDate is null to allow timeline to step across midnight boundaries and preserve nocturnal doses
            val baseDate = medicine.startDate ?: LocalDate.of(2020, 1, 1)
            val baseAnchor = LocalDateTime.of(baseDate, startTime)
            val now = LocalDateTime.now()

            var cursor = baseAnchor
            if (cursor.isBefore(effectiveStart.atStartOfDay())) {
                val hoursDiff = java.time.Duration.between(cursor, effectiveStart.atStartOfDay()).toHours()
                val steps = (hoursDiff / interval).coerceAtLeast(0)
                cursor = cursor.plusHours(steps * interval)
                while (cursor.isBefore(effectiveStart.atStartOfDay())) {
                    cursor = cursor.plusHours(interval.toLong())
                }
            }

            val endDateTime = effectiveEnd.atTime(LocalTime.MAX)
            while (!cursor.isAfter(endDateTime)) {
                if (medicine.endDate != null && cursor.toLocalDate().isAfter(medicine.endDate)) break
                val itemDate = cursor.toLocalDate()
                val cleanTime = cursor.toLocalTime().withSecond(0).withNano(0)
                val scheduledDateTime = cursor.withSecond(0).withNano(0)

                // Skip past reminder times on the current day to avoid immediate missed status
                if (!(itemDate.isEqual(now.toLocalDate()) && scheduledDateTime.isBefore(now.minusMinutes(15)))) {
                    val entryId = "${medicine.medicineId}_${itemDate}_${cleanTime}".replace(":", "_").replace("-", "_")
                    val entry = ScheduleEntry(
                        entryId = entryId,
                        userId = medicine.userId,
                        medicineId = medicine.medicineId,
                        scheduledDateTime = scheduledDateTime,
                        status = MedicationStatus.PENDING,
                        dosage = medicine.getDosageForTime(cleanTime)
                    )
                    entries.add(entry)
                }
                cursor = cursor.plusHours(interval.toLong())
            }
            return entries
        }

        var currentDate = effectiveStart
        while (currentDate.isBefore(effectiveEnd) || currentDate.isEqual(effectiveEnd)) {
            // Check if medicine should be taken on this day based on frequency
            if (shouldTakeOnDate(medicine, currentDate)) {
                val now = LocalDateTime.now()
                val effectiveTimes = medicine.scheduledTimes

                effectiveTimes.forEach { time ->
                    val cleanTime = time.withSecond(0).withNano(0)
                    val scheduledDateTime = LocalDateTime.of(currentDate, cleanTime)

                    // Skip past reminder times on the current day to avoid immediate missed status
                    if (currentDate.isEqual(now.toLocalDate()) && scheduledDateTime.isBefore(now.minusMinutes(15))) {
                        return@forEach
                    }

                    val entryId = "${medicine.medicineId}_${currentDate}_${cleanTime}".replace(":", "_").replace("-", "_")
                    val entry = ScheduleEntry(
                        entryId = entryId,
                        userId = medicine.userId,
                        medicineId = medicine.medicineId,
                        scheduledDateTime = scheduledDateTime,
                        status = MedicationStatus.PENDING,
                        dosage = medicine.getDosageForTime(cleanTime)
                    )
                    entries.add(entry)
                }
            }
            currentDate = currentDate.plusDays(1)
        }

        return entries
    }

    /**
     * Determines if the medicine should be taken on the given date based on its frequency pattern.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun shouldTakeOnDate(medicine: Medicine, date: LocalDate): Boolean {
        // Verify within finite course bounds
        if (medicine.startDate != null && date.isBefore(medicine.startDate)) return false
        if (medicine.endDate != null && date.isAfter(medicine.endDate)) return false

        return when (medicine.frequency.pattern) {
            FrequencyPattern.DAILY -> true
            FrequencyPattern.AS_NEEDED -> false
            FrequencyPattern.EVERY_X_HOURS -> true
            FrequencyPattern.EVERY_X_DAYS -> {
                val interval = (medicine.frequency.intervalDays ?: 2).coerceAtLeast(1)
                val baseDate = medicine.startDate ?: LocalDate.of(2020, 1, 1)
                val daysDiff = ChronoUnit.DAYS.between(baseDate, date)
                // Guard: Use Math.floorMod so negative day differences symmetrically follow the interval pattern
                Math.floorMod(daysDiff, interval.toLong()) == 0L
            }
            FrequencyPattern.WEEKLY -> {
                val selectedDays = medicine.frequency.selectedDaysOfWeek
                if (!selectedDays.isNullOrEmpty()) {
                    // Use explicitly selected days (1=Mon ... 7=Sun, ISO standard)
                    date.dayOfWeek.value in selectedDays
                } else {
                    // Fallback: treat daysPerWeek as "first N days of the week"
                    val daysPerWeek = medicine.frequency.daysPerWeek ?: 7
                    date.dayOfWeek.value <= daysPerWeek
                }
            }
            FrequencyPattern.MONTHLY -> {
                val selectedDays = medicine.frequency.selectedDaysOfMonth
                if (!selectedDays.isNullOrEmpty()) {
                    // Use explicitly selected days of month
                    val maxDayInMonth = date.lengthOfMonth()
                    selectedDays.any { targetDay ->
                        val effectiveDay = targetDay.coerceAtMost(maxDayInMonth)
                        date.dayOfMonth == effectiveDay
                    }
                } else {
                    // Fallback: treat daysPerMonth as "first N days of the month"
                    val daysPerMonth = medicine.frequency.daysPerMonth ?: 30
                    date.dayOfMonth <= daysPerMonth
                }
            }
            FrequencyPattern.CUSTOM -> {
                val intervalWks = (medicine.frequency.intervalWeeks ?: 1).coerceAtLeast(1)
                val baseDate = medicine.startDate ?: LocalDate.of(2020, 1, 6) // Jan 6, 2020 was Monday
                val baseMonday = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val dateMonday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val weeksDiff = ChronoUnit.WEEKS.between(baseMonday, dateMonday)
                // Guard: Use Math.floorMod so negative week offsets symmetrically follow the multi-week interval cycle
                val isIntervalWeek = Math.floorMod(weeksDiff, intervalWks.toLong()) == 0L

                val selectedDays = medicine.frequency.selectedDaysOfWeek
                val isDayMatch = if (!selectedDays.isNullOrEmpty()) {
                    date.dayOfWeek.value in selectedDays
                } else {
                    if (medicine.frequency.intervalWeeks != null && medicine.frequency.intervalWeeks > 1) {
                        date.dayOfWeek == baseDate.dayOfWeek
                    } else {
                        true
                    }
                }
                isIntervalWeek && isDayMatch
            }
        }
    }
}
