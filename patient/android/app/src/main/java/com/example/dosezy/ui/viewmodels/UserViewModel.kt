package com.example.dosezy.ui.viewmodels

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import com.example.dosezy.notifications.MedicineNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    val userRepository: UserRepository,
    val medicineRepository: MedicineRepository,
    val scheduleRepository: ScheduleRepository,
    private val medicineNotificationManager: MedicineNotificationManager
) : ViewModel() {

    // debouncing to prevent rapid updates
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadAllUsers()
    }

    private fun loadAllUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                userRepository.getAllUsers()
                    .distinctUntilChanged()
                    .collect { userList ->
                        _users.value = userList

                        // Find current user
                        val current = userList.firstOrNull { it.isCurrentUser }
                        _currentUser.value = current

                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _isLoading.value = false

                // Log error
                android.util.Log.e("UserViewModel", "Error loading users", e)
            }
        }
    }

    fun setCurrentUser(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // coroutine scope to run in background
                withContext(Dispatchers.IO) {
                    // Clear current user flag from all users
                    val allUsers = userRepository.getAllUsersList()
                    allUsers.forEach { existingUser ->
                        if (existingUser.isCurrentUser && existingUser.userId != user.userId) {
                            userRepository.updateUser(existingUser.copy(isCurrentUser = false))
                        }
                    }

                    // Set new current user
                    val updatedUser = user.copy(isCurrentUser = true)
                    userRepository.updateUser(updatedUser)

                    // Update state on main thread
                    withContext(Dispatchers.Main) {
                        _currentUser.value = updatedUser
                    }

                    // Schedule alarms for the new current user
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        medicineNotificationManager.scheduleAlarmsForUser(updatedUser.userId)
                    }

                    // Update home screen widget for the new current user
                    try {
                        com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(medicineRepository.context)
                    } catch (_: Exception) {}
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                android.util.Log.e("UserViewModel", "Error setting current user", e)
            }
        }
    }

    fun addUser(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    val allUsers = userRepository.getAllUsersList()
                    val shouldBeCurrent = user.isCurrentUser || allUsers.isEmpty() || allUsers.none { it.isCurrentUser }

                    if (shouldBeCurrent) {
                        // Demote existing users to maintain the single current user invariant
                        allUsers.forEach { existingUser ->
                            if (existingUser.isCurrentUser) {
                                userRepository.updateUser(existingUser.copy(isCurrentUser = false))
                            }
                        }
                    }

                    val userToInsert = user.copy(isCurrentUser = shouldBeCurrent)
                    userRepository.insertUser(userToInsert)

                    if (shouldBeCurrent) {
                        withContext(Dispatchers.Main) {
                            _currentUser.value = userToInsert
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            medicineNotificationManager.scheduleAlarmsForUser(userToInsert.userId)
                        }
                        try {
                            com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(medicineRepository.context)
                        } catch (_: Exception) {}
                    }
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                android.util.Log.e("UserViewModel", "Error adding user", e)
            }
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            userRepository.updateUser(user)
            // Update current user state if this is the current user
            if (user.isCurrentUser) {
                _currentUser.value = user
                try {
                    com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(medicineRepository.context)
                } catch (_: Exception) {}
            }
        }
    }

    fun deleteUser(user: User, onNextUserSelected: ((User?) -> Unit)? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    // Cancel alarms for the deleted user
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        medicineNotificationManager.cancelAllAlarmsForUser(user.userId)
                    }

                    // Delete the user from DB
                    userRepository.deleteUser(user)

                    // Fetch remaining users directly from DB
                    val remainingUsers = userRepository.getAllUsersList().filter { it.userId != user.userId }
                    if (user.isCurrentUser && remainingUsers.isNotEmpty()) {
                        // Pick next available user and set as current
                        val nextUser = remainingUsers.first().copy(isCurrentUser = true)
                        userRepository.updateUser(nextUser)

                        withContext(Dispatchers.Main) {
                            _currentUser.value = nextUser
                            onNextUserSelected?.invoke(nextUser)
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            medicineNotificationManager.scheduleAlarmsForUser(nextUser.userId)
                        }
                    } else if (remainingUsers.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            _currentUser.value = null
                            onNextUserSelected?.invoke(null)
                        }
                    }

                    try {
                        com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(medicineRepository.context)
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                android.util.Log.e("UserViewModel", "Error deleting user", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}