/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.dosezy.MainActivity
import com.example.dosezy.R
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.utils.TimeCalculationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Coordination manager for the ADHD intake countdown timer with re-alert safeguard.
 * When enabled, tapping Take initiates a countdown buffer instead of immediately writing intake to Room.
 * If unconfirmed when the timer expires, Dosezy re-alerts the user.
 */
object IntakeSafeguardManager {

    private const val TAG = "IntakeSafeguardManager"
    private const val PREFS_SAFEGUARD = "dosezy_intake_safeguard"
    const val INTAKE_NOTIFICATION_BASE_ID = 900000

    const val ACTION_INTAKE_CONFIRM = "com.example.dosezy.INTAKE_CONFIRM_ACTION"
    const val ACTION_INTAKE_EXTEND = "com.example.dosezy.INTAKE_EXTEND_ACTION"
    const val ACTION_INTAKE_CANCEL = "com.example.dosezy.INTAKE_CANCEL_ACTION"

    data class IntakeCountdownState(
        val entryId: String,
        val entryIds: List<String>,
        val medicineName: String,
        val medicineNames: List<String>,
        val scheduledTime: String,
        val startTimestamp: Long,
        val targetTimestamp: Long,
        val totalDurationSeconds: Int
    )

    private val _activeCountdown = MutableStateFlow<IntakeCountdownState?>(null)
    val activeCountdown: StateFlow<IntakeCountdownState?> = _activeCountdown.asStateFlow()

    fun initialize(context: Context) {
        val state = restoreState(context)
        if (state != null) {
            val now = System.currentTimeMillis()
            if (now >= state.targetTimestamp) {
                // Timer already expired while process was killed; trigger expiration
                handleCountdownExpired(
                    context = context,
                    entryId = state.entryId,
                    medicineName = state.medicineName,
                    entryIds = state.entryIds,
                    medicineNames = state.medicineNames,
                    scheduledTime = state.scheduledTime
                )
            } else {
                _activeCountdown.value = state
            }
        }
    }

    fun startCountdown(
        context: Context,
        entryId: String,
        entryIds: List<String>,
        medicineName: String,
        medicineNames: List<String>,
        scheduledTime: String,
        durationSeconds: Int
    ): IntakeCountdownState {
        val scheduler = AlarmScheduler(context)

        // Guard: Cancel and dismiss previous active countdown alarm and notification if another countdown was already running to prevent ghost alarms and notification leaks (#104)
        val priorState = _activeCountdown.value ?: restoreState(context)
        if (priorState != null && priorState.entryId != entryId) {
            scheduler.cancelIntakeSafeguardAlarm(priorState.entryId)
            dismissIntakeNotification(context, priorState.entryId)
        }

        val allTargetIds = if (entryIds.isEmpty()) listOf(entryId) else entryIds.distinct()

        // Guard: Cancel active nagging reminders and snooze alarms across all entries in the cohort so alarms do not ring over the user during the countdown buffer
        allTargetIds.forEach { id ->
            scheduler.cancelNagging(id)
            scheduler.cancelSnooze(id)
        }

        val now = System.currentTimeMillis()
        val target = now + (durationSeconds * 1000L)
        val state = IntakeCountdownState(
            entryId = entryId,
            entryIds = allTargetIds,
            medicineName = medicineName,
            medicineNames = if (medicineNames.isEmpty()) listOf(medicineName) else medicineNames.distinct(),
            scheduledTime = scheduledTime,
            startTimestamp = now,
            targetTimestamp = target,
            totalDurationSeconds = durationSeconds
        )

        saveState(context, state)
        _activeCountdown.value = state

        // Guard: Schedule exact re-alert alarm via AlarmManager in case the user leaves the app or puts phone to sleep (#104)
        scheduler.scheduleIntakeSafeguardAlarm(
            entryId = entryId,
            triggerTime = target,
            medicineName = medicineName,
            entryIds = ArrayList(state.entryIds),
            medicineNames = ArrayList(state.medicineNames),
            scheduledTime = scheduledTime
        )

        showIntakeNotification(context, state)
        Log.d(TAG, "Started intake countdown for $medicineName ($durationSeconds s, target: $target)")
        return state
    }

