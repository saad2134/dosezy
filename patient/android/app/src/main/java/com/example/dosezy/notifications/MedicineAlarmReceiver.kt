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
        const val CHANNEL_ID = "medicine_reminders"
        const val EXTRA_ENTRY_ID = "entry_id"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_SCHEDULED_TIME = "scheduled_time"
        const val EXTRA_ENTRY_IDS = "entry_ids"
        const val EXTRA_MEDICINE_NAMES = "medicine_names"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED) {
            
            Log.d(TAG, "Received system broadcast action: $action - rescheduling all alarms")
            // Reschedule all alarms after system time/zone change or boot
            CoroutineScope(Dispatchers.IO).launch {
                rescheduleAllAlarms(context)
            }
            return
        }

        val entryId = intent?.getStringExtra(EXTRA_ENTRY_ID)
        val entryIds = intent?.getStringArrayListExtra(EXTRA_ENTRY_IDS)
        val medicineName = intent?.getStringExtra(EXTRA_MEDICINE_NAME)
        val medicineNames = intent?.getStringArrayListExtra(EXTRA_MEDICINE_NAMES)
        val scheduledTime = intent?.getStringExtra(EXTRA_SCHEDULED_TIME)

        if (entryId != null && medicineName != null) {
            showNotification(context, entryId, entryIds, medicineName, medicineNames, scheduledTime)
        }
    }

    private fun showNotification(
        context: Context,
        entryId: String,
        entryIds: ArrayList<String>?,
        medicineName: String,
        medicineNames: ArrayList<String>?,
        scheduledTime: String?
    ) {
        // Acquire wake lock to ensure CPU stays awake while firing alarm
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        val wakeLock = powerManager?.newWakeLock(
            android.os.PowerManager.PARTIAL_WAKE_LOCK,
            "Dosezy:MedicineAlarmWakeLock"
        )
        wakeLock?.acquire(10 * 1000L) // 10 seconds

        createNotificationChannel(context)

        val alarmSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            entryId.hashCode() + 10,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Launch AlarmActivity directly if permitted (e.g., when screen is active / app foreground)
        try {
            context.startActivity(alarmIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not start AlarmActivity directly", e)
        }

        val vibrationPattern = longArrayOf(0, 1000, 500, 1000)

        // Create notification with sound, vibration, and fullScreenIntent
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medicine_notification)
            .setContentTitle("Medicine Reminder: $medicineName")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmSoundUri, android.media.AudioManager.STREAM_ALARM)
            .setVibrate(vibrationPattern)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                getNotificationIcon(context, Icons.Filled.Check),
                "Taken",
                takenPendingIntent
            )
            .addAction(
                getNotificationIcon(context, Icons.Filled.Snooze),
                "Snooze (10 min)",
                snoozePendingIntent
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(entryId.hashCode(), notification)

        Log.d(TAG, "Showing notification and launching full-screen alarm for: $medicineName")
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val vibrationPattern = longArrayOf(0, 1000, 500, 1000)

            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(com.example.dosezy.R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(com.example.dosezy.R.string.notif_channel_desc)
                enableLights(true)
                enableVibration(true)
                this.vibrationPattern = vibrationPattern
                setSound(alarmSoundUri, audioAttributes)
                setBypassDnd(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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