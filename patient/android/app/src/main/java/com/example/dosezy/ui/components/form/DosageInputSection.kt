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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import com.example.dosezy.ui.components.displayName
import com.example.dosezy.utils.TimeFormatUtils
import java.time.LocalTime
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DosageInputSection(
    selectedFrequency: FrequencyPattern,
    scheduledTimesList: List<LocalTime>,
    hasDifferentDosages: Boolean,
    onHasDifferentDosagesChange: (Boolean) -> Unit,
    dosage: String,
    onDosageChange: (String) -> Unit,
    selectedDosageUnit: DosageUnit,
    onDosageUnitChange: (DosageUnit) -> Unit,
    perTimeDosages: Map<String, String>,
    onPerTimeDosagesChange: (Map<String, String>) -> Unit,
    timeFormat: TimeFormat,
    modifier: Modifier = Modifier
) {
    var dosageUnitExpanded by remember { mutableStateOf(false) }
    val showDifferentDosagesOption = scheduledTimesList.size > 1 && selectedFrequency != FrequencyPattern.AS_NEEDED

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(18.dp))

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
