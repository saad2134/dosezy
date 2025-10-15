package com.example.dosezy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dosezy.ui.screens.HomeScreen
import com.example.dosezy.ui.screens.LoadingScreen
import com.example.dosezy.ui.screens.NewUserScreen
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
    var isLoading by remember { mutableStateOf(true) }
    var hasUserData by remember { mutableStateOf(false) }
    var dataCheckError by remember { mutableStateOf(false) }

    val userViewModel: UserViewModel = hiltViewModel()
    val users by userViewModel.users.collectAsState()

    // Check user data on app start
    LaunchedEffect(users) {
        if (isLoading) {
            try {
                val userHasData = checkIfUserHasData(users)
                hasUserData = userHasData
                isLoading = false
                dataCheckError = false

                navigateBasedOnUserData(
                    navController = navController,
                    hasUserData = hasUserData,
                    users = users
                )
            } catch (e: Exception) {
                // If there's an error, default to onboarding
                isLoading = false
                dataCheckError = true
                navController.navigate("newuser/1") {
                    popUpTo("loading") { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "loading"
    ) {
        composable("loading") {
            LoadingScreen(
                isLoading = isLoading,
                hasError = dataCheckError
            )
        }
        composable("home") {
            HomeScreen(navController)
        }
        composable("newuser/{frameNumber}") { backStackEntry ->
            val frame = backStackEntry.arguments?.getString("frameNumber")?.toIntOrNull() ?: 1
            NewUserScreen(
                navController = navController,
                currentFrame = frame,
                onNext = { nextFrame ->
                    if (nextFrame <= 3) {
                        navController.navigate("newuser/$nextFrame")
                    } else {
                        // Onboarding complete - navigate to home
                        navController.navigate("home") {
                            popUpTo("loading") { inclusive = true }
                        }
                    }
                },
                onSkip = {
                    // Skip onboarding - navigate to home
                    navController.navigate("home") {
                        popUpTo("loading") { inclusive = true }
                    }
                },
                isCreatingNewProfile = users.isEmpty() // Show existing profiles if users exist
            )
        }
    }
}

/**
 * Check if user has any data (current user exists and has medicines)
 */
private suspend fun checkIfUserHasData(users: List<com.example.dosezy.data.model.User>): Boolean {
    // Check if there's a current user
    val currentUser = users.find { it.isCurrentUser }
    return currentUser != null
    // You can add additional checks here later, like:
    // - Check if current user has medicines
    // - Check if onboarding was completed
    // - Check user preferences, etc.
}

/**
 * Navigate based on user data availability
 */
private fun navigateBasedOnUserData(
    navController: androidx.navigation.NavController,
    hasUserData: Boolean,
    users: List<com.example.dosezy.data.model.User>
) {
    if (hasUserData) {
        // User has data - go directly to home
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
}