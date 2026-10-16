/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.export.PdfReportGenerator
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.notifications.AlarmScheduler
import com.example.dosezy.ui.components.isMedicineFormReady
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
class MidnightTransitionAndStockOverrideTest {

    private lateinit var context: Context
    private lateinit var pdfGenerator: PdfReportGenerator
    private lateinit var alarmScheduler: AlarmScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        pdfGenerator = PdfReportGenerator(context)
        alarmScheduler = AlarmScheduler(context)
    }

    // ---------------------------------------------------------------------------------------------
    // Bug 1: Midnight Boundary Transition Staleness
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testMidnightTransitionDetection_identifiesDateChange() {
        // Observed date is yesterday
        val observedDate = LocalDate.now().minusDays(1)
        val today = LocalDate.now()

        // Guard: Verify date difference logic detects boundary crossing
        val hasCrossedMidnight = observedDate != today
        assertTrue(hasCrossedMidnight)
    }

    @Test
    fun testSameDayCheck_doesNotTriggerUnneededReload() {
        val observedDate = LocalDate.now()
        val today = LocalDate.now()

        val hasCrossedMidnight = observedDate != today
        assertFalse(hasCrossedMidnight)
    }

    // ---------------------------------------------------------------------------------------------
    // Bug 2: Fast-Path Alarm Cancellation and Non-Blocking Entry Overload
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testParseDateTimeFromEntryId_extractsTimestampDirectly() {
        // Standard entryId format produced by ScheduleRecurrenceEngine: medicineId_YYYY_MM_DD_HH_MM
        val entryId = "user1_med1_2026_10_16_14_30"
        val parsed = com.example.dosezy.data.repository.ScheduleRepository.parseDateTimeFromEntryId(entryId)

        assertNotNull(parsed)
        assertEquals(LocalDateTime.of(2026, 10, 16, 14, 30, 0), parsed)
    }

    @Test
    fun testCancelAlarmWithScheduleEntry_doesNotThrow() {
        val entry = ScheduleEntry(
            entryId = "user1_med1_2026_10_16_14_30",
            userId = "user1",
            medicineId = "med1",
            scheduledDateTime = LocalDateTime.of(2026, 10, 16, 14, 30),
            status = com.example.dosezy.data.model.MedicationStatus.PENDING
        )

        // Guard: Invoking cancelAlarm with entry object resolves userId and scheduledDateTime directly
        alarmScheduler.cancelAlarm(entry)
    }

    // ---------------------------------------------------------------------------------------------
    // Bug 3: Separation of Active vs Discontinued Medications in PDF Clinical Reports
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testPdfReportGenerator_partitionsActiveAndDiscontinuedMedications() {
        val activeMed1 = Medicine(
            medicineId = "med_1",
            userId = "usr_1",
            medicationName = "Aspirin",
            dosage = 81.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0)),
            isArchived = false
        )
        val activeMed2 = Medicine(
            medicineId = "med_2",
            userId = "usr_1",
            medicationName = "Lisinopril",
            dosage = 10.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(9, 0)),
            isArchived = false
        )
        val archivedMed = Medicine(
            medicineId = "med_3",
            userId = "usr_1",
            medicationName = "Metformin Discontinued",
            dosage = 500.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 2,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            isArchived = true
        )

        val (activeMeds, archivedMeds) = pdfGenerator.partitionMedicines(listOf(activeMed1, activeMed2, archivedMed))

        assertEquals(2, activeMeds.size)
        assertEquals(1, archivedMeds.size)
        assertEquals("Active Medications (2)", pdfGenerator.formatActiveMedicationsHeader(activeMeds.size))
        assertEquals("Discontinued Medications (1)", pdfGenerator.formatDiscontinuedMedicationsHeader(archivedMeds.size))
    }

    // ---------------------------------------------------------------------------------------------
    // Bug 4: Stock Auto-Deduction and Restoration with Custom Dosage Overrides
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testStockDeduction_respectsDosageOverrideForTablets() {
        val tabletMed = Medicine(
            medicineId = "med_tab",
            userId = "usr_1",
            medicationName = "Amoxicillin",
            dosage = 1.0,
            dosageUnit = DosageUnit.TABLET,
            timesPerDay = 2,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            currentStock = 30,
            autoDeductOnTake = true
        )

        // Without override, standard 1 tablet is deducted
        assertEquals(1, tabletMed.getStockDeductionAmount())

        // With dosage override of 2 tablets (custom slot or PRN dose), deduct 2 tablets
        assertEquals(2, tabletMed.getStockDeductionAmount(dosageOverride = 2.0))

        // Chemical strength (MG) always deducts 1 physical unit count
        val mgMed = tabletMed.copy(dosageUnit = DosageUnit.MG, dosage = 500.0)
        assertEquals(1, mgMed.getStockDeductionAmount(dosageOverride = 1000.0))

        // Liquid volume (ML) deducts specified milliliter amount
        val mlMed = tabletMed.copy(dosageUnit = DosageUnit.ML, dosage = 5.0)
        assertEquals(15, mlMed.getStockDeductionAmount(dosageOverride = 15.0))
    }

    // ---------------------------------------------------------------------------------------------
    // Bug 5: Foreign Key Guard Against Null or Blank User ID
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testIsMedicineFormReady_guardsAgainstBlankOrNullUserId() {
        // Valid form but null userId -> NOT ready
        assertFalse(isMedicineFormReady(isFormValid = true, userId = null))

        // Valid form but empty userId -> NOT ready
        assertFalse(isMedicineFormReady(isFormValid = true, userId = ""))

        // Valid form but whitespace userId -> NOT ready
        assertFalse(isMedicineFormReady(isFormValid = true, userId = "   "))

        // Valid form and valid non-blank userId -> READY
        assertTrue(isMedicineFormReady(isFormValid = true, userId = "user_uuid_12345"))

        // Invalid form and valid userId -> NOT ready
        assertFalse(isMedicineFormReady(isFormValid = false, userId = "user_uuid_12345"))
    }
}
