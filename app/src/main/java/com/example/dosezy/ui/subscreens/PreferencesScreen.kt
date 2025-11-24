package com.example.dosezy.ui.subscreens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.example.dosezy.data.model.Language
import com.example.dosezy.data.model.Theme
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.ui.components.PreferenceItem
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.viewmodels.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(navController: NavController) {
    val userViewModel: UserViewModel = hiltViewModel()
    val currentUser by userViewModel.currentUser.collectAsState()

    // State for showing dialogs
    var showThemeDialog by remember { mutableStateOf(false) }
    var showTimeFormatDialog by remember { mutableStateOf(false) }
    var showMissedAfterDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar(
                navController = navController,
                currentUser = currentUser,
                title = "Preferences",
                showBackButton = true,
                actions = {} // Add empty actions
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                // Theme Preference
                PreferenceItem(
                    title = "Theme",
                    currentValue = currentUser?.theme?.let {
                        when (it) {
                            Theme.LIGHT -> "Light"
                            Theme.DARK -> "Dark"
                        }
                    } ?: "Light",
                    iconName = "palette",
                    onClick = { showThemeDialog = true }
                )
            }

            item {
                // Time Format Preference
                PreferenceItem(
                    title = "Time Format",
                    currentValue = currentUser?.timeFormat?.let {
                        when (it) {
                            TimeFormat.HOUR_12 -> "12-hour"
                            TimeFormat.HOUR_24 -> "24-hour"
                        }
                    } ?: "12-hour",
                    iconName = "schedule",
                    onClick = { showTimeFormatDialog = true }
                )
            }

            item {
                // Consider Missed After Preference
                PreferenceItem(
                    title = "Consider Missed After",
                    currentValue = currentUser?.considerMissedAfter?.let {
                        "$it hour${if (it > 1) "s" else ""}"
                    } ?: "3 hours",
                    iconName = "timer",
                    onClick = { showMissedAfterDialog = true }
                )
            }

            item {
                // Language Preference
                PreferenceItem(
                    title = "Language",
                    currentValue = currentUser?.language?.let {
                        when (it) {
                            Language.ENGLISH -> "English"
                        }
                    } ?: "English",
                    iconName = "language",
                    onClick = { showLanguageDialog = true }
                )
            }
        }

        // Theme Selection Dialog
        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = currentUser?.theme ?: Theme.LIGHT,
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

        // Missed After Selection Dialog
        if (showMissedAfterDialog) {
            MissedAfterSelectionDialog(
                currentHours = currentUser?.considerMissedAfter ?: 3,
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
            LanguageSelectionDialog(
                currentLanguage = currentUser?.language ?: Language.ENGLISH,
                onLanguageSelected = { newLanguage ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(language = newLanguage))
                    }
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false }
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
        title = "Select Theme",
        options = listOf("Light" to Theme.LIGHT),
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
        title = "Select Time Format",
        options = listOf(
            "12-hour" to TimeFormat.HOUR_12,
            "24-hour" to TimeFormat.HOUR_24
        ),
        currentSelection = currentTimeFormat,
        onOptionSelected = onTimeFormatSelected,
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
    com.example.dosezy.ui.components.SelectionDialog(
        title = "Mark Missed After",
        options = (1..6).map { hours ->
            "$hours hour${if (hours > 1) "s" else ""}" to hours
        },
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
    com.example.dosezy.ui.components.SelectionDialog(
        title = "Select Language",
        options = listOf("English" to Language.ENGLISH),
        currentSelection = currentLanguage,
        onOptionSelected = onLanguageSelected,
        onDismiss = onDismiss
    )
}