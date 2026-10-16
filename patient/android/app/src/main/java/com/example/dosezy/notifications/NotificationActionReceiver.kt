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

                // Guard: If intake safeguard is enabled, start countdown timer instead of immediately marking taken (#104)
                if (user?.intakeSafeguardEnabled == true) {
                    val medicineNames = allIds.mapNotNull { id ->
                        val entry = database.scheduleDao().getScheduleEntryById(id)
                        val med = entry?.let { database.medicineDao().getMedicineByIdDirect(it.medicineId) }
                        med?.medicationName
                    }
                    val primaryMedName = medicineNames.firstOrNull() ?: "Medicine"
                    IntakeSafeguardManager.startCountdown(
                        context = context,
                        entryId = primaryEntryId,
                        entryIds = allIds,
                        medicineName = primaryMedName,
                        medicineNames = medicineNames,
                        scheduledTime = "",
                        durationSeconds = user.intakeCountdownSeconds
                    )
                    Log.d(TAG, "Started intake countdown safeguard for ${allIds.size} medicines")
                    return
                }

                val now = LocalDateTime.now()
                val takenAt = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
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
                    scheduleRepository.recordDoseTaken(id, status, takenAt, context)
                }
                Log.d(TAG, "Marked ${allIds.size} medicines as taken and processed")
            }
            "SNOOZE_ACTION" -> {
                Log.d(TAG, "Snoozing medicine reminder for ${allIds.size} entries")
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