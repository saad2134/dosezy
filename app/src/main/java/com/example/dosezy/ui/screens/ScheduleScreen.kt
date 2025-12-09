package com.example.dosezy.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.ui.components.ScheduleCalendar
import com.example.dosezy.ui.components.ScheduleList
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.theme.DosezyTheme
import com.example.dosezy.ui.viewmodels.ScheduleViewModel
import com.example.dosezy.ui.viewmodels.UserViewModel
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScheduleScreen(navController: NavController) {
    val userViewModel: UserViewModel = hiltViewModel()
    val scheduleViewModel: ScheduleViewModel = hiltViewModel()

    val currentUser by userViewModel.currentUser.collectAsState()
    val selectedDate by scheduleViewModel.selectedDate.collectAsState()
    val scheduleWithMedicine by scheduleViewModel.scheduleWithMedicine.collectAsState()

    // Get user's time format preference
    val timeFormat = currentUser?.timeFormat ?: TimeFormat.HOUR_12

    // Load initial schedule when screen is first composed
    LaunchedEffect(Unit) {
        currentUser?.let { user ->
            Log.d("ScheduleScreen", "Initial load for user: ${user.userId}")
            scheduleViewModel.loadScheduleForDate(user.userId, selectedDate)
        }
    }

    // Reload schedule when selected date changes
    LaunchedEffect(selectedDate) {
        currentUser?.let { user ->
            Log.d("ScheduleScreen", "Date changed to: $selectedDate")
            scheduleViewModel.loadScheduleForDate(user.userId, selectedDate)
        }
    }

    // Reload when user changes
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            Log.d("ScheduleScreen", "User changed, reloading schedule")
            scheduleViewModel.loadScheduleForDate(user.userId, selectedDate)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopBar(
                navController = navController,
                currentUser = currentUser,
                title = "Schedule",
                showBackButton = false,
                actions = {}
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                // Calendar Section
                ScheduleCalendar(
                    selectedDate = selectedDate,
                    scheduleEntries = scheduleWithMedicine.map { it.scheduleEntry },
                    onDateSelected = { date ->
                        scheduleViewModel.setSelectedDate(date)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                )

                // Schedule List Section - takes remaining space
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // Date Header
                    Text(
                        text = selectedDate.format(
                            DateTimeFormatter.ofPattern("MMMM d, yyyy")
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // Schedule Items
                    if (scheduleWithMedicine.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 16.dp)
                        ) {
                            ScheduleList(
                                scheduleWithMedicine = scheduleWithMedicine,
                                timeFormat = timeFormat,
                                onMarkAsTaken = { entryId, takenAt ->
                                    scheduleViewModel.markAsTaken(entryId, takenAt)
                                },
                                onMarkAsLate = { entryId, takenAt ->
                                    scheduleViewModel.markAsLate(entryId, takenAt)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No medications scheduled for this date",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun ScheduleScreenPreview() {
    DosezyTheme {
        ScheduleScreen(navController = androidx.navigation.compose.rememberNavController())
    }
}