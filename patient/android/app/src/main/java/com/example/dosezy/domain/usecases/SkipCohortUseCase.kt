/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.domain.usecases

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.notifications.AlarmScheduler
import com.example.dosezy.widget.DosezyAppWidgetProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Use case coordinating skipping medicine cohorts or individual doses.
 * Cancels active alarms and records dose skipped in repository.
 */
@RequiresApi(Build.VERSION_CODES.O)
class SkipCohortUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SkipCohortUseCase"
    }

    suspend operator fun invoke(
        entryIds: List<String>,
        reason: String
    ) {
        if (entryIds.isEmpty()) return
        val alarmScheduler = AlarmScheduler(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager

        entryIds.forEach { id ->
            alarmScheduler.cancelNagging(id)
            alarmScheduler.cancelSnooze(id)
            // Guard: Cancel active intake safeguard alarm for each entry to prevent orphaned re-alerts after skipping dose (#104)
            alarmScheduler.cancelIntakeSafeguardAlarm(id)
            notificationManager?.cancel(id.hashCode())
            scheduleRepository.recordDoseSkipped(id, reason, null)
        }

        // Guard: Reconcile active ADHD intake countdown notification if skipped entries match active countdown to prevent dropping remaining pending medications (#BUG-SAFEGUARD-PARTIAL-COHORT)
        try {
            val activeCountdown = com.example.dosezy.notifications.IntakeSafeguardManager.activeCountdown.value
            if (activeCountdown != null && (entryIds.contains(activeCountdown.entryId) || activeCountdown.entryIds.any { entryIds.contains(it) })) {
                com.example.dosezy.notifications.IntakeSafeguardManager.reconcileCountdown(context, entryIds)
            }
        } catch (_: Exception) {}

        // Guard: Coordinate slot notification sync and widget updates across each distinct time slot in entryIds (#BUG-MULTI-SLOT-COHORT)
        val entries = entryIds.mapNotNull { scheduleRepository.getScheduleEntryById(it) }
        val distinctSlots = entries.distinctBy { it.scheduledDateTime.withSecond(0).withNano(0) }
        if (distinctSlots.isNotEmpty()) {
            distinctSlots.forEach { slotEntry ->
                scheduleRepository.syncSlotNotificationAndAlarms(slotEntry.entryId, slotEntry, context)
            }
        } else {
            val anchorEntry = entryIds.firstOrNull()?.let { scheduleRepository.getScheduleEntryById(it) }
            if (anchorEntry != null) {
                // Guard: Fallback to anchor entry slot sync when distinctSlots is empty to disarm slot alarms and cancel notification shade entries (#BUG-COHORT-SLOT-FALLBACK)
                scheduleRepository.syncSlotNotificationAndAlarms(anchorEntry.entryId, anchorEntry, context)
            } else {
                try {
                    DosezyAppWidgetProvider.updateAppWidgets(context)
                } catch (_: Exception) {}
            }
        }
        Log.d(TAG, "Marked ${entryIds.size} doses as skipped")
    }
}
