/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dosezy.R
import com.example.dosezy.data.model.Language
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.utils.LocaleHelper
import com.example.dosezy.utils.TimeFormatUtils
import java.time.LocalTime

/**
 * Dosezy 12-First Grid Time Picker Dialog
 *
 * Designed specifically for high accessibility and tremor safety:
 * - 48dp+ tap targets arranged in a familiar 4x3 grid
 * - 12-first layout (12 at the top, followed by 1..11)
 * - Explicit AM / PM segmented filter chips
 * - Quick-select minute chips (00, 15, 30, 45) plus fine slider adjustment
 * - Fully styled for Material 3 light/dark mode
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridTimePickerDialog(
    initialTime: LocalTime,
    timeFormat: TimeFormat = TimeFormat.HOUR_12,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTimeState by remember { mutableStateOf(initialTime) }

    val is12Hour = timeFormat == TimeFormat.HOUR_12
    val isAm = selectedTimeState.hour < 12
    val hour12Display = run {
        val h = selectedTimeState.hour % 12
        if (h == 0) 12 else h
    }

    val activeLocale = remember {
        LocaleHelper.getLocale(Language.SYSTEM)
    }
    val displayString = TimeFormatUtils.formatLocalTime(selectedTimeState, timeFormat, activeLocale)

    AlertDialog(
        onDismissRequest = onDismiss,
        tonalElevation = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.form_time), style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Selected: $displayString",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (is12Hour) {
                    // AM / PM Toggle chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isAm,
                            onClick = {
                                if (!isAm) {
                                    selectedTimeState = selectedTimeState.minusHours(12)
                                }
                            },
                            label = { Text("AM", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        FilterChip(
                            selected = !isAm,
                            onClick = {
                                if (isAm) {
                                    selectedTimeState = selectedTimeState.plusHours(12)
                                }
                            },
                            label = { Text("PM", fontWeight = FontWeight.Bold) }
                        )
                    }

                    Text(
                        stringResource(R.string.time_picker_hour_12_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // 12-hour grid with 12 FIRST: 12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
                    val hours12 = listOf(12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
                    val rows12 = hours12.chunked(4)

                    rows12.forEach { rowHours ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowHours.forEach { h12 ->
                                val isSelected = hour12Display == h12
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            val hour24 = if (isAm) {
                                                if (h12 == 12) 0 else h12
                                            } else {
                                                if (h12 == 12) 12 else h12 + 12
                                            }
                                            selectedTimeState = selectedTimeState.withHour(hour24)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = h12.toString(),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        stringResource(R.string.time_picker_hour_24_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val hours24 = (0..23).toList().chunked(6)
                    hours24.forEach { rowHours ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowHours.forEach { h24 ->
                                val isSelected = selectedTimeState.hour == h24
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            selectedTimeState = selectedTimeState.withHour(h24)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format("%02d", h24),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    stringResource(R.string.time_picker_minute_label, selectedTimeState.minute),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Quick Minute Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0, 15, 30, 45).forEach { min ->
                        val isSelected = selectedTimeState.minute == min
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTimeState = selectedTimeState.withMinute(min) },
                            label = { Text(":$min", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Slider(
                    value = selectedTimeState.minute.toFloat(),
                    onValueChange = { newMinute ->
                        selectedTimeState = selectedTimeState.withMinute(newMinute.toInt().coerceIn(0, 59))
                    },
                    valueRange = 0f..59f
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onTimeSelected(selectedTimeState) }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
