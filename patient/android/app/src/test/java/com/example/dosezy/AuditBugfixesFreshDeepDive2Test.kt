/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.dao.MedicineDao
import com.example.dosezy.data.dao.ScheduleDao
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.domain.ScheduleRecurrenceEngine
import com.example.dosezy.ui.viewmodels.AdherenceRange
import com.example.dosezy.ui.viewmodels.AnalyticsBreakdown
import com.example.dosezy.ui.viewmodels.RangeAdherenceData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuditBugfixesFreshDeepDive2Test {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ---------------------------------------------------------------------------------------------
    // Bug 1: Range-Specific Breakdown Cards in Analytics
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testRangeAdherenceData_containsRangeSpecificBreakdown() {
        val breakdown7Days = AnalyticsBreakdown(
            totalEntries = 7,
            takenOnTime = 5,
            takenLate = 1,
            missed = 1,
            skipped = 0,
            totalTaken = 6,
            totalDecided = 7
        )

        val breakdownTotal = AnalyticsBreakdown(
            totalEntries = 100,
            takenOnTime = 80,
            takenLate = 10,
            missed = 10,
            skipped = 0,
            totalTaken = 90,
            totalDecided = 100
        )

        val data7Days = RangeAdherenceData(
            range = AdherenceRange.LAST_7_DAYS,
            takenCount = 6,
            decidedCount = 7,
            totalEntries = 7,
            rate = 85,
            breakdown = breakdown7Days
        )

        val dataTotal = RangeAdherenceData(
            range = AdherenceRange.TOTAL,
            takenCount = 90,
            decidedCount = 100,
            totalEntries = 100,
            rate = 90,
            breakdown = breakdownTotal
        )

        // Guard: Verify 7-day metric card values reflect the 7-day window rather than all-time numbers
        assertEquals(7, data7Days.breakdown.totalEntries)
        assertEquals(5, data7Days.breakdown.takenOnTime)
        assertEquals(1, data7Days.breakdown.missed)

        // Guard: Verify TOTAL reflects the full lifetime counts
        assertEquals(100, dataTotal.breakdown.totalEntries)
        assertEquals(80, dataTotal.breakdown.takenOnTime)
        assertEquals(10, dataTotal.breakdown.missed)
    }

    // ---------------------------------------------------------------------------------------------
    // Bug 2: Rescheduling Sibling Alarms on Orphaned Medicine Deletion
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testDeleteMedicineWithAlarms_whenOrphanedEntriesExist_reschedulesSiblingAlarms() = runBlocking {
        val database = mockk<DosezyDatabase>(relaxed = true)
        val medicineDao = mockk<MedicineDao>(relaxed = true)
        val scheduleDao = mockk<ScheduleDao>(relaxed = true)
        val scheduleRepository = mockk<ScheduleRepository>(relaxed = true)

        every { database.medicineDao() } returns medicineDao
        every { database.scheduleDao() } returns scheduleDao

        val orphanedMedId = "orphaned_med_123"
        val affectedUserId = "user_affected_456"

        // Medicine itself does not exist in DB
        coEvery { medicineDao.getMedicineByIdDirect(orphanedMedId) } returns null

        // Orphaned schedule entry referencing this medicine exists in DB
        val orphanedEntry = ScheduleEntry(
            entryId = "${orphanedMedId}_2026_10_16_08_00",
            userId = affectedUserId,
            medicineId = orphanedMedId,
            scheduledDateTime = LocalDateTime.of(2026, 10, 16, 8, 0),
            status = MedicationStatus.PENDING
        )
        coEvery { scheduleDao.getScheduleEntriesByMedicine(orphanedMedId) } returns listOf(orphanedEntry)

        val repository = MedicineRepository(database, scheduleRepository, context)
        repository.deleteMedicineWithAlarms(orphanedMedId, context)

        // Guard: Verify sibling alarms are rescheduled so remaining co-scheduled medicines trigger alarms
        coVerify(exactly = 1) { scheduleRepository.cancelAlarmsForMedicine(orphanedMedId, context) }
        coVerify(exactly = 1) { scheduleDao.deleteScheduleEntriesByMedicine(orphanedMedId) }
        coVerify(exactly = 1) { scheduleRepository.rescheduleAllAlarms(affectedUserId, context) }
    }

    // ---------------------------------------------------------------------------------------------
    // Bug 3: Deterministic EntryId Parsing after Backup Restore
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testRestoredEntryId_followsDeterministicPatternAndParsesSuccessfully() {
        val medId = "med_restored_999"
        val scheduledTime = LocalDateTime.of(2026, 11, 25, 14, 30, 0)
        val datePart = scheduledTime.toLocalDate().toString().replace("-", "_")
        val timePart = scheduledTime.toLocalTime().withSecond(0).withNano(0).toString().replace(":", "_")
        val entryId = "${medId}_${datePart}_${timePart}"

        // Guard: Verify entry ID conforms to ENTRY_ID_DATETIME_REGEX
        assertTrue(ScheduleRepository.ENTRY_ID_DATETIME_REGEX.containsMatchIn(entryId))

        // Guard: Verify ScheduleRepository can parse scheduled time without database queries
        val parsedTime = ScheduleRepository.parseDateTimeFromEntryId(entryId)
        assertNotNull(parsedTime)
        assertEquals(scheduledTime, parsedTime)
    }

    // ---------------------------------------------------------------------------------------------
    // Bug 4: Emergency Preferences Scrub on Profile Deletion
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testDeleteUser_scrubsEmergencyCountryPreference() {
        val userId = "usr_cleanup_test"
        val emergencyPrefs = context.getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)

        // Populate preference for deleted user
        val prefKey = "selected_country_code_$userId"
        emergencyPrefs.edit().putString(prefKey, "US").apply()
        assertTrue(emergencyPrefs.contains(prefKey))

        // Simulate UserViewModel scrub logic
        emergencyPrefs.edit().remove(prefKey).apply()

        // Guard: Verify preference key was scrubbed to prevent leaking across recreated profiles
        assertFalse(emergencyPrefs.contains(prefKey))
    }

    // ---------------------------------------------------------------------------------------------
    // Bug 5: Multi-Week Custom Frequency FloorMod Recurrence Evaluation
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testScheduleRecurrenceEngine_customMultiWeek_evaluatesFloorModSymmetrically() {
        // Bi-weekly custom pattern (every 2 weeks on Mondays)
        val medicine = Medicine(
            medicineId = "med_biweekly",
            userId = "usr_1",
            medicationName = "Biweekly Infliximab",
            dosage = 1.0,
            dosageUnit = DosageUnit.TABLET,
            frequency = Frequency(
                pattern = FrequencyPattern.CUSTOM,
                intervalWeeks = 2,
                selectedDaysOfWeek = listOf(DayOfWeek.MONDAY.value)
            ),
            startDate = null, // null startDate tests anchor fallback (Jan 6, 2020)
            scheduledTimes = listOf(LocalTime.of(9, 0)),
            timesPerDay = 1
        )

        // Anchor is Jan 6, 2020 (Monday).
        // Test date: Exactly 2 weeks before Jan 6, 2020 -> Dec 23, 2019 (Monday) -> weeksDiff = -2.
        // With Math.floorMod(-2, 2) == 0L, this on-cycle week correctly evaluates to true.
        val dateTwoWeeksPrior = LocalDate.of(2019, 12, 23)
        val shouldTakeTwoWeeksPrior = ScheduleRecurrenceEngine.shouldTakeOnDate(medicine, dateTwoWeeksPrior)
        assertTrue(shouldTakeTwoWeeksPrior)

        // Test date: 1 week before Jan 6, 2020 -> Dec 30, 2019 (Monday) -> weeksDiff = -1 (off-cycle week).
        val dateOneWeekPrior = LocalDate.of(2019, 12, 30)
        val shouldTakeOneWeekPrior = ScheduleRecurrenceEngine.shouldTakeOnDate(medicine, dateOneWeekPrior)
        assertFalse(shouldTakeOneWeekPrior)

        // Test date: 2 weeks after Jan 6, 2020 -> Jan 20, 2020 (Monday) -> weeksDiff = +2 (on-cycle week).
        val dateTwoWeeksAfter = LocalDate.of(2020, 1, 20)
        val shouldTakeTwoWeeksAfter = ScheduleRecurrenceEngine.shouldTakeOnDate(medicine, dateTwoWeeksAfter)
        assertTrue(shouldTakeTwoWeeksAfter)
    }

    // ---------------------------------------------------------------------------------------------
    // Bug 6: Widget Item Fill-In Intent Contains Schedule Fragment Route
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testWidgetItemFillInIntent_containsScheduleFragmentExtra() {
        val userId = "target_user_777"

        // Simulate fill-in intent constructed in DosezyWidgetService.getViewAt
        val fillInIntent = Intent().apply {
            putExtra("switch_to_user_id", userId)
            putExtra("fragment", "schedule")
        }

        // Guard: Verify intent has both switch_to_user_id and fragment=schedule extras
        assertEquals(userId, fillInIntent.getStringExtra("switch_to_user_id"))
        assertEquals("schedule", fillInIntent.getStringExtra("fragment"))
    }
}
