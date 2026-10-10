/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.dosezy.data.model.User
import com.example.dosezy.utils.TimeCalculationUtils

// Guard: Timeline UI mode displaying today's medication schedule with clean vertical progress line and nodes
@Composable
fun HomeTimelineView(
    groupedEntries: List<Pair<String, List<ScheduleWithMedicine>>>,
    currentDateTime: java.time.LocalDateTime,
    currentUser: User?,
    onMarkAsTaken: (String) -> Unit,
    onMarkAsLate: (String) -> Unit,
    onUndo: (String) -> Unit = {},
    onSkip: (String) -> Unit = {},
    onEditNote: (String) -> Unit = {}
) {
    val lateAfter = currentUser?.considerLateAfter ?: 3
    val missedAfter = currentUser?.considerMissedAfter ?: 6

    Column(modifier = Modifier.fillMaxWidth()) {
        groupedEntries.forEachIndexed { index, (time, entries) ->
            val isLast = index == groupedEntries.lastIndex

            // Guard: Differentiate taken vs skipped slot states so skipping a dose does not falsely report "All medications taken" with green checkmarks
            val allTaken = entries.all {
                it.scheduleEntry.status == MedicationStatus.TAKEN_ON_TIME ||
                        it.scheduleEntry.status == MedicationStatus.TAKEN_LATE
            }

            val allSkipped = entries.all {
                it.scheduleEntry.status == MedicationStatus.SKIPPED
            }

            val allCompleted = entries.all {
                it.scheduleEntry.status == MedicationStatus.TAKEN_ON_TIME ||
                        it.scheduleEntry.status == MedicationStatus.TAKEN_LATE ||
                        it.scheduleEntry.status == MedicationStatus.SKIPPED
            }

            val hasMissed = entries.any { entryWithMed ->
                val entry = entryWithMed.scheduleEntry
                val isTakenOrSkipped = entry.status == MedicationStatus.TAKEN_ON_TIME ||
                        entry.status == MedicationStatus.TAKEN_LATE ||
                        entry.status == MedicationStatus.SKIPPED
                entry.status == MedicationStatus.MISSED || (!isTakenOrSkipped && currentDateTime.isAfter(entry.scheduledDateTime) && TimeCalculationUtils.isMissed(entry.scheduledDateTime, currentDateTime, missedAfter))
            }

            val hasLate = !hasMissed && !allCompleted && entries.any { entryWithMed ->
                val entry = entryWithMed.scheduleEntry
                val isTakenOrSkipped = entry.status == MedicationStatus.TAKEN_ON_TIME ||
                        entry.status == MedicationStatus.TAKEN_LATE ||
                        entry.status == MedicationStatus.SKIPPED
                !isTakenOrSkipped && currentDateTime.isAfter(entry.scheduledDateTime) && TimeCalculationUtils.isLate(entry.scheduledDateTime, currentDateTime, lateAfter, missedAfter)
            }

            val (nodeColor, nodeIcon) = when {
                allTaken -> Color(0xFF10B981) to Icons.Default.Check
                allSkipped -> Color(0xFF64748B) to Icons.Default.RemoveCircleOutline
                allCompleted -> Color(0xFF10B981) to Icons.Default.Check
                hasMissed -> Color(0xFFEF4444) to Icons.Default.PriorityHigh
                hasLate -> Color(0xFFF59E0B) to Icons.Default.HourglassTop
                else -> Color(0xFF1193D4) to Icons.Default.Schedule
            }

            // Calculate relative time or status text for badge
            val pendingEntries = entries.filter {
                it.scheduleEntry.status == MedicationStatus.PENDING ||
                        it.scheduleEntry.status == MedicationStatus.MISSED
            }
            val firstPending = pendingEntries.firstOrNull()
            val timeDiff = firstPending?.let {
                TimeCalculationUtils.calculateTimeDifference(it.scheduleEntry.scheduledDateTime, currentDateTime)
            }

            val badgeText = when {
                allTaken -> stringResource(R.string.status_all_medications_taken)
                allSkipped -> stringResource(R.string.home_action_skipped)
                allCompleted -> stringResource(R.string.status_all_medications_taken)
                timeDiff != null -> {
                    if (timeDiff.isLate) {
                        stringResource(R.string.time_diff_ago, timeDiff.hours, timeDiff.minutes)
                    } else {
                        stringResource(R.string.to_be_taken_in, timeDiff.hours, timeDiff.minutes)
                    }
                }
                else -> stringResource(R.string.status_scheduled_for_this_time)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                // Left Timeline track & node
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(38.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(nodeColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = nodeIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (!isLast) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .weight(1f)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Content: Time header + badge + cards
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = if (isLast) 8.dp else 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = time,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = nodeColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, nodeColor.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = nodeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    entries.forEach { entry ->
                        HomeMedicationCard(
                            scheduleWithMedicine = entry,
                            currentDateTime = currentDateTime,
                            currentUser = currentUser,
                            onMarkAsTaken = onMarkAsTaken,
                            onMarkAsLate = onMarkAsLate,
                            onUndo = onUndo,
                            onSkip = onSkip,
                            onEditNote = onEditNote
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
