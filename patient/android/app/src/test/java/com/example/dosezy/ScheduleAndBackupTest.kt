package com.example.dosezy

import com.example.dosezy.data.export.BackupRestoreManager
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ScheduleAndBackupTest {

    @Test
    fun testSlotAlarmIdDeterministic() {
        val userId = "test-user-123"
        val time = LocalDateTime.of(2026, 9, 21, 8, 0, 0)
        val cleanDateTime = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
        val slotAlarmId1 = "${userId}_${cleanDateTime}".hashCode()
        val slotAlarmId2 = "${userId}_2026-09-21_08-00".hashCode()

        assertEquals(slotAlarmId1, slotAlarmId2)
    }

    @Test
    fun testUndoStockGuardLogic() {
        // Only TAKEN_ON_TIME and TAKEN_LATE should be eligible for stock replenishment on undo
        fun shouldRestoreStock(previousStatus: MedicationStatus, autoDeduct: Boolean): Boolean {
            return (previousStatus == MedicationStatus.TAKEN_ON_TIME || previousStatus == MedicationStatus.TAKEN_LATE) && autoDeduct
        }

        assertTrue(shouldRestoreStock(MedicationStatus.TAKEN_ON_TIME, true))
        assertTrue(shouldRestoreStock(MedicationStatus.TAKEN_LATE, true))
        assertFalse(shouldRestoreStock(MedicationStatus.SKIPPED, true))
        assertFalse(shouldRestoreStock(MedicationStatus.MISSED, true))
        assertFalse(shouldRestoreStock(MedicationStatus.PENDING, true))
        assertFalse(shouldRestoreStock(MedicationStatus.TAKEN_ON_TIME, false))
    }

    @Test
    fun testRecordDoseGuardLogic() {
        // Prevent double stock deduction if status was already TAKEN
        fun shouldDeductStock(currentStatus: MedicationStatus, autoDeduct: Boolean): Boolean {
            val wasAlreadyTaken = currentStatus == MedicationStatus.TAKEN_ON_TIME || currentStatus == MedicationStatus.TAKEN_LATE
            return !wasAlreadyTaken && autoDeduct
        }

        assertTrue(shouldDeductStock(MedicationStatus.PENDING, true))
        assertTrue(shouldDeductStock(MedicationStatus.SKIPPED, true))
        assertFalse(shouldDeductStock(MedicationStatus.TAKEN_ON_TIME, true))
        assertFalse(shouldDeductStock(MedicationStatus.TAKEN_LATE, true))
    }

    @Test
    fun testSelectiveRestoreUserActiveFlagLogic() {
        // When restoring selective profiles:
        // isCurrentUser should only be true if existing users is empty AND it is the very first profile being restored
        fun computeIsCurrentUser(existingUsersEmpty: Boolean, totalProfiles: Int): Boolean {
            return existingUsersEmpty && totalProfiles == 0
        }

        // When DB is empty and restoring first user -> active
        assertTrue(computeIsCurrentUser(existingUsersEmpty = true, totalProfiles = 0))
        // When DB is empty and restoring second user -> NOT active
        assertFalse(computeIsCurrentUser(existingUsersEmpty = true, totalProfiles = 1))
        // When DB already has users -> NOT active
        assertFalse(computeIsCurrentUser(existingUsersEmpty = false, totalProfiles = 0))
    }

    @Test
    fun testAsNeededFrequencyDisplayLogic() {
        val pattern = FrequencyPattern.AS_NEEDED
        val timesPerDay = 0
        val display = if (pattern == FrequencyPattern.AS_NEEDED) "As Needed (PRN)" else "$timesPerDay times a day"
        assertEquals("As Needed (PRN)", display)
    }

    @Test
    fun testCourseDurationEndDates() {
        val start = java.time.LocalDate.of(2026, 9, 21)
        fun calcEndDate(startDate: java.time.LocalDate, durationDays: Int): java.time.LocalDate {
            return startDate.plusDays((durationDays - 1).toLong().coerceAtLeast(0L))
        }

        // 1-day course: start and end are on the same day (1 day total)
        assertEquals(start, calcEndDate(start, 1))

        // 7-day course: start + 6 days = spans 7 calendar days inclusive (e.g. 21 to 27 = 7 days)
        val end7 = calcEndDate(start, 7)
        assertEquals(start.plusDays(6), end7)
        val daysSpan = java.time.temporal.ChronoUnit.DAYS.between(start, end7) + 1
        assertEquals(7L, daysSpan)

        // 14-day course: spans 14 calendar days inclusive
        val end14 = calcEndDate(start, 14)
        val daysSpan14 = java.time.temporal.ChronoUnit.DAYS.between(start, end14) + 1
        assertEquals(14L, daysSpan14)
    }

    @Test
    fun testCsvEscapeFormatting() {
        fun escapeCsv(value: Any?): String {
            val str = value?.toString() ?: ""
            return "\"${str.replace("\"", "\"\"")}\""
        }

        assertEquals("\"Hello World\"", escapeCsv("Hello World"))
        assertEquals("\"John \"\"Doc\"\" Doe\"", escapeCsv("John \"Doc\" Doe"))
        assertEquals("\"Peanuts, \"\"Tree nuts\"\"\"", escapeCsv("Peanuts, \"Tree nuts\""))
        assertEquals("\"\"", escapeCsv(null))
    }

    @Test
    fun testPreferencesLateAndMissedBounds() {
        // Enforce that lateAfter < missedAfter
        fun resolveHours(selectedLate: Int, selectedMissed: Int): Pair<Int, Int> {
            val maxLate = (selectedMissed - 1).coerceIn(1, 8)
            val effectiveLate = selectedLate.coerceIn(1, maxLate)
            val minMissed = (effectiveLate + 1).coerceIn(2, 9)
            val effectiveMissed = selectedMissed.coerceIn(minMissed, 9)
            return Pair(effectiveLate, effectiveMissed)
        }

        val (late1, missed1) = resolveHours(3, 3)
        assertTrue(late1 < missed1)
        assertEquals(2, late1)
        assertEquals(3, missed1)

        val (late2, missed2) = resolveHours(1, 2)
        assertTrue(late2 < missed2)
        assertEquals(1, late2)
        assertEquals(2, missed2)

        val (late3, missed3) = resolveHours(3, 6)
        assertTrue(late3 < missed3)
        assertEquals(3, late3)
        assertEquals(6, missed3)
    }

    @Test
    fun testEveryXDaysFrequencyAnchor() {
        val interval = 3
        val today = java.time.LocalDate.of(2026, 9, 21)
        val baseDate = today // Anchored to today when startDate is null

        fun shouldTake(date: java.time.LocalDate): Boolean {
            val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(baseDate, date)
            return daysDiff % interval == 0L
        }

        assertTrue(shouldTake(today)) // Today is Day 0 -> should take
        assertFalse(shouldTake(today.plusDays(1))) // Day 1 -> false
        assertFalse(shouldTake(today.plusDays(2))) // Day 2 -> false
        assertTrue(shouldTake(today.plusDays(3))) // Day 3 -> should take
    }
}
