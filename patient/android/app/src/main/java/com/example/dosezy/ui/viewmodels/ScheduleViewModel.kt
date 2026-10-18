package com.example.dosezy.ui.viewmodels

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.ScheduleWithMedicine
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import com.example.dosezy.utils.TimeCalculationUtils
import android.content.Context
import com.example.dosezy.domain.usecases.TakeCohortUseCase
import com.example.dosezy.domain.usecases.SkipCohortUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context,
    private val takeCohortUseCase: TakeCohortUseCase? = null,
    private val skipCohortUseCase: SkipCohortUseCase? = null
) : ViewModel() {

    companion object {
        private const val TAG = "ScheduleViewModel"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    @RequiresApi(Build.VERSION_CODES.O)
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _scheduleEntries = MutableStateFlow<List<ScheduleEntry>>(emptyList())
    val scheduleEntries: StateFlow<List<ScheduleEntry>> = _scheduleEntries.asStateFlow()

    private val _scheduleWithMedicine = MutableStateFlow<List<ScheduleWithMedicine>>(emptyList())
    val scheduleWithMedicine: StateFlow<List<ScheduleWithMedicine>> = _scheduleWithMedicine.asStateFlow()

    private val _todayScheduleWithMedicine = MutableStateFlow<List<ScheduleWithMedicine>>(emptyList())
    val todayScheduleWithMedicine: StateFlow<List<ScheduleWithMedicine>> = _todayScheduleWithMedicine.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    private val _isRefreshing = MutableStateFlow(true)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isTodayLoading = MutableStateFlow(true)
    val isTodayLoading: StateFlow<Boolean> = _isTodayLoading.asStateFlow()

    private var scheduleJob: kotlinx.coroutines.Job? = null
    private var rawScheduleJob: kotlinx.coroutines.Job? = null
    private var todayJob: kotlinx.coroutines.Job? = null

    init {
        loadCurrentUserAndSchedule()
    }

    private var currentObservedTodayDate: LocalDate? = null

    @RequiresApi(Build.VERSION_CODES.O)
    internal fun observeTodaySchedule(userId: String) {
        todayJob?.cancel()
        _isTodayLoading.value = true
        val today = LocalDate.now()
        currentObservedTodayDate = today
        todayJob = viewModelScope.launch {
            try {
                scheduleRepository.getScheduleWithMedicineForDate(userId, today).collect { entries ->
                    _todayScheduleWithMedicine.value = entries
                    _isTodayLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing today schedule", e)
                _isTodayLoading.value = false
            }
        }
    }

    // Guard: Reconnect today's schedule subscription when passing midnight so HomeScreen displays today's doses instead of remaining stuck on yesterday's query
    @RequiresApi(Build.VERSION_CODES.O)
    fun checkMidnightDateTransition() {
        val today = LocalDate.now()
        if (currentObservedTodayDate != null && currentObservedTodayDate != today) {
            val previousObserved = currentObservedTodayDate
            _currentUserId.value?.let { userId ->
                observeTodaySchedule(userId)
                if (_selectedDate.value == previousObserved) {
                    _selectedDate.value = today
                    loadScheduleForDate(userId, today)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadCurrentUserAndSchedule() {
        viewModelScope.launch {
            userRepository.getAllUsers().collect { users ->
                val currentUser = users.find { it.isCurrentUser }
                _currentUserId.value = currentUser?.userId

                currentUser?.let { user ->
                    launch {
                        scheduleRepository.reconcileLegacyScheduleEntries(user.userId, context)
                    }
                    observeTodaySchedule(user.userId)
                    loadScheduleForDate(user.userId, _selectedDate.value)
                } ?: run {
                    // Guard: Cancel observation jobs and flush cached StateFlow lists when no active user exists to prevent stale schedule data retention across profile lifecycles
                    todayJob?.cancel()
                    scheduleJob?.cancel()
                    rawScheduleJob?.cancel()
                    _todayScheduleWithMedicine.value = emptyList()
                    _scheduleWithMedicine.value = emptyList()
                    _scheduleEntries.value = emptyList()
                    currentCalendarUserId = null
                    _isTodayLoading.value = false
                    _isRefreshing.value = false
                }
            }
        }
    }

    private var currentCalendarUserId: String? = null

    fun setSelectedDate(date: LocalDate) {
        if (_selectedDate.value == date && !_isRefreshing.value) return
        _selectedDate.value = date
        _scheduleWithMedicine.value = emptyList()
        _isRefreshing.value = true
        _currentUserId.value?.let { userId ->
            loadScheduleForDate(userId, date)
        }
    }

    fun loadScheduleForDate(userId: String, date: LocalDate) {
        scheduleJob?.cancel()
        _isRefreshing.value = true

        // Load month-wide indicators once per user or on updates, not on every date click
        if (currentCalendarUserId != userId || _scheduleEntries.value.isEmpty()) {
            currentCalendarUserId = userId
            rawScheduleJob?.cancel()
            rawScheduleJob = viewModelScope.launch {
                try {
                    scheduleRepository.getScheduleForUser(userId).collect { entries ->
                        Log.d(TAG, "Successfully loaded ${entries.size} user schedule entries for month-wide indicators")
                        _scheduleEntries.value = entries
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading user schedule", e)
                }
            }
        }

        scheduleJob = viewModelScope.launch {
            try {
                Log.d(TAG, "Loading schedule for user: $userId, date: $date")
                scheduleRepository.getScheduleWithMedicineForDate(userId, date).collect { scheduleWithMedicine ->
                    Log.d(TAG, "Successfully loaded ${scheduleWithMedicine.size} schedule entries with medicine")
                    _scheduleWithMedicine.value = scheduleWithMedicine
                    _isRefreshing.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading schedule with medicine", e)
                _isRefreshing.value = false
            }
        }
    }

    fun refreshAfterMedicineAdded() {
        currentCalendarUserId = null
        _currentUserId.value?.let { userId ->
            observeTodaySchedule(userId)
            loadScheduleForDate(userId, _selectedDate.value)
        }
    }

    fun markAsTaken(entryId: String, takenAt: String, notes: String? = null) {
        viewModelScope.launch {
            val entry = scheduleRepository.getScheduleEntryById(entryId)
            val userId = entry?.userId ?: _currentUserId.value
            val user = userId?.let { userRepository.getUserByIdSync(it) } ?: userRepository.getAllUsersList().find { it.isCurrentUser }
            val lateAfter = user?.considerLateAfter ?: 3
            val parsedTakenAt = try { LocalDateTime.parse(takenAt) } catch (_: Exception) { LocalDateTime.now() }
            // Guard: Evaluate isTakenLate so overdue doses marked taken via ScheduleViewModel are accurately recorded as TAKEN_LATE rather than TAKEN_ON_TIME (#BUG-VM-LATE-STATUS)
            val status = if (entry != null && TimeCalculationUtils.isTakenLate(entry.scheduledDateTime, parsedTakenAt, lateAfter)) {
                "TAKEN_LATE"
            } else {
                "TAKEN_ON_TIME"
            }
            scheduleRepository.recordDoseTaken(entryId, status, takenAt, context, notes)
            currentCalendarUserId = null
            // Refresh the schedule after updating status
            _currentUserId.value?.let { activeUserId ->
                loadScheduleForDate(activeUserId, _selectedDate.value)
            }
        }
    }

    fun markAsLate(entryId: String, takenAt: String, notes: String? = null) {
        viewModelScope.launch {
            scheduleRepository.recordDoseTaken(entryId, "TAKEN_LATE", takenAt, context, notes)
            currentCalendarUserId = null
            // Refresh the schedule after updating status
            _currentUserId.value?.let { userId ->
                loadScheduleForDate(userId, _selectedDate.value)
            }
        }
    }

    fun markCohortAsTaken(
        entryIds: List<String>,
        takenAt: String,
        lateAfter: Int = 3,
        notes: String? = null
    ) {
        viewModelScope.launch {
            if (takeCohortUseCase != null) {
                val now = try { LocalDateTime.parse(takenAt) } catch (_: Exception) { LocalDateTime.now() }
                // Guard: Pass notes to takeCohortUseCase to persist clinical dose notes for cohort intakes (#BUG-COHORT-DOSE-NOTES)
                takeCohortUseCase.invoke(entryIds, now, notes = notes)
            } else {
                val now = try { LocalDateTime.parse(takenAt) } catch (_: Exception) { LocalDateTime.now() }
                entryIds.forEach { entryId ->
                    val entry = scheduleRepository.getScheduleEntryById(entryId)
                    val status = if (entry != null && TimeCalculationUtils.isTakenLate(entry.scheduledDateTime, now, lateAfter)) {
                        "TAKEN_LATE"
                    } else {
                        "TAKEN_ON_TIME"
                    }
                    // Guard: Record each cohort dose taken with notes to ensure atomic stock decrement, alarm cancellation, and clinical notes persistence (#102, #BUG-COHORT-DOSE-NOTES)
                    scheduleRepository.recordDoseTaken(entryId, status, takenAt, context, notes)
                }
            }
            currentCalendarUserId = null
            _currentUserId.value?.let { userId ->
                loadScheduleForDate(userId, _selectedDate.value)
            }
        }
    }

    fun updateDoseNotes(entryId: String, notes: String?) {
        viewModelScope.launch {
            scheduleRepository.updateDoseNotes(entryId, notes)
            currentCalendarUserId = null
            _currentUserId.value?.let { userId ->
                loadScheduleForDate(userId, _selectedDate.value)
            }
        }
    }

    fun undoDoseTaken(entryId: String) {
        viewModelScope.launch {
            scheduleRepository.undoDoseTaken(entryId, context)
            currentCalendarUserId = null
            // Refresh the schedule after reverting status
            _currentUserId.value?.let { userId ->
                loadScheduleForDate(userId, _selectedDate.value)
            }
        }
    }

    fun markAsMissed(entryId: String) {
        viewModelScope.launch {
            // Guard: Use recordDoseMissed to dismiss notification, disarm snooze/nagging alarms, and update app widgets
            scheduleRepository.recordDoseMissed(entryId, context)
            currentCalendarUserId = null
            // Refresh the schedule after updating status
            _currentUserId.value?.let { userId ->
                loadScheduleForDate(userId, _selectedDate.value)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun markAsSkipped(entryId: String, reason: String) {
        viewModelScope.launch {
            if (skipCohortUseCase != null) {
                skipCohortUseCase.invoke(listOf(entryId), reason)
            } else {
                scheduleRepository.recordDoseSkipped(entryId, reason, context)
            }
            currentCalendarUserId = null
            // Refresh the schedule after updating status
            _currentUserId.value?.let { userId ->
                loadScheduleForDate(userId, _selectedDate.value)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun autoMarkMissedMedications(userId: String, missedAfterHours: Int) {
        // Guard: Re-evaluate today's schedule subscription if crossing midnight before checking missed status
        checkMidnightDateTransition()
        viewModelScope.launch {
            val currentTime = LocalDateTime.now()
            val cutoffTime = currentTime.minusHours(missedAfterHours.toLong())
            try {
                // Query only pending entries that have passed the missed cutoff time
                val overdueEntries = scheduleRepository.getPendingEntriesBefore(userId, cutoffTime)

                var hasUpdated = false
                overdueEntries.forEach { entry ->
                    val isMissed = TimeCalculationUtils.isMissed(
                        entry.scheduledDateTime,
                        currentTime,
                        missedAfterHours
                    )
                    if (isMissed) {
                        // Guard: Use recordDoseMissed to clear notification shade and disarm Doze wakeups via AlarmManager
                        scheduleRepository.recordDoseMissed(entry.entryId, context)
                        hasUpdated = true
                    }
                }

                // Refresh the current view only if any entry was marked missed
                if (hasUpdated) {
                    // Guard: Refresh schedule if active user matches or if current user ID is pending initial load
                    if (_currentUserId.value == null || _currentUserId.value == userId) {
                        loadScheduleForDate(userId, _selectedDate.value)
                    }
                    try {
                        com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error auto-marking missed medications", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun logAsNeededDose(medicine: com.example.dosezy.data.model.Medicine) {
        viewModelScope.launch {
            _currentUserId.value?.let { userId ->
                scheduleRepository.logAsNeededDose(medicine, userId, LocalDateTime.now(), context)
                loadScheduleForDate(userId, _selectedDate.value)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getFormattedDate(locale: java.util.Locale = java.util.Locale.getDefault()): String {
        return selectedDate.value.format(
            java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG).withLocale(locale)
        )
    }
}