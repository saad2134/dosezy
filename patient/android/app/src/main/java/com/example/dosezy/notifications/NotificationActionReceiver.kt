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

import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun checkAndPostRefillNotification(context: Context, database: DosezyDatabase, medicineId: String) {
        try {
            val medicine = database.medicineDao().getMedicineByIdDirect(medicineId)
            if (medicine != null && medicine.currentStock != null && medicine.refillThreshold != null) {
                if (medicine.currentStock <= medicine.refillThreshold) {
                    val savedLanguage = com.example.dosezy.utils.LocaleHelper.getSavedLanguage(context)
                    val localizedContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, savedLanguage)
                    val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    val contentIntent = android.app.PendingIntent.getActivity(
                        context,
                        (medicineId + "_refill_topbar_click").hashCode(),
                        Intent(context, com.example.dosezy.MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        },
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    val builder = androidx.core.app.NotificationCompat.Builder(context, MedicineAlarmReceiver.REFILL_CHANNEL_ID)
                        .setSmallIcon(com.example.dosezy.R.drawable.loader_icon)
                        .setContentTitle(localizedContext.getString(com.example.dosezy.R.string.notif_refill_alert_title, medicine.medicationName))
                        .setContentText(localizedContext.getString(com.example.dosezy.R.string.notif_refill_alert_text, medicine.currentStock))
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(contentIntent)
                        .setAutoCancel(true)
                    nManager.notify((medicineId + "_refill_topbar").hashCode(), builder.build())
                }
            }
        } catch (_: Exception) {}
    }


    @Inject
    lateinit var database: DosezyDatabase

    companion object {
        private const val TAG = "NotificationActionReceiver"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent?) {
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
                val takenAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                allIds.forEach { id ->
                    Log.d(TAG, "Marking medicine as taken for entry: $id")
                    scheduleRepository.recordDoseTaken(id, "TAKEN_ON_TIME", takenAt, context)
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
            }
            else -> {
                Log.w(TAG, "Unknown action received: $action for entry: $primaryEntryId")
            }
        }
    }
}