package com.example.dosezy.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    val userRepository: UserRepository,
    val medicineRepository: MedicineRepository,
    val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadAllUsers()
    }

    private fun loadAllUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                userRepository.getAllUsers().collect { userList ->
                    _users.value = userList

                    // Find current user
                    val current = userList.find { it.isCurrentUser }
                    _currentUser.value = current

                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
                // Error is handled by the flow
            }
        }
    }

    fun setCurrentUser(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Clear current user flag from all users
                val allUsers = _users.value
                allUsers.forEach { existingUser ->
                    if (existingUser.isCurrentUser && existingUser.userId != user.userId) {
                        userRepository.updateUser(existingUser.copy(isCurrentUser = false))
                    }
                }

                // Set new current user
                val updatedUser = user.copy(isCurrentUser = true)
                userRepository.updateUser(updatedUser)
                _currentUser.value = updatedUser

                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                // Error is handled by the flow
            }
        }
    }

    fun addUser(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                userRepository.insertUser(user)

                // If this is the first user, automatically set as current
                if (_users.value.isEmpty() || _users.value.none { it.isCurrentUser }) {
                    setCurrentUser(user)
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                // Error is handled by the flow
            }
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            userRepository.updateUser(user)
            // Update current user state if this is the current user
            if (user.isCurrentUser) {
                _currentUser.value = user
            }
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            userRepository.deleteUser(user)
            // If we deleted the current user, select a new one
            if (user.isCurrentUser && _users.value.isNotEmpty()) {
                setCurrentUser(_users.value.first())
            }
        }
    }
}