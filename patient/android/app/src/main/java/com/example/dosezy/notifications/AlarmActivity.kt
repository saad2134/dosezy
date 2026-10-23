/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.notifications

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.dosezy.R
import com.example.dosezy.data.model.Theme
import com.example.dosezy.notifications.components.AlarmScreenContent
import com.example.dosezy.ui.theme.DosezyTheme
import com.example.dosezy.ui.viewmodels.AlarmViewModel
import com.example.dosezy.utils.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    companion object {
        private var activeInstance: WeakReference<AlarmActivity>? = null

        fun stopActiveAlarm() {
            try {
                AlarmAudioPlayer.stop()
                activeInstance?.get()?.let { activity ->
                    activity.runOnUiThread {
                        activity.stopAlarm()
                        activity.finish()
                    }
                }
            } catch (_: Exception) {}
            activeInstance = null
        }
    }

    private val alarmViewModel: AlarmViewModel by viewModels()

    private val activeEntryIdsState = mutableStateOf<List<String>>(emptyList())
    private val activeMedicineNameState = mutableStateOf("Medication")
    private val activeScheduledTimeState = mutableStateOf("")
    private val activeSnoozeMinutesState = mutableStateOf(10)

    private val isKeyguardLockedState = mutableStateOf(false)

    private fun checkKeyguardState() {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        isKeyguardLockedState.value = keyguardManager?.isKeyguardLocked == true
    }

    private fun isLockscreenPrivacyActive(): Boolean {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val defaultPref = prefs.getBoolean("lockscreen_details_enabled", true)
        val intentPref = intent?.getBooleanExtra("allow_lockscreen_details", defaultPref) ?: defaultPref
        val userPref = alarmViewModel.uiState.value.user?.lockscreenDetailsEnabled ?: intentPref
        return !userPref
    }

    fun requestDeviceUnlock(onSuccess: (() -> Unit)? = null) {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager?.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    super.onDismissSucceeded()
                    isKeyguardLockedState.value = false
                    onSuccess?.invoke()
                }
            })
        }
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (overrideConfiguration != null) {
            val savedLanguage = LocaleHelper.getSavedLanguage(this)
            val targetLocale = LocaleHelper.getLocale(savedLanguage)
            overrideConfiguration.setLocale(targetLocale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                overrideConfiguration.setLocales(LocaleList(targetLocale))
            }
            overrideConfiguration.setLayoutDirection(targetLocale)
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkKeyguardState()
        setupWindowFlags()

        val newId = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID) ?: ""
        val newIds = intent.getStringArrayListExtra(MedicineAlarmReceiver.EXTRA_ENTRY_IDS) ?: if (newId.isNotEmpty()) arrayListOf(newId) else arrayListOf()
        val newMedName = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME) ?: "Medication"
        val newSchedTime = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME) ?: ""
        val newSnoozeMinutes = intent.getIntExtra(MedicineAlarmReceiver.EXTRA_SNOOZE_MINUTES, activeSnoozeMinutesState.value)

        val merged = (activeEntryIdsState.value + newIds + listOf(newId)).filter { it.isNotEmpty() }.distinct()
        activeEntryIdsState.value = merged
        activeMedicineNameState.value = newMedName
        activeScheduledTimeState.value = newSchedTime
        activeSnoozeMinutesState.value = newSnoozeMinutes

        // Guard: Resume playback for incoming alarm if audio was silenced by volume rocker while activity remained open
        alarmViewModel.startPlaybackIfNeeded(newId)
        alarmViewModel.loadCohort(merged)
    }

    override fun onResume() {
        super.onResume()
        checkKeyguardState()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            checkKeyguardState()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activeInstance = WeakReference(this)

        checkKeyguardState()
        // Turn screen on and setup window flags
        setupWindowFlags()

        val entryId = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID) ?: ""
        val entryIds = intent.getStringArrayListExtra(MedicineAlarmReceiver.EXTRA_ENTRY_IDS) ?: if (entryId.isNotEmpty()) arrayListOf(entryId) else arrayListOf()
        val initialMedicineName = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME) ?: "Medication"
        val initialScheduledTime = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME) ?: ""
        // Guard: Retrieve passed snooze duration so initial UI frame defaults to user's setting before Room async load completes (#113)
        val initialSnoozeMinutes = intent.getIntExtra(MedicineAlarmReceiver.EXTRA_SNOOZE_MINUTES, 10)

        val distinctIds = entryIds.filter { it.isNotEmpty() }.distinct()
        activeEntryIdsState.value = distinctIds
        activeMedicineNameState.value = initialMedicineName
        activeScheduledTimeState.value = initialScheduledTime
        activeSnoozeMinutesState.value = initialSnoozeMinutes

        // Start playback and load medication cohort via ViewModel
        alarmViewModel.startPlaybackIfNeeded(entryId)
        alarmViewModel.loadCohort(distinctIds)

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
            val spTheme = prefs.getString("theme", "system") ?: "system"

            val uiState by alarmViewModel.uiState.collectAsState()
            val user = uiState.user

            val isKeyguardLocked by isKeyguardLockedState
            val lockscreenDetailsAllowed = user?.lockscreenDetailsEnabled
                ?: intent.getBooleanExtra("allow_lockscreen_details", prefs.getBoolean("lockscreen_details_enabled", true))
            val isDetailsMasked = !lockscreenDetailsAllowed && isKeyguardLocked

            val systemIsDark = isSystemInDarkTheme()
            val isDark = when (spTheme) {
                "dark" -> true
                "light" -> false
                else -> when (user?.theme) {
                    Theme.DARK -> true
                    Theme.LIGHT -> false
                    else -> systemIsDark
                }
            }

            val currentLang = user?.language ?: LocaleHelper.getSavedLanguage(context)
            val currentLocale = LocaleHelper.getLocale(currentLang)
            val isRtl = currentLocale.language == "ar"
            val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            val localizedContext = remember(currentLang) {
                LocaleHelper.updateContextLocale(context, currentLang)
            }
            val localizedConfig = remember(currentLang) {
                val config = Configuration(context.resources.configuration)
                config.setLocale(currentLocale)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    config.setLocales(LocaleList(currentLocale))
                }
                config.setLayoutDirection(currentLocale)
                config
            }

            val currentEntryIds by activeEntryIdsState
            val currentMedName by activeMedicineNameState
            val currentSchedTime by activeScheduledTimeState
            val currentSnoozeMinutes by activeSnoozeMinutesState

            val dismissAction: () -> Unit = {
                currentEntryIds.forEach { id ->
                    if (id.isNotEmpty()) {
                        try {
                            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.cancel(id.hashCode())
                        } catch (_: Exception) {}
                    }
                }
                stopAlarm()
                finish()
            }

            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection,
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfig
            ) {
                DosezyTheme(darkTheme = isDark) {
                    AlarmScreenContent(
                        initialMedicineName = currentMedName,
                        initialScheduledTime = currentSchedTime,
                        initialSnoozeMinutes = currentSnoozeMinutes,
                        medicinesList = uiState.medicinesList,
                        user = user,
                        isLoaded = uiState.isLoaded,
                        isDarkTheme = isDark,
                        isDetailsMasked = isDetailsMasked,
                        isIntakeReAlert = intent?.getBooleanExtra(MedicineAlarmReceiver.EXTRA_IS_INTAKE_REALERT, false) ?: false,
                        onUnlockRequest = { requestDeviceUnlock() },
                        onTakeAction = {
                            val isIntakeReAlert = intent?.getBooleanExtra(MedicineAlarmReceiver.EXTRA_IS_INTAKE_REALERT, false) ?: false
                            if (user?.intakeSafeguardEnabled == true && !isIntakeReAlert) {
                                // Guard: Start intake countdown timer for ADHD support on alarm dismissal (#104, #BUG-SAFEGUARD-COHORT-NAMES)
                                val medNames = uiState.medicinesList.map { it.second.medicationName }.ifEmpty { listOf(currentMedName) }
                                val compositeMedName = if (medNames.size > 1) medNames.joinToString(", ") else currentMedName
                                IntakeSafeguardManager.startCountdown(
                                    context = applicationContext,
                                    entryId = currentEntryIds.firstOrNull() ?: "",
                                    entryIds = currentEntryIds,
                                    medicineName = compositeMedName,
                                    medicineNames = medNames,
                                    scheduledTime = currentSchedTime,
                                    durationSeconds = user.intakeCountdownSeconds,
                                    userId = user.userId
                                )
                                dismissAction()
                            } else {
                                alarmViewModel.takeCohort(currentEntryIds) {
                                    dismissAction()
                                }
                            }
                        },
                        // Guard: Support dynamic snooze duration chosen directly on alarm screen (#103)
                        onSnoozeAction = { minutes ->
                            try {
                                // Guard: Use localizedContext so snooze confirmation toast renders in the active alarm recipient's language (#BUG-ALARM-SNOOZE-TOAST-LOCALE)
                                Toast.makeText(
                                    context,
                                    localizedContext.getString(R.string.alarm_snoozed_toast, minutes),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (_: Exception) {}
                            alarmViewModel.snoozeCohort(currentEntryIds, minutes, currentMedName) {
                                dismissAction()
                            }
                        },
                        onSkipAction = { reason ->
                            alarmViewModel.skipCohort(currentEntryIds, reason) {
                                dismissAction()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun setupWindowFlags() {
        val lockscreenPrivacy = isLockscreenPrivacyActive()
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = keyguardManager?.isKeyguardLocked == true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            // Guard: Safe-cast KeyguardManager to prevent NullPointerException or TypeCastException crash on headless or custom OEM builds
            // Guard: Keep keyguard locked when user has disabled lockscreen details to prevent premature PHI exposure
            if (!lockscreenPrivacy || !isLocked) {
                keyguardManager?.requestDismissKeyguard(this, null)
            }
        }
        @Suppress("DEPRECATION")
        window.apply {
            addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
            if (!lockscreenPrivacy || !isLocked) {
                addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            } else {
                clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            }
        }
    }

    fun stopAlarm() {
        AlarmAudioPlayer.stop()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    // Silence alarm audio immediately when physical volume keys are pressed
                    stopAlarm()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        if (activeInstance?.get() == this) {
            activeInstance = null
        }
    }
}
