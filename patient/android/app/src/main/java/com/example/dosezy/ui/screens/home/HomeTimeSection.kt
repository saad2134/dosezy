/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dosezy.R
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleWithMedicine
import com.example.dosezy.data.model.User
import com.example.dosezy.utils.TimeCalculationUtils

@Composable
fun HomeTimeSection(
    time: String,
    entries: List<ScheduleWithMedicine>,
    currentDateTime: java.time.LocalDateTime,
    currentUser: User?,
    onMarkAsTaken: (String) -> Unit,
    onMarkAsLate: (String) -> Unit,
    onUndo: (String) -> Unit = {},
    onSkip: (String) -> Unit = {},
    onEditNote: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Time Header
        Text(
            text = time,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Calculate time difference only for pending/late entries (not taken ones)
        val pendingEntries = entries.filter {
            it.scheduleEntry.status == MedicationStatus.PENDING ||
                    it.scheduleEntry.status == MedicationStatus.MISSED
        }

        val firstPendingEntry = pendingEntries.firstOrNull()
        val timeDiff = firstPendingEntry?.let { entry ->
            TimeCalculationUtils.calculateTimeDifference(
                entry.scheduleEntry.scheduledDateTime,
                currentDateTime
            )
        }

        // Guard: Differentiate taken vs skipped slot states so skipping a dose does not falsely report "All medications taken"
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

        val statusText = when {
            allTaken -> {
                stringResource(R.string.status_all_medications_taken)
            }
            allSkipped -> {
                stringResource(R.string.home_action_skipped)
            }
            allCompleted -> {
                stringResource(R.string.status_all_medications_taken)
            }
            timeDiff != null -> {
                if (timeDiff.isLate) {
                    stringResource(R.string.time_diff_ago, timeDiff.hours, timeDiff.minutes)
                } else {
                    stringResource(R.string.to_be_taken_in, timeDiff.hours, timeDiff.minutes)
                }
            }
            else -> stringResource(R.string.status_scheduled_for_this_time)
        }

        // Always normal color below time header (not transparent, not red, not orange)
        val statusColor = MaterialTheme.colorScheme.onSurfaceVariant

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = statusColor,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Medication cards for this time
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
