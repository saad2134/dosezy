/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.components.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dosezy.R
import com.example.dosezy.data.model.PillShape
import com.example.dosezy.ui.components.MedicinePhotoVisualPicker

/**
 * Card 1: Medicine Visual & Identification Card
 */
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
) {
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
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(R.string.form_med_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                MedicinePhotoVisualPicker(
                    imagePath = medicineImagePath,
                    pillShape = selectedPillShape,
                    pillColor = selectedPillColor,
                    onImageSelected = onImageSelected,
                    onVisualSelected = onVisualSelected
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (medicineImagePath != null) stringResource(R.string.form_change_img) else stringResource(R.string.form_upload_img),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                shape = RoundedCornerShape(16.dp),
                value = medicationName,
                onValueChange = onMedicationNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp),
                placeholder = {
                    Text(
                        stringResource(R.string.form_med_name_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                isError = isNameError,
                supportingText = if (isNameError && !nameErrorMessage.isNullOrBlank()) {
                    { Text(text = nameErrorMessage, color = MaterialTheme.colorScheme.error) }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isNameError) MaterialTheme.colorScheme.error else Color(0xFF1193D4),
                    unfocusedBorderColor = if (isNameError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    focusedLabelColor = Color(0xFF1193D4),
                    unfocusedLabelColor = Color(0xFF6B7280),
                    cursorColor = Color(0xFF1193D4)
                ),
                singleLine = true
            )
        }
    }
}
