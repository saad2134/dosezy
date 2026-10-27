/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.PillShape
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.data.model.normalizeArabicDigits
import java.time.LocalDate
import java.time.LocalTime

val DosageUnit.displayName: String
    get() = when (this) {
        DosageUnit.MG -> "mg"
        DosageUnit.MCG -> "mcg"
        DosageUnit.ML -> "ml"
        DosageUnit.DROP -> "drop"
        DosageUnit.TABLET -> "tablet"
        DosageUnit.CAPSULE -> "capsule"
        DosageUnit.MEQ -> "mEq"
        DosageUnit.PUFF -> "puff"
        DosageUnit.AMPULE -> "ampule"
        DosageUnit.SPRAY -> "spray"
    }

fun isMedicineFormValid(
    medicationName: String,
    isDosageValid: Boolean,
    selectedFrequency: FrequencyPattern,
    selectedDaysOfWeek: List<Int>,
    selectedDaysOfMonth: List<Int>,
    intervalWeeksText: String,
    isFiniteCourse: Boolean = false,
    durationDaysText: String = "",
    hasExistingEndDate: Boolean = false
): Boolean {
    // Guard: Validate that finite courses specify a positive duration or retain an existing valid end date to prevent silent conversion to indefinite schedules
    val isFiniteValid = !isFiniteCourse || hasExistingEndDate || ((durationDaysText.normalizeArabicDigits().toIntOrNull() ?: 0) > 0)
    return isFiniteValid && medicationName.isNotBlank() && isDosageValid && (
        selectedFrequency == FrequencyPattern.DAILY ||
        selectedFrequency == FrequencyPattern.AS_NEEDED ||
        selectedFrequency == FrequencyPattern.EVERY_X_HOURS ||
        selectedFrequency == FrequencyPattern.EVERY_X_DAYS ||
        (selectedFrequency == FrequencyPattern.WEEKLY && selectedDaysOfWeek.isNotEmpty()) ||
        (selectedFrequency == FrequencyPattern.MONTHLY && selectedDaysOfMonth.isNotEmpty()) ||
        (selectedFrequency == FrequencyPattern.CUSTOM && selectedDaysOfWeek.isNotEmpty() && (intervalWeeksText.normalizeArabicDigits().toIntOrNull() ?: 0) >= 1)
    )
}

data class MedicineFormValidationResult(
    val isValid: Boolean,
    val isNameValid: Boolean,
    val isDosageValid: Boolean,
    val isFrequencyValid: Boolean,
    val isFiniteDurationValid: Boolean,
    val invalidTimeSlotKeys: Set<String> = emptySet()
)

fun validateMedicineForm(
    medicationName: String,
    dosage: String,
    hasDifferentDosages: Boolean,
    scheduledTimesList: List<LocalTime>,
    perTimeDosages: Map<String, String>,
    selectedFrequency: FrequencyPattern,
    selectedDaysOfWeek: List<Int>,
    selectedDaysOfMonth: List<Int>,
    intervalWeeksText: String,
    isFiniteCourse: Boolean = false,
    durationDaysText: String = "",
    hasExistingEndDate: Boolean = false
): MedicineFormValidationResult {
    val isNameValid = medicationName.isNotBlank()

    val invalidSlots = if (hasDifferentDosages && scheduledTimesList.size > 1 && selectedFrequency != FrequencyPattern.AS_NEEDED) {
        scheduledTimesList.mapNotNull { t ->
            val key = String.format(java.util.Locale.US, "%02d:%02d", t.hour, t.minute)
            val valueStr = perTimeDosages[key] ?: (if (dosage.isNotBlank()) dosage else null)
            val d = valueStr?.normalizeArabicDigits()?.toDoubleOrNull()
            if (d == null || d <= 0.0) key else null
        }.toSet()
    } else {
        emptySet()
    }

    val isDosageValid = if (hasDifferentDosages && scheduledTimesList.size > 1 && selectedFrequency != FrequencyPattern.AS_NEEDED) {
        invalidSlots.isEmpty()
    } else {
        val d = dosage.normalizeArabicDigits().toDoubleOrNull()
        d != null && d > 0.0
    }

    val isFrequencyValid = when (selectedFrequency) {
        FrequencyPattern.DAILY, FrequencyPattern.AS_NEEDED, FrequencyPattern.EVERY_X_HOURS, FrequencyPattern.EVERY_X_DAYS -> true
        FrequencyPattern.WEEKLY -> selectedDaysOfWeek.isNotEmpty()
        FrequencyPattern.MONTHLY -> selectedDaysOfMonth.isNotEmpty()
        FrequencyPattern.CUSTOM -> selectedDaysOfWeek.isNotEmpty() && (intervalWeeksText.normalizeArabicDigits().toIntOrNull() ?: 0) >= 1
    }

    val isFiniteValid = !isFiniteCourse || hasExistingEndDate || ((durationDaysText.normalizeArabicDigits().toIntOrNull() ?: 0) > 0)

    val isValid = isNameValid && isDosageValid && isFrequencyValid && isFiniteValid

    return MedicineFormValidationResult(
        isValid = isValid,
        isNameValid = isNameValid,
        isDosageValid = isDosageValid,
        isFrequencyValid = isFrequencyValid,
        isFiniteDurationValid = isFiniteValid,
        invalidTimeSlotKeys = invalidSlots
    )
}

