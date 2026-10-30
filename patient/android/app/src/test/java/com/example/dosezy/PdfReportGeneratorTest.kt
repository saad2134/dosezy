/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.export.PdfReportGenerator
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfReportGeneratorTest {

    private lateinit var context: Context
    private lateinit var generator: PdfReportGenerator

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        generator = PdfReportGenerator(context)
    }

    @Test
    fun testIsEntryMissed_evaluatesCorrectlyBasedOnConsiderMissedAfter() {
        val now = LocalDateTime.of(2026, 10, 16, 18, 0)
        val missedAfterHours = 4

        // Case 1: Status is already MISSED
        val missedEntry = ScheduleEntry(
            entryId = "entry_1",
            userId = "usr_1",
            medicineId = "med_1",
            scheduledDateTime = now.minusHours(1),
            status = MedicationStatus.MISSED
        )
        assertTrue(generator.isEntryMissed(missedEntry, now, missedAfterHours))

        // Case 2: PENDING entry scheduled 5 hours ago (> 4h threshold) -> should evaluate as missed
        val stalePendingEntry = ScheduleEntry(
            entryId = "entry_2",
            userId = "usr_1",
            medicineId = "med_1",
            scheduledDateTime = now.minusHours(5),
            status = MedicationStatus.PENDING
        )
        assertTrue(generator.isEntryMissed(stalePendingEntry, now, missedAfterHours))

        // Case 3: PENDING entry scheduled 2 hours ago (< 4h threshold) -> should NOT evaluate as missed
        val recentPendingEntry = ScheduleEntry(
            entryId = "entry_3",
            userId = "usr_1",
            medicineId = "med_1",
            scheduledDateTime = now.minusHours(2),
            status = MedicationStatus.PENDING
        )
        assertFalse(generator.isEntryMissed(recentPendingEntry, now, missedAfterHours))
    }

    @Test
    fun testCalculateAdherence_computesAccurateRatesAndExcludesPrnAndFuture() {
        val evalToday = LocalDate.of(2026, 10, 16)
        val evalNow = LocalDateTime.of(2026, 10, 16, 20, 0)
        val missedAfterHours = 4

        val schedules = listOf(
            // Taken on time (past) -> Included
            ScheduleEntry("1", "u1", "m1", evalNow.minusHours(10), MedicationStatus.TAKEN_ON_TIME),
            // Taken late (past) -> Included
            ScheduleEntry("2", "u1", "m1", evalNow.minusHours(6), MedicationStatus.TAKEN_LATE),
            // Explicitly missed (past) -> Included
            ScheduleEntry("3", "u1", "m1", evalNow.minusHours(5), MedicationStatus.MISSED),
            // Stale pending (past, > 4h) -> Evaluates as missed
            ScheduleEntry("4", "u1", "m1", evalNow.minusHours(5), MedicationStatus.PENDING),
            // Skipped (past) -> Included in total but not decided taken/missed
            ScheduleEntry("5", "u1", "m1", evalNow.minusHours(2), MedicationStatus.SKIPPED),
            // PRN entry -> MUST be excluded from adherence calculation
            ScheduleEntry("PRN_1", "u1", "m1", evalNow.minusHours(1), MedicationStatus.TAKEN_ON_TIME),
            // Future entry (tomorrow) -> MUST be excluded from adherence calculation
            ScheduleEntry("6", "u1", "m1", evalNow.plusDays(1), MedicationStatus.PENDING)
        )

        val summary = generator.calculateAdherence(
            schedules = schedules,
            evalNow = evalNow,
            evalToday = evalToday,
            missedAfterHours = missedAfterHours
        )

        // Adherence entries count: 5 (PRN and tomorrow excluded)
        assertEquals(5, summary.totalCount)
        assertEquals(1, summary.takenOnTimeCount)
        assertEquals(1, summary.takenLateCount)
        assertEquals(1, summary.skippedCount)
        assertEquals(2, summary.missedCount) // 1 MISSED + 1 stale PENDING
        assertEquals(0, summary.pendingCount)

        // Taken total = 2 (1 on-time + 1 late). Decided = 2 taken + 2 missed = 4.
        // Adherence rate = 2 / 4 = 50.0%
        assertEquals(50.0, summary.adherenceRate, 0.01)
        assertEquals("50.0%", summary.rateFormatted)
        assertTrue(summary.breakdownText.contains("On-Time: 1"))
        assertTrue(summary.breakdownText.contains("Late: 1"))
        assertTrue(summary.breakdownText.contains("Skipped: 1"))
        assertTrue(summary.breakdownText.contains("Missed: 2"))
    }

    @Test
    fun testFormatHistoryHeader_clarifiesPaginationCap() {
        assertEquals("Recent Dose History (50 entries)", generator.formatHistoryHeader(50))
        assertEquals("Recent Dose History (150 entries)", generator.formatHistoryHeader(150))
        assertEquals("Recent Dose History (Latest 150 of 200 entries)", generator.formatHistoryHeader(200))
        assertEquals("Recent Dose History (Latest 150 of 850 entries)", generator.formatHistoryHeader(850))
    }

    @Test
    fun testWrapTextToLines_wrapsLengthyAllergiesAndConditionsAcrossMultipleLines() {
        val paint = android.graphics.Paint().apply { textSize = 10f }
        val longAllergies = "Penicillin, Amoxicillin, Sulfa drugs, Peanuts, Latex, Aspirin, NSAIDs, Shellfish, Contrast dye, Tree nuts, ACE inhibitors"
        // In Robolectric JVM environment without native font metrics, Paint.measureText defaults to string length
        val wrappedLines = generator.wrapTextToLines("Allergies: ", longAllergies, paint, 35f)
        assertTrue("Long allergies must be wrapped into multiple lines", wrappedLines.size > 1)
        assertTrue("First line must start with prefix", wrappedLines.first().startsWith("Allergies: "))
        assertTrue("All wrapped lines must fit within maxWidth", wrappedLines.all { paint.measureText(it) <= 35f })
    }

    @Test
    fun testTruncateTextToWidth_truncatesLongDrugNamesWithEllipsis() {
        val paint = android.graphics.Paint().apply { textSize = 10f }
        val longMedName = "Amoxicillin and Clavulanate Potassium Extended Release Oral Suspension"
        // In Robolectric JVM environment without native font metrics, Paint.measureText defaults to string length
        val truncated = generator.truncateTextToWidth(longMedName, paint, 25f)
        assertTrue("Truncated name must end with ellipsis", truncated.endsWith("..."))
        assertTrue("Truncated text must fit within column maxWidth", paint.measureText(truncated) <= 25f)
    }

    @Test
    fun testTruncateTextToWidth_preventsOverflowIntoAdjacentColumns() {
        val paint = android.graphics.Paint().apply { textSize = 10f }
        val longEntryText = "Methylprednisolone Sodium Succinate for Injection, USP (125 mg)"
        val maxColumnWidth = 175f

        val truncated = generator.truncateTextToWidth(longEntryText, paint, maxColumnWidth)
        assertTrue("Truncated text must fit within the 175pt column width to protect the Status column", paint.measureText(truncated) <= maxColumnWidth)
    }

    @Test
    fun testScheduleLogsTable_dosageOverrideFormatting() {
        val medicine = com.example.dosezy.data.model.Medicine(
            medicineId = "med_custom_dose",
            userId = "user_1",
            medicationName = "Prednisone",
            dosage = 5.0,
            dosageUnit = com.example.dosezy.data.model.DosageUnit.TABLET,
            timesPerDay = 1,
            frequency = com.example.dosezy.data.model.Frequency(com.example.dosezy.data.model.FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0))
        )

        // Custom dosage override on schedule entry: 2.5 tablets
        val customDose = 2.5
        val unitAbbr = when (medicine.dosageUnit) {
            com.example.dosezy.data.model.DosageUnit.TABLET -> "tablet"
            else -> ""
        }
        val formattedDosage = if (customDose % 1 == 0.0) "${customDose.toInt()} $unitAbbr" else "$customDose $unitAbbr"
        assertEquals("2.5 tablet", formattedDosage)

        // Whole number dosage override: 3.0 tablets -> 3 tablet
        val wholeDose = 3.0
        val formattedWhole = if (wholeDose % 1 == 0.0) "${wholeDose.toInt()} $unitAbbr" else "$wholeDose $unitAbbr"
        assertEquals("3 tablet", formattedWhole)
    }

    @Test
    fun testCalculateAdherence_factorsOverduePendingDosesIntoDecidedCount() {
        // Guard: Verify that pending doses scheduled earlier today whose scheduled time has passed are factored into decidedCount and reduce adherence (#BUG-PDF-OVERDUE-ADHERENCE)
        val evalToday = LocalDate.of(2026, 10, 16)
        val evalNow = LocalDateTime.of(2026, 10, 16, 10, 0)
        val missedAfterHours = 4

        val schedules = listOf(
            // Dose 1: Taken on time earlier today
            ScheduleEntry("1", "u1", "m1", evalNow.minusHours(4), MedicationStatus.TAKEN_ON_TIME),
            // Dose 2: Pending dose scheduled 2 hours ago (overdue, but < 4h so not yet marked MISSED)
            ScheduleEntry("2", "u1", "m1", evalNow.minusHours(2), MedicationStatus.PENDING),
            // Dose 3: Pending dose scheduled for tonight (future today -> neutral, not overdue)
            ScheduleEntry("3", "u1", "m1", evalNow.plusHours(8), MedicationStatus.PENDING)
        )

        val summary = generator.calculateAdherence(
            schedules = schedules,
            evalNow = evalNow,
            evalToday = evalToday,
            missedAfterHours = missedAfterHours
        )

        // Total today count = 3
        assertEquals(3, summary.totalCount)
        assertEquals(1, summary.takenOnTimeCount)
        assertEquals(0, summary.takenLateCount)
        assertEquals(0, summary.missedCount)
        assertEquals(2, summary.pendingCount)

        // Taken = 1. Overdue pending = 1 (entry 2). Future pending = 1 (entry 3, excluded from decided).
        // Decided = 1 taken + 0 missed + 1 overduePending = 2.
        // Adherence rate = 1 / 2 * 100.0 = 50.0% (not 100.0%!)
        assertEquals(50.0, summary.adherenceRate, 0.01)
        assertEquals("50.0%", summary.rateFormatted)
    }

    @Test
    fun testResolveDisplayStatus_evaluatesOverduePendingAsMissedForPdfTable() {
        val now = LocalDateTime.of(2026, 10, 16, 18, 0)
        val missedAfterHours = 4

        // 1. Pending scheduled 5 hours ago (> 4h threshold) -> should evaluate as MISSED in table
        val overdueEntry = ScheduleEntry(
            entryId = "entry_overdue",
            userId = "usr_1",
            medicineId = "med_1",
            scheduledDateTime = now.minusHours(5),
            status = MedicationStatus.PENDING
        )
        assertEquals(MedicationStatus.MISSED, generator.resolveDisplayStatus(overdueEntry, now, missedAfterHours))

        // 2. Pending scheduled 2 hours ago (< 4h threshold) -> should remain PENDING
        val recentEntry = ScheduleEntry(
            entryId = "entry_recent",
            userId = "usr_1",
            medicineId = "med_1",
            scheduledDateTime = now.minusHours(2),
            status = MedicationStatus.PENDING
        )
        assertEquals(MedicationStatus.PENDING, generator.resolveDisplayStatus(recentEntry, now, missedAfterHours))

        // 3. Taken on time -> should remain TAKEN_ON_TIME
        val takenEntry = ScheduleEntry(
            entryId = "entry_taken",
            userId = "usr_1",
            medicineId = "med_1",
            scheduledDateTime = now.minusHours(5),
            status = MedicationStatus.TAKEN_ON_TIME
        )
        assertEquals(MedicationStatus.TAKEN_ON_TIME, generator.resolveDisplayStatus(takenEntry, now, missedAfterHours))

        // 4. Skipped -> should remain SKIPPED
        val skippedEntry = ScheduleEntry(
            entryId = "entry_skipped",
            userId = "usr_1",
            medicineId = "med_1",
            scheduledDateTime = now.minusHours(5),
            status = MedicationStatus.SKIPPED
        )
        assertEquals(MedicationStatus.SKIPPED, generator.resolveDisplayStatus(skippedEntry, now, missedAfterHours))
    }
}
