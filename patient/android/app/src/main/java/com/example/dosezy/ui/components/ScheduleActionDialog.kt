/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dosezy.R
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleWithMedicine
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.data.model.User
import com.example.dosezy.ui.screens.MedicineImage
import com.example.dosezy.utils.TimeCalculationUtils
import com.example.dosezy.utils.TimeFormatUtils
import java.time.LocalDateTime
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScheduleActionDialog(
    scheduleWithMedicine: ScheduleWithMedicine,
    currentUser: User?,
    timeFormat: TimeFormat = TimeFormat.HOUR_12,
    targetLocale: Locale = Locale.getDefault(),
    onTakeDose: () -> Unit,
    onSkipDose: () -> Unit,
    onEditNote: () -> Unit,
    onUndoDose: () -> Unit,
    onDismiss: () -> Unit
) {
    val entry = scheduleWithMedicine.scheduleEntry
    val medicine = scheduleWithMedicine.medicine
    val now = LocalDateTime.now()

    // Guard: Doses older than 48 hours are permanently locked to preserve medical record integrity before doctor visits (#109)
    val isLocked = entry.scheduledDateTime.isBefore(now.minusHours(48))

    val isTaken = entry.status == MedicationStatus.TAKEN_ON_TIME || entry.status == MedicationStatus.TAKEN_LATE
    val isSkipped = entry.status == MedicationStatus.SKIPPED
    val missedAfter = currentUser?.considerMissedAfter ?: 6
    val isPassed = now.isAfter(entry.scheduledDateTime)
    val isMissed = entry.status == MedicationStatus.MISSED || (!isTaken && !isSkipped && isPassed && TimeCalculationUtils.isMissed(
        entry.scheduledDateTime,
        now,
        missedAfter
    ))

    val resolvedStatus = when {
        isTaken -> entry.status
        isSkipped -> MedicationStatus.SKIPPED
        isMissed -> MedicationStatus.MISSED
        else -> MedicationStatus.PENDING
    }

    val (statusText, statusColor) = when (resolvedStatus) {
        MedicationStatus.TAKEN_ON_TIME -> Pair(stringResource(R.string.status_taken), MaterialTheme.colorScheme.primary)
        MedicationStatus.TAKEN_LATE -> Pair(stringResource(R.string.analytics_taken_late), MaterialTheme.colorScheme.tertiary)
        MedicationStatus.SKIPPED -> Pair(stringResource(R.string.home_action_skipped), MaterialTheme.colorScheme.outline)
        MedicationStatus.MISSED -> Pair(stringResource(R.string.home_action_missed), MaterialTheme.colorScheme.error)
        MedicationStatus.PENDING -> Pair(stringResource(R.string.status_pending), MaterialTheme.colorScheme.onSurfaceVariant)
    }

    val formattedTime = TimeFormatUtils.formatTime(entry.scheduledDateTime, timeFormat, targetLocale)
    val formattedDate = entry.scheduledDateTime.toLocalDate().format(
        java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(targetLocale)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        tonalElevation = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                MedicineImage(
                    imageUri = medicine?.imageUri,
                    pillShape = medicine?.pillShape,
                    pillColor = medicine?.pillColor,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medicine?.medicationName ?: stringResource(R.string.form_med_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$formattedDate • $formattedTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dosage and Status Badge Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = medicine?.getDosageDisplay(entry.scheduledDateTime.toLocalTime()) ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Existing dose note preview
                if (!entry.doseNotes.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💬 ${entry.doseNotes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Guard: If locked (>48 hours), explain medical history preservation policy to the user (#109)
                if (isLocked) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.schedule_dose_locked_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Action Buttons for Editable Entries (within 48 hours)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isTaken && !isSkipped) {
                            // Primary Take Dose Action
                            Button(
                                onClick = onTakeDose,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.schedule_action_take_dose),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Secondary Skip Dose Action (if enabled in preferences)
                            if (currentUser?.allowDoseSkipping == true) {
                                OutlinedButton(
                                    onClick = onSkipDose,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.schedule_action_skip_dose),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Optional Add/Edit Note Action (if enabled in preferences)
                            if (currentUser?.allowDoseNotes == true) {
                                OutlinedButton(
                                    onClick = onEditNote,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.schedule_action_add_note),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            // Doses Already Taken or Skipped
                            if (currentUser?.allowDoseNotes == true) {
                                OutlinedButton(
                                    onClick = onEditNote,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.schedule_action_edit_note),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Undo action (if allowed by preferences)
                            if (currentUser?.allowDoseUndo == true) {
                                OutlinedButton(
                                    onClick = onUndoDose,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Undo,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.schedule_action_undo_dose),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isLocked) stringResource(R.string.close) else stringResource(R.string.cancel),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}