    fun extendCountdown(context: Context, additionalSeconds: Int = 60): IntakeCountdownState? {
        val current = _activeCountdown.value ?: restoreState(context) ?: return null
        val newTarget = current.targetTimestamp + (additionalSeconds * 1000L)
        val newDuration = current.totalDurationSeconds + additionalSeconds
        val updated = current.copy(
            targetTimestamp = newTarget,
            totalDurationSeconds = newDuration
        )

        saveState(context, updated)
        _activeCountdown.value = updated

        // Guard: Reschedule exact re-alert alarm with extended target timestamp
        val scheduler = AlarmScheduler(context)
        scheduler.cancelIntakeSafeguardAlarm(current.entryId)
        scheduler.scheduleIntakeSafeguardAlarm(
            entryId = current.entryId,
            triggerTime = newTarget,
            medicineName = current.medicineName,
            entryIds = ArrayList(current.entryIds),
            medicineNames = ArrayList(current.medicineNames),
            scheduledTime = current.scheduledTime
        )

        showIntakeNotification(context, updated)
        Log.d(TAG, "Extended intake countdown by $additionalSeconds s (new target: $newTarget)")
        return updated
    }

    suspend fun confirmIntake(context: Context, database: DosezyDatabase): Boolean {
        val current = _activeCountdown.value ?: restoreState(context) ?: return false

        // Guard: Cancel the exact safeguard re-alert alarm and clear notification on confirmed intake
        val scheduler = AlarmScheduler(context)
        scheduler.cancelIntakeSafeguardAlarm(current.entryId)
        dismissIntakeNotification(context, current.entryId)
        clearState(context)
        _activeCountdown.value = null

        val scheduleRepository = ScheduleRepository(database)
        val now = LocalDateTime.now()
        val takenAt = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        current.entryIds.forEach { id ->
            val entry = database.scheduleDao().getScheduleEntryById(id)
            val user = entry?.let { database.userDao().getUserByIdDirect(it.userId) }
            val lateAfter = user?.considerLateAfter ?: 3
            val status = if (entry != null && TimeCalculationUtils.isTakenLate(entry.scheduledDateTime, now, lateAfter)) {
                "TAKEN_LATE"
            } else {
                "TAKEN_ON_TIME"
            }
            Log.d(TAG, "Recording confirmed dose intake ($status) for entry: $id")
            scheduleRepository.recordDoseTaken(id, status, takenAt, context)
            scheduler.cancelNagging(id)
            scheduler.cancelSnooze(id)
        }

        return true
    }

    fun cancelCountdown(context: Context) {
        val current = _activeCountdown.value ?: restoreState(context)
        if (current != null) {
            val scheduler = AlarmScheduler(context)
            scheduler.cancelIntakeSafeguardAlarm(current.entryId)
            dismissIntakeNotification(context, current.entryId)
        }
        clearState(context)
        _activeCountdown.value = null
        try {
            com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
        } catch (_: Exception) {}
        Log.d(TAG, "Cancelled intake countdown")
    }

    fun handleCountdownExpired(
        context: Context,
        entryId: String,
        medicineName: String,
        entryIds: List<String>?,
        medicineNames: List<String>?,
        scheduledTime: String?,
        onComplete: (() -> Unit)? = null
    ) {
        dismissIntakeNotification(context, entryId)
        clearState(context)
        _activeCountdown.value = null
        try {
            com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
        } catch (_: Exception) {}

        // Trigger the re-alert via MedicineAlarmReceiver
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DosezyDatabase.getInstance(context)
                val entry = db.scheduleDao().getScheduleEntryById(entryId)
                // Guard: Only re-alert if entry still exists in database; do not fire ghost alarms if medication was deleted
                val isStillPending = entry != null && (entry.status == com.example.dosezy.data.model.MedicationStatus.PENDING || entry.status == com.example.dosezy.data.model.MedicationStatus.MISSED)

                if (isStillPending) {
                    Log.d(TAG, "Safeguard timer expired for unconfirmed entry $entryId; re-alerting user")
                    val user = entry?.let { db.userDao().getUserByIdDirect(it.userId) }
                    MedicineAlarmReceiver.triggerReAlert(
                        context = context,
                        entryId = entryId,
                        medicineName = medicineName,
                        entryIds = if (!entryIds.isNullOrEmpty()) ArrayList(entryIds) else null,
                        medicineNames = if (!medicineNames.isNullOrEmpty()) ArrayList(medicineNames) else null,
                        scheduledTime = scheduledTime,
                        user = user
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling expired intake safeguard", e)
            } finally {
                // Guard: Always invoke onComplete to release WakeLock and finish goAsync BroadcastReceiver lifecycle
                onComplete?.invoke()
            }
        }
    }

