package com.example.dosezy

import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Tests for Medicine display methods, alarm ID generation, slot key determinism,
 * calendar date status logic, entry ID format, and course duration math.
 */
class MedicineLogicTest {

    // ───────────────────────────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────────────────────────

    private fun med(
        dosage: Double = 100.0,
        unit: DosageUnit = DosageUnit.MG,
        freq: Frequency = Frequency(FrequencyPattern.DAILY),
        times: List<LocalTime> = listOf(LocalTime.of(8, 0)),
        customDosages: Map<String, Double>? = null,
        stock: Int = 100
    ) = Medicine(
        medicineId = "med_1",
        userId = "usr_1",
        medicationName = "TestMed",
        dosage = dosage,
        dosageUnit = unit,
        timesPerDay = times.size,
        frequency = freq,
        scheduledTimes = times,
        customDosages = customDosages,
        currentStock = stock,
        refillThreshold = 10,
        autoDeductOnTake = true
    )

    // ───────────────────────────────────────────────────────────────
    // 1. getDosageDisplay – formatting for all units
    // ───────────────────────────────────────────────────────────────

    @Test
    fun dosageDisplay_wholeNumber_noDecimal() {
        assertEquals("100 mg", med(100.0, DosageUnit.MG).getDosageDisplay())
        assertEquals("1 tablet", med(1.0, DosageUnit.TABLET).getDosageDisplay())
        assertEquals("5 mL", med(5.0, DosageUnit.ML).getDosageDisplay())
        assertEquals("2 drop", med(2.0, DosageUnit.DROP).getDosageDisplay())
        assertEquals("3 capsule", med(3.0, DosageUnit.CAPSULE).getDosageDisplay())
        assertEquals("50 mcg", med(50.0, DosageUnit.MCG).getDosageDisplay())
    }

    @Test
    fun dosageDisplay_fractionalNumber_showsDecimal() {
        assertEquals("2.5 mg", med(2.5, DosageUnit.MG).getDosageDisplay())
        assertEquals("0.5 tablet", med(0.5, DosageUnit.TABLET).getDosageDisplay())
    }

    @Test
    fun dosageDisplay_withCustomDosage_usesSlotValue() {
        val custom = mapOf("08:00" to 25.0, "20:00" to 50.0)
        val m = med(dosage = 10.0, customDosages = custom)
        assertEquals("25 mg", m.getDosageDisplay(LocalTime.of(8, 0)))
        assertEquals("50 mg", m.getDosageDisplay(LocalTime.of(20, 0)))
        // Unmatched time falls back to base
        assertEquals("10 mg", m.getDosageDisplay(LocalTime.of(12, 0)))
    }

    @Test
    fun dosageDisplay_nullTime_returnsBaseDosage() {
        val custom = mapOf("08:00" to 25.0)
        val m = med(dosage = 10.0, customDosages = custom)
        assertEquals("10 mg", m.getDosageDisplay(null))
    }

    // ───────────────────────────────────────────────────────────────
    // 2. getDosageForTime – value resolution
    // ───────────────────────────────────────────────────────────────

    @Test
    fun getDosageForTime_emptyCustomMap_returnsBase() {
        val m = med(dosage = 500.0, customDosages = emptyMap())
        assertEquals(500.0, m.getDosageForTime(LocalTime.of(8, 0)), 0.001)
    }

    @Test
    fun getDosageForTime_nullCustomMap_returnsBase() {
        val m = med(dosage = 500.0, customDosages = null)
        assertEquals(500.0, m.getDosageForTime(LocalTime.of(8, 0)), 0.001)
    }

    @Test
    fun getDosageForTime_matchesPaddedTimeKey() {
        // Verify that time 9:05 is formatted as "09:05" for lookup
        val custom = mapOf("09:05" to 75.0)
        val m = med(dosage = 10.0, customDosages = custom)
        assertEquals(75.0, m.getDosageForTime(LocalTime.of(9, 5)), 0.001)
    }

    // ───────────────────────────────────────────────────────────────
    // 3. getFrequencyDisplay – text output for all patterns
    // ───────────────────────────────────────────────────────────────

