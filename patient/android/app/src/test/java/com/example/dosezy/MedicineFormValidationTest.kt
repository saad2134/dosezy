package com.example.dosezy

import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.ui.components.displayName
import com.example.dosezy.ui.components.isMedicineFormValid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
