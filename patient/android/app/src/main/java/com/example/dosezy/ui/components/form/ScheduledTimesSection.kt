/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.components.form

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dosezy.R
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.utils.TimeFormatUtils
import java.time.LocalTime
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScheduledTimesSection(
    selectedFrequency: FrequencyPattern,
    scheduledTimesList: List<LocalTime>,
    onScheduledTimesChange: (List<LocalTime>) -> Unit,
    selectedDosePreset: String,
    onDosePresetChange: (String) -> Unit,
    perTimeDosages: Map<String, String>,
    onPerTimeDosagesChange: (Map<String, String>) -> Unit,
    timeFormat: TimeFormat,
    defaultInitialTime: LocalTime,
    onOpenTimePicker: (timeIndex: Int?, initialTime: LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedFrequency == FrequencyPattern.AS_NEEDED) return

    Column(modifier = modifier.fillMaxWidth()) {
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
}