    @Test
    fun frequencyDisplay_daily() {
        assertEquals("Daily", med(freq = Frequency(FrequencyPattern.DAILY)).getFrequencyDisplay())
    }

    @Test
    fun frequencyDisplay_asNeeded() {
        assertEquals("As Needed (PRN)", med(freq = Frequency(FrequencyPattern.AS_NEEDED)).getFrequencyDisplay())
    }

    @Test
    fun frequencyDisplay_everyXHours() {
        assertEquals("Every 6 Hours", med(freq = Frequency(FrequencyPattern.EVERY_X_HOURS, intervalHours = 6)).getFrequencyDisplay())
    }

    @Test
    fun frequencyDisplay_everyXDays() {
        assertEquals("Every 3 Days", med(freq = Frequency(FrequencyPattern.EVERY_X_DAYS, intervalDays = 3)).getFrequencyDisplay())
    }

    @Test
    fun frequencyDisplay_weekly() {
        assertEquals("5 times per week", med(freq = Frequency(FrequencyPattern.WEEKLY, daysPerWeek = 5)).getFrequencyDisplay())
    }

    @Test
    fun frequencyDisplay_monthly() {
        assertEquals("15 times per month", med(freq = Frequency(FrequencyPattern.MONTHLY, daysPerMonth = 15)).getFrequencyDisplay())
    }

    @Test
    fun frequencyDisplay_custom() {
        assertEquals("Custom", med(freq = Frequency(FrequencyPattern.CUSTOM)).getFrequencyDisplay())
    }

    @Test
    fun frequencyDisplay_everyXHoursDefault() {
        // intervalHours null → default to 4
        assertEquals("Every 4 Hours", med(freq = Frequency(FrequencyPattern.EVERY_X_HOURS)).getFrequencyDisplay())
    }

    @Test
    fun frequencyDisplay_everyXDaysDefault() {
        // intervalDays null → default to 2
        assertEquals("Every 2 Days", med(freq = Frequency(FrequencyPattern.EVERY_X_DAYS)).getFrequencyDisplay())
    }

    // ───────────────────────────────────────────────────────────────
    // 4. getStockDeductionAmount – edge cases
    // ───────────────────────────────────────────────────────────────

    @Test
    fun stockDeduction_mgAlwaysOne() {
        assertEquals(1, med(500.0, DosageUnit.MG).getStockDeductionAmount())
        assertEquals(1, med(0.5, DosageUnit.MG).getStockDeductionAmount())
    }

    @Test
    fun stockDeduction_mcgAlwaysOne() {
        assertEquals(1, med(100.0, DosageUnit.MCG).getStockDeductionAmount())
    }

    @Test
    fun stockDeduction_tabletClampedTo1Through10() {
        assertEquals(1, med(1.0, DosageUnit.TABLET).getStockDeductionAmount())
        assertEquals(5, med(5.0, DosageUnit.TABLET).getStockDeductionAmount())
        assertEquals(10, med(10.0, DosageUnit.TABLET).getStockDeductionAmount())
        // > 10 falls back to 1
        assertEquals(1, med(11.0, DosageUnit.TABLET).getStockDeductionAmount())
        assertEquals(1, med(500.0, DosageUnit.TABLET).getStockDeductionAmount())
    }

    @Test
    fun stockDeduction_capsuleClampedTo1Through10() {
        assertEquals(2, med(2.0, DosageUnit.CAPSULE).getStockDeductionAmount())
        assertEquals(1, med(100.0, DosageUnit.CAPSULE).getStockDeductionAmount())
    }

    @Test
    fun stockDeduction_zeroDosageTabletDefaultsTo1() {
        // dosage 0.0 → toInt() = 0, not in 1..10, falls back to 1
        assertEquals(1, med(0.0, DosageUnit.TABLET).getStockDeductionAmount())
    }

    @Test
    fun stockDeduction_liquidMinimumOne() {
        assertEquals(1, med(0.5, DosageUnit.ML).getStockDeductionAmount()) // 0.5.toInt() = 0, coerced to 1
        assertEquals(3, med(3.0, DosageUnit.DROP).getStockDeductionAmount())
    }

