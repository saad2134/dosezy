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
            notificationManager?.cancel(id.hashCode())
            scheduleRepository.recordDoseSkipped(id, reason, context)
        }

        try {
            DosezyAppWidgetProvider.updateAppWidgets(context)
        } catch (_: Exception) {}
        Log.d(TAG, "Marked ${entryIds.size} doses as skipped")
    }
}
