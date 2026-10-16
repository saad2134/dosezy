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
import com.example.dosezy.domain.usecases.TakeCohortUseCase
import com.example.dosezy.domain.usecases.SnoozeCohortUseCase
import com.example.dosezy.domain.usecases.SkipCohortUseCase
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
    @ApplicationContext private val context: Context,
    private val takeCohortUseCase: TakeCohortUseCase,
    private val snoozeCohortUseCase: SnoozeCohortUseCase,
    private val skipCohortUseCase: SkipCohortUseCase
) : ViewModel() {

    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    constructor(
        database: DosezyDatabase,
        scheduleRepository: ScheduleRepository,
        context: Context,
        ioDispatcher: CoroutineDispatcher
    ) : this(
        database = database,
        scheduleRepository = scheduleRepository,
        context = context,
        takeCohortUseCase = TakeCohortUseCase(database, scheduleRepository, context),
        snoozeCohortUseCase = SnoozeCohortUseCase(database, context),
        skipCohortUseCase = SkipCohortUseCase(scheduleRepository, context)
    ) {
        this.ioDispatcher = ioDispatcher
    }

    constructor(
        database: DosezyDatabase,
        scheduleRepository: ScheduleRepository,
        context: Context
    ) : this(
        database = database,
        scheduleRepository = scheduleRepository,
        context = context,
        takeCohortUseCase = TakeCohortUseCase(database, scheduleRepository, context),
        snoozeCohortUseCase = SnoozeCohortUseCase(database, context),
        skipCohortUseCase = SkipCohortUseCase(scheduleRepository, context)
    )

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
            val medicinesList = _uiState.value.medicinesList
            val user = _uiState.value.user
            val allIds = if (medicinesList.isNotEmpty()) {
                medicinesList.map { it.first.entryId }
            } else {
                entryIds
            }
            takeCohortUseCase(allIds, user = user)
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
            val medicinesList = _uiState.value.medicinesList
            val user = _uiState.value.user
            val allIds = if (medicinesList.isNotEmpty()) {
                medicinesList.map { it.first.entryId }
            } else {
                entryIds
            }
            val names = if (medicinesList.isNotEmpty()) {
                medicinesList.map { it.second.medicationName }
            } else {
                listOf(initialMedicineName)
            }
            snoozeCohortUseCase(
                entryIds = allIds,
                explicitSnoozeMinutes = snoozeMinutes,
                fallbackMedicineName = initialMedicineName,
                medicineNames = names,
                user = user
            )
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
            val medicinesList = _uiState.value.medicinesList
            val allIds = if (medicinesList.isNotEmpty()) {
                medicinesList.map { it.first.entryId }
            } else {
                entryIds
            }
            skipCohortUseCase(allIds, reason)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}