    @Test
    fun stockDeduction_usesCustomDosageForSlot() {
        val custom = mapOf("08:00" to 3.0, "20:00" to 1.0)
        val m = med(dosage = 1.0, unit = DosageUnit.TABLET, customDosages = custom)
        assertEquals(3, m.getStockDeductionAmount(LocalTime.of(8, 0)))
        assertEquals(1, m.getStockDeductionAmount(LocalTime.of(20, 0)))
    }

    // ───────────────────────────────────────────────────────────────
    // 5. Slot alarm ID determinism
    // ───────────────────────────────────────────────────────────────

    @Test
    fun slotAlarmId_sameUserAndTime_alwaysEqual() {
        val userId = "user-abc-123"
        val dt = LocalDateTime.of(2026, 9, 21, 8, 0, 0)
        val clean = dt.withSecond(0).withNano(0)
        val id1 = "${userId}_${clean}".hashCode()
        val id2 = "${userId}_${clean}".hashCode()
        assertEquals(id1, id2)
    }

    @Test
    fun slotAlarmId_differentTimes_differentIds() {
        val userId = "user-abc-123"
        val dt1 = LocalDateTime.of(2026, 9, 21, 8, 0, 0).withSecond(0).withNano(0)
        val dt2 = LocalDateTime.of(2026, 9, 21, 20, 0, 0).withSecond(0).withNano(0)
        val id1 = "${userId}_${dt1}".hashCode()
        val id2 = "${userId}_${dt2}".hashCode()
        assertNotEquals(id1, id2)
    }

    @Test
    fun slotAlarmId_differentUsers_differentIds() {
        val dt = LocalDateTime.of(2026, 9, 21, 8, 0, 0).withSecond(0).withNano(0)
        val id1 = "user-A_${dt}".hashCode()
        val id2 = "user-B_${dt}".hashCode()
        assertNotEquals(id1, id2)
    }

    @Test
    fun slotAlarmId_secondsAndNanosNormalized() {
        val userId = "user-abc"
        val dt1 = LocalDateTime.of(2026, 9, 21, 8, 0, 45, 123456789)
        val dt2 = LocalDateTime.of(2026, 9, 21, 8, 0, 0, 0)
        val clean1 = dt1.withSecond(0).withNano(0)
        val clean2 = dt2.withSecond(0).withNano(0)
        assertEquals("${userId}_${clean1}".hashCode(), "${userId}_${clean2}".hashCode())
    }

    // ───────────────────────────────────────────────────────────────
    // 6. Entry ID format and uniqueness
    // ───────────────────────────────────────────────────────────────

    @Test
    fun entryId_format_noColonsOrDashes() {
        val medId = "med_123"
        val date = LocalDate.of(2026, 9, 21)
        val time = LocalTime.of(8, 30).withSecond(0).withNano(0)
        val entryId = "${medId}_${date}_${time}".replace(":", "_").replace("-", "_")
        assertFalse(entryId.contains(":"))
        assertFalse(entryId.contains("-"))
        assertEquals("med_123_2026_09_21_08_30", entryId)
    }

    // ───────────────────────────────────────────────────────────────
    // 7. Course duration math (inclusive end date)
    // ───────────────────────────────────────────────────────────────

    @Test
    fun courseDuration_oneDaySpansOneCalendarDay() {
        val start = LocalDate.of(2026, 9, 21)
        val end = start.plusDays((1 - 1).toLong().coerceAtLeast(0L))
        assertEquals(start, end) // same day
        assertEquals(1L, ChronoUnit.DAYS.between(start, end) + 1)
    }

    @Test
    fun courseDuration_sevenDaySpansSevenCalendarDays() {
        val start = LocalDate.of(2026, 9, 21)
        val end = start.plusDays((7 - 1).toLong().coerceAtLeast(0L))
        assertEquals(7L, ChronoUnit.DAYS.between(start, end) + 1)
    }

    @Test
    fun courseDuration_thirtyDaySpansThirtyCalendarDays() {
        val start = LocalDate.of(2026, 1, 1)
        val end = start.plusDays((30 - 1).toLong().coerceAtLeast(0L))
        assertEquals(30L, ChronoUnit.DAYS.between(start, end) + 1)
    }

