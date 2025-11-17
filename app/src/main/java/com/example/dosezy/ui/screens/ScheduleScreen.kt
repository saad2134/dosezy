package com.example.dosezy.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dosezy.ui.components.ScheduleCalendar
import com.example.dosezy.ui.components.ScheduleList
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.viewmodels.ScheduleViewModel
import com.example.dosezy.ui.viewmodels.UserViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScheduleScreen(navController: NavController) {
    val userViewModel: UserViewModel = hiltViewModel()
    val scheduleViewModel: ScheduleViewModel = hiltViewModel()

    val currentUser by userViewModel.currentUser.collectAsState()
    val selectedDate by scheduleViewModel.selectedDate.collectAsState()
    val scheduleEntries by scheduleViewModel.scheduleEntries.collectAsState()

    var showNotificationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        TopBar(
            navController = navController,
            currentUser = currentUser,
            title = "Schedule",
            showBackButton = false,
            actions = {
                IconButton(onClick = { showNotificationDialog = true }) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White
                        )
                        // Notification status indicator (green check)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(12.dp)
                                .background(Color.Green, MaterialTheme.shapes.small)
                        )
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Calendar Section
            ScheduleCalendar(
                selectedDate = selectedDate,
                scheduleEntries = scheduleEntries,
                onDateSelected = { date ->
                    scheduleViewModel.setSelectedDate(date)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 8.dp)
            )

            // Schedule List Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 16.dp)
            ) {
                // Date Header
                Text(
                    text = selectedDate.format(
                        java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy")
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D171B),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Schedule Items
                if (scheduleEntries.isNotEmpty()) {
                    ScheduleList(
                        entries = scheduleEntries,
                        onMarkAsTaken = { entryId, takenAt ->
                            scheduleViewModel.markAsTaken(entryId, takenAt)
                        },
                        onMarkAsLate = { entryId, takenAt ->
                            scheduleViewModel.markAsLate(entryId, takenAt)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No medications scheduled for this date",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }


}

@Preview
@Composable
fun ScheduleScreenPreview() {
    MaterialTheme {
        ScheduleScreen(navController = androidx.navigation.compose.rememberNavController())
    }
}