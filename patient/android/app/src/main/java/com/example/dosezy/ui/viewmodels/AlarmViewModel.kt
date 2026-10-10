/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.AlarmSound
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.notifications.AlarmAudioPlayer
import com.example.dosezy.notifications.AlarmScheduler
import com.example.dosezy.utils.TimeCalculationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class AlarmUiState(
    val isLoaded: Boolean = false,
    val medicinesList: List<Pair<ScheduleEntry, Medicine>> = emptyList(),
    val user: User? = null
)

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val database: DosezyDatabase,
    private val scheduleRepository: ScheduleRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    constructor(
        database: DosezyDatabase,
        scheduleRepository: ScheduleRepository,
        context: Context,
        ioDispatcher: CoroutineDispatcher
    ) : this(database, scheduleRepository, context) {
        this.ioDispatcher = ioDispatcher
    }

    private val _uiState = MutableStateFlow(AlarmUiState())
    val uiState: StateFlow<AlarmUiState> = _uiState.asStateFlow()

    fun loadCohort(entryIds: List<String>) {
        viewModelScope.launch(ioDispatcher) {
            val list = mutableListOf<Pair<ScheduleEntry, Medicine>>()
            var targetUser: User? = null
            for (id in entryIds) {
                val entry = database.scheduleDao().getScheduleEntryById(id)
                if (entry != null) {
                    val med = database.medicineDao().getMedicineByIdDirect(entry.medicineId)
                    if (med != null) {
                        list.add(Pair(entry, med))
                        if (targetUser == null) {
                            targetUser = database.userDao().getUserByIdDirect(entry.userId)
                        }
                    }
                }
            }
            if (targetUser == null) {
                val users = database.userDao().getAllUsersDirect()
                targetUser = users.find { it.isCurrentUser } ?: users.firstOrNull()
            }
            _uiState.value = AlarmUiState(
                isLoaded = true,
                medicinesList = list,
                user = targetUser
            )
        }
    }

    fun startPlaybackIfNeeded(entryId: String) {
        if (AlarmAudioPlayer.isPlaying()) return
        viewModelScope.launch(ioDispatcher) {
            var targetSound: AlarmSound = AlarmSound.SYSTEM_DEFAULT
            var customSoundPath: String? = null
            var autoSilenceSeconds: Int? = null
            try {
                if (entryId.isNotEmpty()) {
                    val entry = database.scheduleDao().getScheduleEntryById(entryId)
                    if (entry != null) {
                        val u = database.userDao().getUserByIdDirect(entry.userId)
                        if (u != null) {
                            targetSound = u.alarmSound
                            customSoundPath = u.customAlarmSoundPath
                            autoSilenceSeconds = u.alarmDurationSeconds
                        }
                    }
                }
                if (autoSilenceSeconds == null) {
                    val users = database.userDao().getAllUsersDirect()
                    val currentUser = users.find { it.isCurrentUser } ?: users.firstOrNull()
                    if (currentUser != null) {
                        if (targetSound == AlarmSound.SYSTEM_DEFAULT && customSoundPath == null) {
                            targetSound = currentUser.alarmSound
                            customSoundPath = currentUser.customAlarmSoundPath
                        }
                        autoSilenceSeconds = currentUser.alarmDurationSeconds
                    }
                }
            } catch (_: Exception) {}

            AlarmAudioPlayer.play(
                context = context,
                sound = targetSound,
                customPath = customSoundPath,
                autoSilenceSeconds = autoSilenceSeconds ?: 0
            )
        }
    }

    fun takeCohort(
        entryIds: List<String>,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch(ioDispatcher) {
            val alarmScheduler = AlarmScheduler(context)
            val medicinesList = _uiState.value.medicinesList
            val user = _uiState.value.user
            val allIds = if (medicinesList.isNotEmpty()) {
                medicinesList.map { it.first.entryId }
            } else {
                entryIds
            }
            allIds.forEach { id ->
                // Guard: Cancel both nagging and snooze alarms upfront for all cohort IDs to prevent delayed phantom snooze triggers
                alarmScheduler.cancelNagging(id)
                alarmScheduler.cancelSnooze(id)
            }
            val now = LocalDateTime.now()
            val nowStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val lateAfter = user?.considerLateAfter ?: 3
            if (medicinesList.isNotEmpty()) {
                medicinesList.forEach { (entry, _) ->
                    // Guard: Use isTakenLate so delayed/snoozed or overdue doses past missedAfter are correctly recorded as TAKEN_LATE
                    val status = if (TimeCalculationUtils.isTakenLate(entry.scheduledDateTime, now, lateAfter)) {
                        "TAKEN_LATE"
                    } else {
                        "TAKEN_ON_TIME"
                    }
                    scheduleRepository.recordDoseTaken(entry.entryId, status, nowStr, context)
                }
            } else if (entryIds.isNotEmpty()) {
                entryIds.forEach { id ->
                    val entry = database.scheduleDao().getScheduleEntryById(id)
                    val entryUser = entry?.let { database.userDao().getUserByIdDirect(it.userId) } ?: user
                    val entryLateAfter = entryUser?.considerLateAfter ?: 3
                    // Guard: Use isTakenLate so delayed/snoozed or overdue doses past missedAfter are correctly recorded as TAKEN_LATE
                    val status = if (entry != null && TimeCalculationUtils.isTakenLate(entry.scheduledDateTime, now, entryLateAfter)) {
                        "TAKEN_LATE"
                    } else {
                        "TAKEN_ON_TIME"
                    }
                    scheduleRepository.recordDoseTaken(id, status, nowStr, context)
                }
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun snoozeCohort(
        entryIds: List<String>,
        snoozeMinutes: Int,
        initialMedicineName: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch(ioDispatcher) {
            val alarmScheduler = AlarmScheduler(context)
            val medicinesList = _uiState.value.medicinesList
            val user = _uiState.value.user
            val allIds = if (medicinesList.isNotEmpty()) {
                medicinesList.map { it.first.entryId }
            } else {
                entryIds
            }
            allIds.forEach { id ->
                alarmScheduler.cancelNagging(id)
            }
            if (medicinesList.isNotEmpty()) {
                val ids = medicinesList.map { it.first.entryId }
                val names = medicinesList.map { it.second.medicationName }
                alarmScheduler.scheduleGroupedSnooze(ids, snoozeMinutes, names, timeFormat = user?.timeFormat)
            } else if (entryIds.isNotEmpty()) {
                alarmScheduler.scheduleGroupedSnooze(entryIds, snoozeMinutes, listOf(initialMedicineName), timeFormat = user?.timeFormat)
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun skipCohort(
        entryIds: List<String>,
        reason: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch(ioDispatcher) {
            val alarmScheduler = AlarmScheduler(context)
            val medicinesList = _uiState.value.medicinesList
            val allIds = if (medicinesList.isNotEmpty()) {
                medicinesList.map { it.first.entryId }
            } else {
                entryIds
            }
            allIds.forEach { id ->
                // Guard: Cancel both nagging and snooze alarms upfront for all cohort IDs to prevent delayed phantom snooze triggers when skipped
                alarmScheduler.cancelNagging(id)
                alarmScheduler.cancelSnooze(id)
            }
            if (medicinesList.isNotEmpty()) {
                medicinesList.forEach { (entry, _) ->
                    scheduleRepository.recordDoseSkipped(entry.entryId, reason, context)
                }
            } else if (entryIds.isNotEmpty()) {
                entryIds.forEach { id ->
                    scheduleRepository.recordDoseSkipped(id, reason, context)
                }
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}