    fun showIntakeNotification(context: Context, state: IntakeCountdownState) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        MedicineAlarmReceiver.createNotificationChannels(context)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("fragment", "schedule")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            state.entryId.hashCode() + 4001,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val confirmIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_INTAKE_CONFIRM
            putExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID, state.entryId)
            putStringArrayListExtra(MedicineAlarmReceiver.EXTRA_ENTRY_IDS, ArrayList(state.entryIds))
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            context,
            state.entryId.hashCode() + 4002,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val extendIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_INTAKE_EXTEND
            putExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID, state.entryId)
        }
        val extendPendingIntent = PendingIntent.getBroadcast(
            context,
            state.entryId.hashCode() + 4003,
            extendIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_INTAKE_CANCEL
            putExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID, state.entryId)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            state.entryId.hashCode() + 4004,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Guard: Use setUsesChronometer and setChronometerCountDown so Android natively counts down in shade without continuous waking
        val builder = NotificationCompat.Builder(context, MedicineAlarmReceiver.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medicine_notification)
            .setContentTitle(context.getString(R.string.notif_intake_in_progress_title))
            .setContentText(context.getString(R.string.notif_intake_in_progress_desc, state.medicineName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setContentIntent(contentPendingIntent)
            .setWhen(state.targetTimestamp)
            .setUsesChronometer(true)
            .setShowWhen(true)
            .addAction(
                android.R.drawable.ic_menu_agenda,
                context.getString(R.string.notif_intake_action_confirm),
                confirmPendingIntent
            )
            .addAction(
                android.R.drawable.ic_input_add,
                context.getString(R.string.notif_intake_action_extend),
                extendPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.notif_intake_action_cancel),
                cancelPendingIntent
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setChronometerCountDown(true)
        }

        notificationManager.notify(INTAKE_NOTIFICATION_BASE_ID, builder.build())
    }

    fun dismissIntakeNotification(context: Context, @Suppress("UNUSED_PARAMETER") entryId: String = "") {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.cancel(INTAKE_NOTIFICATION_BASE_ID)
        } catch (_: Exception) {}
    }

    private fun saveState(context: Context, state: IntakeCountdownState) {
        val prefs = context.getSharedPreferences(PREFS_SAFEGUARD, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("entry_id", state.entryId)
            .putString("entry_ids", state.entryIds.joinToString(","))
            .putString("med_name", state.medicineName)
            .putString("med_names", state.medicineNames.joinToString("|||"))
            .putString("scheduled_time", state.scheduledTime)
            .putLong("start_time", state.startTimestamp)
            .putLong("target_time", state.targetTimestamp)
            .putInt("duration_sec", state.totalDurationSeconds)
            .apply()
    }

    private fun restoreState(context: Context): IntakeCountdownState? {
        val prefs = context.getSharedPreferences(PREFS_SAFEGUARD, Context.MODE_PRIVATE)
        val entryId = prefs.getString("entry_id", null) ?: return null
        val entryIdsStr = prefs.getString("entry_ids", "") ?: ""
        val medName = prefs.getString("med_name", "") ?: ""
        val medNamesStr = prefs.getString("med_names", "") ?: ""
        val scheduledTime = prefs.getString("scheduled_time", "") ?: ""
        val startTime = prefs.getLong("start_time", 0L)
        val targetTime = prefs.getLong("target_time", 0L)
        val durationSec = prefs.getInt("duration_sec", 120)

        val entryIds = entryIdsStr.split(",").filter { it.isNotBlank() }
        val medNames = medNamesStr.split("|||").filter { it.isNotBlank() }

        return IntakeCountdownState(
            entryId = entryId,
            entryIds = if (entryIds.isEmpty()) listOf(entryId) else entryIds,
            medicineName = medName,
            medicineNames = if (medNames.isEmpty()) listOf(medName) else medNames,
            scheduledTime = scheduledTime,
            startTimestamp = startTime,
            targetTimestamp = targetTime,
            totalDurationSeconds = durationSec
        )
    }

    private fun clearState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_SAFEGUARD, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
