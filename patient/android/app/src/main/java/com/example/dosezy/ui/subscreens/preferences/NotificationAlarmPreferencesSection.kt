/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.subscreens.preferences

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dosezy.R
import com.example.dosezy.data.model.AlarmSound
import com.example.dosezy.data.model.User
import com.example.dosezy.ui.components.PreferenceItem

fun LazyListScope.notificationAlarmPreferencesSection(
    currentUser: User?,
    onSlideActionsChange: (Boolean) -> Unit,
    onAlarmSoundClick: () -> Unit,
    onAlarmDurationClick: () -> Unit,
    onSnoozeClick: () -> Unit,
    onNaggingEnabledChange: (Boolean) -> Unit,
    onNaggingIntervalClick: () -> Unit,
    onNaggingRepeatsClick: () -> Unit
) {
    item {
        Text(
            text = stringResource(R.string.pref_section_notifications),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
        )
    }

    // Slide to Take & Snooze Switch
    item {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Swipe,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.pref_slide_actions_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.pref_slide_actions_desc),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = currentUser?.slideActionsEnabled != false,
                    onCheckedChange = onSlideActionsChange
                )
            }
        }
    }

    item {
        // Alarm Sound Preference
        val currentSound = currentUser?.alarmSound ?: AlarmSound.SYSTEM_DEFAULT
        val customTitle = currentUser?.customAlarmSoundTitle
        val soundDisplayValue = if (currentSound == AlarmSound.CUSTOM && !customTitle.isNullOrBlank()) {
            customTitle
        } else {
            stringResource(currentSound.getTitleRes())
        }
        PreferenceItem(
            title = stringResource(R.string.pref_alarm_sound),
            currentValue = soundDisplayValue,
            iconName = "alarm_sound",
            onClick = onAlarmSoundClick
        )
    }

    item {
        // Alarm Ring Duration Preference
        val currentDuration = currentUser?.alarmDurationSeconds ?: 0
        val durationText = when (currentDuration) {
            -1 -> stringResource(R.string.alarm_duration_once)
            15 -> stringResource(R.string.alarm_duration_15s)
            30 -> stringResource(R.string.alarm_duration_30s)
            60 -> stringResource(R.string.alarm_duration_1m)
            120 -> stringResource(R.string.alarm_duration_2m)
            300 -> stringResource(R.string.alarm_duration_5m)
            else -> stringResource(R.string.alarm_duration_continuous)
        }
        PreferenceItem(
            title = stringResource(R.string.pref_alarm_duration_title),
            currentValue = durationText,
            iconName = "alarm_duration",
            onClick = onAlarmDurationClick
        )
    }

    item {
        // Snooze Duration Preference
        PreferenceItem(
            title = stringResource(R.string.pref_snooze_duration),
            currentValue = currentUser?.snoozeDuration?.let {
                stringResource(R.string.minutes_format, it)
            } ?: stringResource(R.string.minutes_format, 10),
            iconName = "snooze",
            onClick = onSnoozeClick
        )
    }

    // Follow-up / Nagging Reminders Switch & Settings
    item {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.pref_nagging_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.pref_nagging_desc),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = currentUser?.naggingRemindersEnabled == true,
                        onCheckedChange = onNaggingEnabledChange
                    )
                }

                AnimatedVisibility(
                    visible = currentUser?.naggingRemindersEnabled == true,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Sub-option 1: Interval (Indented)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onNaggingIntervalClick)
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.pref_nagging_interval),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = stringResource(R.string.minutes_format, currentUser?.naggingIntervalMinutes ?: 10),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Sub-option 2: Repeats (Indented)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onNaggingRepeatsClick)
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Snooze,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.pref_nagging_max_repeats),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = stringResource(R.string.pref_nagging_repeats_format, currentUser?.naggingMaxRepeats ?: 3),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
