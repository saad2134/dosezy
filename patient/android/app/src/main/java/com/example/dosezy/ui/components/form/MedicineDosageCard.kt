/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.components.form

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.ui.components.displayName

/**
 * Step 1: Dosage & Strength Card (#145, F13)
 * Displays base intake dosage amount and dosage unit dropdown menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineDosageCard(
    dosage: String,
    onDosageChange: (String) -> Unit,
    selectedDosageUnit: DosageUnit,
    onDosageUnitChange: (DosageUnit) -> Unit,
    modifier: Modifier = Modifier,
    isDosageError: Boolean = false,
    dosageErrorMessage: String? = null
) {
    var dosageUnitExpanded by remember { mutableStateOf(false) }

    // Guard: Intercept back press when dosage unit dropdown is open to close menu instead of popping form step (#BUG-FORM-BACK-DROPDOWN)
    BackHandler(enabled = dosageUnitExpanded) {
        dosageUnitExpanded = false
    }

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
                    imageVector = Icons.Default.Vaccines,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(R.string.form_dosage_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                    isError = isDosageError,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isDosageError) MaterialTheme.colorScheme.error else Color(0xFF1193D4),
                        unfocusedBorderColor = if (isDosageError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    )
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

            // Quick fractional dose chips for discrete units (tablets and capsules)
            if (selectedDosageUnit == DosageUnit.TABLET || selectedDosageUnit == DosageUnit.CAPSULE) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickDoses = listOf("0.5" to "½", "1" to "1", "1.5" to "1½", "2" to "2")
                    quickDoses.forEach { (value, label) ->
                        val isSelected = dosage.trim() == value
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF1193D4).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF1193D4) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onDosageChange(value) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    ),
                                    color = if (isSelected) Color(0xFF1193D4) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            if (isDosageError && !dosageErrorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = dosageErrorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
