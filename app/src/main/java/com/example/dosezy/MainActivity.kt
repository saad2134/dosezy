package com.example.dosezy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dosezy.ui.screens.HomeScreen
import com.example.dosezy.ui.screens.LoadingScreen
import com.example.dosezy.ui.screens.NewUserScreen
import com.example.dosezy.ui.theme.DosezyTheme

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

    // Check user data on app start
    LaunchedEffect(Unit) {
        val userHasData = checkIfUserHasData()
        hasUserData = userHasData
        isLoading = false

        if (userHasData) {
            navController.navigate("home") {
                popUpTo("loading") { inclusive = true }
            }
        } else {
            navController.navigate("newuser/1") {
                popUpTo("loading") { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "loading"
    ) {
        composable("loading") {
            LoadingScreen()
        }
        composable("home") {
            HomeScreen(navController)
        }
        // ... rest of your screens remain the same
        composable("newuser/{frameNumber}") { backStackEntry ->
            val frame = backStackEntry.arguments?.getString("frameNumber")?.toIntOrNull() ?: 1
            NewUserScreen(
                currentFrame = frame,
                onNext = { nextFrame ->
                    if (nextFrame <= 3) {
                        navController.navigate("newuser/$nextFrame")
                    } else {
                        navController.navigate("home") {
                            popUpTo("loading") { inclusive = true }
                        }
                    }
                },
                onSkip = {
                    navController.navigate("home") {
                        popUpTo("loading") { inclusive = true }
                    }
                }
            )
        }
    }
}

// Your existing LoadingScreen and other composables remain the same

// Add this function after the DosezyApp composable
private suspend fun checkIfUserHasData(): Boolean {
    // TODO: Replace with your actual data check logic
    // For now, we'll simulate a check and return false to show onboarding

    // Simulate network/database check with delay
    kotlinx.coroutines.delay(1000) // 1 second delay

    // Check if user has any medicines or data in your storage
    // You'll replace this with actual checks later

    // For now, always return false to see the onboarding flow
    return false

    // Later, you'll implement real checks like:
    // return medicineRepository.hasAnyMedicines()
    // return sharedPreferences.getBoolean("has_completed_onboarding", false)
}