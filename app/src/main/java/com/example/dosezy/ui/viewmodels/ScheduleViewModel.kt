package com.example.dosezy.ui.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dosezy.data.model.ScheduleEntry
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

    @RequiresApi(Build.VERSION_CODES.O)
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    @RequiresApi(Build.VERSION_CODES.O)
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _scheduleEntries = MutableStateFlow<List<ScheduleEntry>>(emptyList())
    val scheduleEntries: StateFlow<List<ScheduleEntry>> = _scheduleEntries.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)

    init {
        loadCurrentUserAndSchedule()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadCurrentUserAndSchedule() {
        viewModelScope.launch {
            userRepository.getAllUsers().collect { users ->
                val currentUser = users.find { it.isCurrentUser }
                _currentUserId.value = currentUser?.userId

                // Load schedule for current date and user
                currentUser?.let { user ->
                    loadScheduleForDate(user.userId, _selectedDate.value)
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
            scheduleRepository.getScheduleForDate(userId, date).collect { entries ->
                _scheduleEntries.value = entries
            }
        }
    }

    fun markAsTaken(entryId: String, takenAt: String) {
        viewModelScope.launch {
            scheduleRepository.updateMedicationStatus(entryId, "TAKEN_ON_TIME", takenAt)
        }
    }

    fun markAsLate(entryId: String, takenAt: String) {
        viewModelScope.launch {
            scheduleRepository.updateMedicationStatus(entryId, "TAKEN_LATE", takenAt)
        }
    }

    // Add this function to ScheduleViewModel
    @RequiresApi(Build.VERSION_CODES.O)
    fun getFormattedDate(): String {
        return selectedDate.value.format(
            java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy")
        )
    }
}