package com.example.dosezy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.dosezy.ui.theme.DosezyTheme
import com.example.dosezy.ui.viewmodels.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.setBackgroundColor(android.graphics.Color.parseColor("#0F172A"))
        super.onCreate(savedInstanceState)
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        setContent {
            val userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
            val currentUser by userViewModel.currentUser.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current

            // Read theme from SharedPreferences synchronously to prevent launch flash
            val prefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
            var spTheme by remember { mutableStateOf(prefs.getString("theme", "system")) }

            LaunchedEffect(currentUser?.theme) {
                currentUser?.theme?.let { theme ->
                    val themeStr = theme.name.lowercase()
                    if (prefs.getString("theme", "system") != themeStr) {
                        prefs.edit().putString("theme", themeStr).apply()
                        spTheme = themeStr
                    }
                }
            }

            LaunchedEffect(currentUser?.language) {
                currentUser?.language?.let { lang ->
                    com.example.dosezy.utils.LocaleHelper.applyLanguage(context, lang)
                }
            }

            val isDark = when (spTheme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            DosezyTheme(darkTheme = isDark) {
                androidx.compose.material3.Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DosezyApp()
                }
            }
        }
    }
}

@Composable
fun DosezyApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val context = androidx.compose.ui.platform.LocalContext.current

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        // Delay slightly to prevent standard permission dialog overlap
        kotlinx.coroutines.delay(1000)
        if (!com.example.dosezy.utils.NotificationUtils.isIgnoringBatteryOptimizations(context)) {
            com.example.dosezy.utils.NotificationUtils.requestBatteryOptimizationExemption(context)
        }
    }

    val userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
    val users by userViewModel.users.collectAsState()
    val currentUser by userViewModel.currentUser.collectAsState()
    val isViewModelLoading by userViewModel.isLoading.collectAsState()

    var isInitialized by remember { mutableStateOf(false) }

    // Check user data on app start
    LaunchedEffect(users, currentUser, isViewModelLoading) {
        if (!isInitialized && !isViewModelLoading) {
            isInitialized = true
            if (currentUser == null) {
                navController.navigate("newuser/1") {
                    popUpTo("home") { inclusive = true }
                }
            }
        }
    }

    // Timeout fallback - if loading takes too long and no user, go to onboarding
    LaunchedEffect(Unit) {
        if (!isInitialized) {
            kotlinx.coroutines.delay(2000)
            if (!isInitialized) {
                isInitialized = true
                if (currentUser == null) {
                    navController.navigate("newuser/1") {
                        popUpTo("home") { inclusive = true }
                    }
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
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            // Main Screens
            composable("loading") {
                com.example.dosezy.ui.components.HomeSkeletonView()
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

            // New User Onboarding Flow
            composable("newuser/{frameNumber}") { backStackEntry ->
                val frame = backStackEntry.arguments?.getString("frameNumber")?.toIntOrNull() ?: 1

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