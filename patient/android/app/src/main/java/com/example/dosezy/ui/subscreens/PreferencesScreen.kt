package com.example.dosezy.ui.subscreens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dosezy.R
import com.example.dosezy.data.model.Language
import com.example.dosezy.data.model.Theme
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.ui.components.PreferenceItem
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.viewmodels.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(navController: NavController) {
    val userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
    val currentUser by userViewModel.currentUser.collectAsState()

    // State for showing dialogs
    var showThemeDialog by remember { mutableStateOf(false) }
    var showTimeFormatDialog by remember { mutableStateOf(false) }
    var showLateAfterDialog by remember { mutableStateOf(false) }
    var showMissedAfterDialog by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar(
                navController = navController,
                currentUser = currentUser,
                title = androidx.compose.ui.res.stringResource(R.string.pref_title),
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
            item {
                Spacer(modifier = Modifier.height(12.dp))
                // Theme Preference
                PreferenceItem(
                    title = androidx.compose.ui.res.stringResource(R.string.pref_theme),
                    currentValue = currentUser?.theme?.let {
                        when (it) {
                            Theme.LIGHT -> androidx.compose.ui.res.stringResource(R.string.theme_light)
                            Theme.DARK -> androidx.compose.ui.res.stringResource(R.string.theme_dark)
                            Theme.SYSTEM -> androidx.compose.ui.res.stringResource(R.string.theme_system)
                        }
                    } ?: androidx.compose.ui.res.stringResource(R.string.theme_system),
                    iconName = "palette",
                    onClick = { showThemeDialog = true }
                )
            }

            item {
                // Time Format Preference
                PreferenceItem(
                    title = androidx.compose.ui.res.stringResource(R.string.pref_time_format),
                    currentValue = currentUser?.timeFormat?.let {
                        when (it) {
                            TimeFormat.HOUR_12 -> androidx.compose.ui.res.stringResource(R.string.time_format_12)
                            TimeFormat.HOUR_24 -> androidx.compose.ui.res.stringResource(R.string.time_format_24)
                        }
                    } ?: androidx.compose.ui.res.stringResource(R.string.time_format_12),
                    iconName = "schedule",
                    onClick = { showTimeFormatDialog = true }
                )
            }

            item {
                // Consider Late After Preference
                PreferenceItem(
                    title = androidx.compose.ui.res.stringResource(R.string.pref_late_after),
                    currentValue = currentUser?.considerLateAfter?.let {
                        androidx.compose.ui.res.stringResource(R.string.hours_format, it)
                    } ?: androidx.compose.ui.res.stringResource(R.string.hours_format, 3),
                    iconName = "timer",
                    onClick = { showLateAfterDialog = true }
                )
            }

            item {
                // Consider Missed After Preference
                PreferenceItem(
                    title = androidx.compose.ui.res.stringResource(R.string.pref_missed_after),
                    currentValue = currentUser?.considerMissedAfter?.let {
                        androidx.compose.ui.res.stringResource(R.string.hours_format, it)
                    } ?: androidx.compose.ui.res.stringResource(R.string.hours_format, 6),
                    iconName = "timer_off",
                    onClick = { showMissedAfterDialog = true }
                )
            }

            item {
                // Snooze Duration Preference
                PreferenceItem(
                    title = androidx.compose.ui.res.stringResource(R.string.pref_snooze_duration),
                    currentValue = currentUser?.snoozeDuration?.let {
                        androidx.compose.ui.res.stringResource(R.string.minutes_format, it)
                    } ?: androidx.compose.ui.res.stringResource(R.string.minutes_format, 10),
                    iconName = "snooze",
                    onClick = { showSnoozeDialog = true }
                )
            }

            item {
                // Language Preference
                val sysLangName = com.example.dosezy.utils.LocaleHelper.getSystemLanguageDisplayName()
                PreferenceItem(
                    title = androidx.compose.ui.res.stringResource(R.string.pref_language),
                    currentValue = currentUser?.language?.let {
                        when (it) {
                            Language.SYSTEM -> "${androidx.compose.ui.res.stringResource(R.string.system_default)} ($sysLangName)"
                            Language.ENGLISH -> "English"
                            Language.SPANISH -> "Español"
                            Language.HINDI -> "हिन्दी"
                            Language.CHINESE -> "中文"
                            Language.PORTUGUESE -> "Português"
                            Language.ARABIC -> "العربية"
                            Language.FRENCH -> "Français"
                            Language.GERMAN -> "Deutsch"
                            Language.JAPANESE -> "日本語"
                            Language.RUSSIAN -> "Русский"
                            Language.ITALIAN -> "Italiano"
                            Language.BENGALI -> "বাংলা"
                        }
                    } ?: "${androidx.compose.ui.res.stringResource(R.string.system_default)} ($sysLangName)",
                    iconName = "language",
                    onClick = { showLanguageDialog = true }
                )
            }
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
                onHoursSelected = { newHours ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(considerLateAfter = newHours))
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
                onHoursSelected = { newHours ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(considerMissedAfter = newHours))
                    }
                    showMissedAfterDialog = false
                },
                onDismiss = { showMissedAfterDialog = false }
            )
        }

        // Language Selection Dialog
        if (showLanguageDialog) {
            val context = androidx.compose.ui.platform.LocalContext.current
            LanguageSelectionDialog(
                currentLanguage = currentUser?.language ?: Language.SYSTEM,
                onLanguageSelected = { newLanguage ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(language = newLanguage))
                        com.example.dosezy.utils.LocaleHelper.applyLanguage(context, newLanguage, forceRecreate = true)
                    }
                    showLanguageDialog = false
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
    }
}

