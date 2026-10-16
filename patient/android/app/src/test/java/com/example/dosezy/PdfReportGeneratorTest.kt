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
}
