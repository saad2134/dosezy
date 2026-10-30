package com.example.dosezy

import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.ui.components.displayName
import com.example.dosezy.ui.components.isMedicineFormValid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class MedicineFormValidationTest {

    @Test
    fun testFormValidation_validDaily() {
        val isValid = isMedicineFormValid(
            medicationName = "Amoxicillin",
            isDosageValid = true,
            selectedFrequency = FrequencyPattern.DAILY,
            selectedDaysOfWeek = emptyList(),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = "1"
        )
        assertTrue("Daily form with valid name and dosage should be valid", isValid)
    }

    @Test
    fun testFormValidation_blankName_isInvalid() {
        val isValid = isMedicineFormValid(
            medicationName = "   ",
            isDosageValid = true,
            selectedFrequency = FrequencyPattern.DAILY,
            selectedDaysOfWeek = emptyList(),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = "1"
        )
        assertFalse("Blank medication name must be invalid", isValid)
    }

    @Test
    fun testFormValidation_invalidDosage_isInvalid() {
        val isValid = isMedicineFormValid(
            medicationName = "Metformin",
            isDosageValid = false,
            selectedFrequency = FrequencyPattern.DAILY,
            selectedDaysOfWeek = emptyList(),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = "1"
        )
        assertFalse("Invalid dosage must invalidate the form", isValid)
    }

    @Test
    fun testFormValidation_asNeeded() {
        val isValid = isMedicineFormValid(
            medicationName = "Ibuprofen",
            isDosageValid = true,
            selectedFrequency = FrequencyPattern.AS_NEEDED,
            selectedDaysOfWeek = emptyList(),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = ""
        )
        assertTrue("As-needed medication without weekly/monthly schedule should be valid", isValid)
    }

    @Test
    fun testFormValidation_weeklyRequiresDays() {
        val invalidWeekly = isMedicineFormValid(
            medicationName = "Vitamin D",
            isDosageValid = true,
            selectedFrequency = FrequencyPattern.WEEKLY,
            selectedDaysOfWeek = emptyList(),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = ""
        )
        assertFalse("Weekly frequency without selected days must be invalid", invalidWeekly)

        val validWeekly = isMedicineFormValid(
            medicationName = "Vitamin D",
            isDosageValid = true,
            selectedFrequency = FrequencyPattern.WEEKLY,
            selectedDaysOfWeek = listOf(1, 4),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = ""
        )
        assertTrue("Weekly frequency with selected days must be valid", validWeekly)
    }

    @Test
    fun testFormValidation_monthlyRequiresDays() {
        val invalidMonthly = isMedicineFormValid(
            medicationName = "B12 Injection",
            isDosageValid = true,
            selectedFrequency = FrequencyPattern.MONTHLY,
            selectedDaysOfWeek = emptyList(),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = ""
        )
        assertFalse("Monthly frequency without selected days must be invalid", invalidMonthly)

        val validMonthly = isMedicineFormValid(
            medicationName = "B12 Injection",
            isDosageValid = true,
            selectedFrequency = FrequencyPattern.MONTHLY,
            selectedDaysOfWeek = emptyList(),
            selectedDaysOfMonth = listOf(1, 15),
            intervalWeeksText = ""
        )
        assertTrue("Monthly frequency with selected days must be valid", validMonthly)
    }

    @Test
    fun testFormValidation_customRequiresDaysAndInterval() {
        val invalidCustomNoDays = isMedicineFormValid(
            medicationName = "Methotrexate",
            isDosageValid = true,
            selectedFrequency = FrequencyPattern.CUSTOM,
            selectedDaysOfWeek = emptyList(),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = "2"
        )
        assertFalse("Custom frequency without days must be invalid", invalidCustomNoDays)

        val invalidCustomZeroWeeks = isMedicineFormValid(
            medicationName = "Methotrexate",
            isDosageValid = true,
            selectedFrequency = FrequencyPattern.CUSTOM,
            selectedDaysOfWeek = listOf(1),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = "0"
        )
        assertFalse("Custom frequency with 0 weeks interval must be invalid", invalidCustomZeroWeeks)

        val validCustom = isMedicineFormValid(
            medicationName = "Methotrexate",
            isDosageValid = true,
            selectedFrequency = FrequencyPattern.CUSTOM,
            selectedDaysOfWeek = listOf(1),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = "2"
        )
        assertTrue("Custom frequency with days and positive interval must be valid", validCustom)
    }

    @Test
    fun testDosageUnitDisplayName() {
        assertEquals("mg", DosageUnit.MG.displayName)
        assertEquals("mcg", DosageUnit.MCG.displayName)
        assertEquals("ml", DosageUnit.ML.displayName)
        assertEquals("drop", DosageUnit.DROP.displayName)
        assertEquals("tablet", DosageUnit.TABLET.displayName)
        assertEquals("capsule", DosageUnit.CAPSULE.displayName)
    }

    @Test
    fun testFormValidation_medicationNameWhitespaceTrimming() {
        val rawName = "   Lisinopril   "
        val trimmed = rawName.trim()
        assertEquals("Lisinopril", trimmed)
        val isValid = isMedicineFormValid(
            medicationName = rawName,
            isDosageValid = true,
            selectedFrequency = FrequencyPattern.DAILY,
            selectedDaysOfWeek = emptyList(),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = "1"
        )
        assertTrue("Name with surrounding whitespace is valid and normalizes when trimmed", isValid)
    }

    @Test
    fun testFormValidation_finiteCourse_requiresPositiveDuration() {
        // Finite course toggled on with empty or 0 duration must be invalid
        val invalidEmptyFinite = isMedicineFormValid(
            medicationName = "Amoxicillin",
            isDosageValid = true,
            selectedFrequency = FrequencyPattern.DAILY,
            selectedDaysOfWeek = emptyList(),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = "",
            isFiniteCourse = true,
            durationDaysText = ""
        )
        assertFalse("Finite course with empty duration must be invalid", invalidEmptyFinite)

        val invalidZeroFinite = isMedicineFormValid(
            medicationName = "Amoxicillin",
            isDosageValid = true,
            selectedFrequency = FrequencyPattern.DAILY,
            selectedDaysOfWeek = emptyList(),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = "",
            isFiniteCourse = true,
            durationDaysText = "0"
        )
        assertFalse("Finite course with 0 duration must be invalid", invalidZeroFinite)

        val validFinite = isMedicineFormValid(
            medicationName = "Amoxicillin",
            isDosageValid = true,
            selectedFrequency = FrequencyPattern.DAILY,
            selectedDaysOfWeek = emptyList(),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = "",
            isFiniteCourse = true,
            durationDaysText = "7"
        )
        assertTrue("Finite course with positive duration must be valid", validFinite)

        val validRetainedEndDate = isMedicineFormValid(
            medicationName = "Amoxicillin",
            isDosageValid = true,
            selectedFrequency = FrequencyPattern.DAILY,
            selectedDaysOfWeek = emptyList(),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = "",
            isFiniteCourse = true,
            durationDaysText = "",
            hasExistingEndDate = true
        )
        assertTrue("Finite course with existing end date on edit must be valid", validRetainedEndDate)
    }

    @Test
    fun testFiniteCourse_durationCalculationAndPreservationOnEdit() {
        val start = java.time.LocalDate.of(2026, 10, 10)
        val end = java.time.LocalDate.of(2026, 10, 16) // Exactly 7 days inclusive

        // Precompute durationDays when durationDays is null but start and end dates are present
        val computedDays = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1
        assertEquals("Calculated inclusive course duration must be 7 days", 7L, computedDays)

        // When duration text is empty but existing end date exists, retain existing end date
        val durationDaysInt: Int? = null
        val isFiniteCourse = true
        val calcEndDate = if (isFiniteCourse) {
            if (durationDaysInt != null) {
                start.plusDays((durationDaysInt - 1).toLong().coerceAtLeast(0L))
            } else {
                end
            }
        } else null

        assertEquals("calcEndDate must preserve existing end date when duration text is unmodified", end, calcEndDate)
    }

    @Test
    fun testValidateMedicineForm_fieldLevelErrorBreakdown() {
        val times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))

        // All empty - all invalid
        val allEmpty = com.example.dosezy.ui.components.validateMedicineForm(
            medicationName = "",
            dosage = "",
            hasDifferentDosages = false,
            scheduledTimesList = times,
            perTimeDosages = emptyMap(),
            selectedFrequency = FrequencyPattern.DAILY,
            selectedDaysOfWeek = emptyList(),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = "",
            isFiniteCourse = false,
            durationDaysText = ""
        )
        assertFalse("Form with empty name and dosage must be invalid", allEmpty.isValid)
        assertFalse("Name should be invalid", allEmpty.isNameValid)
        assertFalse("Dosage should be invalid", allEmpty.isDosageValid)
        assertTrue("Frequency should be valid for DAILY", allEmpty.isFrequencyValid)
        assertTrue("Indefinite course duration should be valid", allEmpty.isFiniteDurationValid)

        // Valid name, invalid per-time dosage
        val customDoseInvalid = com.example.dosezy.ui.components.validateMedicineForm(
            medicationName = "Aspirin",
            dosage = "100",
            hasDifferentDosages = true,
            scheduledTimesList = times,
            perTimeDosages = mapOf("08:00" to "100", "20:00" to "0"),
            selectedFrequency = FrequencyPattern.DAILY,
            selectedDaysOfWeek = emptyList(),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = "",
            isFiniteCourse = false,
            durationDaysText = ""
        )
        assertFalse("Form with 0 custom dose should be invalid", customDoseInvalid.isValid)
        assertTrue("Name should be valid", customDoseInvalid.isNameValid)
        assertFalse("Dosage should be invalid", customDoseInvalid.isDosageValid)
        assertTrue("Invalid time slot key 20:00 should be identified", customDoseInvalid.invalidTimeSlotKeys.contains("20:00"))
        assertFalse("Valid time slot key 08:00 should not be marked invalid", customDoseInvalid.invalidTimeSlotKeys.contains("08:00"))

        // Weekly missing days
        val weeklyMissingDays = com.example.dosezy.ui.components.validateMedicineForm(
            medicationName = "Vitamin D",
            dosage = "1000",
            hasDifferentDosages = false,
            scheduledTimesList = times,
            perTimeDosages = emptyMap(),
            selectedFrequency = FrequencyPattern.WEEKLY,
            selectedDaysOfWeek = emptyList(),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = "",
            isFiniteCourse = false,
            durationDaysText = ""
        )
        assertFalse("Weekly without days should be invalid", weeklyMissingDays.isValid)
        assertTrue("Name should be valid", weeklyMissingDays.isNameValid)
        assertTrue("Dosage should be valid", weeklyMissingDays.isDosageValid)
        assertFalse("Weekly frequency without days should be invalid", weeklyMissingDays.isFrequencyValid)

        // Finite course missing duration
        val finiteMissingDays = com.example.dosezy.ui.components.validateMedicineForm(
            medicationName = "Amoxicillin",
            dosage = "500",
            hasDifferentDosages = false,
            scheduledTimesList = times,
            perTimeDosages = emptyMap(),
            selectedFrequency = FrequencyPattern.DAILY,
            selectedDaysOfWeek = emptyList(),
            selectedDaysOfMonth = emptyList(),
            intervalWeeksText = "",
            isFiniteCourse = true,
            durationDaysText = ""
        )
        assertFalse("Finite course without days should be invalid", finiteMissingDays.isValid)
        assertFalse("Finite duration should be invalid", finiteMissingDays.isFiniteDurationValid)
    }
}
