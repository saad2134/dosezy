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
        setupWindowFlags()

        val newId = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID) ?: ""
        val newIds = intent.getStringArrayListExtra(MedicineAlarmReceiver.EXTRA_ENTRY_IDS) ?: if (newId.isNotEmpty()) arrayListOf(newId) else arrayListOf()
        val newMedName = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME) ?: "Medication"
        val newSchedTime = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME) ?: ""

        val merged = (activeEntryIdsState.value + newIds + listOf(newId)).filter { it.isNotEmpty() }.distinct()
        activeEntryIdsState.value = merged
        activeMedicineNameState.value = newMedName
        activeScheduledTimeState.value = newSchedTime

        // Guard: Resume playback for incoming alarm if audio was silenced by volume rocker while activity remained open
        alarmViewModel.startPlaybackIfNeeded(newId)
        alarmViewModel.loadCohort(merged)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activeInstance = WeakReference(this)

        // Turn screen on and unlock keyguard
        setupWindowFlags()

        val entryId = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID) ?: ""
        val entryIds = intent.getStringArrayListExtra(MedicineAlarmReceiver.EXTRA_ENTRY_IDS) ?: if (entryId.isNotEmpty()) arrayListOf(entryId) else arrayListOf()
        val initialMedicineName = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME) ?: "Medication"
        val initialScheduledTime = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME) ?: ""

        val distinctIds = entryIds.filter { it.isNotEmpty() }.distinct()
        activeEntryIdsState.value = distinctIds
        activeMedicineNameState.value = initialMedicineName
        activeScheduledTimeState.value = initialScheduledTime

        // Start playback and load medication cohort via ViewModel
        alarmViewModel.startPlaybackIfNeeded(entryId)
        alarmViewModel.loadCohort(distinctIds)

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
            val spTheme = prefs.getString("theme", "system") ?: "system"

            val uiState by alarmViewModel.uiState.collectAsState()
            val user = uiState.user

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
                        medicinesList = uiState.medicinesList,
                        user = user,
                        isLoaded = uiState.isLoaded,
                        isDarkTheme = isDark,
                        onTakeAction = {
                            val isIntakeReAlert = intent?.getBooleanExtra(MedicineAlarmReceiver.EXTRA_IS_INTAKE_REALERT, false) ?: false
                            if (user?.intakeSafeguardEnabled == true && !isIntakeReAlert) {
                                // Guard: Start intake countdown timer for ADHD support on alarm dismissal (#104)
                                val medNames = uiState.medicinesList.map { it.second.medicationName }.ifEmpty { listOf(currentMedName) }
                                IntakeSafeguardManager.startCountdown(
                                    context = applicationContext,
                                    entryId = currentEntryIds.firstOrNull() ?: "",
                                    entryIds = currentEntryIds,
                                    medicineName = currentMedName,
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
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.alarm_snoozed_toast, minutes),
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
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