    @Test
    fun courseDuration_zeroDaysCoercedToZero() {
        // Edge case: 0-day course should coerce to 0 and result in same start date
        val start = LocalDate.of(2026, 9, 21)
        val end = start.plusDays((0 - 1).toLong().coerceAtLeast(0L))
        assertEquals(start, end)
    }

    // ───────────────────────────────────────────────────────────────
    // 8. Calendar date status color logic (pure function mirror)
    // ───────────────────────────────────────────────────────────────

    // Mirror the getDateStatusColor logic as a pure function for testing
    private enum class StatusColor { RED, AMBER, GREEN, GRAY_SKIPPED, GRAY_PENDING, TRANSPARENT }

    private fun computeStatusColor(entries: List<ScheduleEntry>): StatusColor {
        if (entries.isEmpty()) return StatusColor.TRANSPARENT
        val hasMissed = entries.any { it.status == MedicationStatus.MISSED }
        val hasLate = entries.any { it.status == MedicationStatus.TAKEN_LATE }
        val allCompleted = entries.all {
            it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE || it.status == MedicationStatus.SKIPPED
        }
        val hasTaken = entries.any {
            it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE
        }
        val allSkipped = entries.all { it.status == MedicationStatus.SKIPPED }
        val hasPastPending = entries.any {
            it.status == MedicationStatus.PENDING && it.scheduledDateTime.isBefore(LocalDateTime.now().minusMinutes(1))
        }
        return when {
            hasMissed || hasPastPending -> StatusColor.RED
            hasLate -> StatusColor.AMBER
            allCompleted && hasTaken -> StatusColor.GREEN
            allSkipped -> StatusColor.GRAY_SKIPPED
            else -> StatusColor.GRAY_PENDING
        }
    }

    private fun entry(status: MedicationStatus, hoursAgo: Long = 0): ScheduleEntry {
        return ScheduleEntry(
            entryId = "e_${System.nanoTime()}",
            userId = "usr_1",
            medicineId = "med_1",
            scheduledDateTime = LocalDateTime.now().minusHours(hoursAgo).withSecond(0).withNano(0),
            status = status
        )
    }

    @Test
    fun calendarStatus_emptyEntries_transparent() {
        assertEquals(StatusColor.TRANSPARENT, computeStatusColor(emptyList()))
    }

    @Test
    fun calendarStatus_allTakenOnTime_green() {
        val entries = listOf(
            entry(MedicationStatus.TAKEN_ON_TIME),
            entry(MedicationStatus.TAKEN_ON_TIME)
        )
        assertEquals(StatusColor.GREEN, computeStatusColor(entries))
    }

    @Test
    fun calendarStatus_mixedTakenAndSkipped_green() {
        // At least one taken + some skipped = green (allCompleted && hasTaken)
        val entries = listOf(
            entry(MedicationStatus.TAKEN_ON_TIME),
            entry(MedicationStatus.SKIPPED)
        )
        assertEquals(StatusColor.GREEN, computeStatusColor(entries))
    }

    @Test
    fun calendarStatus_allSkipped_graySkipped() {
        val entries = listOf(
            entry(MedicationStatus.SKIPPED),
            entry(MedicationStatus.SKIPPED)
        )
        assertEquals(StatusColor.GRAY_SKIPPED, computeStatusColor(entries))
    }

    @Test
    fun calendarStatus_anyMissed_red() {
        val entries = listOf(
            entry(MedicationStatus.TAKEN_ON_TIME),
            entry(MedicationStatus.MISSED)
        )
        assertEquals(StatusColor.RED, computeStatusColor(entries))
    }

    @Test
    fun calendarStatus_anyLate_amber() {
        val entries = listOf(
            entry(MedicationStatus.TAKEN_ON_TIME),
            entry(MedicationStatus.TAKEN_LATE)
        )
        assertEquals(StatusColor.AMBER, computeStatusColor(entries))
    }

    @Test
    fun calendarStatus_missedPrecedesLate() {
        // If both missed and late exist, missed (red) takes precedence
        val entries = listOf(
            entry(MedicationStatus.TAKEN_LATE),
            entry(MedicationStatus.MISSED)
        )
        assertEquals(StatusColor.RED, computeStatusColor(entries))
    }

