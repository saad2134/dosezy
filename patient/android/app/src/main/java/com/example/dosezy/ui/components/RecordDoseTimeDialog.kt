package com.example.dosezy.ui.components

import android.app.TimePickerDialog
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dosezy.R
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.utils.TimeFormatUtils
import java.time.LocalDateTime
import java.time.LocalTime

enum class DoseTimeSelectionMode {
    JUST_NOW,
    SCHEDULED_TIME,
    CUSTOM_TIME
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RecordDoseTimeDialog(
    entry: ScheduleEntry,
    medicineName: String,
    timeFormat: TimeFormat = TimeFormat.HOUR_12,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(DoseTimeSelectionMode.JUST_NOW) }

    val now = remember { LocalDateTime.now() }
    val scheduledTime = entry.scheduledDateTime

    var customTime by remember {
        mutableStateOf(
            if (scheduledTime.isBefore(now)) scheduledTime.toLocalTime() else now.toLocalTime()
        )
    }

    val is24Hour = timeFormat == TimeFormat.HOUR_24

    fun showTimePicker() {
        val picker = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                customTime = LocalTime.of(hourOfDay, minute)
                selectedMode = DoseTimeSelectionMode.CUSTOM_TIME
            },
            customTime.hour,
            customTime.minute,
            is24Hour
        )
        picker.show()
    }

    val formattedNow = TimeFormatUtils.formatTime(now, timeFormat)
    val formattedScheduled = TimeFormatUtils.formatTime(scheduledTime, timeFormat)
    val formattedCustom = TimeFormatUtils.formatLocalTime(customTime, timeFormat)

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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.record_dose_time_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.record_dose_time_subtitle, medicineName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Option 1: Just Now
                DoseTimeOptionCard(
                    title = stringResource(R.string.record_dose_time_just_now, formattedNow),
                    isSelected = selectedMode == DoseTimeSelectionMode.JUST_NOW,
                    onClick = { selectedMode = DoseTimeSelectionMode.JUST_NOW }
                )

                // Option 2: Scheduled Time
                DoseTimeOptionCard(
                    title = stringResource(R.string.record_dose_time_scheduled, formattedScheduled),
                    isSelected = selectedMode == DoseTimeSelectionMode.SCHEDULED_TIME,
                    onClick = { selectedMode = DoseTimeSelectionMode.SCHEDULED_TIME }
                )

                // Option 3: Custom Time
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedMode == DoseTimeSelectionMode.CUSTOM_TIME) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedMode == DoseTimeSelectionMode.CUSTOM_TIME) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMode = DoseTimeSelectionMode.CUSTOM_TIME }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            RadioButton(
                                selected = selectedMode == DoseTimeSelectionMode.CUSTOM_TIME,
                                onClick = { selectedMode = DoseTimeSelectionMode.CUSTOM_TIME }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.record_dose_time_custom, formattedCustom),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedMode == DoseTimeSelectionMode.CUSTOM_TIME) FontWeight.SemiBold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        TextButton(
                            onClick = { showTimePicker() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.record_dose_time_select),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val resolvedDateTime = when (selectedMode) {
                        DoseTimeSelectionMode.JUST_NOW -> LocalDateTime.now()
                        DoseTimeSelectionMode.SCHEDULED_TIME -> entry.scheduledDateTime
                        DoseTimeSelectionMode.CUSTOM_TIME -> {
                            entry.scheduledDateTime.toLocalDate().atTime(customTime)
                        }
                    }
                    onConfirm(resolvedDateTime)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = stringResource(R.string.record_dose_time_confirm),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun DoseTimeOptionCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