// Dialog Composable for Theme Selection
@Composable
fun ThemeSelectionDialog(
    currentTheme: Theme,
    onThemeSelected: (Theme) -> Unit,
    onDismiss: () -> Unit
) {
    com.example.dosezy.ui.components.SelectionDialog(
        title = androidx.compose.ui.res.stringResource(R.string.pref_select_theme),
        options = listOf(
            androidx.compose.ui.res.stringResource(R.string.theme_light) to Theme.LIGHT,
            androidx.compose.ui.res.stringResource(R.string.theme_dark) to Theme.DARK,
            androidx.compose.ui.res.stringResource(R.string.theme_system) to Theme.SYSTEM
        ),
        currentSelection = currentTheme,
        onOptionSelected = onThemeSelected,
        onDismiss = onDismiss
    )
}

// Dialog Composable for Time Format Selection
@Composable
fun TimeFormatSelectionDialog(
    currentTimeFormat: TimeFormat,
    onTimeFormatSelected: (TimeFormat) -> Unit,
    onDismiss: () -> Unit
) {
    com.example.dosezy.ui.components.SelectionDialog(
        title = androidx.compose.ui.res.stringResource(R.string.pref_select_time_format),
        options = listOf(
            androidx.compose.ui.res.stringResource(R.string.time_format_12) to TimeFormat.HOUR_12,
            androidx.compose.ui.res.stringResource(R.string.time_format_24) to TimeFormat.HOUR_24
        ),
        currentSelection = currentTimeFormat,
        onOptionSelected = onTimeFormatSelected,
        onDismiss = onDismiss
    )
}

// Dialog Composable for Late After Selection
@Composable
fun LateAfterSelectionDialog(
    currentHours: Int,
    onHoursSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val hourOptionStrings = (1..3).map { hours ->
        androidx.compose.ui.res.stringResource(R.string.hours_format, hours) to hours
    }
    com.example.dosezy.ui.components.SelectionDialog(
        title = androidx.compose.ui.res.stringResource(R.string.pref_late_after),
        options = hourOptionStrings,
        currentSelection = currentHours,
        onOptionSelected = onHoursSelected,
        onDismiss = onDismiss
    )
}

// Dialog Composable for Missed After Selection
@Composable
fun MissedAfterSelectionDialog(
    currentHours: Int,
    onHoursSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val hourOptionStrings = (3..9).map { hours ->
        androidx.compose.ui.res.stringResource(R.string.hours_format, hours) to hours
    }
    com.example.dosezy.ui.components.SelectionDialog(
        title = androidx.compose.ui.res.stringResource(R.string.pref_mark_missed_after),
        options = hourOptionStrings,
        currentSelection = currentHours,
        onOptionSelected = onHoursSelected,
        onDismiss = onDismiss
    )
}

// Dialog Composable for Language Selection
@Composable
fun LanguageSelectionDialog(
    currentLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    val sysDefaultStr = androidx.compose.ui.res.stringResource(R.string.system_default)
    com.example.dosezy.ui.components.SelectionDialog(
        title = androidx.compose.ui.res.stringResource(R.string.select_language),
        options = listOf(
            sysDefaultStr to Language.SYSTEM,
            "English" to Language.ENGLISH,
            "Español (Spanish)" to Language.SPANISH,
            "हिन्दी (Hindi)" to Language.HINDI,
            "中文 (Chinese)" to Language.CHINESE,
            "Português (Portuguese)" to Language.PORTUGUESE,
            "العربية (Arabic)" to Language.ARABIC,
            "Français (French)" to Language.FRENCH,
            "Deutsch (German)" to Language.GERMAN,
            "日本語 (Japanese)" to Language.JAPANESE,
            "Русский (Russian)" to Language.RUSSIAN,
            "Italiano (Italian)" to Language.ITALIAN,
            "বাংলা (Bengali)" to Language.BENGALI
        ),
        currentSelection = currentLanguage,
        onOptionSelected = onLanguageSelected,
        onDismiss = onDismiss
    )
}

// Dialog Composable for Snooze Duration Selection
@Composable
fun SnoozeSelectionDialog(
    currentMinutes: Int,
    onMinutesSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(5, 10, 15, 20, 30).map { mins ->
        androidx.compose.ui.res.stringResource(R.string.minutes_format, mins) to mins
    }
    com.example.dosezy.ui.components.SelectionDialog(
        title = androidx.compose.ui.res.stringResource(R.string.pref_select_snooze_duration),
        options = options,
        currentSelection = currentMinutes,
        onOptionSelected = onMinutesSelected,
        onDismiss = onDismiss
    )
}