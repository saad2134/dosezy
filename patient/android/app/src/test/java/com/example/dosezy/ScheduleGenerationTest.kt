package com.example.dosezy

import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.MedicationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Tests for [Medicine.generateScheduleEntries] and the underlying
 * `shouldTakeOnDate` frequency logic across all patterns.
 */
class ScheduleGenerationTest {

    // ───────────────────────────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────────────────────────

    private fun med(
        freq: Frequency,
        times: List<LocalTime> = listOf(LocalTime.of(8, 0)),
        start: LocalDate? = null,
        end: LocalDate? = null,
        archived: Boolean = false,
        customDosages: Map<String, Double>? = null,
        dosage: Double = 100.0
    ) = Medicine(
        medicineId = "med_1",
        userId = "usr_1",
        medicationName = "TestMed",
        dosage = dosage,
        dosageUnit = DosageUnit.MG,
        timesPerDay = times.size,
        frequency = freq,
        scheduledTimes = times,
        startDate = start,
        endDate = end,
        isArchived = archived,
        customDosages = customDosages
    )

    // ───────────────────────────────────────────────────────────────
    // 1. DAILY frequency
    // ───────────────────────────────────────────────────────────────

    @Test
    fun dailyMedicine_generatesOneEntryPerSlotPerDay() {
        val start = LocalDate.of(2026, 12, 1)
        val m = med(Frequency(FrequencyPattern.DAILY), times = listOf(LocalTime.of(9, 0), LocalTime.of(21, 0)))
        val entries = m.generateScheduleEntries(startDateRange = start, days = 2) // 3 days inclusive (start, +1, +2)
        // 3 days × 2 slots = 6
        assertEquals(6, entries.size)
    }

    @Test
    fun dailyMedicine_entryIdsAreDeterministic() {
        val start = LocalDate.of(2026, 12, 1)
        val m = med(Frequency(FrequencyPattern.DAILY))
        val run1 = m.generateScheduleEntries(startDateRange = start, days = 0)
        val run2 = m.generateScheduleEntries(startDateRange = start, days = 0)
        assertEquals(run1.map { it.entryId }, run2.map { it.entryId })
    }

    @Test
    fun dailyMedicine_entryIdsUniqueAcrossSlots() {
        val start = LocalDate.of(2026, 12, 1)
        val m = med(
            Frequency(FrequencyPattern.DAILY),
            times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
        )
        val entries = m.generateScheduleEntries(startDateRange = start, days = 2)
        val ids = entries.map { it.entryId }
        assertEquals(ids.size, ids.toSet().size) // all unique
    }

    // ───────────────────────────────────────────────────────────────
    // 2. WEEKLY frequency with explicit selected days
    // ───────────────────────────────────────────────────────────────

