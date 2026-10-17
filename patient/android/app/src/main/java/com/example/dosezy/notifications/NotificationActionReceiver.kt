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

    override fun onReceive(context: Context, intent: Intent?) {
        database = DosezyDatabase.getInstance(context)
        val action = intent?.action

        // Guard: Handle ADHD intake countdown safeguard notification interactions (#104)
        when (action) {
            IntakeSafeguardManager.ACTION_INTAKE_CONFIRM -> {
                // Guard: goAsync() returns null in unit tests and can throw if receiver lifecycle expired; guard against NPE
                val pendingResult = try { goAsync() } catch (_: Exception) { null }
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        IntakeSafeguardManager.confirmIntake(context, database)
                        try {
                            com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
                        } catch (_: Exception) {}
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling intake confirmation", e)
                    } finally {
                        pendingResult?.finish()
                    }
                }
                return
            }
            IntakeSafeguardManager.ACTION_INTAKE_EXTEND -> {
                IntakeSafeguardManager.extendCountdown(context, 60)
                return
            }
            IntakeSafeguardManager.ACTION_INTAKE_CANCEL -> {
                IntakeSafeguardManager.cancelCountdown(context)
                return
            }
        }

        val entryId = intent?.getStringExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID)
        val entryIds = intent?.getStringArrayListExtra(MedicineAlarmReceiver.EXTRA_ENTRY_IDS)
        val allIds = (entryIds ?: if (entryId != null) listOf(entryId) else emptyList()).filter { it.isNotEmpty() }.distinct()

        if (allIds.isNotEmpty()) {
            // Guard: goAsync() returns null in unit tests and can throw if receiver lifecycle expired; guard against NPE
            val pendingResult = try { goAsync() } catch (_: Exception) { null }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    handleAction(context, action, allIds, entryId ?: allIds.first(), intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling notification action", e)
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }

    private suspend fun handleAction(
        context: Context,
        action: String?,
        allIds: List<String>,
        primaryEntryId: String,
        intent: Intent? = null
    ) {
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
            if (action == "SNOOZE_ACTION") {
                // Guard: Cancel active intake safeguard alarm for each entry to prevent orphaned re-alerts after snoozing (#104)
                alarmScheduler.cancelIntakeSafeguardAlarm(id)
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
                val firstEntry = database.scheduleDao().getScheduleEntryById(primaryEntryId)
                val user = firstEntry?.let { database.userDao().getUserByIdDirect(it.userId) }

                // Guard: Only start intake countdown safeguard if not responding to an expired safeguard re-alert to prevent infinite notification loops (#104)
                val isIntakeReAlert = intent?.getBooleanExtra(MedicineAlarmReceiver.EXTRA_IS_INTAKE_REALERT, false) ?: false
                if (user?.intakeSafeguardEnabled == true && !isIntakeReAlert) {
                    val medicineNames = allIds.mapNotNull { id ->
                        val entry = database.scheduleDao().getScheduleEntryById(id)
                        val med = entry?.let { database.medicineDao().getMedicineByIdDirect(it.medicineId) }
                        med?.medicationName
                    }
                    val primaryMedName = medicineNames.firstOrNull() ?: "Medicine"
                    // Guard: Pass formatted scheduled time to IntakeSafeguardManager so safeguard notifications and expiry re-alerts display accurate time
                    val is24 = user.timeFormat == com.example.dosezy.data.model.TimeFormat.HOUR_24 ||
                            android.text.format.DateFormat.is24HourFormat(context)
                    val timePattern = if (is24) "HH:mm" else "h:mm a"
                    val savedLang = com.example.dosezy.utils.LocaleHelper.getSavedLanguage(context)
                    val locale = com.example.dosezy.utils.LocaleHelper.getLocale(savedLang)
                    val scheduledTimeStr = intent?.getStringExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME)
                        ?.takeIf { it.isNotBlank() }
                        ?: firstEntry.scheduledDateTime.let { it.format(java.time.format.DateTimeFormatter.ofPattern(timePattern, locale)) }
                    IntakeSafeguardManager.startCountdown(
                        context = context,
                        entryId = primaryEntryId,
                        entryIds = allIds,
                        medicineName = primaryMedName,
                        medicineNames = medicineNames,
                        scheduledTime = scheduledTimeStr,
                        durationSeconds = user.intakeCountdownSeconds,
                        userId = firstEntry.userId
                    )
                    Log.d(TAG, "Started intake countdown safeguard for ${allIds.size} medicines")
                    return
                }

                val now = LocalDateTime.now()
                val takenAt = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                val notifAlarmScheduler = AlarmScheduler(context)
                allIds.forEach { id ->
                    val entry = database.scheduleDao().getScheduleEntryById(id)
                    val lateAfter = user?.considerLateAfter ?: 3
                    // Guard: Use isTakenLate so doses taken past missedAfter are recorded as TAKEN_LATE rather than TAKEN_ON_TIME
                    val status = if (entry != null && com.example.dosezy.utils.TimeCalculationUtils.isTakenLate(entry.scheduledDateTime, now, lateAfter)) {
                        "TAKEN_LATE"
                    } else {
                        "TAKEN_ON_TIME"
                    }
                    Log.d(TAG, "Marking medicine as taken ($status) for entry: $id")
                    notifAlarmScheduler.cancelNagging(id)
                    notifAlarmScheduler.cancelSnooze(id)
                    notifAlarmScheduler.cancelIntakeSafeguardAlarm(id)
                    // Guard: Pass context with syncSlotNotification = false so stock deduction triggers refill alert while intermediate slot synchronization is deferred
                    scheduleRepository.recordDoseTaken(id, status, takenAt, context, notes = null, syncSlotNotification = false)
                }

                // Guard: Perform coordinated single slot sync and widget update after all cohort entries in SQLite are updated to prevent shade notification flickering and duplicate widget updates
                val updatedFirstEntry = database.scheduleDao().getScheduleEntryById(primaryEntryId) ?: firstEntry
                if (updatedFirstEntry != null) {
                    scheduleRepository.syncSlotNotificationAndAlarms(primaryEntryId, updatedFirstEntry, context)
                } else {
                    try {
                        com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
                    } catch (_: Exception) {}
                }
                Log.d(TAG, "Marked ${allIds.size} medicines as taken and processed")
            }
            "SNOOZE_ACTION" -> {
                Log.d(TAG, "Snoozing medicine reminder for ${allIds.size} entries")
                // Guard: Reconcile and dismiss active ADHD intake countdown notification if snoozed entries match active countdown (#104)
                try {
                    val activeCountdown = com.example.dosezy.notifications.IntakeSafeguardManager.activeCountdown.value
                    if (activeCountdown != null && (allIds.contains(activeCountdown.entryId) || activeCountdown.entryIds.any { allIds.contains(it) })) {
                        com.example.dosezy.notifications.IntakeSafeguardManager.cancelCountdown(context)
                    }
                } catch (_: Exception) {}

                val firstEntry = database.scheduleDao().getScheduleEntryById(primaryEntryId)
                val user = firstEntry?.let { database.userDao().getUserByIdDirect(it.userId) }
                // Guard: Read explicit dynamic snooze duration from intent if passed, else fallback to user preference (#103)
                val intentSnoozeMinutes = intent?.getIntExtra(MedicineAlarmReceiver.EXTRA_SNOOZE_MINUTES, -1) ?: -1
                val snoozeMinutes = if (intentSnoozeMinutes > 0) intentSnoozeMinutes else (user?.snoozeDuration ?: 10)

                val medicineNames = allIds.mapNotNull { id ->
                    val entry = database.scheduleDao().getScheduleEntryById(id)
                    val med = entry?.let { database.medicineDao().getMedicineByIdDirect(it.medicineId) }
                    med?.medicationName
                }

                // Guard: Pass user's timeFormat so snoozed alarm time matches user's 12h/24h preference
                alarmScheduler.scheduleGroupedSnooze(allIds, snoozeMinutes, medicineNames, timeFormat = user?.timeFormat)
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