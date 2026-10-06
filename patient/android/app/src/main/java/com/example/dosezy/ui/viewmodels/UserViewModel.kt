package com.example.dosezy.ui.viewmodels

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
    private val userRepository: UserRepository,
    private val medicineRepository: MedicineRepository,
    private val scheduleRepository: ScheduleRepository,
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
                        var current = userList.firstOrNull { it.isCurrentUser }
                        // Guard: Auto-promote first profile if profiles exist but none has isCurrentUser=true (e.g. after selective restore or migration) to ensure an active profile always exists
                        if (current == null && userList.isNotEmpty()) {
                            val promoted = userList.first().copy(isCurrentUser = true)
                            userRepository.updateUser(promoted)
                            current = promoted
                        }
                        _currentUser.value = current
                        if (current != null) {
                            try {
                                val prefs = medicineRepository.context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                                prefs.edit().putString("theme", current.theme.name.lowercase()).apply()
                                com.example.dosezy.widget.DosezyWidgetPrefs.saveWidgetProfileTheme(medicineRepository.context, current.userId, current.theme.name.lowercase())
                            } catch (_: Exception) {}
                        }

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

                    // Guard: Core library desugaring supports API 24+; schedule alarms across all supported OS versions
                    medicineNotificationManager.scheduleAlarmsForUser(updatedUser.userId)

                    try {
                        val prefs = medicineRepository.context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putString("theme", updatedUser.theme.name.lowercase()).apply()
                        com.example.dosezy.widget.DosezyWidgetPrefs.saveWidgetProfileTheme(medicineRepository.context, updatedUser.userId, updatedUser.theme.name.lowercase())
                    } catch (_: Exception) {}

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

    fun setCurrentUserById(userId: String) {
        viewModelScope.launch {
            val user = userRepository.getUserByIdSync(userId)
            if (user != null) {
                setCurrentUser(user)
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
                        // Guard: Core library desugaring supports API 24+; schedule alarms for new profile across all supported OS versions
                        medicineNotificationManager.scheduleAlarmsForUser(userToInsert.userId)
                        try {
                            val prefs = medicineRepository.context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                            prefs.edit().putString("theme", userToInsert.theme.name.lowercase()).apply()
                            com.example.dosezy.widget.DosezyWidgetPrefs.saveWidgetProfileTheme(medicineRepository.context, userToInsert.userId, userToInsert.theme.name.lowercase())
                        } catch (_: Exception) {}
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
                    val prefs = medicineRepository.context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString("theme", user.theme.name.lowercase()).apply()
                } catch (_: Exception) {}
            }
            try {
                com.example.dosezy.widget.DosezyWidgetPrefs.saveWidgetProfileTheme(medicineRepository.context, user.userId, user.theme.name.lowercase())
                com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(medicineRepository.context)
            } catch (_: Exception) {}
        }
    }

    fun deleteUser(user: User, onNextUserSelected: ((User?) -> Unit)? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    // Guard: Cancel alarms across all supported versions (API 24+) to prevent orphaned alarms ringing after user deletion
                    medicineNotificationManager.cancelAllAlarmsForUser(user.userId)

                    // Delete user's profile picture from internal storage if it exists
                    user.profilePicPath?.let { path ->
                        try {
                            val cleanPath = path.removePrefix("file://")
                            val file = java.io.File(cleanPath)
                            val filesDir = medicineRepository.context.filesDir
                            if (file.exists() && (file.parentFile?.canonicalPath == filesDir.canonicalPath || file.canonicalPath.startsWith(filesDir.canonicalPath + java.io.File.separator))) {
                                file.delete()
                            }
                        } catch (_: Exception) {}
                    }

                    // Clean up associated medicine images before database cascade deletion
                    try {
                        val meds = medicineRepository.getMedicinesByUserDirect(user.userId)
                        val filesDir = medicineRepository.context.filesDir
                        meds.forEach { med ->
                            med.imageUri?.let { uriStr ->
                                val cleanPath = uriStr.removePrefix("file://")
                                val file = java.io.File(cleanPath)
                                if (file.exists() && (file.parentFile?.canonicalPath == filesDir.canonicalPath || file.canonicalPath.startsWith(filesDir.canonicalPath + java.io.File.separator))) {
                                    file.delete()
                                }
                            }
                        }
                    } catch (_: Exception) {}

                    // Delete the user from DB (cascades to medicines & schedules)
                    userRepository.deleteUser(user)

                    // Guard: Scrub orphaned emergency contacts and widget profile preferences for the deleted profile to prevent SharedPreferences leakage
                    try {
                        val emPrefs = medicineRepository.context.getSharedPreferences("emergency_contacts", android.content.Context.MODE_PRIVATE)
                        emPrefs.edit().remove("contacts_json_${user.userId}").apply()
                    } catch (_: Exception) {}

                    try {
                        com.example.dosezy.widget.DosezyWidgetPrefs.deleteWidgetProfileTheme(medicineRepository.context, user.userId)
                    } catch (_: Exception) {}

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

                        // Guard: Synchronize app theme and widget preferences for promoted active profile to prevent theme lock on deleted profile
                        try {
                            val prefs = medicineRepository.context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                            prefs.edit().putString("theme", nextUser.theme.name.lowercase()).apply()
                            com.example.dosezy.widget.DosezyWidgetPrefs.saveWidgetProfileTheme(medicineRepository.context, nextUser.userId, nextUser.theme.name.lowercase())
                        } catch (_: Exception) {}

                        // Guard: Core library desugaring supports API 24+; schedule alarms for next active profile across all OS versions
                        medicineNotificationManager.scheduleAlarmsForUser(nextUser.userId)
                    } else if (remainingUsers.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            _currentUser.value = null
                            onNextUserSelected?.invoke(null)
                        }
                        // Guard: Clean up app theme back to system default when no user profiles remain
                        try {
                            val prefs = medicineRepository.context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                            prefs.edit().putString("theme", "system").apply()
                        } catch (_: Exception) {}
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

    fun createDataExporter(context: android.content.Context): com.example.dosezy.data.export.DataExporter {
        return com.example.dosezy.data.export.DataExporter(
            context = context,
            userRepository = userRepository,
            medicineRepository = medicineRepository,
            scheduleRepository = scheduleRepository
        )
    }
}