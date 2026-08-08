package com.example.dosezy.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.example.dosezy.R
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
    val userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
    val scheduleViewModel: ScheduleViewModel = com.example.dosezy.utils.sharedScheduleViewModel()

    val currentUser by userViewModel.currentUser.collectAsState()
    val selectedDate by scheduleViewModel.selectedDate.collectAsState()
    val scheduleWithMedicine by scheduleViewModel.scheduleWithMedicine.collectAsState()
    val isRefreshing by scheduleViewModel.isRefreshing.collectAsState()

    // Get user's time format preference
    val timeFormat = currentUser?.timeFormat ?: TimeFormat.HOUR_12

    // Load schedule and auto-mark missed medications when screen is composed or when user/date changes
    LaunchedEffect(currentUser, selectedDate) {
        currentUser?.let { user ->
            Log.d("ScheduleScreen", "Loading schedule and auto-marking missed for user: ${user.userId}, date: $selectedDate")
            scheduleViewModel.autoMarkMissedMedications(
                user.userId,
                user.considerMissedAfter
            )
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
                title = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.schedule_title),
                showBackButton = false,
                actions = {}
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp)
            ) {
                // Calendar Section wrapped in Card-styled Surface
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 2.dp
                ) {
                    ScheduleCalendar(
                        selectedDate = selectedDate,
                        scheduleEntries = scheduleWithMedicine.map { it.scheduleEntry },
                        onDateSelected = { date ->
                            scheduleViewModel.setSelectedDate(date)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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
                if (isRefreshing && scheduleWithMedicine.isEmpty()) {
                    com.example.dosezy.ui.components.ScheduleSkeletonList(count = 3)
                } else if (scheduleWithMedicine.isNotEmpty()) {
                    scheduleWithMedicine.forEach { swm ->
                        com.example.dosezy.ui.components.ScheduleListItem(
                            scheduleWithMedicine = swm,
                            timeFormat = timeFormat,
                            onMarkAsTaken = { entryId, takenAt ->
                                scheduleViewModel.markAsTaken(entryId, takenAt)
                            },
                            onMarkAsLate = { entryId, takenAt ->
                                scheduleViewModel.markAsLate(entryId, takenAt)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.schedule_no_meds_date),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
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