    @Test
    fun weeklyMedicine_onlyOnSelectedDaysOfWeek() {
        // Mon=1, Wed=3, Fri=5
        val start = LocalDate.of(2026, 12, 7) // Monday
        val m = med(
            Frequency(FrequencyPattern.WEEKLY, selectedDaysOfWeek = listOf(1, 3, 5))
        )
        val entries = m.generateScheduleEntries(startDateRange = start, days = 6) // Mon-Sun
        val days = entries.map { it.scheduledDateTime.dayOfWeek }
        assertTrue(days.all { it in listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY) })
        assertEquals(3, entries.size) // 1 slot × 3 matching days
    }

    @Test
    fun weeklyMedicine_fallbackDaysPerWeek() {
        // No selectedDaysOfWeek, daysPerWeek = 3 means Mon,Tue,Wed (value ≤ 3)
        val start = LocalDate.of(2026, 12, 7) // Monday
        val m = med(
            Frequency(FrequencyPattern.WEEKLY, daysPerWeek = 3)
        )
        val entries = m.generateScheduleEntries(startDateRange = start, days = 6) // Mon-Sun
        val days = entries.map { it.scheduledDateTime.dayOfWeek }
        assertTrue(days.all { it.value <= 3 })
        assertEquals(3, entries.size)
    }

    // ───────────────────────────────────────────────────────────────
    // 3. MONTHLY frequency
    // ───────────────────────────────────────────────────────────────

    @Test
    fun monthlyMedicine_selectedDaysOfMonth() {
        // Take on 1st, 15th, 28th of the month
        val start = LocalDate.of(2026, 12, 1)
        val m = med(
            Frequency(FrequencyPattern.MONTHLY, selectedDaysOfMonth = listOf(1, 15, 28))
        )
        val entries = m.generateScheduleEntries(startDateRange = start, days = 30)
        val daysOfMonth = entries.map { it.scheduledDateTime.dayOfMonth }
        assertTrue(daysOfMonth.all { it in listOf(1, 15, 28) })
    }

    @Test
    fun monthlyMedicine_day31CoercedInShortMonth() {
        // Selected day 31 in February → should coerce to 28 (or 29 in leap year)
        val start = LocalDate.of(2026, 2, 1) // February 2026 has 28 days
        val m = med(
            Frequency(FrequencyPattern.MONTHLY, selectedDaysOfMonth = listOf(31))
        )
        val entries = m.generateScheduleEntries(startDateRange = start, days = 27) // Entire Feb
        // Day 31 coerced to 28 → should match Feb 28
        val daysOfMonth = entries.map { it.scheduledDateTime.dayOfMonth }
        assertTrue(28 in daysOfMonth)
    }

    // ───────────────────────────────────────────────────────────────
    // 4. EVERY_X_DAYS frequency
    // ───────────────────────────────────────────────────────────────

    @Test
    fun everyXDays_respectsIntervalFromStartDate() {
        val start = LocalDate.of(2026, 12, 1)
        val m = med(
            Frequency(FrequencyPattern.EVERY_X_DAYS, intervalDays = 3),
            start = start
        )
        // Generate 10 days: Dec 1..Dec 10
        val entries = m.generateScheduleEntries(startDateRange = start, days = 9)
        val dates = entries.map { it.scheduledDateTime.toLocalDate() }
        // Day 0 (Dec 1), Day 3 (Dec 4), Day 6 (Dec 7), Day 9 (Dec 10) — 4 entries
        assertEquals(4, entries.size)
        assertTrue(start in dates)
        assertTrue(start.plusDays(3) in dates)
        assertTrue(start.plusDays(6) in dates)
        assertTrue(start.plusDays(9) in dates)
    }

    @Test
    fun everyXDays_intervalDefaultsTo2WhenNull() {
        val start = LocalDate.of(2026, 12, 1)
        val m = med(
            Frequency(FrequencyPattern.EVERY_X_DAYS, intervalDays = null),
            start = start
        )
        val entries = m.generateScheduleEntries(startDateRange = start, days = 5) // 6 days
        // Default interval 2: Day 0, Day 2, Day 4 → 3 entries
        assertEquals(3, entries.size)
    }

    // ───────────────────────────────────────────────────────────────
    // 5. AS_NEEDED (PRN) — should never generate entries
    // ───────────────────────────────────────────────────────────────

    @Test
    fun asNeededMedicine_generatesZeroEntries() {
        val start = LocalDate.of(2026, 12, 1)
        val m = med(Frequency(FrequencyPattern.AS_NEEDED))
        val entries = m.generateScheduleEntries(startDateRange = start, days = 30)
        assertTrue(entries.isEmpty())
    }

    // ───────────────────────────────────────────────────────────────
    // 6. Archived medicine guard
    // ───────────────────────────────────────────────────────────────

    @Test
    fun archivedMedicine_generatesZeroEntries() {
        val start = LocalDate.of(2026, 12, 1)
        val m = med(Frequency(FrequencyPattern.DAILY), archived = true)
        val entries = m.generateScheduleEntries(startDateRange = start, days = 30)
        assertTrue(entries.isEmpty())
    }

    // ───────────────────────────────────────────────────────────────
    // 7. Course date bounds
    // ───────────────────────────────────────────────────────────────

    @Test
    fun medicine_respectsStartAndEndDateBounds() {
        val rangeStart = LocalDate.of(2026, 12, 1)
        val medStart = LocalDate.of(2026, 12, 5)
        val medEnd = LocalDate.of(2026, 12, 10)
        val m = med(Frequency(FrequencyPattern.DAILY), start = medStart, end = medEnd)
        val entries = m.generateScheduleEntries(startDateRange = rangeStart, days = 30)
        val dates = entries.map { it.scheduledDateTime.toLocalDate() }
        // No entries before medStart or after medEnd
        assertTrue(dates.all { !it.isBefore(medStart) && !it.isAfter(medEnd) })
        // Should have 6 entries: Dec 5,6,7,8,9,10
        assertEquals(6, entries.size)
    }

    @Test
    fun medicine_emptyWhenStartAfterEnd() {
        val rangeStart = LocalDate.of(2026, 12, 15)
        val medStart = LocalDate.of(2026, 12, 20)
        val medEnd = LocalDate.of(2026, 12, 10) // end before start — invalid course
        val m = med(Frequency(FrequencyPattern.DAILY), start = medStart, end = medEnd)
        val entries = m.generateScheduleEntries(startDateRange = rangeStart, days = 30)
        assertTrue(entries.isEmpty())
    }

    // ───────────────────────────────────────────────────────────────
    // 8. Custom dosages in generated entries
    // ───────────────────────────────────────────────────────────────

    @Test
    fun generatedEntries_carryCorrectCustomDosage() {
        val start = LocalDate.of(2026, 12, 1)
        val custom = mapOf("08:00" to 25.0, "20:00" to 50.0)
        val m = med(
            Frequency(FrequencyPattern.DAILY),
            times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            customDosages = custom,
            dosage = 10.0
        )
        val entries = m.generateScheduleEntries(startDateRange = start, days = 0) // 1 day
        val morning = entries.first { it.scheduledDateTime.toLocalTime() == LocalTime.of(8, 0) }
        val evening = entries.first { it.scheduledDateTime.toLocalTime() == LocalTime.of(20, 0) }
        assertEquals(25.0, morning.dosage ?: 0.0, 0.001)
        assertEquals(50.0, evening.dosage ?: 0.0, 0.001)
    }

    // ───────────────────────────────────────────────────────────────
    // 9. Empty scheduled times
    // ───────────────────────────────────────────────────────────────

    @Test
    fun medicine_withNoScheduledTimes_generatesZeroEntries() {
        val start = LocalDate.of(2026, 12, 1)
        val m = med(Frequency(FrequencyPattern.DAILY), times = emptyList())
        val entries = m.generateScheduleEntries(startDateRange = start, days = 5)
        assertTrue(entries.isEmpty())
    }

    // ───────────────────────────────────────────────────────────────
    // 10. All generated entries default to PENDING
    // ───────────────────────────────────────────────────────────────

    @Test
    fun allGeneratedEntries_havePendingStatus() {
        val start = LocalDate.of(2026, 12, 1)
        val m = med(Frequency(FrequencyPattern.DAILY))
        val entries = m.generateScheduleEntries(startDateRange = start, days = 3)
        assertTrue(entries.all { it.status == MedicationStatus.PENDING })
    }

    @Test
    fun allGeneratedEntries_haveCorrectUserAndMedicineIds() {
        val start = LocalDate.of(2026, 12, 1)
        val m = med(Frequency(FrequencyPattern.DAILY))
        val entries = m.generateScheduleEntries(startDateRange = start, days = 2)
        assertTrue(entries.all { it.userId == "usr_1" })
        assertTrue(entries.all { it.medicineId == "med_1" })
    }

    // ───────────────────────────────────────────────────────────────
    // 11. EVERY_X_HOURS frequency
    // ───────────────────────────────────────────────────────────────

    @Test
    fun everyXHours_generatesIntervalsThroughoutDay() {
        // Start date in future so no "skip past reminders" interference
        val start = LocalDate.of(2030, 1, 1)
        val m = med(
            Frequency(FrequencyPattern.EVERY_X_HOURS, intervalHours = 4),
            times = listOf(LocalTime.of(8, 0)),
            start = start
        )
        // 1 day (days = 0)
        val entries = m.generateScheduleEntries(startDateRange = start, days = 0)
        val times = entries.map { it.scheduledDateTime.toLocalTime() }

        // Starting at 08:00 every 4 hours: 08:00, 12:00, 16:00, 20:00 -> 4 slots
        assertEquals(4, entries.size)
        assertEquals(
            listOf(LocalTime.of(8, 0), LocalTime.of(12, 0), LocalTime.of(16, 0), LocalTime.of(20, 0)),
            times
        )
    }

    @Test
    fun everyXHours_defaultsTo4HoursWhenIntervalNull() {
        val start = LocalDate.of(2030, 1, 1)
        val m = med(
            Frequency(FrequencyPattern.EVERY_X_HOURS, intervalHours = null),
            times = listOf(LocalTime.of(6, 0)),
            start = start
        )
        // Interval null -> default 4. Starting at 06:00 -> 06:00, 10:00, 14:00, 18:00, 22:00 -> 5 slots
        val entries = m.generateScheduleEntries(startDateRange = start, days = 0)
        val times = entries.map { it.scheduledDateTime.toLocalTime() }
        assertEquals(5, entries.size)
        assertEquals(
            listOf(LocalTime.of(6, 0), LocalTime.of(10, 0), LocalTime.of(14, 0), LocalTime.of(18, 0), LocalTime.of(22, 0)),
            times
        )
    }

    @Test
    fun everyXHours_sixHourIntervalSpansFourSlots() {
        val start = LocalDate.of(2030, 1, 1)
        val m = med(
            Frequency(FrequencyPattern.EVERY_X_HOURS, intervalHours = 6),
            times = listOf(LocalTime.of(0, 0)),
            start = start
        )
        // 00:00, 06:00, 12:00, 18:00 -> 4 slots
        val entries = m.generateScheduleEntries(startDateRange = start, days = 0)
        assertEquals(4, entries.size)
        assertEquals(
            listOf(LocalTime.of(0, 0), LocalTime.of(6, 0), LocalTime.of(12, 0), LocalTime.of(18, 0)),
            entries.map { it.scheduledDateTime.toLocalTime() }
        )
    }
}
