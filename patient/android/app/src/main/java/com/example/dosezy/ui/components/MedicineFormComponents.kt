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
    }

fun isMedicineFormValid(
    medicationName: String,
    isDosageValid: Boolean,
    selectedFrequency: FrequencyPattern,
    selectedDaysOfWeek: List<Int>,
    selectedDaysOfMonth: List<Int>,
    intervalWeeksText: String
): Boolean {
    return medicationName.isNotBlank() && isDosageValid && (
        selectedFrequency == FrequencyPattern.DAILY ||
        selectedFrequency == FrequencyPattern.AS_NEEDED ||
        selectedFrequency == FrequencyPattern.EVERY_X_HOURS ||
        selectedFrequency == FrequencyPattern.EVERY_X_DAYS ||
        (selectedFrequency == FrequencyPattern.WEEKLY && selectedDaysOfWeek.isNotEmpty()) ||
        (selectedFrequency == FrequencyPattern.MONTHLY && selectedDaysOfMonth.isNotEmpty()) ||
        (selectedFrequency == FrequencyPattern.CUSTOM && selectedDaysOfWeek.isNotEmpty() && (intervalWeeksText.normalizeArabicDigits().toIntOrNull() ?: 0) >= 1)
    )
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
    modifier: Modifier = Modifier
) = com.example.dosezy.ui.components.form.MedicineIdentificationCard(
    medicineImagePath = medicineImagePath,
    selectedPillShape = selectedPillShape,
    selectedPillColor = selectedPillColor,
    medicationName = medicationName,
    onImageSelected = onImageSelected,
    onVisualSelected = onVisualSelected,
    onMedicationNameChange = onMedicationNameChange,
    modifier = modifier
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
    modifier: Modifier = Modifier
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
    modifier = modifier
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
    modifier: Modifier = Modifier
) = com.example.dosezy.ui.components.form.MedicineCourseDurationCard(
    isFiniteCourse = isFiniteCourse,
    onIsFiniteCourseChange = onIsFiniteCourseChange,
    selectedStartDate = selectedStartDate,
    onStartDateChange = onStartDateChange,
    durationDaysText = durationDaysText,
    onDurationDaysChange = onDurationDaysChange,
    calculatedEndDate = calculatedEndDate,
    modifier = modifier
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
