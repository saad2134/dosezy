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
import com.example.dosezy.notifications.AlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Use case coordinating snoozing medicine cohorts or single doses.
 * Encapsulates canceling active alarms, resolving explicit vs preferred snooze durations,
 * and scheduling grouped or individual snooze reminders.
 */
@RequiresApi(Build.VERSION_CODES.O)
class SnoozeCohortUseCase @Inject constructor(
    private val database: DosezyDatabase,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SnoozeCohortUseCase"
    }

    suspend operator fun invoke(
        entryIds: List<String>,
        explicitSnoozeMinutes: Int? = null,
        fallbackMedicineName: String = "Medicine",
        medicineNames: List<String>? = null,
        user: User? = null
    ) {
        if (entryIds.isEmpty()) return
        val alarmScheduler = AlarmScheduler(context)

        // Cancel nagging reminders upfront
        entryIds.forEach { id ->
            alarmScheduler.cancelNagging(id)
        }

        val finalSnoozeMinutes = if (explicitSnoozeMinutes != null && explicitSnoozeMinutes > 0) {
            explicitSnoozeMinutes
        } else {
            user?.snoozeDuration ?: run {
                val firstEntry = database.scheduleDao().getScheduleEntryById(entryIds.first())
                val u = firstEntry?.let { database.userDao().getUserByIdDirect(it.userId) }
                u?.snoozeDuration ?: 10
            }
        }

        val names = if (!medicineNames.isNullOrEmpty()) {
            medicineNames
        } else if (fallbackMedicineName.isNotBlank() && fallbackMedicineName != "Medicine") {
            listOf(fallbackMedicineName)
        } else {
            val queried = entryIds.mapNotNull { id ->
                val entry = database.scheduleDao().getScheduleEntryById(id)
                val med = entry?.let { database.medicineDao().getMedicineByIdDirect(it.medicineId) }
                med?.medicationName
            }
            if (queried.isNotEmpty()) queried else listOf(fallbackMedicineName)
        }

        alarmScheduler.scheduleGroupedSnooze(
            entryIds = entryIds,
            minutes = finalSnoozeMinutes,
            medicineNames = names,
            timeFormat = user?.timeFormat
        )
        Log.d(TAG, "Scheduled snooze for ${entryIds.size} medicines for $finalSnoozeMinutes minutes")
    }
}
