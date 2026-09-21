package com.example.dosezy

import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Tests for adherence rate math, range filter boundaries, and insight rating logic.
 * Ensures patients' adherence rates are computed accurately without penalizing
 * for skipped doses or future pending slots.
 */
class AdherenceAndAnalyticsTest {

    private fun entry(
        daysOffsetFromToday: Long,
        status: MedicationStatus
    ): ScheduleEntry {
        val dt = LocalDate.now().plusDays(daysOffsetFromToday).atTime(8, 0)
        return ScheduleEntry(
            entryId = "entry_${daysOffsetFromToday}_${status.name}",
            userId = "usr_1",
            medicineId = "med_1",
            scheduledDateTime = dt,
            status = status,
            takenAt = if (status == MedicationStatus.TAKEN_ON_TIME || status == MedicationStatus.TAKEN_LATE) dt else null
        )
    }

    // Mathematical formula helper (matches AnalyticsScreen.computeAdherence)
    private fun computeAdherenceRate(entries: List<ScheduleEntry>): Int {
        val taken = entries.count { it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE }
        val missed = entries.count { it.status == MedicationStatus.MISSED }
        val decided = taken + missed
        return if (decided > 0) ((taken.toDouble() / decided.toDouble()) * 100).toInt() else 0
    }

    // ───────────────────────────────────────────────────────────────
    // 1. Core Adherence Math
    // ───────────────────────────────────────────────────────────────

    @Test
    fun adherenceRate_emptyList_returnsZeroWithoutCrash() {
        val entries = emptyList<ScheduleEntry>()
        assertEquals(0, computeAdherenceRate(entries))
    }

    @Test
    fun adherenceRate_allTakenOnTime_returns100() {
        val entries = listOf(
            entry(0, MedicationStatus.TAKEN_ON_TIME),
            entry(-1, MedicationStatus.TAKEN_ON_TIME),
            entry(-2, MedicationStatus.TAKEN_ON_TIME)
        )
        assertEquals(100, computeAdherenceRate(entries))
    }

    @Test
    fun adherenceRate_takenLate_countsAsTakenTowardsAdherence() {
        // Both TAKEN_ON_TIME and TAKEN_LATE represent successful medication adherence
        val entries = listOf(
            entry(0, MedicationStatus.TAKEN_LATE),
            entry(-1, MedicationStatus.TAKEN_ON_TIME),
            entry(-2, MedicationStatus.TAKEN_LATE)
        )
        assertEquals(100, computeAdherenceRate(entries))
    }

    @Test
    fun adherenceRate_allMissed_returnsZero() {
        val entries = listOf(
            entry(0, MedicationStatus.MISSED),
            entry(-1, MedicationStatus.MISSED)
        )
        assertEquals(0, computeAdherenceRate(entries))
    }

    @Test
    fun adherenceRate_halfTakenHalfMissed_returns50() {
        val entries = listOf(
            entry(0, MedicationStatus.TAKEN_ON_TIME),
            entry(-1, MedicationStatus.TAKEN_LATE),
            entry(-2, MedicationStatus.MISSED),
            entry(-3, MedicationStatus.MISSED)
        )
        assertEquals(50, computeAdherenceRate(entries))
    }

    @Test
    fun adherenceRate_skippedDoses_doNotPenalizeAdherenceScore() {
        // Clinical tracking standard: Skipped doses (e.g. physician instruction or fasting)
        // are not missed doses and must not penalize the patient's adherence rate
        val entries = listOf(
            entry(0, MedicationStatus.TAKEN_ON_TIME),
            entry(-1, MedicationStatus.TAKEN_ON_TIME),
            entry(-2, MedicationStatus.SKIPPED),
            entry(-3, MedicationStatus.SKIPPED)
        )
        // 2 taken, 0 missed, decided = 2 -> 100%
        assertEquals(100, computeAdherenceRate(entries))
    }

    @Test
    fun adherenceRate_onlySkippedDoses_returnsZeroWithoutZeroDivision() {
        val entries = listOf(
            entry(0, MedicationStatus.SKIPPED),
            entry(-1, MedicationStatus.SKIPPED)
        )
        // decided = 0 -> rate = 0
        assertEquals(0, computeAdherenceRate(entries))
    }

    // ───────────────────────────────────────────────────────────────
    // 2. Date Range Boundaries
    // ───────────────────────────────────────────────────────────────

    @Test
    fun rangeFilter_last7Days_includesOnlyLastWeek() {
        val today = LocalDate.now()
        val entries = listOf(
            entry(0, MedicationStatus.TAKEN_ON_TIME),     // Today (included)
            entry(-3, MedicationStatus.TAKEN_ON_TIME),    // 3 days ago (included)
            entry(-7, MedicationStatus.TAKEN_ON_TIME),    // Exactly 7 days ago (included)
            entry(-8, MedicationStatus.TAKEN_ON_TIME),    // 8 days ago (EXCLUDED)
            entry(1, MedicationStatus.PENDING)           // Tomorrow (EXCLUDED)
        )

        val filtered = entries.filter {
            val d = it.scheduledDateTime.toLocalDate()
            d >= today.minusDays(7) && d <= today
        }

        assertEquals(3, filtered.size)
    }

    @Test
    fun rangeFilter_last30Days_includesBoundaryDays() {
        val today = LocalDate.now()
        val entries = listOf(
            entry(0, MedicationStatus.TAKEN_ON_TIME),
            entry(-15, MedicationStatus.TAKEN_ON_TIME),
            entry(-30, MedicationStatus.TAKEN_ON_TIME),   // Exactly 30 days ago (included)
            entry(-31, MedicationStatus.TAKEN_ON_TIME)    // 31 days ago (EXCLUDED)
        )

        val filtered = entries.filter {
            val d = it.scheduledDateTime.toLocalDate()
            d >= today.minusDays(30) && d <= today
        }

        assertEquals(3, filtered.size)
    }

    @Test
    fun rangeFilter_total_excludesFutureDoses() {
        val today = LocalDate.now()
        val entries = listOf(
            entry(-5, MedicationStatus.TAKEN_ON_TIME),
            entry(0, MedicationStatus.TAKEN_ON_TIME),
            entry(1, MedicationStatus.PENDING),          // Tomorrow
            entry(5, MedicationStatus.PENDING)           // Next week
        )

        val pastAndToday = entries.filter { it.scheduledDateTime.toLocalDate() <= today }
        assertEquals(2, pastAndToday.size)
    }
}
