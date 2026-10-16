/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.components.form

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dosezy.R
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.data.model.getLocalizedName
import com.example.dosezy.ui.components.displayName
import com.example.dosezy.utils.TimeFormatUtils
import java.time.LocalTime
import java.util.Locale

/**
 * Card 2: Timings, Frequency & Dosages Card
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
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
) {
    var frequencyExpanded by remember { mutableStateOf(false) }
    var dosageUnitExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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

            // 1. Frequency Dropdown
            Text(
                text = stringResource(R.string.form_frequency),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            ExposedDropdownMenuBox(
                expanded = frequencyExpanded,
                onExpandedChange = { frequencyExpanded = !frequencyExpanded }
            ) {
                OutlinedTextField(
                    shape = RoundedCornerShape(16.dp),
                    value = selectedFrequency.getLocalizedName(),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1193D4),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded)
                    }
                )

                ExposedDropdownMenu(
                    expanded = frequencyExpanded,
                    onDismissRequest = { frequencyExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    FrequencyPattern.values().forEach { frequency ->
                        DropdownMenuItem(
                            text = { Text(frequency.getLocalizedName()) },
                            onClick = {
                                onFrequencyChange(frequency)
                                frequencyExpanded = false
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        )
                    }
                }
            }

            // Specific frequency configurations
            if (selectedFrequency == FrequencyPattern.AS_NEEDED) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.freq_as_needed_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (selectedFrequency == FrequencyPattern.EVERY_X_HOURS) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = intervalHoursText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) onIntervalHoursChange(it) },
                    label = { Text(stringResource(R.string.interval_hours_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1193D4),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            if (selectedFrequency == FrequencyPattern.EVERY_X_DAYS) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = intervalDaysText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) onIntervalDaysChange(it) },
                    label = { Text(stringResource(R.string.interval_days_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1193D4),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            val daysOfWeekNames = listOf(
                stringResource(R.string.day_mon),
                stringResource(R.string.day_tue),
                stringResource(R.string.day_wed),
                stringResource(R.string.day_thu),
                stringResource(R.string.day_fri),
                stringResource(R.string.day_sat),
                stringResource(R.string.day_sun)
            )

            if (selectedFrequency == FrequencyPattern.CUSTOM) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = intervalWeeksText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) onIntervalWeeksChange(it) },
                    label = { Text(stringResource(R.string.custom_recurrence_interval_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1193D4),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.form_select_days_week),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    daysOfWeekNames.forEachIndexed { index, name ->
                        val dayValue = index + 1
                        val isSelected = selectedDaysOfWeek.contains(dayValue)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable {
                                    onDaysOfWeekChange(
                                        if (isSelected) selectedDaysOfWeek - dayValue else selectedDaysOfWeek + dayValue
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (selectedFrequency == FrequencyPattern.WEEKLY) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.form_select_days_week),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    daysOfWeekNames.forEachIndexed { index, name ->
                        val dayValue = index + 1
                        val isSelected = selectedDaysOfWeek.contains(dayValue)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable {
                                    onDaysOfWeekChange(
                                        if (isSelected) selectedDaysOfWeek - dayValue else selectedDaysOfWeek + dayValue
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (selectedFrequency == FrequencyPattern.MONTHLY) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.form_select_days_month),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                val rows = (1..31).chunked(7)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    rows.forEach { rowDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowDays.forEach { day ->
                                val isSelected = selectedDaysOfMonth.contains(day)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 40.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .clickable {
                                            onDaysOfMonthChange(
                                                if (isSelected) selectedDaysOfMonth - day else selectedDaysOfMonth + day
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$day",
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (rowDays.size < 7) {
                                repeat(7 - rowDays.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Presets and custom times (only for scheduled frequencies, not AS_NEEDED)
            if (selectedFrequency != FrequencyPattern.AS_NEEDED) {
                Spacer(modifier = Modifier.height(18.dp))

                if (selectedFrequency == FrequencyPattern.DAILY) {
                    Text(
                        text = stringResource(R.string.daily_frequency_presets_title),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    val presets = listOf(
                        "1x" to listOf(defaultInitialTime),
                        "2x" to listOf(defaultInitialTime, defaultInitialTime.plusHours(12)).sorted(),
                        "3x" to listOf(defaultInitialTime, defaultInitialTime.plusHours(6), defaultInitialTime.plusHours(12)).sorted(),
                        "4x" to listOf(defaultInitialTime, defaultInitialTime.plusHours(4), defaultInitialTime.plusHours(8), defaultInitialTime.plusHours(12)).sorted()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { (label, times) ->
                            val isSelected = selectedDosePreset == label
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clickable {
                                        onDosePresetChange(label)
                                        onScheduledTimesChange(times)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Scheduled times grid / list
                Text(
                    text = stringResource(R.string.scheduled_dosing_times_title),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                scheduledTimesList.chunked(2).forEachIndexed { rowIndex, pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEachIndexed { colIndex, time ->
                            val itemIndex = rowIndex * 2 + colIndex
                            val formatted = TimeFormatUtils.formatLocalTime(time, timeFormat)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onOpenTimePicker(itemIndex, time)
                                    },
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = formatted,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 6.dp)
                                    )
                                    if (scheduledTimesList.size > 1) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    val newList = scheduledTimesList - time
                                                    onScheduledTimesChange(newList)
                                                    val removedKey = String.format(Locale.US, "%02d:%02d", time.hour, time.minute)
                                                    val updated = perTimeDosages.toMutableMap()
                                                    updated.remove(removedKey)
                                                    onPerTimeDosagesChange(updated)
                                                    onDosePresetChange("Custom")
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(R.string.med_time_remove_cd),
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (pair.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Add Time Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable {
                            val nextTime = scheduledTimesList.lastOrNull()?.plusHours(4) ?: defaultInitialTime
                            onOpenTimePicker(null, nextTime)
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.med_time_add_btn),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val showDifferentDosagesOption = scheduledTimesList.size > 1 && selectedFrequency != FrequencyPattern.AS_NEEDED

            // Guard: Toggle switch for different dosage per time, placed ABOVE single dose with enlarged typography
            if (showDifferentDosagesOption) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onHasDifferentDosagesChange(!hasDifferentDosages) }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.med_different_dosages_toggle),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    )
                    Switch(
                        checked = hasDifferentDosages,
                        onCheckedChange = { onHasDifferentDosagesChange(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF1193D4)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Guard: Match container heights (56.dp) between Time surface and dose text field to prevent baseline layout misalignment; hide single dose when different dosages is enabled to prevent confusing duplicate inputs
            if (hasDifferentDosages && showDifferentDosagesOption) {
                ExposedDropdownMenuBox(
                    expanded = dosageUnitExpanded,
                    onExpandedChange = { dosageUnitExpanded = !dosageUnitExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        shape = RoundedCornerShape(16.dp),
                        value = selectedDosageUnit.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.form_dosage_unit)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1193D4),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dosageUnitExpanded)
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = dosageUnitExpanded,
                        onDismissRequest = { dosageUnitExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DosageUnit.values().forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.displayName) },
                                onClick = {
                                    onDosageUnitChange(unit)
                                    dosageUnitExpanded = false
                                },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Per-time doses list with aligned, equal-height (56.dp) time and dose containers
                scheduledTimesList.forEach { time ->
                    val key = String.format(Locale.US, "%02d:%02d", time.hour, time.minute)
                    val timeStr = TimeFormatUtils.formatLocalTime(time, timeFormat)
                    val currentVal = perTimeDosages[key] ?: (if (dosage.isNotBlank()) dosage else "")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .width(105.dp)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Guard: Use suffix instead of floating label and defaultMinSize(minHeight = 56.dp) so text values and decimals are vertically centered and never clipped at bottom
                        OutlinedTextField(
                            value = currentVal,
                            onValueChange = { newVal ->
                                val updated = perTimeDosages.toMutableMap()
                                updated[key] = newVal
                                onPerTimeDosagesChange(updated)
                            },
                            suffix = {
                                Text(
                                    text = selectedDosageUnit.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            placeholder = { Text(if (dosage.isNotBlank()) dosage else "0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 56.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1193D4),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            } else {
                // Single dose and unit row when different dosages is disabled
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        shape = RoundedCornerShape(16.dp),
                        value = dosage,
                        onValueChange = onDosageChange,
                        modifier = Modifier
                            .weight(1.3f)
                            .defaultMinSize(minHeight = 56.dp),
                        label = { Text(stringResource(R.string.form_dosage)) },
                        placeholder = { Text(stringResource(R.string.form_dosage_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1193D4),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        singleLine = true
                    )

                    ExposedDropdownMenuBox(
                        expanded = dosageUnitExpanded,
                        onExpandedChange = { dosageUnitExpanded = !dosageUnitExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            shape = RoundedCornerShape(16.dp),
                            value = selectedDosageUnit.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.form_dosage_unit)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1193D4),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dosageUnitExpanded)
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = dosageUnitExpanded,
                            onDismissRequest = { dosageUnitExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DosageUnit.values().forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.displayName) },
                                    onClick = {
                                        onDosageUnitChange(unit)
                                        dosageUnitExpanded = false
                                    },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
