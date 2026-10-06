// NotificationActionReceiver.kt
package com.example.dosezy.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.repository.ScheduleRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class NotificationActionReceiver : BroadcastReceiver() {

    private lateinit var database: DosezyDatabase

    companion object {
        private const val TAG = "NotificationActionReceiver"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent?) {
        database = DosezyDatabase.getInstance(context)
        val action = intent?.action
        val entryId = intent?.getStringExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID)
        val entryIds = intent?.getStringArrayListExtra(MedicineAlarmReceiver.EXTRA_ENTRY_IDS)
        val allIds = (entryIds ?: if (entryId != null) listOf(entryId) else emptyList()).filter { it.isNotEmpty() }.distinct()

        if (allIds.isNotEmpty()) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    handleAction(context, action, allIds, entryId ?: allIds.first())
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun handleAction(context: Context, action: String?, allIds: List<String>, primaryEntryId: String) {
        val scheduleRepository = ScheduleRepository(database)

        // Immediately stop active alarm sound and dismiss full-screen activity
        AlarmAudioPlayer.stop()
        AlarmActivity.stopActiveAlarm()

        val alarmScheduler = AlarmScheduler(context)
        allIds.forEach { id ->
            alarmScheduler.cancelNagging(id)
            if (action == "TAKEN_ACTION") {
                // Guard: Cancel snooze alarm so taking medication prevents delayed phantom snooze triggers
                alarmScheduler.cancelSnooze(id)
            }
        }

        // Cancel notification for both Taken and Snooze actions
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(primaryEntryId.hashCode())
        allIds.forEach { id ->
            notificationManager.cancel(id.hashCode())
        }

        when (action) {
            "TAKEN_ACTION" -> {
                val now = LocalDateTime.now()
                val takenAt = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                allIds.forEach { id ->
                    val entry = database.scheduleDao().getScheduleEntryById(id)
                    val user = entry?.let { database.userDao().getUserByIdDirect(it.userId) }
                    val lateAfter = user?.considerLateAfter ?: 3
                    val missedAfter = user?.considerMissedAfter ?: 6
                    // Guard: Use isTakenLate so doses taken past missedAfter are recorded as TAKEN_LATE rather than TAKEN_ON_TIME
                    val status = if (entry != null && com.example.dosezy.utils.TimeCalculationUtils.isTakenLate(entry.scheduledDateTime, now, lateAfter)) {
                        "TAKEN_LATE"
                    } else {
                        "TAKEN_ON_TIME"
                    }
                    Log.d(TAG, "Marking medicine as taken ($status) for entry: $id")
                    scheduleRepository.recordDoseTaken(id, status, takenAt, context)
                }
                Log.d(TAG, "Marked ${allIds.size} medicines as taken and processed")
            }
            "SNOOZE_ACTION" -> {
                Log.d(TAG, "Snoozing medicine reminder for ${allIds.size} entries")
                val firstEntry = database.scheduleDao().getScheduleEntryById(primaryEntryId)
                val user = firstEntry?.let { database.userDao().getUserByIdDirect(it.userId) }
                val snoozeMinutes = user?.snoozeDuration ?: 10

                val medicineNames = allIds.mapNotNull { id ->
                    val entry = database.scheduleDao().getScheduleEntryById(id)
                    val med = entry?.let { database.medicineDao().getMedicineByIdDirect(it.medicineId) }
                    med?.medicationName
                }

                alarmScheduler.scheduleGroupedSnooze(allIds, snoozeMinutes, medicineNames)
                Log.d(TAG, "Medicine reminder snoozed for $snoozeMinutes minutes for ${allIds.size} entries")
                try {
                    com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
                } catch (_: Exception) {}
            }
            else -> {
                Log.w(TAG, "Unknown action received: $action for entry: $primaryEntryId")
            }
        }
    }
}