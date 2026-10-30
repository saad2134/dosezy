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
import android.content.Context
import com.example.dosezy.data.preferences.UserPreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val medicineRepository: MedicineRepository,
    private val scheduleRepository: ScheduleRepository,
    private val medicineNotificationManager: MedicineNotificationManager,
    @ApplicationContext private val appContext: Context? = null,
    private val userPreferencesDataStore: UserPreferencesDataStore? = null
) : ViewModel() {

    // Context resolution: uses injected ApplicationContext with fallback for testing
    private val context: Context
        get() = appContext ?: medicineRepository.context

    private val preferencesStore: UserPreferencesDataStore by lazy {
        userPreferencesDataStore ?: UserPreferencesDataStore(context)
    }

    val preferencesDataStore: UserPreferencesDataStore
        get() = preferencesStore

    private suspend fun syncAppTheme(themeName: String) {
        try {
            preferencesStore.setTheme(themeName)
        } catch (_: Exception) {
            // Guard: Fallback to SharedPreferences if DataStore is uninitialized or in mock context
            try {
                val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("theme", themeName).apply()
            } catch (_: Exception) {}
        }
    }

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
                                syncAppTheme(current.theme.name.lowercase())
                                com.example.dosezy.widget.DosezyWidgetPrefs.saveWidgetProfileTheme(context, current.userId, current.theme.name.lowercase(), isCurrentProfile = true)
                                // Guard: Persist lock screen privacy toggle synchronously to app_prefs for receivers
                                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("lockscreen_details_enabled", current.lockscreenDetailsEnabled)
                                    .commit()
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
                    // Guard: Atomically update isCurrentUser flag in a single SQLite statement to prevent Room intermediate emissions with 0 active users triggering auto-promotion
                    userRepository.setCurrentUser(user.userId)

                    // Set new current user
                    val updatedUser = user.copy(isCurrentUser = true)

                    // Update state on main thread
                    withContext(Dispatchers.Main) {
                        _currentUser.value = updatedUser
                    }

                    // Guard: Core library desugaring supports API 24+; schedule alarms across all supported OS versions
                    medicineNotificationManager.scheduleAlarmsForUser(updatedUser.userId)

                    try {
                        syncAppTheme(updatedUser.theme.name.lowercase())
                        com.example.dosezy.widget.DosezyWidgetPrefs.saveWidgetProfileTheme(context, updatedUser.userId, updatedUser.theme.name.lowercase(), isCurrentProfile = true)
                    } catch (_: Exception) {}

                    // Update home screen widget for the new current user
                    try {
                        com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
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

                    val userToInsert = user.copy(isCurrentUser = shouldBeCurrent)
                    userRepository.insertUser(userToInsert)

                    if (shouldBeCurrent) {
                        // Guard: Atomically ensure only the newly added profile is current without intermediate 0-active states
                        userRepository.setCurrentUser(userToInsert.userId)

                        withContext(Dispatchers.Main) {
                            _currentUser.value = userToInsert
                        }
                        // Guard: Core library desugaring supports API 24+; schedule alarms for new profile across all supported OS versions
                        medicineNotificationManager.scheduleAlarmsForUser(userToInsert.userId)
                        try {
                            syncAppTheme(userToInsert.theme.name.lowercase())
                            com.example.dosezy.widget.DosezyWidgetPrefs.saveWidgetProfileTheme(context, userToInsert.userId, userToInsert.theme.name.lowercase(), isCurrentProfile = shouldBeCurrent)
                        } catch (_: Exception) {}
                        try {
                            com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
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
            withContext(Dispatchers.IO) {
                userRepository.updateUser(user)
                // Guard: Reschedule alarms so updated reminderType, timeFormat, language, and snooze settings take effect immediately in AlarmManager
                medicineNotificationManager.scheduleAlarmsForUser(user.userId)
            }
            // Update current user state if this is the current user
            if (user.isCurrentUser) {
                _currentUser.value = user
                try {
                    syncAppTheme(user.theme.name.lowercase())
                    // Guard: Persist lock screen privacy toggle synchronously to app_prefs for receivers
                    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("lockscreen_details_enabled", user.lockscreenDetailsEnabled)
                        .commit()
                } catch (_: Exception) {}
            }
            try {
                com.example.dosezy.widget.DosezyWidgetPrefs.saveWidgetProfileTheme(context, user.userId, user.theme.name.lowercase(), isCurrentProfile = user.isCurrentUser)
                com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
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

                    // Guard: Dismiss active ADHD intake countdown notification if active countdown belongs to deleted user
                    try {
                        val activeCountdown = com.example.dosezy.notifications.IntakeSafeguardManager.activeCountdown.value
                        if (activeCountdown != null) {
                            val entry = scheduleRepository.getScheduleEntryById(activeCountdown.entryId)
                            if (entry == null || entry.userId == user.userId) {
                                com.example.dosezy.notifications.IntakeSafeguardManager.cancelCountdown(context)
                            }
                        }
                    } catch (_: Exception) {}

                    // Delete user's profile picture from internal storage if it exists
                    user.profilePicPath?.let { path ->
                        try {
                            val cleanPath = path.removePrefix("file://")
                            val file = java.io.File(cleanPath)
                            val filesDir = context.filesDir
                            if (file.exists() && (file.parentFile?.canonicalPath == filesDir.canonicalPath || file.canonicalPath.startsWith(filesDir.canonicalPath + java.io.File.separator))) {
                                file.delete()
                            }
                        } catch (_: Exception) {}
                    }

                    // Guard: Unlink custom alarm sound from internal storage upon profile deletion to prevent disk leakage
                    com.example.dosezy.utils.SoundUtils.deleteCustomAudio(context, user.customAlarmSoundPath)

                    // Clean up associated medicine images before database cascade deletion
                    try {
                        val meds = medicineRepository.getMedicinesByUserDirect(user.userId)
                        val filesDir = context.filesDir
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

                    // Guard: Scrub orphaned emergency contacts, emergency preferences, and widget profile preferences for the deleted profile to prevent SharedPreferences leakage
                    try {
                        val emPrefs = context.getSharedPreferences("emergency_contacts", android.content.Context.MODE_PRIVATE)
                        emPrefs.edit().remove("contacts_json_${user.userId}").apply()
                    } catch (_: Exception) {}

                    try {
                        val emergencyPrefs = context.getSharedPreferences("emergency_prefs", android.content.Context.MODE_PRIVATE)
                        emergencyPrefs.edit().remove("selected_country_code_${user.userId}").apply()
                    } catch (_: Exception) {}

                    try {
                        // Guard: Scrub orphaned widget preferences and unbind widgets from deleted user to prevent SharedPreferences leakage and stale widget bindings
                        com.example.dosezy.widget.DosezyWidgetPrefs.deleteWidgetPreferencesForUser(context, user.userId)
                    } catch (_: Exception) {}

                    // Delete the user from DB (cascades to medicines & schedules)
                    userRepository.deleteUser(user)

                    // Fetch remaining users directly from DB
                    val remainingUsers = userRepository.getAllUsersList().filter { it.userId != user.userId }
                    if (user.isCurrentUser && remainingUsers.isNotEmpty()) {
                        // Pick next available user and set as current
                        val nextUser = remainingUsers.first().copy(isCurrentUser = true)
                        // Guard: Atomically set nextUser as current to reset all other profiles to inactive and prevent multiple active profiles in SQLite
                        userRepository.setCurrentUser(nextUser.userId)

                        withContext(Dispatchers.Main) {
                            _currentUser.value = nextUser
                            onNextUserSelected?.invoke(nextUser)
                        }

                        // Guard: Synchronize app theme and widget preferences for promoted active profile to prevent theme lock on deleted profile
                        try {
                            syncAppTheme(nextUser.theme.name.lowercase())
                            com.example.dosezy.widget.DosezyWidgetPrefs.saveWidgetProfileTheme(context, nextUser.userId, nextUser.theme.name.lowercase(), isCurrentProfile = true)
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
                            syncAppTheme("system")
                        } catch (_: Exception) {}
                    } else {
                        // Guard: When deleting a secondary profile, notify completion with the unchanged active current profile
                        withContext(Dispatchers.Main) {
                            onNextUserSelected?.invoke(_currentUser.value)
                        }
                    }

                    try {
                        com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
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