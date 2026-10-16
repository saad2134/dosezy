/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.subscreens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dosezy.R
import com.example.dosezy.data.model.AlarmSound
import com.example.dosezy.data.model.Language
import com.example.dosezy.data.model.Theme
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.subscreens.preferences.AlarmDurationSelectionDialog
import com.example.dosezy.ui.subscreens.preferences.AlarmSoundSelectionDialog
import com.example.dosezy.ui.subscreens.preferences.IntakeDurationSelectionDialog
import com.example.dosezy.ui.subscreens.preferences.LanguageSelectionDialog
import com.example.dosezy.ui.subscreens.preferences.LateAfterSelectionDialog
import com.example.dosezy.ui.subscreens.preferences.MissedAfterSelectionDialog
import com.example.dosezy.ui.subscreens.preferences.SnoozeSelectionDialog
import com.example.dosezy.ui.subscreens.preferences.ThemeSelectionDialog
import com.example.dosezy.ui.subscreens.preferences.TimeFormatSelectionDialog
import com.example.dosezy.ui.subscreens.preferences.appearancePreferencesSection
import com.example.dosezy.ui.subscreens.preferences.doseTrackingPreferencesSection
import com.example.dosezy.ui.subscreens.preferences.generalPreferencesSection
import com.example.dosezy.ui.subscreens.preferences.notificationAlarmPreferencesSection
import com.example.dosezy.ui.viewmodels.UserViewModel
import com.example.dosezy.utils.LocaleHelper
import com.example.dosezy.utils.sharedUserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(navController: NavController) {
    val userViewModel: UserViewModel = sharedUserViewModel()
    val currentUser by userViewModel.currentUser.collectAsState()
    val context = LocalContext.current

    // State for showing dialogs
    var showThemeDialog by remember { mutableStateOf(false) }
    var showTimeFormatDialog by remember { mutableStateOf(false) }
    var showLateAfterDialog by remember { mutableStateOf(false) }
    var showMissedAfterDialog by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showAlarmSoundDialog by remember { mutableStateOf(false) }
    var showAlarmDurationDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showNaggingIntervalDialog by remember { mutableStateOf(false) }
    var showNaggingRepeatsDialog by remember { mutableStateOf(false) }
    var showIntakeDurationDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopBar(
                navController = navController,
                currentUser = currentUser,
                title = stringResource(R.string.pref_title),
                showBackButton = true,
                actions = {}
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── General Section ──
            generalPreferencesSection(
                currentUser = currentUser,
                onLanguageClick = { showLanguageDialog = true }
            )

            // ── Appearance Section ──
            appearancePreferencesSection(
                currentUser = currentUser,
                onThemeClick = { showThemeDialog = true },
                onTimelineModeChange = { isChecked ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(timelineModeEnabled = isChecked))
                    }
                },
                onThickerCalendarChange = { isChecked ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(thickerCalendarDayHighlight = isChecked))
                    }
                },
                onHideAddNavButtonChange = { isChecked ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(hideAddMedicineNavButton = isChecked))
                    }
                }
            )

            // ── Notifications & Alarms Section ──
            notificationAlarmPreferencesSection(
                currentUser = currentUser,
                onSlideActionsChange = { isChecked ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(slideActionsEnabled = isChecked))
                    }
                },
                onAlarmSoundClick = { showAlarmSoundDialog = true },
                onAlarmDurationClick = { showAlarmDurationDialog = true },
                onSnoozeClick = { showSnoozeDialog = true },
                onNaggingEnabledChange = { isChecked ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(naggingRemindersEnabled = isChecked))
                    }
                },
                onNaggingIntervalClick = { showNaggingIntervalDialog = true },
                onNaggingRepeatsClick = { showNaggingRepeatsDialog = true }
            )

            // ── Dose Tracking Section ──
            doseTrackingPreferencesSection(
                currentUser = currentUser,
                onAllowDoseSkippingChange = { isChecked ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(allowDoseSkipping = isChecked))
                    }
                },
                onAllowCustomDoseTimeChange = { isChecked ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(allowCustomDoseTime = isChecked))
                    }
                },
                onAllowDoseUndoChange = { isChecked ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(allowDoseUndo = isChecked))
                    }
                },
                onAllowDoseNotesChange = { isChecked ->
                    currentUser?.let { user ->
                        val updated = if (!isChecked) {
                            user.copy(allowDoseNotes = false, promptDoseNotes = false)
                        } else {
                            user.copy(allowDoseNotes = true)
                        }
                        userViewModel.updateUser(updated)
                    }
                },
                onPromptDoseNotesChange = { isChecked ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(promptDoseNotes = isChecked))
                    }
                },
                onIntakeSafeguardChange = { isChecked ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(intakeSafeguardEnabled = isChecked))
                    }
                },
                onIntakeDurationClick = { showIntakeDurationDialog = true },
                onLateAfterClick = { showLateAfterDialog = true },
                onMissedAfterClick = { showMissedAfterDialog = true },
                onTimeFormatClick = { showTimeFormatDialog = true }
            )

            item {
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Nagging Interval Dialog
        if (showNaggingIntervalDialog) {
            val options = listOf(5, 10, 15, 20, 30).map { mins ->
                stringResource(R.string.minutes_format, mins) to mins
            }
            com.example.dosezy.ui.components.SelectionDialog(
                title = stringResource(R.string.pref_nagging_interval),
                options = options,
                currentSelection = currentUser?.naggingIntervalMinutes ?: 10,
                onOptionSelected = { mins ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(naggingIntervalMinutes = mins))
                    }
                    showNaggingIntervalDialog = false
                },
                onDismiss = { showNaggingIntervalDialog = false }
            )
        }

        // Nagging Repeats Dialog
        if (showNaggingRepeatsDialog) {
            val options = listOf(1, 2, 3, 5).map { repeats ->
                stringResource(R.string.pref_nagging_repeats_format, repeats) to repeats
            }
            com.example.dosezy.ui.components.SelectionDialog(
                title = stringResource(R.string.pref_nagging_max_repeats),
                options = options,
                currentSelection = currentUser?.naggingMaxRepeats ?: 3,
                onOptionSelected = { count ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(naggingMaxRepeats = count))
                    }
                    showNaggingRepeatsDialog = false
                },
                onDismiss = { showNaggingRepeatsDialog = false }
            )
        }

        // Theme Selection Dialog
        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = currentUser?.theme ?: Theme.SYSTEM,
                onThemeSelected = { newTheme ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(theme = newTheme))
                    }
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }

        // Time Format Selection Dialog
        if (showTimeFormatDialog) {
            TimeFormatSelectionDialog(
                currentTimeFormat = currentUser?.timeFormat ?: TimeFormat.HOUR_12,
                onTimeFormatSelected = { newTimeFormat ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(timeFormat = newTimeFormat))
                    }
                    showTimeFormatDialog = false
                },
                onDismiss = { showTimeFormatDialog = false }
            )
        }

        // Late After Selection Dialog
        if (showLateAfterDialog) {
            LateAfterSelectionDialog(
                currentHours = currentUser?.considerLateAfter ?: 3,
                currentMissedHours = currentUser?.considerMissedAfter ?: 6,
                onHoursSelected = { newHours ->
                    currentUser?.let { user ->
                        val safeMissed = if (user.considerMissedAfter <= newHours) newHours + 1 else user.considerMissedAfter
                        userViewModel.updateUser(user.copy(considerLateAfter = newHours, considerMissedAfter = safeMissed))
                    }
                    showLateAfterDialog = false
                },
                onDismiss = { showLateAfterDialog = false }
            )
        }

        // Missed After Selection Dialog
        if (showMissedAfterDialog) {
            MissedAfterSelectionDialog(
                currentHours = currentUser?.considerMissedAfter ?: 6,
                currentLateHours = currentUser?.considerLateAfter ?: 3,
                onHoursSelected = { newHours ->
                    currentUser?.let { user ->
                        val safeLate = if (user.considerLateAfter >= newHours) newHours - 1 else user.considerLateAfter
                        userViewModel.updateUser(user.copy(considerMissedAfter = newHours, considerLateAfter = safeLate))
                    }
                    showMissedAfterDialog = false
                },
                onDismiss = { showMissedAfterDialog = false }
            )
        }

        // Language Selection Dialog
        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguage = currentUser?.language ?: Language.SYSTEM,
                onLanguageSelected = { newLanguage ->
                    showLanguageDialog = false
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(language = newLanguage))
                        LocaleHelper.applyLanguage(context, newLanguage, forceRecreate = true)
                    }
                },
                onDismiss = { showLanguageDialog = false }
            )
        }

        // Snooze Selection Dialog
        if (showSnoozeDialog) {
            SnoozeSelectionDialog(
                currentMinutes = currentUser?.snoozeDuration ?: 10,
                onMinutesSelected = { newMinutes ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(snoozeDuration = newMinutes))
                    }
                    showSnoozeDialog = false
                },
                onDismiss = { showSnoozeDialog = false }
            )
        }

        // Alarm Sound Selection Dialog
        if (showAlarmSoundDialog) {
            AlarmSoundSelectionDialog(
                currentSound = currentUser?.alarmSound ?: AlarmSound.SYSTEM_DEFAULT,
                customSoundTitle = currentUser?.customAlarmSoundTitle,
                customSoundPath = currentUser?.customAlarmSoundPath,
                onSoundSelected = { newSound, customPath, customTitle ->
                    currentUser?.let { user ->
                        // Guard: Delete previous custom alarm sound file when replaced by a new custom sound to prevent disk storage leaks
                        if (customPath != null && customPath != user.customAlarmSoundPath) {
                            com.example.dosezy.utils.SoundUtils.deleteCustomAudio(context, user.customAlarmSoundPath)
                        }
                        userViewModel.updateUser(
                            user.copy(
                                alarmSound = newSound,
                                customAlarmSoundPath = customPath ?: user.customAlarmSoundPath,
                                customAlarmSoundTitle = customTitle ?: user.customAlarmSoundTitle
                            )
                        )
                    }
                },
                onDismiss = { showAlarmSoundDialog = false }
            )
        }

        // Alarm Duration Selection Dialog
        if (showAlarmDurationDialog) {
            AlarmDurationSelectionDialog(
                currentDuration = currentUser?.alarmDurationSeconds ?: 0,
                onDurationSelected = { newDuration ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(alarmDurationSeconds = newDuration))
                    }
                    showAlarmDurationDialog = false
                },
                onDismiss = { showAlarmDurationDialog = false }
            )
        }

        // Guard: ADHD support intake countdown duration selection dialog (#104)
        if (showIntakeDurationDialog) {
            IntakeDurationSelectionDialog(
                currentSeconds = currentUser?.intakeCountdownSeconds ?: 120,
                onDurationSelected = { newSeconds ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(intakeCountdownSeconds = newSeconds))
                    }
                    showIntakeDurationDialog = false
                },
                onDismiss = { showIntakeDurationDialog = false }
            )
        }
    }
}