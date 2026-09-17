// MedicineAlarmReceiver.kt
package com.example.dosezy.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Snooze
import androidx.core.app.NotificationCompat
import com.example.dosezy.MainActivity
import com.example.dosezy.R
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MedicineAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var database: DosezyDatabase

    companion object {
        const val TAG = "MedicineAlarmReceiver"
        const val CHANNEL_ID = "dosezy_medicine_reminders_v3"
        const val EXTRA_ENTRY_ID = "entry_id"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_SCHEDULED_TIME = "scheduled_time"
        const val EXTRA_ENTRY_IDS = "entry_ids"
        const val EXTRA_MEDICINE_NAMES = "medicine_names"
        const val EXTRA_NAGGING_COUNT = "nagging_count"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "android.intent.action.LOCKED_BOOT_COMPLETED" ||
            action == Intent.ACTION_REBOOT ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED) {
            
            Log.d(TAG, "Received system broadcast action: $action - rescheduling all alarms")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    rescheduleAllAlarms(context)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val entryId = intent?.getStringExtra(EXTRA_ENTRY_ID)
        val entryIds = intent?.getStringArrayListExtra(EXTRA_ENTRY_IDS)
        val medicineName = intent?.getStringExtra(EXTRA_MEDICINE_NAME)
        val medicineNames = intent?.getStringArrayListExtra(EXTRA_MEDICINE_NAMES)
        val scheduledTime = intent?.getStringExtra(EXTRA_SCHEDULED_TIME)
        val naggingCount = intent?.getIntExtra(EXTRA_NAGGING_COUNT, 0) ?: 0

        if (entryId != null && medicineName != null) {
            val pendingResult = goAsync()
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            val wakeLock = powerManager?.newWakeLock(
                android.os.PowerManager.PARTIAL_WAKE_LOCK or
                        android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        android.os.PowerManager.ON_AFTER_RELEASE,
                "Dosezy:MedicineAlarmWakeLock"
            )
            wakeLock?.acquire(30 * 1000L) // 30 seconds

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val entry = database.scheduleDao().getScheduleEntryById(entryId)
                    if (entry != null && entry.status == com.example.dosezy.data.model.MedicationStatus.PENDING) {
                        val user = database.userDao().getUserByIdDirect(entry.userId)
                        val isNagging = naggingCount > 0

                        // Immediately trigger centralized alarm audio & vibration
                        val sound = user?.alarmSound ?: com.example.dosezy.data.model.AlarmSound.SYSTEM_DEFAULT
                        val customPath = user?.customAlarmSoundPath
                        val duration = user?.alarmDurationSeconds ?: 0

                        AlarmAudioPlayer.play(
                            context = context,
                            sound = sound,
                            customPath = customPath,
                            autoSilenceSeconds = duration
                        )

                        showNotification(
                            context = context,
                            entryId = entryId,
                            entryIds = entryIds,
                            medicineName = medicineName,
                            medicineNames = medicineNames,
                            scheduledTime = scheduledTime,
                            isNagging = isNagging,
                            naggingCount = naggingCount,
                            maxNagging = user?.naggingMaxRepeats ?: 3,
                            snoozeMinutes = user?.snoozeDuration ?: 10
                        )

                        // Schedule next follow-up nagging reminder if enabled and below limit
                        if (user != null && user.naggingRemindersEnabled && naggingCount < user.naggingMaxRepeats) {
                            val alarmScheduler = AlarmScheduler(context)
                            alarmScheduler.scheduleNaggingReminder(
                                entryId = entryId,
                                minutes = user.naggingIntervalMinutes,
                                medicineName = medicineName,
                                naggingCount = naggingCount + 1
                            )
                        }
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Error processing alarm in background", ex)
                    AlarmAudioPlayer.play(
                        context = context,
                        sound = com.example.dosezy.data.model.AlarmSound.SYSTEM_DEFAULT,
                        customPath = null,
                        autoSilenceSeconds = 0
                    )
                    showNotification(context, entryId, entryIds, medicineName, medicineNames, scheduledTime, false, 0, 3)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showNotification(
        context: Context,
        entryId: String,
        entryIds: ArrayList<String>?,
        medicineName: String,
        medicineNames: ArrayList<String>?,
        scheduledTime: String?,
        isNagging: Boolean = false,
        naggingCount: Int = 0,
        maxNagging: Int = 3,
        snoozeMinutes: Int = 10
    ) {
        createNotificationChannel(context)

        val contentText = scheduledTime?.let {
            "Scheduled for $it - Time to take your medicine!"
        } ?: "Time to take your medicine!"

        // Create intent for opening the app
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("fragment", "schedule")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            entryId.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create action intents
        val takenIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "TAKEN_ACTION"
            putExtra(EXTRA_ENTRY_ID, entryId)
        }

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "SNOOZE_ACTION"
            putExtra(EXTRA_ENTRY_ID, entryId)
        }

        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            entryId.hashCode() + 1,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            entryId.hashCode() + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Full screen alarm intent
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra(EXTRA_ENTRY_ID, entryId)
            if (entryIds != null) {
                putStringArrayListExtra(EXTRA_ENTRY_IDS, entryIds)
            }
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            if (medicineNames != null) {
                putStringArrayListExtra(EXTRA_MEDICINE_NAMES, medicineNames)
            }
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime ?: "")
        }

        // Android 14+ background activity start options
        val optionsBundle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            android.app.ActivityOptions.makeBasic().apply {
                setPendingIntentBackgroundActivityStartMode(
                    android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
            }.toBundle()
        } else {
            null
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            entryId.hashCode() + 10,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            optionsBundle
        )

        // Launch AlarmActivity directly if permitted (e.g., overlay granted or app in foreground)
        try {
            if (optionsBundle != null) {
                context.startActivity(alarmIntent, optionsBundle)
            } else {
                context.startActivity(alarmIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not start AlarmActivity directly", e)
        }

        val notificationTitle = if (isNagging) {
            context.getString(R.string.notif_nagging_title, medicineName)
        } else if (medicineNames != null && medicineNames.size > 1) {
            context.getString(R.string.alarm_medication_reminder_multi, medicineNames.size)
        } else {
            context.getString(R.string.alarm_medication_reminder)
        }

        val notificationText = if (isNagging) {
            context.getString(R.string.notif_nagging_text, naggingCount, maxNagging, scheduledTime ?: "")
        } else {
            contentText
        }

        // Create silent notification: AlarmAudioPlayer is the single source of sound & vibration
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.loader_icon)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setSilent(true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(
                getNotificationIcon(context, Icons.Filled.Check),
                context.getString(R.string.home_action_taken),
                takenPendingIntent
            )
            .addAction(
                getNotificationIcon(context, Icons.Filled.Snooze),
                context.getString(R.string.notif_action_snooze_format, snoozeMinutes),
                snoozePendingIntent
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(entryId.hashCode(), notification)

        Log.d(TAG, "Showing notification and launching full-screen alarm for: $medicineName")
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Remove legacy channels to avoid system audio caching conflicts
            try {
                notificationManager.deleteNotificationChannel("dosezy_medicine_reminders_v2")
                notificationManager.deleteNotificationChannel("dosezy_medicine_reminders")
            } catch (_: Exception) {}

            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(com.example.dosezy.R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(com.example.dosezy.R.string.notif_channel_desc)
                enableLights(true)
                enableVibration(false) // Managed directly by AlarmAudioPlayer
                setSound(null, null)  // Silent channel: eliminates duplicate system ringtone
                setBypassDnd(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun rescheduleAllAlarms(context: Context) {
        val userRepository = UserRepository(database)
        val scheduleRepository = ScheduleRepository(database)

        try {
            // Get all users
            val allUsers = userRepository.getAllUsersList()

            // Reschedule alarms for each user
            allUsers.forEach { user ->
                Log.d(TAG, "Rescheduling alarms for user: ${user.fullName} (${user.userId})")
                scheduleRepository.rescheduleAllAlarms(user.userId, context)
            }

            Log.d(TAG, "Successfully rescheduled alarms for ${allUsers.size} users after boot")
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling alarms after boot", e)
        }
    }

    // Helper function to convert Compose icons to NotificationCompat.Action
    private fun getNotificationIcon(context: Context, icon: androidx.compose.ui.graphics.vector.ImageVector): Int {
        // Using different built-in system icons for the actions
        return when (icon) {
            Icons.Filled.Check -> android.R.drawable.checkbox_on_background
            Icons.Filled.Snooze -> android.R.drawable.ic_lock_idle_alarm
            else -> android.R.drawable.ic_dialog_info
        }
    }
}