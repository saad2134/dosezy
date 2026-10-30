/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.components.form

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dosezy.R
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.TimeFormat
import java.time.LocalTime

/**
 * Card 2: Timings, Frequency & Dosages Card
 * Orchestrates FrequencyConfigSection, ScheduledTimesSection, and DosageInputSection.
 */
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
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(R.string.timings_and_dosages_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Frequency and interval configuration
            FrequencyConfigSection(
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
                onDaysOfMonthChange = onDaysOfMonthChange
            )

            if (isFrequencyError && !frequencyErrorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = frequencyErrorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // 2. Scheduled times and daily presets
            ScheduledTimesSection(
                selectedFrequency = selectedFrequency,
                scheduledTimesList = scheduledTimesList,
                onScheduledTimesChange = onScheduledTimesChange,
                selectedDosePreset = selectedDosePreset,
                onDosePresetChange = onDosePresetChange,
                perTimeDosages = perTimeDosages,
                onPerTimeDosagesChange = onPerTimeDosagesChange,
                timeFormat = timeFormat,
                defaultInitialTime = defaultInitialTime,
                onOpenTimePicker = onOpenTimePicker
            )

            // 3. Single dosage / per-time dosage configuration
            DosageInputSection(
                selectedFrequency = selectedFrequency,
                scheduledTimesList = scheduledTimesList,
                hasDifferentDosages = hasDifferentDosages,
                onHasDifferentDosagesChange = onHasDifferentDosagesChange,
                dosage = dosage,
                onDosageChange = onDosageChange,
                selectedDosageUnit = selectedDosageUnit,
                onDosageUnitChange = onDosageUnitChange,
                perTimeDosages = perTimeDosages,
                onPerTimeDosagesChange = onPerTimeDosagesChange,
                timeFormat = timeFormat,
                isDosageError = isDosageError,
                dosageErrorMessage = dosageErrorMessage,
                invalidTimeSlotKeys = invalidTimeSlotKeys
            )
        }
    }
}