// Guard: Validate that target userId is present and non-blank to prevent SQLiteConstraintException foreign key crash
fun isMedicineFormReady(isFormValid: Boolean, userId: String?): Boolean {
    return isFormValid && !userId.isNullOrBlank()
}

// -------------------------------------------------------------------------------------------------
// Backward-compatible delegates to focused components in com.example.dosezy.ui.components.form
// -------------------------------------------------------------------------------------------------

@Composable
fun MedicineIdentificationCard(
    medicineImagePath: String?,
    selectedPillShape: PillShape,
    selectedPillColor: String,
    medicationName: String,
    onImageSelected: (String?) -> Unit,
    onVisualSelected: (PillShape, String) -> Unit,
    onMedicationNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isNameError: Boolean = false,
    nameErrorMessage: String? = null
) = com.example.dosezy.ui.components.form.MedicineIdentificationCard(
    medicineImagePath = medicineImagePath,
    selectedPillShape = selectedPillShape,
    selectedPillColor = selectedPillColor,
    medicationName = medicationName,
    onImageSelected = onImageSelected,
    onVisualSelected = onVisualSelected,
    onMedicationNameChange = onMedicationNameChange,
    modifier = modifier,
    isNameError = isNameError,
    nameErrorMessage = nameErrorMessage
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MedicineTimingsAndDosagesCard(
    selectedFrequency: FrequencyPattern,
    onFrequencyChange: (FrequencyPattern) -> Unit,
    intervalHoursText: String,
    onIntervalHoursChange: (String) -> Unit,
    intervalDaysText: String,
    onIntervalDaysChange: (String) -> Unit,
    intervalWeeksText: String,
    onIntervalWeeksChange: (String) -> Unit,
    selectedDaysOfWeek: List<Int>,
    onDaysOfWeekChange: (List<Int>) -> Unit,
    selectedDaysOfMonth: List<Int>,
    onDaysOfMonthChange: (List<Int>) -> Unit,
    scheduledTimesList: List<LocalTime>,
    onScheduledTimesChange: (List<LocalTime>) -> Unit,
    selectedDosePreset: String,
    onDosePresetChange: (String) -> Unit,
    hasDifferentDosages: Boolean,
    onHasDifferentDosagesChange: (Boolean) -> Unit,
    perTimeDosages: Map<String, String>,
    onPerTimeDosagesChange: (Map<String, String>) -> Unit,
    dosage: String,
    onDosageChange: (String) -> Unit,
    selectedDosageUnit: DosageUnit,
    onDosageUnitChange: (DosageUnit) -> Unit,
    timeFormat: TimeFormat,
    defaultInitialTime: LocalTime,
    onOpenTimePicker: (timeIndex: Int?, initialTime: LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    isDosageError: Boolean = false,
    dosageErrorMessage: String? = null,
    invalidTimeSlotKeys: Set<String> = emptySet(),
    isFrequencyError: Boolean = false,
    frequencyErrorMessage: String? = null
) = com.example.dosezy.ui.components.form.MedicineTimingsAndDosagesCard(
    selectedFrequency = selectedFrequency,
    onFrequencyChange = onFrequencyChange,
    intervalHoursText = intervalHoursText,
    onIntervalHoursChange = onIntervalHoursChange,
    intervalDaysText = intervalDaysText,
    onIntervalDaysChange = onIntervalDaysChange,
    intervalWeeksText = intervalWeeksText,
    onIntervalWeeksChange = onIntervalWeeksChange,
    selectedDaysOfWeek = selectedDaysOfWeek,
    onDaysOfWeekChange = onDaysOfWeekChange,
    selectedDaysOfMonth = selectedDaysOfMonth,
    onDaysOfMonthChange = onDaysOfMonthChange,
    scheduledTimesList = scheduledTimesList,
    onScheduledTimesChange = onScheduledTimesChange,
    selectedDosePreset = selectedDosePreset,
    onDosePresetChange = onDosePresetChange,
    hasDifferentDosages = hasDifferentDosages,
    onHasDifferentDosagesChange = onHasDifferentDosagesChange,
    perTimeDosages = perTimeDosages,
    onPerTimeDosagesChange = onPerTimeDosagesChange,
    dosage = dosage,
    onDosageChange = onDosageChange,
    selectedDosageUnit = selectedDosageUnit,
    onDosageUnitChange = onDosageUnitChange,
    timeFormat = timeFormat,
    defaultInitialTime = defaultInitialTime,
    onOpenTimePicker = onOpenTimePicker,
    modifier = modifier,
    isDosageError = isDosageError,
    dosageErrorMessage = dosageErrorMessage,
    invalidTimeSlotKeys = invalidTimeSlotKeys,
    isFrequencyError = isFrequencyError,
    frequencyErrorMessage = frequencyErrorMessage
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MedicineCourseDurationCard(
    isFiniteCourse: Boolean,
    onIsFiniteCourseChange: (Boolean) -> Unit,
    selectedStartDate: LocalDate,
    onStartDateChange: (LocalDate) -> Unit,
    durationDaysText: String,
    onDurationDaysChange: (String) -> Unit,
    calculatedEndDate: LocalDate?,
    modifier: Modifier = Modifier,
    isDurationError: Boolean = false,
    durationErrorMessage: String? = null
) = com.example.dosezy.ui.components.form.MedicineCourseDurationCard(
    isFiniteCourse = isFiniteCourse,
    onIsFiniteCourseChange = onIsFiniteCourseChange,
    selectedStartDate = selectedStartDate,
    onStartDateChange = onStartDateChange,
    durationDaysText = durationDaysText,
    onDurationDaysChange = onDurationDaysChange,
    calculatedEndDate = calculatedEndDate,
    modifier = modifier,
    isDurationError = isDurationError,
    durationErrorMessage = durationErrorMessage
)

@Composable
fun MedicineStockInventoryCard(
    currentStockText: String,
    onCurrentStockChange: (String) -> Unit,
    refillThresholdText: String,
    onRefillThresholdChange: (String) -> Unit,
    modifier: Modifier = Modifier
) = com.example.dosezy.ui.components.form.MedicineStockInventoryCard(
    currentStockText = currentStockText,
    onCurrentStockChange = onCurrentStockChange,
    refillThresholdText = refillThresholdText,
    onRefillThresholdChange = onRefillThresholdChange,
    modifier = modifier
)

@Composable
fun MedicineNotesCard(
    doctorNotes: String,
    onDoctorNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) = com.example.dosezy.ui.components.form.MedicineNotesCard(
    doctorNotes = doctorNotes,
    onDoctorNotesChange = onDoctorNotesChange,
    modifier = modifier
)

@Composable
fun MedicineReminderStyleCard(
    selectedReminderType: com.example.dosezy.data.model.ReminderType?,
    onReminderTypeChange: (com.example.dosezy.data.model.ReminderType?) -> Unit,
    profileDefaultType: com.example.dosezy.data.model.ReminderType = com.example.dosezy.data.model.ReminderType.ALARM,
    modifier: Modifier = Modifier
) = com.example.dosezy.ui.components.form.MedicineReminderStyleCard(
    selectedReminderType = selectedReminderType,
    onReminderTypeChange = onReminderTypeChange,
    profileDefaultType = profileDefaultType,
    modifier = modifier
)

