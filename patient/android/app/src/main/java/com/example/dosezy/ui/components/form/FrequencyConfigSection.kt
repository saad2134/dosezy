/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.components.form

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.getLocalizedName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencyConfigSection(
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
    modifier: Modifier = Modifier,
    isRollingInterval: Boolean = false,
    onRollingIntervalChange: (Boolean) -> Unit = {},
    cycleActiveDaysText: String = "21",
    onCycleActiveDaysChange: (String) -> Unit = {},
    cycleRestDaysText: String = "7",
    onCycleRestDaysChange: (String) -> Unit = {},
    asNeededIntervalHoursText: String = "",
    onAsNeededIntervalHoursChange: (String) -> Unit = {},
    asNeededNotifyEligible: Boolean = true,
    onAsNeededNotifyEligibleChange: (Boolean) -> Unit = {}
) {
    var frequencyExpanded by remember { mutableStateOf(false) }

    // Guard: Intercept back navigation when frequency dropdown is expanded so back press closes menu instead of exiting medication form (#BUG-FORM-BACK-DROPDOWN)
    BackHandler(enabled = frequencyExpanded) {
        frequencyExpanded = false
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Frequency Dropdown
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

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.freq_as_needed_interval_title),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = stringResource(R.string.freq_as_needed_interval_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Guard: Interval preset chips with squircle corners (None, 4h, 6h, 8h, Custom) (#142)
            val currentIntervalInt = asNeededIntervalHoursText.toIntOrNull()
            val isNone = asNeededIntervalHoursText.isBlank() || currentIntervalInt == 0
            val is4h = currentIntervalInt == 4
            val is6h = currentIntervalInt == 6
            val is8h = currentIntervalInt == 8
            val isCustomPrn = !isNone && !is4h && !is6h && !is8h

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple(stringResource(R.string.freq_as_needed_interval_none), isNone) {
                        onAsNeededIntervalHoursChange("")
                    },
                    Triple("4 hrs", is4h) {
                        onAsNeededIntervalHoursChange("4")
                    },
                    Triple("6 hrs", is6h) {
                        onAsNeededIntervalHoursChange("6")
                    },
                    Triple("8 hrs", is8h) {
                        onAsNeededIntervalHoursChange("8")
                    },
                    Triple(stringResource(R.string.freq_cycle_preset_custom), isCustomPrn) {
                        if (isNone) onAsNeededIntervalHoursChange("3")
                    }
                ).forEach { (label, isSelected, onClick) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { onClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (!isNone) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = asNeededIntervalHoursText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) onAsNeededIntervalHoursChange(it) },
                    label = { Text(stringResource(R.string.freq_as_needed_interval_hours_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1193D4),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAsNeededNotifyEligibleChange(!asNeededNotifyEligible) }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.freq_as_needed_notify_eligible),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Switch(
                        checked = asNeededNotifyEligible,
                        onCheckedChange = onAsNeededNotifyEligibleChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF1193D4)
                        )
                    )
                }
            }
        }

        if (selectedFrequency == FrequencyPattern.CYCLICAL) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.freq_cyclical_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.cycle_presets_label),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Guard: Preset chips with squircle corners (21/7, 24/4, Custom) (#141)
            val is21_7 = cycleActiveDaysText == "21" && cycleRestDaysText == "7"
            val is24_4 = cycleActiveDaysText == "24" && cycleRestDaysText == "4"
            val isCustomCycle = !is21_7 && !is24_4

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(stringResource(R.string.freq_cycle_preset_21_7), is21_7) {
                        onCycleActiveDaysChange("21")
                        onCycleRestDaysChange("7")
                    },
                    Triple(stringResource(R.string.freq_cycle_preset_24_4), is24_4) {
                        onCycleActiveDaysChange("24")
                        onCycleRestDaysChange("4")
                    },
                    Triple(stringResource(R.string.freq_cycle_preset_custom), isCustomCycle) {
                        // User can directly edit the text fields below
                    }
                ).forEach { (label, isSelected, onClick) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { onClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = cycleActiveDaysText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) onCycleActiveDaysChange(it) },
                    label = { Text(stringResource(R.string.freq_cycle_active_days)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1193D4),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                OutlinedTextField(
                    value = cycleRestDaysText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) onCycleRestDaysChange(it) },
                    label = { Text(stringResource(R.string.freq_cycle_rest_days)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1193D4),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
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
            Spacer(modifier = Modifier.height(6.dp))
            // Guard: Clarify that medication will only be scheduled on highlighted days of the week (#134)
            Text(
                text = stringResource(R.string.freq_weekly_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

        // Rolling intake interval switch for interval-compatible regimens (#119)
        if (selectedFrequency == FrequencyPattern.DAILY ||
            selectedFrequency == FrequencyPattern.EVERY_X_HOURS ||
            selectedFrequency == FrequencyPattern.EVERY_X_DAYS
        ) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onRollingIntervalChange(!isRollingInterval) }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.freq_rolling_interval_title),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.freq_rolling_interval_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isRollingInterval,
                    onCheckedChange = onRollingIntervalChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF1193D4)
                    )
                )
            }
        }
    }
}
