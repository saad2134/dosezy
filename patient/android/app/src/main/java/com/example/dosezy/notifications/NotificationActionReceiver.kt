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
                // Guard: Hold goAsync() to prevent process termination before reminder notification is restored (#BUG-SAFEGUARD-CANCEL-RESTORE)
                val pendingResult = try { goAsync() } catch (_: Exception) { null }
                IntakeSafeguardManager.cancelCountdown(context, restoreReminderIfPending = true) {
                    pendingResult?.finish()
                }
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

        // Guard: Reconcile all pending sibling schedule entries in the same time slot to ensure 4+ medication cohorts are fully loaded (#115)
        val targetIds = allIds.toMutableList()
        val firstEntry = database.scheduleDao().getScheduleEntryById(primaryEntryId)
        if (firstEntry != null) {
            val slotMillis = firstEntry.scheduledDateTime.withSecond(0).withNano(0)
                .atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            val slotEntries = database.scheduleDao().getScheduleForDateRangeDirect(
                firstEntry.userId,
                slotMillis,
                slotMillis + 59999L
            ).filter { it.status == com.example.dosezy.data.model.MedicationStatus.PENDING && !it.entryId.startsWith("PRN_") }
            val existingSet = targetIds.toSet()
            for (sibling in slotEntries) {
                if (sibling.entryId !in existingSet) {
                    targetIds.add(sibling.entryId)
                }
            }
        }
        val finalAllIds = targetIds.distinct()

        val alarmScheduler = AlarmScheduler(context)
        finalAllIds.forEach { id ->
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
        finalAllIds.forEach { id ->
            notificationManager.cancel(id.hashCode())
        }

        when (action) {
            "TAKEN_ACTION" -> {
                val user = firstEntry?.let { database.userDao().getUserByIdDirect(it.userId) }

                // Guard: Only start intake countdown safeguard if not responding to an expired safeguard re-alert to prevent infinite notification loops (#104)
                val isIntakeReAlert = intent?.getBooleanExtra(MedicineAlarmReceiver.EXTRA_IS_INTAKE_REALERT, false) ?: false
                if (user?.intakeSafeguardEnabled == true && !isIntakeReAlert) {
                    val medicineNames = finalAllIds.mapNotNull { id ->
                        val entry = database.scheduleDao().getScheduleEntryById(id)
                        val med = entry?.let { database.medicineDao().getMedicineByIdDirect(it.medicineId) }
                        med?.medicationName
                    }
                    val compositeMedName = if (medicineNames.size > 1) {
                        medicineNames.joinToString(", ")
                    } else {
                        medicineNames.firstOrNull() ?: "Medicine"
                    }
                    // Guard: Pass formatted scheduled time and composite names so safeguard notifications and expiry re-alerts display accurate time and all cohort medicines (#BUG-SAFEGUARD-COHORT-NAMES)
                    val is24 = user.timeFormat == com.example.dosezy.data.model.TimeFormat.HOUR_24 ||
                            android.text.format.DateFormat.is24HourFormat(context)
                    val timePattern = if (is24) "HH:mm" else "h:mm a"
                    // Guard: Derive locale from user's explicit profile language to ensure safeguard notification time matches profile locale (#BUG-NOTIF-LOCALE)
                    val targetLang = user.language
                    val locale = com.example.dosezy.utils.LocaleHelper.getLocale(targetLang)
                    val scheduledTimeStr = intent?.getStringExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME)
                        ?.takeIf { it.isNotBlank() }
                        ?: firstEntry.scheduledDateTime.let { it.format(java.time.format.DateTimeFormatter.ofPattern(timePattern, locale)) }
                        ?: ""
                    IntakeSafeguardManager.startCountdown(
                        context = context,
                        entryId = primaryEntryId,
                        entryIds = finalAllIds,
                        medicineName = compositeMedName,
                        medicineNames = medicineNames,
                        scheduledTime = scheduledTimeStr,
                        durationSeconds = user.intakeCountdownSeconds,
                        userId = firstEntry.userId
                    )
                    Log.d(TAG, "Started intake countdown safeguard for ${finalAllIds.size} medicines ($compositeMedName)")
                    return
                }

                val now = LocalDateTime.now()
                val takenAt = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                val notifAlarmScheduler = AlarmScheduler(context)
                finalAllIds.forEach { id ->
                    val entry = database.scheduleDao().getScheduleEntryById(id)
                    val entryUser = user ?: entry?.let { database.userDao().getUserByIdDirect(it.userId) }
                    val lateAfter = entryUser?.considerLateAfter ?: 3
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

                // Guard: Coordinate slot notification sync and disarm alarms across each distinct time slot in finalAllIds to prevent leaking alarms or shade entries (#BUG-RECEIVER-MULTI-SLOT)
                val entries = finalAllIds.mapNotNull { database.scheduleDao().getScheduleEntryById(it) }
                val distinctSlots = entries.distinctBy { it.scheduledDateTime.withSecond(0).withNano(0) }
                if (distinctSlots.isNotEmpty()) {
                    distinctSlots.forEach { slotEntry ->
                        scheduleRepository.syncSlotNotificationAndAlarms(slotEntry.entryId, slotEntry, context)
                    }
                } else {
                    val updatedFirstEntry = database.scheduleDao().getScheduleEntryById(primaryEntryId) ?: firstEntry
                    if (updatedFirstEntry != null) {
                        scheduleRepository.syncSlotNotificationAndAlarms(primaryEntryId, updatedFirstEntry, context)
                    } else {
                        try {
                            com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
                        } catch (_: Exception) {}
                    }
                }
                Log.d(TAG, "Marked ${finalAllIds.size} medicines as taken and processed")
            }
            "SNOOZE_ACTION" -> {
                Log.d(TAG, "Snoozing medicine reminder for ${finalAllIds.size} entries")
                // Guard: Reconcile active ADHD intake countdown notification if snoozed entries match active countdown to prevent dropping remaining pending medications (#BUG-SAFEGUARD-PARTIAL-COHORT)
                try {
                    val activeCountdown = com.example.dosezy.notifications.IntakeSafeguardManager.activeCountdown.value
                    if (activeCountdown != null && (finalAllIds.contains(activeCountdown.entryId) || activeCountdown.entryIds.any { finalAllIds.contains(it) })) {
                        com.example.dosezy.notifications.IntakeSafeguardManager.reconcileCountdown(context, finalAllIds)
                    }
                } catch (_: Exception) {}

                val user = firstEntry?.let { database.userDao().getUserByIdDirect(it.userId) }
                // Guard: Read explicit dynamic snooze duration from intent if passed, else fallback to user preference (#103)
                val intentSnoozeMinutes = intent?.getIntExtra(MedicineAlarmReceiver.EXTRA_SNOOZE_MINUTES, -1) ?: -1
                val snoozeMinutes = if (intentSnoozeMinutes > 0) intentSnoozeMinutes else (user?.snoozeDuration ?: 10)

                val medicineNames = finalAllIds.mapNotNull { id ->
                    val entry = database.scheduleDao().getScheduleEntryById(id)
                    val med = entry?.let { database.medicineDao().getMedicineByIdDirect(it.medicineId) }
                    med?.medicationName
                }

                // Guard: Pass user's timeFormat and userId so snoozed alarm matches time format and routes to correct profile on tap
                alarmScheduler.scheduleGroupedSnooze(finalAllIds, snoozeMinutes, medicineNames, timeFormat = user?.timeFormat, userId = user?.userId)
                Log.d(TAG, "Medicine reminder snoozed for $snoozeMinutes minutes for ${finalAllIds.size} entries")
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