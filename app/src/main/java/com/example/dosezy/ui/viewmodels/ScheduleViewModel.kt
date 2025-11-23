package com.example.dosezy.ui.viewmodels

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.ScheduleWithMedicine
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val userRepository: UserRepository
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

    private val _currentUserId = MutableStateFlow<String?>(null)
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadCurrentUserAndSchedule()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadCurrentUserAndSchedule() {
        viewModelScope.launch {
            userRepository.getAllUsers().collect { users ->
                val currentUser = users.find { it.isCurrentUser }
                _currentUserId.value = currentUser?.userId

                currentUser?.let { user ->
                    // Load schedule for current date
                    loadScheduleForDate(user.userId, LocalDate.now())
                }
            }
        }
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        _currentUserId.value?.let { userId ->
            loadScheduleForDate(userId, date)
        }
    }

    fun loadScheduleForDate(userId: String, date: LocalDate) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                Log.d(TAG, "Loading schedule for user: $userId, date: $date")

                // Load schedule with medicine using our manual filtering
                scheduleRepository.getScheduleWithMedicineForDate(userId, date).collect { scheduleWithMedicine ->
                    Log.d(TAG, "Successfully loaded ${scheduleWithMedicine.size} schedule entries with medicine")
                    _scheduleWithMedicine.value = scheduleWithMedicine
                }

                // Also load regular schedule entries
                scheduleRepository.getScheduleForDate(userId, date).collect { entries ->
                    Log.d(TAG, "Successfully loaded ${entries.size} raw schedule entries")
                    _scheduleEntries.value = entries
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading schedule", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refreshAfterMedicineAdded() {
        _currentUserId.value?.let { userId ->
            loadScheduleForDate(userId, _selectedDate.value)
        }
    }

    fun markAsTaken(entryId: String, takenAt: String) {
        viewModelScope.launch {
            scheduleRepository.updateMedicationStatus(entryId, "TAKEN_ON_TIME", takenAt)
            // Refresh the schedule after updating status
            _currentUserId.value?.let { userId ->
                loadScheduleForDate(userId, _selectedDate.value)
            }
        }
    }

    fun markAsLate(entryId: String, takenAt: String) {
        viewModelScope.launch {
            scheduleRepository.updateMedicationStatus(entryId, "TAKEN_LATE", takenAt)
            // Refresh the schedule after updating status
            _currentUserId.value?.let { userId ->
                loadScheduleForDate(userId, _selectedDate.value)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getFormattedDate(): String {
        return selectedDate.value.format(
            java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy")
        )
    }
}