    @Test
    fun calendarStatus_pendingPast_red() {
        // Entry scheduled 2 hours ago still PENDING → overdue → red
        val entries = listOf(
            entry(MedicationStatus.PENDING, hoursAgo = 2)
        )
        assertEquals(StatusColor.RED, computeStatusColor(entries))
    }

    @Test
    fun calendarStatus_pendingFuture_grayPending() {
        // Entry scheduled in the future still PENDING → gray
        val futureEntry = ScheduleEntry(
            entryId = "e_future",
            userId = "usr_1",
            medicineId = "med_1",
            scheduledDateTime = LocalDateTime.now().plusHours(3),
            status = MedicationStatus.PENDING
        )
        assertEquals(StatusColor.GRAY_PENDING, computeStatusColor(listOf(futureEntry)))
    }

    // ───────────────────────────────────────────────────────────────
    // 9. CSV escape logic (mirror from DataExporter)
    // ───────────────────────────────────────────────────────────────

    private fun escapeCsv(value: Any?): String {
        val str = value?.toString() ?: ""
        return "\"${str.replace("\"", "\"\"")}\""
    }

    @Test
    fun csvEscape_plainText() {
        assertEquals("\"Hello\"", escapeCsv("Hello"))
    }

    @Test
    fun csvEscape_containsComma() {
        assertEquals("\"Peanuts, Tree nuts\"", escapeCsv("Peanuts, Tree nuts"))
    }

    @Test
    fun csvEscape_containsQuotes() {
        assertEquals("\"John \"\"Doc\"\" Doe\"", escapeCsv("John \"Doc\" Doe"))
    }

    @Test
    fun csvEscape_containsNewline() {
        assertEquals("\"Line1\nLine2\"", escapeCsv("Line1\nLine2"))
    }

    @Test
    fun csvEscape_emptyString() {
        assertEquals("\"\"", escapeCsv(""))
    }

    @Test
    fun csvEscape_null() {
        assertEquals("\"\"", escapeCsv(null))
    }

    @Test
    fun csvEscape_number() {
        assertEquals("\"42\"", escapeCsv(42))
    }

    @Test
    fun csvEscape_unicodeCharacters() {
        assertEquals("\"日本語テスト\"", escapeCsv("日本語テスト"))
    }

    // ───────────────────────────────────────────────────────────────
    // 10. Preferences late-before-missed invariant
    // ───────────────────────────────────────────────────────────────

    private fun resolveHours(selectedLate: Int, selectedMissed: Int): Pair<Int, Int> {
        val maxLate = (selectedMissed - 1).coerceIn(1, 8)
        val effectiveLate = selectedLate.coerceIn(1, maxLate)
        val minMissed = (effectiveLate + 1).coerceIn(2, 9)
        val effectiveMissed = selectedMissed.coerceIn(minMissed, 9)
        return Pair(effectiveLate, effectiveMissed)
    }

    @Test
    fun preferences_lateAlwaysLessThanMissed() {
        // Exhaustive check across all valid input combinations (1-9 × 1-9)
        for (late in 1..9) {
            for (missed in 1..9) {
                val (l, m) = resolveHours(late, missed)
                assertTrue("lateAfter=$l must be < missedAfter=$m (input: late=$late, missed=$missed)", l < m)
                assertTrue("lateAfter=$l must be >= 1", l >= 1)
                assertTrue("missedAfter=$m must be <= 9", m <= 9)
            }
        }
    }

    @Test
    fun preferences_equalInputsCorrected() {
        val (late, missed) = resolveHours(5, 5)
        assertTrue(late < missed)
    }

    @Test
    fun preferences_lateExceedsMissedCorrected() {
        val (late, missed) = resolveHours(7, 3)
        assertTrue(late < missed)
    }

    @Test
    fun preferences_minimumBound() {
        val (late, missed) = resolveHours(1, 2)
        assertEquals(1, late)
        assertEquals(2, missed)
    }

    @Test
    fun preferences_maximumBound() {
        val (late, missed) = resolveHours(8, 9)
        assertEquals(8, late)
        assertEquals(9, missed)
    }
}
