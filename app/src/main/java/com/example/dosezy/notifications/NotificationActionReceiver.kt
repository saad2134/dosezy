// NotificationActionReceiver.kt
package com.example.dosezy.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.repository.ScheduleRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var database: DosezyDatabase

    companion object {
        private const val TAG = "NotificationActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        val entryId = intent?.getStringExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID)

        if (entryId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                handleAction(context, action, entryId)
            }
        }
    }

    private suspend fun handleAction(context: Context, action: String?, entryId: String) {
        val scheduleRepository = ScheduleRepository(database)

        when (action) {
            "TAKEN_ACTION" -> {
                Log.d(TAG, "Marking medicine as taken for entry: $entryId")
                val takenAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                scheduleRepository.updateMedicationStatus(entryId, "TAKEN_ON_TIME", takenAt)

                // Cancel the notification
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.cancel(entryId.hashCode())

                Log.d(TAG, "Medicine marked as taken and notification cancelled for entry: $entryId")
            }
            "SNOOZE_ACTION" -> {
                Log.d(TAG, "Snoozing medicine reminder for entry: $entryId")
                val alarmScheduler = AlarmScheduler(context)
                alarmScheduler.scheduleSnooze(entryId, 10) // 10 minutes snooze

                // Cancel the current notification
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.cancel(entryId.hashCode())

                Log.d(TAG, "Medicine reminder snoozed for 10 minutes for entry: $entryId")
            }
            else -> {
                Log.w(TAG, "Unknown action received: $action for entry: $entryId")
            }
        }
    }
}