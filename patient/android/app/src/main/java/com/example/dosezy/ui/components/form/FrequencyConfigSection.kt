/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.components.form

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
    modifier: Modifier = Modifier
) {
    var frequencyExpanded by remember { mutableStateOf(false) }

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
    }
}
