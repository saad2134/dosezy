package com.example.dosezy

import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.utils.TimeCalculationUtils
import com.example.dosezy.utils.TimeFormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/**
 * Tests for TimeCalculationUtils and TimeFormatUtils.
 * Validates dose timing math, late/missed window detection, status transitions,
 * and 12-hour/24-hour time formatting and parsing.
 */
class TimeAndFormattingTest {

    // ───────────────────────────────────────────────────────────────
    // 1. TimeCalculationUtils.calculateTimeDifference
    // ───────────────────────────────────────────────────────────────

    @Test
    fun timeDifference_futureDose_returnsPositiveDifferenceNotLate() {
        val current = LocalDateTime.of(2026, 9, 21, 8, 0)
        val scheduled = LocalDateTime.of(2026, 9, 21, 10, 30)

        val diff = TimeCalculationUtils.calculateTimeDifference(scheduled, current)
        assertEquals(2, diff.hours)
        assertEquals(30, diff.minutes)
        assertFalse(diff.isLate)
    }

    @Test
    fun timeDifference_exactTime_returnsZeroNotLate() {
        val current = LocalDateTime.of(2026, 9, 21, 8, 0)
        val scheduled = LocalDateTime.of(2026, 9, 21, 8, 0)

        val diff = TimeCalculationUtils.calculateTimeDifference(scheduled, current)
        assertEquals(0, diff.hours)
        assertEquals(0, diff.minutes)
        assertFalse(diff.isLate)
    }

    @Test
    fun timeDifference_pastDose_returnsLateWithCorrectDuration() {
        val scheduled = LocalDateTime.of(2026, 9, 21, 8, 0)
        val current = LocalDateTime.of(2026, 9, 21, 9, 45)

        val diff = TimeCalculationUtils.calculateTimeDifference(scheduled, current)
        assertEquals(1, diff.hours)
        assertEquals(45, diff.minutes)
        assertTrue(diff.isLate)
    }

    @Test
    fun timeDifference_acrossMidnight_calculatesAccurately() {
        val scheduled = LocalDateTime.of(2026, 9, 21, 23, 45)
        val current = LocalDateTime.of(2026, 9, 22, 0, 15)

        val diff = TimeCalculationUtils.calculateTimeDifference(scheduled, current)
        assertEquals(0, diff.hours)
        assertEquals(30, diff.minutes)
        assertTrue(diff.isLate)
    }

    @Test
    fun timeDifference_formatString_returnsExpectedText() {
        val pastDiff = TimeCalculationUtils.calculateTimeDifference(
            LocalDateTime.of(2026, 9, 21, 8, 0),
            LocalDateTime.of(2026, 9, 21, 10, 15)
        )
        assertEquals("2h 15m ago", TimeCalculationUtils.formatTimeDifference(pastDiff))

        val futureDiff = TimeCalculationUtils.calculateTimeDifference(
            LocalDateTime.of(2026, 9, 21, 14, 0),
            LocalDateTime.of(2026, 9, 21, 12, 20)
        )
        assertEquals("To be taken in 1h 40m", TimeCalculationUtils.formatTimeDifference(futureDiff))
    }

    // ───────────────────────────────────────────────────────────────
    // 2. TimeCalculationUtils.isLate
    // ───────────────────────────────────────────────────────────────

    @Test
    fun isLate_beforeLateThreshold_returnsFalse() {
        val scheduled = LocalDateTime.of(2026, 9, 21, 8, 0)
        // 2 hours later (threshold is 3 hours)
        val current = LocalDateTime.of(2026, 9, 21, 10, 0)

        assertFalse(TimeCalculationUtils.isLate(scheduled, current, lateAfterHours = 3, missedAfterHours = 6))
    }

    @Test
    fun isLate_atLateThreshold_returnsTrue() {
        val scheduled = LocalDateTime.of(2026, 9, 21, 8, 0)
        // Exactly 3 hours later
        val current = LocalDateTime.of(2026, 9, 21, 11, 0)

        assertTrue(TimeCalculationUtils.isLate(scheduled, current, lateAfterHours = 3, missedAfterHours = 6))
    }

    @Test
    fun isLate_withinLateWindow_returnsTrue() {
        val scheduled = LocalDateTime.of(2026, 9, 21, 8, 0)
        // 4.5 hours later
        val current = LocalDateTime.of(2026, 9, 21, 12, 30)

        assertTrue(TimeCalculationUtils.isLate(scheduled, current, lateAfterHours = 3, missedAfterHours = 6))
    }

    @Test
    fun isLate_atOrPastMissedThreshold_returnsFalse() {
        val scheduled = LocalDateTime.of(2026, 9, 21, 8, 0)
        // Exactly 6 hours later (now considered Missed, not Late)
        val currentAtMissed = LocalDateTime.of(2026, 9, 21, 14, 0)
        assertFalse(TimeCalculationUtils.isLate(scheduled, currentAtMissed, lateAfterHours = 3, missedAfterHours = 6))

        // 8 hours later
        val currentPastMissed = LocalDateTime.of(2026, 9, 21, 16, 0)
        assertFalse(TimeCalculationUtils.isLate(scheduled, currentPastMissed, lateAfterHours = 3, missedAfterHours = 6))
    }

    // ───────────────────────────────────────────────────────────────
    // 3. TimeCalculationUtils.isMissed
    // ───────────────────────────────────────────────────────────────

