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

class MedicineAlarmReceiver : BroadcastReceiver() {

    private lateinit var database: DosezyDatabase

    companion object {
        const val TAG = "MedicineAlarmReceiver"
        const val CHANNEL_ID = "dosezy_medicine_reminders_v3"
        const val REFILL_CHANNEL_ID = "dosezy_refill_alerts"
        const val EXTRA_ENTRY_ID = "entry_id"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_SCHEDULED_TIME = "scheduled_time"
        const val EXTRA_ENTRY_IDS = "entry_ids"
        const val EXTRA_MEDICINE_NAMES = "medicine_names"
        const val EXTRA_NAGGING_COUNT = "nagging_count"

        fun createNotificationChannels(context: Context) {
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

                val refillChannel = NotificationChannel(
                    REFILL_CHANNEL_ID,
                    context.getString(com.example.dosezy.R.string.notif_channel_refill_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(com.example.dosezy.R.string.notif_channel_refill_desc)
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(refillChannel)
            }
        }

        // Guard: Update active slot notification in shade when individual medications in a cohort are marked taken in-app without re-triggering full-screen activity
        fun updateCohortNotification(
            context: Context,
            entryId: String,
            entryIds: ArrayList<String>?,
            medicineName: String,
            medicineNames: ArrayList<String>?,
            medicineDetails: List<Pair<String, String>> = emptyList(),
            scheduledTime: String?,
            snoozeMinutes: Int = 10
        ) {
            MedicineAlarmReceiver().showNotification(
                context = context,
                entryId = entryId,
                entryIds = entryIds,
                medicineName = medicineName,
                medicineNames = medicineNames,
                medicineDetails = medicineDetails,
                scheduledTime = scheduledTime,
                isNagging = false,
                naggingCount = 0,
                maxNagging = 3,
                snoozeMinutes = snoozeMinutes,
                isUpdateOnly = true
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        database = DosezyDatabase.getInstance(context)
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "android.intent.action.LOCKED_BOOT_COMPLETED" ||
            action == Intent.ACTION_REBOOT ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED) {
            
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
            // Guard: ACQUIRE_CAUSES_WAKEUP deprecated in API 33+ but required to turn screen on for alarms on API 24-32
            @Suppress("DEPRECATION")
            val wakeLock = powerManager?.newWakeLock(
                android.os.PowerManager.PARTIAL_WAKE_LOCK or
                        android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        android.os.PowerManager.ON_AFTER_RELEASE,
                "Dosezy:MedicineAlarmWakeLock"
            )
            wakeLock?.acquire(30 * 1000L) // 30 seconds

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val allEntryIds = entryIds ?: listOf(entryId)
                    val validPendingEntries = allEntryIds.mapNotNull { id ->
                        database.scheduleDao().getScheduleEntryById(id)
                    }.filter { it.status == com.example.dosezy.data.model.MedicationStatus.PENDING }

                    if (validPendingEntries.isEmpty()) {
                        Log.d(TAG, "Ignoring alarm - no pending schedule entries found for entry IDs: $allEntryIds")
                        return@launch
                    }

                    val primaryEntry = validPendingEntries.first()
                    val user = database.userDao().getUserByIdDirect(primaryEntry.userId)
                    val isNagging = naggingCount > 0

                    // Retrieve medicine dosage details for each valid pending entry
                    val medicineDetails = validPendingEntries.mapNotNull { e ->
                        val m = database.medicineDao().getMedicineByIdDirect(e.medicineId) ?: return@mapNotNull null
                        val dose = m.getDosageDisplay(e.scheduledDateTime.toLocalTime())
                        Pair(m.medicationName, dose)
                    }

                    val activeMedicineNames = ArrayList(validPendingEntries.mapNotNull { e ->
                        database.medicineDao().getMedicineByIdDirect(e.medicineId)?.medicationName
                    }.distinct())
                    val effectiveMedicineName = if (activeMedicineNames.isNotEmpty()) activeMedicineNames.joinToString(", ") else medicineName
                    val activeEntryIds = ArrayList(validPendingEntries.map { it.entryId })

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

                    val savedLanguage = com.example.dosezy.utils.LocaleHelper.getSavedLanguage(context)
                    val currentLocale = com.example.dosezy.utils.LocaleHelper.getLocale(savedLanguage)

                    // Guard: Core library desugaring enables java.time on API 24+; format scheduled time with user's preferred 12h/24h mode and locale across all supported OS versions
                    val formattedScheduledTime = if (user != null) {
                        try {
                            com.example.dosezy.utils.TimeFormatUtils.formatTime(primaryEntry.scheduledDateTime, user.timeFormat, currentLocale)
                        } catch (_: Exception) {
                            scheduledTime
                        }
                    } else {
                        scheduledTime
                    }

                    showNotification(
                        context = context,
                        entryId = primaryEntry.entryId,
                        entryIds = activeEntryIds,
                        medicineName = effectiveMedicineName,
                        medicineNames = activeMedicineNames,
                        medicineDetails = medicineDetails,
                        scheduledTime = formattedScheduledTime,
                        isNagging = isNagging,
                        naggingCount = naggingCount,
                        maxNagging = user?.naggingMaxRepeats ?: 3,
                        snoozeMinutes = user?.snoozeDuration ?: 10
                    )

                    // Schedule next follow-up nagging reminder if enabled and below limit
                    if (user != null && user.naggingRemindersEnabled && naggingCount < user.naggingMaxRepeats) {
                        val alarmScheduler = AlarmScheduler(context)
                        alarmScheduler.scheduleNaggingReminder(
                            entryId = primaryEntry.entryId,
                            minutes = user.naggingIntervalMinutes,
                            medicineName = effectiveMedicineName,
                            naggingCount = naggingCount + 1,
                            entryIds = activeEntryIds,
                            medicineNames = activeMedicineNames,
                            scheduledTime = formattedScheduledTime
                        )
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Error processing alarm in background", ex)
                    try {
                        AlarmAudioPlayer.play(
                            context = context,
                            sound = com.example.dosezy.data.model.AlarmSound.SYSTEM_DEFAULT,
                            customPath = null,
                            autoSilenceSeconds = 0
                        )
                        showNotification(context, entryId, entryIds, medicineName, medicineNames, emptyList(), scheduledTime, false, 0, 3)
                    } catch (fallbackEx: Exception) {
                        Log.e(TAG, "Error in fallback alarm/notification handling", fallbackEx)
                    }
                } finally {
                    try {
                        if (wakeLock?.isHeld == true) {
                            wakeLock.release()
                        }
                    } catch (_: Exception) {}
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
        medicineDetails: List<Pair<String, String>> = emptyList(),
        scheduledTime: String?,
        isNagging: Boolean = false,
        naggingCount: Int = 0,
        maxNagging: Int = 3,
        snoozeMinutes: Int = 10,
        isUpdateOnly: Boolean = false
    ) {
        val savedLanguage = com.example.dosezy.utils.LocaleHelper.getSavedLanguage(context)
        val localizedContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, savedLanguage)

        createNotificationChannels(localizedContext)

        val scheduledTimeStr = scheduledTime?.let {
            localizedContext.getString(R.string.notif_content_scheduled_format, it)
        } ?: localizedContext.getString(R.string.notif_content_generic_reminder)

        val effectiveDetails = if (medicineDetails.isNotEmpty()) {
            medicineDetails
        } else if (medicineNames != null && medicineNames.isNotEmpty()) {
            medicineNames.map { Pair(it, "") }
        } else {
            listOf(Pair(medicineName, ""))
        }

        val primaryMedLabel = if (effectiveDetails.isNotEmpty()) {
            val first = effectiveDetails.first()
            if (first.second.isNotEmpty()) "${first.first} (${first.second})" else first.first
        } else {
            medicineName
        }

        // Guard: Use SINGLE_TOP/CLEAR_TOP so clicking the notification delivers to onNewIntent or launches MainActivity with schedule route
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
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
            if (entryIds != null) {
                putStringArrayListExtra(EXTRA_ENTRY_IDS, entryIds)
            }
        }

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "SNOOZE_ACTION"
            putExtra(EXTRA_ENTRY_ID, entryId)
            if (entryIds != null) {
                putStringArrayListExtra(EXTRA_ENTRY_IDS, entryIds)
            }
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            if (medicineNames != null) {
                putStringArrayListExtra(EXTRA_MEDICINE_NAMES, medicineNames)
            }
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

        // Android 14+ background activity start options for PendingIntent creation
        // Guard: MODE_BACKGROUND_ACTIVITY_START_ALLOWED deprecated in API 35+ but required on Android 14 (API 34)
        val optionsBundle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            android.app.ActivityOptions.makeBasic().apply {
                setPendingIntentCreatorBackgroundActivityStartMode(
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

        // Launch AlarmActivity directly only on full alarms, never during silent shade notification updates
        if (!isUpdateOnly) {
            try {
                context.startActivity(alarmIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Could not start AlarmActivity directly", e)
            }
        }

        val notificationTitle = if (isNagging) {
            localizedContext.getString(R.string.notif_nagging_title, primaryMedLabel)
        } else if (effectiveDetails.size > 1) {
            localizedContext.getString(R.string.alarm_medication_reminder_multi, effectiveDetails.size)
        } else {
            localizedContext.getString(R.string.alarm_medication_reminder)
        }

        val notificationText = if (isNagging) {
            localizedContext.getString(R.string.notif_nagging_text, naggingCount, maxNagging, scheduledTime ?: "")
        } else if (effectiveDetails.size == 1) {
            "$primaryMedLabel • $scheduledTimeStr"
        } else {
            val summary = effectiveDetails.joinToString(", ") { (name, dose) ->
                if (dose.isNotEmpty()) "$name ($dose)" else name
            }
            "$summary • $scheduledTimeStr"
        }

        val bigText = if (isNagging) {
            notificationText
        } else {
            buildString {
                effectiveDetails.forEach { (name, dose) ->
                    append("• ").append(name)
                    if (dose.isNotEmpty()) {
                        append(" (").append(dose).append(")")
                    }
                    append("\n")
                }
                append(scheduledTimeStr)
            }.trimEnd()
        }

        // Create silent notification: AlarmAudioPlayer is the single source of sound & vibration
        val notificationBuilder = NotificationCompat.Builder(localizedContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medicine_notification)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .addAction(
                getNotificationIcon(Icons.Filled.Check),
                localizedContext.getString(R.string.home_action_taken),
                takenPendingIntent
            )
            .addAction(
                getNotificationIcon(Icons.Filled.Snooze),
                localizedContext.getString(R.string.notif_action_snooze_format, snoozeMinutes),
                snoozePendingIntent
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))

        if (!isUpdateOnly) {
            notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        val notification = notificationBuilder.build()

        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(entryId.hashCode(), notification)
            Log.d(TAG, "Showing notification and launching full-screen alarm for: $medicineName")
        } catch (e: Exception) {
            Log.e(TAG, "Could not post alarm notification", e)
        }
    }



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

            Log.d(TAG, "Successfully rescheduled alarms for ${allUsers.size} users after boot/timezone change")
            com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling alarms after boot/timezone change", e)
        }
    }

    // Helper function to convert Compose icons to NotificationCompat.Action
    private fun getNotificationIcon(icon: androidx.compose.ui.graphics.vector.ImageVector): Int {
        // Using different built-in system icons for the actions
        return when (icon) {
            Icons.Filled.Check -> android.R.drawable.checkbox_on_background
            Icons.Filled.Snooze -> android.R.drawable.ic_lock_idle_alarm
            else -> android.R.drawable.ic_dialog_info
        }
    }
}