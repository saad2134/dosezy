package com.example.dosezy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dosezy.ui.components.CustomNavigationBar
import com.example.dosezy.ui.screens.DebugScreen
import com.example.dosezy.ui.screens.HomeScreen
import com.example.dosezy.ui.screens.LoadingScreen
import com.example.dosezy.ui.screens.MedicinesScreen
import com.example.dosezy.ui.screens.MenuScreen
import com.example.dosezy.ui.screens.NewUserScreen
import com.example.dosezy.ui.screens.ScheduleScreen
import com.example.dosezy.ui.subscreens.AddMedScreen
import com.example.dosezy.ui.subscreens.EditMedScreen
import com.example.dosezy.ui.subscreens.EmergencyScreen
import com.example.dosezy.ui.subscreens.HelpSupportScreen
import com.example.dosezy.ui.subscreens.ManageProfileScreen
import com.example.dosezy.ui.subscreens.PreferencesScreen
import com.example.dosezy.ui.subscreens.SwitchProfileScreen
import com.example.dosezy.ui.theme.DosezyTheme
import com.example.dosezy.ui.viewmodels.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DosezyTheme {
                DosezyApp()
            }
        }
    }
}

@Composable
fun DosezyApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination

    var isLoading by remember { mutableStateOf(true) }
    var dataCheckComplete by remember { mutableStateOf(false) }


    val userViewModel: UserViewModel = hiltViewModel()
    val users by userViewModel.users.collectAsState()
    val currentUser by userViewModel.currentUser.collectAsState()

    // Check user data on app start
    LaunchedEffect(users, currentUser) {
        if (isLoading) {
            try {
                // Wait for data to load
                if (users.isNotEmpty() || currentUser != null) {
                    // Data is loaded, now decide navigation
                    val shouldGoToHome = currentUser != null

                    if (shouldGoToHome) {
                        // Current user exists - go directly to home
                        navController.navigate("home") {
                            popUpTo("loading") { inclusive = true }
                        }
                    } else if (users.isNotEmpty()) {
                        // Users exist but no current user selected - show onboarding with existing profiles
                        navController.navigate("newuser/1") {
                            popUpTo("loading") { inclusive = true }
                        }
                    } else {
                        // No users at all - show full onboarding
                        navController.navigate("newuser/1") {
                            popUpTo("loading") { inclusive = true }
                        }
                    }

                    isLoading = false
                    dataCheckComplete = true
                }
                // If both are empty, we'll stay in loading until timeout
            } catch (e: Exception) {
                // If there's an error, default to onboarding
                isLoading = false
                navController.navigate("newuser/1") {
                    popUpTo("loading") { inclusive = true }
                }
            }
        }
    }

    // Timeout fallback - if loading takes too long, go to onboarding
    LaunchedEffect(Unit) {
        if (isLoading) {
            kotlinx.coroutines.delay(5000) // 5 second timeout
            if (isLoading) {
                isLoading = false
                navController.navigate("newuser/1") {
                    popUpTo("loading") { inclusive = true }
                }
            }
        }
    }

    // Show navigation bar only for main screens, not for loading or onboarding
    val showBottomBar = currentDestination?.route in listOf("home", "schedule", "medicines", "menu")

    androidx.compose.material3.Scaffold(
        bottomBar = {
            if (showBottomBar) {
                CustomNavigationBar(
                    navController = navController,
                    currentDestination = currentDestination
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "loading",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Main Screens
            composable("loading") {
                LoadingScreen(
                    isLoading = isLoading,
                    hasError = false,
                    message = if (isLoading) "Loading your data..." else "Redirecting..."
                )
            }
            composable("home") {
                HomeScreen(
                    navController = navController,
                    shouldRefresh = true
                )
            }
            composable("schedule") {
                ScheduleScreen(navController = navController)
            }
            composable("medicines") {
                MedicinesScreen(navController)
            }
            composable("menu") {
                MenuScreen(navController)
            }

            // Debug Screen
            composable("debug") {
                DebugScreen()
            }

            // Profile Management Screens
            composable("manage_profile") {
                ManageProfileScreen(navController)
            }
            composable("switch_profile") {
                SwitchProfileScreen(navController)
            }

            // Medicine Management Screens
            composable("add_med") {
                AddMedScreen(navController)
            }
            composable("edit_med/{medicineId}") { backStackEntry ->
                val medicineId = backStackEntry.arguments?.getString("medicineId")
                EditMedScreen(
                    navController = navController,
                    medicineId = medicineId
                )
            }

            // Support Screens
            composable("emergency") {
                EmergencyScreen(navController)
            }
            composable("help_support") {
                HelpSupportScreen(navController)
            }
            composable("preferences") {
                PreferencesScreen(navController)
            }

            // New User Onboarding Flow - UPDATED
            composable("newuser/{frameNumber}") { backStackEntry ->
                val frame = backStackEntry.arguments?.getString("frameNumber")?.toIntOrNull() ?: 1
                val userViewModel: UserViewModel = hiltViewModel()

                NewUserScreen(
                    navController = navController,
                    currentFrame = frame,
                    onNext = { nextFrame ->
                        if (nextFrame <= 3) {
                            navController.navigate("newuser/$nextFrame")
                        } else {
                            // Onboarding complete - go to home
                            navController.navigate("home") {
                                popUpTo("newuser/1") { inclusive = true }
                            }
                        }
                    },
                    onSkip = {
                        // Skip onboarding - go to home
                        navController.navigate("home") {
                            popUpTo("newuser/1") { inclusive = true }
                        }
                    },
                    isCreatingNewProfile = false // This is regular onboarding
                )
            }

            // Create New Profile Flow (from Switch Profile screen)
            composable("create_profile/{frameNumber}") { backStackEntry ->
                val frame = backStackEntry.arguments?.getString("frameNumber")?.toIntOrNull() ?: 1
                val userViewModel: UserViewModel = hiltViewModel()

                NewUserScreen(
                    navController = navController,
                    currentFrame = frame,
                    onNext = { nextFrame ->
                        if (nextFrame <= 3) {
                            navController.navigate("create_profile/$nextFrame")
                        } else {
                            // Profile creation complete - go back to switch profile
                            navController.popBackStack("switch_profile", false)
                        }
                    },
                    onSkip = {
                        // Skip profile creation - go back to switch profile
                        navController.popBackStack("switch_profile", false)
                    },
                    isCreatingNewProfile = true // This is creating from switch screen
                )
            }
        }
    }
}