    @Test
    fun isMissed_beforeThreshold_returnsFalse() {
        val scheduled = LocalDateTime.of(2026, 9, 21, 8, 0)
        // 5 hours later (missedAfter is 6)
        val current = LocalDateTime.of(2026, 9, 21, 13, 0)

        assertFalse(TimeCalculationUtils.isMissed(scheduled, current, missedAfterHours = 6))
    }

    @Test
    fun isMissed_atOrPastThreshold_returnsTrue() {
        val scheduled = LocalDateTime.of(2026, 9, 21, 8, 0)
        // Exactly 6 hours later
        val current6h = LocalDateTime.of(2026, 9, 21, 14, 0)
        assertTrue(TimeCalculationUtils.isMissed(scheduled, current6h, missedAfterHours = 6))

        // 10 hours later
        val current10h = LocalDateTime.of(2026, 9, 21, 18, 0)
        assertTrue(TimeCalculationUtils.isMissed(scheduled, current10h, missedAfterHours = 6))
    }

    // ───────────────────────────────────────────────────────────────
    // 4. TimeCalculationUtils.getStatus
    // ───────────────────────────────────────────────────────────────

    @Test
    fun getStatus_beforeScheduledTime_returnsPending() {
        val scheduled = LocalDateTime.of(2026, 9, 21, 12, 0)
        val current = LocalDateTime.of(2026, 9, 21, 11, 30)

        assertEquals(MedicationStatus.PENDING, TimeCalculationUtils.getStatus(scheduled, current, 3, 6))
    }

    @Test
    fun getStatus_withinLateWindow_returnsPending() {
        // In the app, late is a UI indicator; its DB status remains PENDING until acted upon
        val scheduled = LocalDateTime.of(2026, 9, 21, 8, 0)
        val current = LocalDateTime.of(2026, 9, 21, 12, 0)

        assertEquals(MedicationStatus.PENDING, TimeCalculationUtils.getStatus(scheduled, current, 3, 6))
    }

    @Test
    fun getStatus_pastMissedWindow_returnsMissed() {
        val scheduled = LocalDateTime.of(2026, 9, 21, 8, 0)
        val current = LocalDateTime.of(2026, 9, 21, 15, 0) // 7h later, threshold 6

        assertEquals(MedicationStatus.MISSED, TimeCalculationUtils.getStatus(scheduled, current, 3, 6))
    }

    // ───────────────────────────────────────────────────────────────
    // 5. TimeFormatUtils.formatTime & formatLocalTime
    // ───────────────────────────────────────────────────────────────

    @Test
    fun formatTime_12Hour_formatsCorrectly() {
        val morning = LocalDateTime.of(2026, 9, 21, 8, 30)
        val noon = LocalDateTime.of(2026, 9, 21, 12, 0)
        val evening = LocalDateTime.of(2026, 9, 21, 20, 15)
        val midnight = LocalDateTime.of(2026, 9, 21, 0, 0)

        assertEquals("8:30 AM", TimeFormatUtils.formatTime(morning, TimeFormat.HOUR_12, Locale.US))
        assertEquals("12:00 PM", TimeFormatUtils.formatTime(noon, TimeFormat.HOUR_12, Locale.US))
        assertEquals("8:15 PM", TimeFormatUtils.formatTime(evening, TimeFormat.HOUR_12, Locale.US))
        assertEquals("12:00 AM", TimeFormatUtils.formatTime(midnight, TimeFormat.HOUR_12, Locale.US))
    }

    @Test
    fun formatTime_24Hour_formatsCorrectly() {
        val morning = LocalDateTime.of(2026, 9, 21, 8, 30)
        val noon = LocalDateTime.of(2026, 9, 21, 12, 0)
        val evening = LocalDateTime.of(2026, 9, 21, 20, 15)
        val midnight = LocalDateTime.of(2026, 9, 21, 0, 0)

        assertEquals("08:30", TimeFormatUtils.formatTime(morning, TimeFormat.HOUR_24, Locale.US))
        assertEquals("12:00", TimeFormatUtils.formatTime(noon, TimeFormat.HOUR_24, Locale.US))
        assertEquals("20:15", TimeFormatUtils.formatTime(evening, TimeFormat.HOUR_24, Locale.US))
        assertEquals("00:00", TimeFormatUtils.formatTime(midnight, TimeFormat.HOUR_24, Locale.US))
    }

    @Test
    fun formatLocalTime_matchesFormatTimeOutput() {
        val time = LocalTime.of(14, 45)
        assertEquals("2:45 PM", TimeFormatUtils.formatLocalTime(time, TimeFormat.HOUR_12, Locale.US))
        assertEquals("14:45", TimeFormatUtils.formatLocalTime(time, TimeFormat.HOUR_24, Locale.US))
    }

    // ───────────────────────────────────────────────────────────────
    // 6. TimeFormatUtils.parseTime
    // ───────────────────────────────────────────────────────────────

    @Test
    fun parseTime_valid12Hour_returnsNonNullDate() {
        val parsed = TimeFormatUtils.parseTime("8:30 AM", TimeFormat.HOUR_12, Locale.US)
        assertNotNull(parsed)
    }

    @Test
    fun parseTime_valid24Hour_returnsNonNullDate() {
        val parsed = TimeFormatUtils.parseTime("20:30", TimeFormat.HOUR_24, Locale.US)
        assertNotNull(parsed)
    }

    @Test
    fun parseTime_invalidString_returnsNullGracefully() {
        val parsed = TimeFormatUtils.parseTime("invalid-time", TimeFormat.HOUR_12, Locale.US)
        assertNull(parsed)
    }
}
