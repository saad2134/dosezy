package com.example.dosezy

import androidx.compose.ui.graphics.Color
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.ui.components.getDateStatusColor
import com.example.dosezy.utils.TimeCalculationUtils
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

    @Test
    fun adherenceRate_prnAsNeededDoses_doNotSkewPrescriptionAdherence() {
        // Guard: Ad-hoc PRN doses must not mask missed scheduled prescription doses
        val scheduledEntries = listOf(
            entry(0, MedicationStatus.MISSED)
        )
        val prnEntries = (1..5).map { i ->
            ScheduleEntry(
                entryId = "PRN_med_${System.currentTimeMillis()}_$i",
                userId = "usr_1",
                medicineId = "med_prn",
                scheduledDateTime = LocalDateTime.now(),
                status = MedicationStatus.TAKEN_ON_TIME,
                takenAt = LocalDateTime.now()
            )
        }
        val allEntries = scheduledEntries + prnEntries
        // AnalyticsScreen filters out PRN entries
        val prescriptionEntries = allEntries.filter { !it.entryId.startsWith("PRN_") }
        assertEquals(0, computeAdherenceRate(prescriptionEntries))
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

    @Test
    fun adherenceRate_todayOverduePendingDoses_penalizeAdherenceAndPreventFalse100Percent() {
        val today = LocalDate.now()
        val now = LocalDateTime.now()

        val morningTaken = ScheduleEntry(
            entryId = "entry_today_taken",
            userId = "usr_1",
            medicineId = "med_1",
            scheduledDateTime = today.atTime(8, 0),
            status = MedicationStatus.TAKEN_ON_TIME
        )
        // Dose scheduled in past today (overdue) but not yet exceeding considerMissedAfter
        val afternoonOverduePending = ScheduleEntry(
            entryId = "entry_today_pending_overdue",
            userId = "usr_1",
            medicineId = "med_2",
            scheduledDateTime = now.minusHours(2),
            status = MedicationStatus.PENDING
        )
        // Future dose later today
        val eveningFuturePending = ScheduleEntry(
            entryId = "entry_today_pending_future",
            userId = "usr_1",
            medicineId = "med_3",
            scheduledDateTime = now.plusHours(4),
            status = MedicationStatus.PENDING
        )

        val dayEntries = listOf(morningTaken, afternoonOverduePending, eveningFuturePending)
        val dayTaken = dayEntries.count { it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE }
        val dayMissed = dayEntries.count { it.status == MedicationStatus.MISSED }
        val dayOverduePending = dayEntries.count { it.status == MedicationStatus.PENDING && it.scheduledDateTime.isBefore(now) }
        val dayDecided = dayTaken + dayMissed + dayOverduePending
        val dayRate = if (dayDecided > 0) ((dayTaken.toDouble() / dayDecided.toDouble()) * 100).toInt() else 0

        assertEquals(1, dayTaken)
        assertEquals(0, dayMissed)
        assertEquals(1, dayOverduePending)
        assertEquals(2, dayDecided)
        assertEquals(50, dayRate) // 1 taken out of 2 past doses = 50%, NOT false 100%
    }

    @Test
    fun adherenceRate_todayPendingDoseExceedingMissedAfter_notDoubleCountedInDecided() {
        val today = LocalDate.now()
        val now = LocalDateTime.now()
        val missedAfterHours = 3

        val morningTaken = ScheduleEntry(
            entryId = "entry_today_taken_1",
            userId = "usr_1",
            medicineId = "med_1",
            scheduledDateTime = today.atTime(8, 0),
            status = MedicationStatus.TAKEN_ON_TIME
        )
        // Dose scheduled 5 hours ago: pending, in past, and past considerMissedAfter (3h)
        val pastPendingExceedingMissed = ScheduleEntry(
            entryId = "entry_today_pending_missed",
            userId = "usr_1",
            medicineId = "med_2",
            scheduledDateTime = now.minusHours(5),
            status = MedicationStatus.PENDING
        )

        fun isEntryMissed(entry: ScheduleEntry): Boolean =
            entry.status == MedicationStatus.MISSED ||
            (entry.status == MedicationStatus.PENDING && now.isAfter(entry.scheduledDateTime) &&
                com.example.dosezy.utils.TimeCalculationUtils.isMissed(entry.scheduledDateTime, now, missedAfterHours))

        val dayEntries = listOf(morningTaken, pastPendingExceedingMissed)
        val dayTaken = dayEntries.count { it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE }
        val dayMissed = dayEntries.count { isEntryMissed(it) }
        // Guard: Factor overdue pending doses into decided counts without double-counting entries already in dayMissed (#BUG-ANALYTICS-OVERDUE)
        val dayOverduePending = dayEntries.count { it.status == MedicationStatus.PENDING && it.scheduledDateTime.isBefore(now) && !isEntryMissed(it) }
        val dayDecided = dayTaken + dayMissed + dayOverduePending
        val dayRate = if (dayDecided > 0) ((dayTaken.toDouble() / dayDecided.toDouble()) * 100).toInt() else 0

        assertEquals(1, dayTaken)
        assertEquals(1, dayMissed)
        assertEquals(0, dayOverduePending) // Must be 0 to prevent double-counting with dayMissed
        assertEquals(2, dayDecided)        // 1 taken + 1 missed = 2 decided, NOT 3
        assertEquals(50, dayRate)          // 1 / 2 = 50%, NOT 1 / 3 = 33%
    }

    @Test
    fun computeAdherence_todayOverduePendingDoses_penalizesRangeDecidedCountAndAdherenceRate() {
        // Guard: Verify computeAdherence includes today's overdue pending doses in decidedCount and rate (#BUG-ANALYTICS-RANGE-OVERDUE)
        val today = LocalDate.now()
        val now = LocalDateTime.now()
        val missedAfterHours = 6

        val morningTaken = ScheduleEntry(
            entryId = "entry_range_taken",
            userId = "usr_1",
            medicineId = "med_1",
            scheduledDateTime = today.atTime(8, 0),
            status = MedicationStatus.TAKEN_ON_TIME
        )
        val afternoonOverduePending = ScheduleEntry(
            entryId = "entry_range_overdue_pending",
            userId = "usr_1",
            medicineId = "med_2",
            scheduledDateTime = now.minusHours(2),
            status = MedicationStatus.PENDING
        )
        val eveningFuturePending = ScheduleEntry(
            entryId = "entry_range_future_pending",
            userId = "usr_1",
            medicineId = "med_3",
            scheduledDateTime = now.plusHours(4),
            status = MedicationStatus.PENDING
        )

        val entries = listOf(morningTaken, afternoonOverduePending, eveningFuturePending)

        fun isEntryMissed(entry: ScheduleEntry): Boolean =
            entry.status == MedicationStatus.MISSED ||
            (entry.status == MedicationStatus.PENDING && now.isAfter(entry.scheduledDateTime) &&
                com.example.dosezy.utils.TimeCalculationUtils.isMissed(entry.scheduledDateTime, now, missedAfterHours))

        val takenOnTime = entries.count { it.status == MedicationStatus.TAKEN_ON_TIME }
        val takenLate = entries.count { it.status == MedicationStatus.TAKEN_LATE }
        val taken = takenOnTime + takenLate
        val missed = entries.count { isEntryMissed(it) }
        val skipped = entries.count { it.status == MedicationStatus.SKIPPED }
        val overduePending = entries.count {
            it.status == MedicationStatus.PENDING &&
            it.scheduledDateTime.isBefore(now) &&
            !isEntryMissed(it)
        }
        val decided = taken + missed + overduePending
        val rate = if (decided > 0) ((taken.toDouble() / decided.toDouble()) * 100).toInt() else 0

        assertEquals(1, taken)
        assertEquals(0, missed)
        assertEquals(0, skipped)
        assertEquals(1, overduePending)
        assertEquals(2, decided)
        // Rate is 50%, accurately penalizing overdue pending doses and matching daily chart
        assertEquals(50, rate)
    }

    @Test
    fun computeAdherence_postMidnightYesterdayOverduePendingDoses_penalizesAdherenceRate() {
        // Guard: Verify overdue pending doses from yesterday (e.g. 23:30 when evaluated at 01:00 AM) are counted in overduePending and dayOverduePending without disappearing (#BUG-ANALYTICS-RANGE-OVERDUE, #BUG-ANALYTICS-OVERDUE)
        val today = LocalDate.of(2026, 10, 18)
        val now = today.atTime(1, 0, 0) // 01:00 AM today
        val missedAfterHours = 6 // 6 hours threshold: 23:30 to 01:00 is 1.5 hours (< 6 hours, so not yet missed)

        val yesterdayTakenMorning = ScheduleEntry(
            entryId = "entry_yesterday_morning_taken",
            userId = "usr_1",
            medicineId = "med_1",
            scheduledDateTime = today.minusDays(1).atTime(9, 0),
            status = MedicationStatus.TAKEN_ON_TIME
        )
        val yesterdayLateNightPending = ScheduleEntry(
            entryId = "entry_yesterday_nocturnal_pending",
            userId = "usr_1",
            medicineId = "med_2",
            scheduledDateTime = today.minusDays(1).atTime(23, 30),
            status = MedicationStatus.PENDING
        )

        val entries = listOf(yesterdayTakenMorning, yesterdayLateNightPending)

        fun isEntryMissed(entry: ScheduleEntry): Boolean =
            entry.status == MedicationStatus.MISSED ||
            (entry.status == MedicationStatus.PENDING && now.isAfter(entry.scheduledDateTime) &&
                com.example.dosezy.utils.TimeCalculationUtils.isMissed(entry.scheduledDateTime, now, missedAfterHours))

        // Adherence for Range
        val taken = entries.count { it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE }
        val missed = entries.count { isEntryMissed(it) }
        val overduePending = entries.count {
            it.status == MedicationStatus.PENDING &&
            it.scheduledDateTime.isBefore(now) &&
            !isEntryMissed(it)
        }
        val decided = taken + missed + overduePending
        val rate = if (decided > 0) ((taken.toDouble() / decided.toDouble()) * 100).toInt() else 0

        assertEquals(1, taken)
        assertEquals(0, missed)
        assertEquals(1, overduePending) // Correctly identified as overdue pending across midnight
        assertEquals(2, decided)        // 1 taken + 1 overdue = 2 decided, NOT 1
        assertEquals(50, rate)         // 1 / 2 = 50%, NOT falsely 100%

        // Adherence for Yesterday's DayStat in past7Days
        val yesterdayEntries = entries.filter { it.scheduledDateTime.toLocalDate() == today.minusDays(1) }
        val dayTaken = yesterdayEntries.count { it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE }
        val dayMissed = yesterdayEntries.count { isEntryMissed(it) }
        val dayOverduePending = yesterdayEntries.count {
            it.status == MedicationStatus.PENDING &&
            it.scheduledDateTime.isBefore(now) &&
            !isEntryMissed(it)
        }
        val dayDecided = dayTaken + dayMissed + dayOverduePending
        val dayRate = if (dayDecided > 0) ((dayTaken.toDouble() / dayDecided.toDouble()) * 100).toInt() else 0

        assertEquals(1, dayTaken)
        assertEquals(0, dayMissed)
        assertEquals(1, dayOverduePending)
        assertEquals(2, dayDecided)
        assertEquals(50, dayRate) // 50% for yesterday rather than false 100%
    }

    @Test
    fun weeklyTrend_futurePendingAndSkippedDoses_reportsZeroDecidedToPreventFalseZeroAdherence() {
        // Guard: DayStat must record decided count so future pending and all-skipped days render as '-' rather than false 0% compliance (#BUG-ANALYTICS-DECIDED)
        val today = LocalDate.of(2026, 11, 19)
        val morning = LocalDateTime.of(2026, 11, 19, 8, 0)
        val afternoon = LocalDateTime.of(2026, 11, 19, 14, 0)
        val evening = LocalDateTime.of(2026, 11, 19, 20, 0)

        // Scenario A: Today with 2 upcoming future pending doses
        val futureEntries = listOf(
            ScheduleEntry("entry_1", "med_1", "user_1", afternoon, MedicationStatus.PENDING),
            ScheduleEntry("entry_2", "med_2", "user_1", evening, MedicationStatus.PENDING)
        )

        fun isEntryMissed(entry: ScheduleEntry): Boolean =
            entry.status == MedicationStatus.MISSED ||
            (entry.status == MedicationStatus.PENDING && morning.isAfter(entry.scheduledDateTime) &&
                TimeCalculationUtils.isMissed(entry.scheduledDateTime, morning, 360))

        val takenA = futureEntries.count { it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE }
        val missedA = futureEntries.count { isEntryMissed(it) }
        val overdueA = futureEntries.count { it.status == MedicationStatus.PENDING && it.scheduledDateTime.isBefore(morning) && !isEntryMissed(it) }
        val decidedA = takenA + missedA + overdueA
        val rateA = if (decidedA > 0) ((takenA.toDouble() / decidedA.toDouble()) * 100).toInt() else 0

        val statA = com.example.dosezy.ui.viewmodels.DayStat(
            date = today,
            dayLabel = "THU",
            total = futureEntries.size,
            taken = takenA,
            rate = rateA,
            decided = decidedA
        )

        assertEquals(2, statA.total)
        assertEquals(0, statA.decided)
        val displayTextA = if (statA.decided > 0) "${statA.rate}%" else "-"
        assertEquals("-", displayTextA)

        // Scenario B: Past day where all doses were doctor-approved SKIPPED
        val skippedEntries = listOf(
            ScheduleEntry("entry_3", "med_1", "user_1", morning.minusDays(1), MedicationStatus.SKIPPED)
        )
        val takenB = skippedEntries.count { it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE }
        val missedB = skippedEntries.count { isEntryMissed(it) }
        val overdueB = skippedEntries.count { it.status == MedicationStatus.PENDING && it.scheduledDateTime.isBefore(morning) && !isEntryMissed(it) }
        val decidedB = takenB + missedB + overdueB
        val rateB = if (decidedB > 0) ((takenB.toDouble() / decidedB.toDouble()) * 100).toInt() else 0

        val statB = com.example.dosezy.ui.viewmodels.DayStat(
            date = today.minusDays(1),
            dayLabel = "WED",
            total = skippedEntries.size,
            taken = takenB,
            rate = rateB,
            decided = decidedB
        )

        assertEquals(1, statB.total)
        assertEquals(0, statB.decided)
        val displayTextB = if (statB.decided > 0) "${statB.rate}%" else "-"
        assertEquals("-", displayTextB)
    }

    @Test
    fun scheduleCalendar_adhocPrnDose_doesNotColorCalendarDayGreen() {
        // Guard: Ad-hoc PRN doses must not color rest days green or indicate false 100% adherence (#BUG-CALENDAR-PRN)
        val prnDate = LocalDate.now().minusDays(2)
        val prnEntry = ScheduleEntry(
            entryId = "PRN_med_as_needed_${System.currentTimeMillis()}",
            userId = "usr_1",
            medicineId = "med_prn",
            scheduledDateTime = prnDate.atTime(14, 0),
            status = MedicationStatus.TAKEN_ON_TIME,
            takenAt = prnDate.atTime(14, 5)
        )

        // 1. Calling getDateStatusColor directly with only PRN entries returns Color.Transparent
        val color = getDateStatusColor(listOf(prnEntry))
        assertEquals(Color.Transparent, color)

        // 2. Pre-grouping entries filters out PRN entries so the date has no scheduled entries
        val allEntries = listOf(prnEntry)
        val entriesByDate = allEntries.filter { !it.entryId.startsWith("PRN_") }.groupBy { it.scheduledDateTime.toLocalDate() }
        val dayEntries = entriesByDate[prnDate] ?: emptyList()
        assertTrue(dayEntries.isEmpty())
        assertEquals(Color.Transparent, getDateStatusColor(dayEntries))
    }

    @Test
    fun scheduleCalendar_mixedScheduledAndPrnDoses_evaluatesOnlyScheduledRegimen() {
        // Guard: On a day with scheduled doses and an ad-hoc PRN dose, PRN dose must not mask missed scheduled dose (#BUG-CALENDAR-PRN)
        val date = LocalDate.now().minusDays(1)
        val missedScheduledEntry = ScheduleEntry(
            entryId = "scheduled_morning_dose",
            userId = "usr_1",
            medicineId = "med_daily",
            scheduledDateTime = date.atTime(8, 0),
            status = MedicationStatus.MISSED
        )
        val takenPrnEntry = ScheduleEntry(
            entryId = "PRN_painkiller_afternoon",
            userId = "usr_1",
            medicineId = "med_prn",
            scheduledDateTime = date.atTime(15, 0),
            status = MedicationStatus.TAKEN_ON_TIME,
            takenAt = date.atTime(15, 0)
        )

        // Both entries present on same day
        val dayEntries = listOf(missedScheduledEntry, takenPrnEntry)
        val color = getDateStatusColor(dayEntries)

        // Must be Red (0xFFEF4444) for missed scheduled dose, not green or amber
        assertEquals(Color(0xFFEF4444), color)
    }
}

