/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dosezy.R
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleWithMedicine
import com.example.dosezy.data.model.User
import com.example.dosezy.ui.screens.MedicineImage
import com.example.dosezy.utils.TimeCalculationUtils

@Composable
fun HomeMedicationCard(
    scheduleWithMedicine: ScheduleWithMedicine,
    currentDateTime: java.time.LocalDateTime,
    currentUser: User?,
    onMarkAsTaken: (String) -> Unit,
    onMarkAsLate: (String) -> Unit,
    onUndo: (String) -> Unit = {},
    onSkip: (String) -> Unit = {},
    onEditNote: (String) -> Unit = {}
) {
    val entry = scheduleWithMedicine.scheduleEntry
    val medicine = scheduleWithMedicine.medicine
    val isTaken = entry.status == MedicationStatus.TAKEN_ON_TIME ||
            entry.status == MedicationStatus.TAKEN_LATE
    val isSkipped = entry.status == MedicationStatus.SKIPPED

    val lateAfter = currentUser?.considerLateAfter ?: 3
    val missedAfter = currentUser?.considerMissedAfter ?: 6

    val isPassed = currentDateTime.isAfter(entry.scheduledDateTime)

    // Calculate isMissed dynamically if it passed the threshold, in addition to DB status
    val isMissed = entry.status == MedicationStatus.MISSED || (!isTaken && !isSkipped && isPassed && TimeCalculationUtils.isMissed(
        entry.scheduledDateTime,
        currentDateTime,
        missedAfter
    ))

    // Calculate if it's currently late (only for pending medications)
    val isLate = !isTaken && !isSkipped && !isMissed && isPassed && TimeCalculationUtils.isLate(
        entry.scheduledDateTime,
        currentDateTime,
        lateAfter,
        missedAfter
    )

    val orangeColor = Color(0xFFF97316)

    // Determine button properties
    val buttonText = when {
        isTaken -> stringResource(R.string.home_action_taken)
        isSkipped -> stringResource(R.string.home_action_skipped)
        isMissed -> stringResource(R.string.home_action_missed)
        isLate -> stringResource(R.string.home_action_mark_late)
        else -> stringResource(R.string.home_action_take)
    }

    val buttonColor = when {
        isTaken || isSkipped -> MaterialTheme.colorScheme.surfaceVariant
        isMissed -> MaterialTheme.colorScheme.error
        isLate -> orangeColor
        else -> MaterialTheme.colorScheme.primary
    }

    val textColor = when {
        isTaken || isSkipped -> MaterialTheme.colorScheme.onSurfaceVariant
        isMissed -> MaterialTheme.colorScheme.onError
        isLate -> Color.White
        else -> MaterialTheme.colorScheme.onPrimary
    }

    // Guard: Support undo for missed doses when allowDoseUndo is true, or retroactive recording when allowCustomDoseTime is true
    val enabled = when {
        isTaken || isSkipped -> currentUser?.allowDoseUndo == true
        isMissed -> currentUser?.allowDoseUndo == true || currentUser?.allowCustomDoseTime == true
        else -> true
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Medicine icon with pill visual support
            MedicineImage(
                imageUri = medicine?.imageUri,
                pillShape = medicine?.pillShape,
                pillColor = medicine?.pillColor,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Medication info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 1. Name
                Text(
                    text = medicine?.medicationName ?: "Unknown Medicine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 2. Qty / Dosage
                Text(
                    text = medicine?.getDosageDisplay(entry.scheduledDateTime.toLocalTime()) ?: "Unknown dosage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 3. Note
                if (!medicine?.notes.isNullOrBlank()) {
                    Text(
                        text = "📝 ${medicine?.notes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF59E0B),
                        maxLines = 2
                    )
                }

                // 3b. Skip reason if skipped
                if (isSkipped && !entry.skipReason.isNullOrBlank()) {
                    Text(
                        text = "⏭️ ${stringResource(R.string.home_action_skipped)}: ${entry.skipReason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                // 3c. Dose note if taken
                if (isTaken) {
                    if (!entry.doseNotes.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .then(
                                    if (currentUser?.allowDoseNotes == true) {
                                        Modifier.clickable { onEditNote(entry.entryId) }
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "💬 " + stringResource(R.string.home_dose_note_label, entry.doseNotes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 2
                            )
                        }
                    } else if (currentUser?.allowDoseNotes == true) {
                        TextButton(
                            onClick = { onEditNote(entry.entryId) },
                            modifier = Modifier.height(24.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "💬 " + stringResource(R.string.btn_add_note),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // 4. Refill warning / stock badge
                medicine?.currentStock?.let { stock ->
                    val isLow = medicine.refillThreshold != null && stock <= medicine.refillThreshold
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val containerBg = if (isLow) {
                        if (isDark) Color(0xFF3F1313) else Color(0xFFFEE2E2)
                    } else {
                        if (isDark) Color(0xFF0F2D14) else Color(0xFFD1FAE5)
                    }
                    val contentColor = if (isLow) {
                        if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                    } else {
                        if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                    }
                    Surface(
                        color = containerBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = if (isLow) {
                                stringResource(R.string.med_stock_refill_warning_badge, stock)
                            } else {
                                stringResource(R.string.med_stock_badge, stock)
                            },
                            color = contentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Action button area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = {
                        when {
                            isTaken || isSkipped -> {
                                if (currentUser?.allowDoseUndo == true) {
                                    onUndo(entry.entryId)
                                }
                            }
                            isMissed -> {
                                // Guard: If allowDoseUndo is enabled, prioritize undoing missed status back to pending; otherwise fallback to custom dose recording
                                if (currentUser?.allowDoseUndo == true) {
                                    onUndo(entry.entryId)
                                } else if (currentUser?.allowCustomDoseTime == true) {
                                    onMarkAsTaken(entry.entryId)
                                }
                            }
                            isLate -> onMarkAsLate(entry.entryId)
                            else -> onMarkAsTaken(entry.entryId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = textColor,
                        disabledContainerColor = buttonColor.copy(alpha = 0.5f),
                        disabledContentColor = textColor.copy(alpha = 0.5f)
                    ),
                    enabled = enabled,
                    modifier = Modifier
                        .height(44.dp)
                        .width(120.dp)
                ) {
                    Text(
                        text = buttonText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                if (currentUser?.allowDoseSkipping == true && !isTaken && !isSkipped && !isMissed) {
                    TextButton(
                        onClick = { onSkip(entry.entryId) },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_action_skip),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
