/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.domain.usecases

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.notifications.AlarmScheduler
import com.example.dosezy.utils.TimeCalculationUtils
import com.example.dosezy.widget.DosezyAppWidgetProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Use case coordinating marking medicine cohorts or single doses as taken.
 * Encapsulates timing calculations, status evaluation (on-time vs late), alarm disarming,
 * inventory deduction, and app widget synchronization.
 */
@RequiresApi(Build.VERSION_CODES.O)
class TakeCohortUseCase @Inject constructor(
    private val database: DosezyDatabase,
    private val scheduleRepository: ScheduleRepository,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TakeCohortUseCase"
    }

    suspend operator fun invoke(
        entryIds: List<String>,
        customTakenAt: LocalDateTime = LocalDateTime.now(),
        notes: String? = null,
        user: User? = null
    ) {
        if (entryIds.isEmpty()) return
        val alarmScheduler = AlarmScheduler(context)
        val nowStr = customTakenAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        // Cancel alarms and notifications upfront for all cohort entries
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        entryIds.forEach { id ->
            alarmScheduler.cancelNagging(id)
            alarmScheduler.cancelSnooze(id)
            notificationManager?.cancel(id.hashCode())
        }

        entryIds.forEach { id ->
            val entry = database.scheduleDao().getScheduleEntryById(id)
            val entryUser = user ?: entry?.let { database.userDao().getUserByIdDirect(it.userId) }
            val lateAfter = entryUser?.considerLateAfter ?: 3
            val status = if (entry != null && TimeCalculationUtils.isTakenLate(entry.scheduledDateTime, customTakenAt, lateAfter)) {
                "TAKEN_LATE"
            } else {
                "TAKEN_ON_TIME"
            }
            Log.d(TAG, "Recording dose taken ($status) for entry: $id")
            scheduleRepository.recordDoseTaken(id, status, nowStr, context, notes)
        }

        try {
            DosezyAppWidgetProvider.updateAppWidgets(context)
        } catch (_: Exception) {}
    }
}
