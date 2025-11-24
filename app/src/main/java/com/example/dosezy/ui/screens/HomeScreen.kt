package com.example.dosezy.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleWithMedicine
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.theme.DosezyTheme
import com.example.dosezy.ui.viewmodels.ScheduleViewModel
import com.example.dosezy.ui.viewmodels.UserViewModel
import com.example.dosezy.utils.DateUtils
import com.example.dosezy.utils.TimeCalculationUtils
import com.example.dosezy.utils.TimeFormatUtils
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    navController: NavController,
    shouldRefresh: Boolean = false
) {
    val userViewModel: UserViewModel = hiltViewModel()
    val scheduleViewModel: ScheduleViewModel = hiltViewModel()

    val currentUser by userViewModel.currentUser.collectAsState()
    val scheduleWithMedicine by scheduleViewModel.scheduleWithMedicine.collectAsState()

    // Get current day for subtitle
    val dayOfWeek = DateUtils.getCurrentDayOfWeekLegacy()
    val today = java.time.LocalDate.now()
    val currentDateTime = java.time.LocalDateTime.now()

    // State for real-time updates
    var currentTime by remember { mutableStateOf(java.time.LocalDateTime.now()) }

    // Auto-refresh every minute for real-time updates
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000) // Update every minute
            currentTime = java.time.LocalDateTime.now()

            // Auto-mark as missed using ViewModel
            currentUser?.let { user ->
                scheduleViewModel.autoMarkMissedMedications(
                    user.userId,
                    user.considerMissedAfter
                )
            }
        }
    }

    // FIXED: Better refresh logic
    LaunchedEffect(shouldRefresh, currentUser) {
        currentUser?.let { user ->
            Log.d("HomeScreen", "Refreshing schedule for user: ${user.userId}")
            scheduleViewModel.setSelectedDate(today)
            // Force immediate refresh
            scheduleViewModel.loadScheduleForDate(user.userId, today)
        }
    }

    // Also listen for navigation events to refresh when coming back from AddMedScreen
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        // Refresh when we come back to home screen (e.g., from adding medicine)
        if (navBackStackEntry?.destination?.route == "home") {
            currentUser?.let { user ->
                scheduleViewModel.loadScheduleForDate(user.userId, java.time.LocalDate.now())
            }
        }
    }

    // Filter today's schedule entries - FIXED: Also filter by today in code as backup
    val todayEntries = scheduleWithMedicine.filter {
        it.scheduleEntry.scheduledDateTime.toLocalDate() == java.time.LocalDate.now()
    }

    // Check if all medications are taken
    val allTaken = todayEntries.all {
        it.scheduleEntry.status == MedicationStatus.TAKEN_ON_TIME ||
                it.scheduleEntry.status == MedicationStatus.TAKEN_LATE
    }

    // Check if there are any medications
    val hasMedications = todayEntries.isNotEmpty()

    // Group entries by time
    val timeFormat = currentUser?.timeFormat ?: TimeFormat.HOUR_12
    val groupedEntries = todayEntries.groupBy { entry ->
        TimeFormatUtils.formatTime(entry.scheduleEntry.scheduledDateTime, timeFormat)
    }.toList().sortedBy { (time, _) ->
        TimeFormatUtils.parseTime(time, timeFormat)?.time ?: 0L
    }

    Surface(
        modifier = Modifier.fillMaxSize(),

    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF3F4F6))
        ) {
            TopBar(
                navController = navController,
                currentUser = currentUser,
                title = "Today's Meds",
                subtitle = "($dayOfWeek)",
                actions = {}
            )

            // Main content with scrolling
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (hasMedications) {
                    if (allTaken) {
                        AllGoodState()
                    } else {
                        groupedEntries.forEach { (time, entries) ->
                            TimeSection(
                                time = time,
                                entries = entries,
                                currentDateTime = currentTime,
                                currentUser = currentUser,
                                onMarkAsTaken = { entryId ->
                                    val takenAt = java.time.LocalDateTime.now().toString()
                                    scheduleViewModel.markAsTaken(entryId, takenAt)
                                },
                                onMarkAsLate = { entryId ->
                                    val takenAt = java.time.LocalDateTime.now().toString()
                                    scheduleViewModel.markAsLate(entryId, takenAt)
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                } else {
                    NoMedicationsState()
                }
            }
        }
    }
}

@Composable
private fun TimeSection(
    time: String,
    entries: List<ScheduleWithMedicine>,
    currentDateTime: java.time.LocalDateTime,
    currentUser: com.example.dosezy.data.model.User?,
    onMarkAsTaken: (String) -> Unit,
    onMarkAsLate: (String) -> Unit
) {
    Column {
        // Time header
        Text(
            text = time,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // Calculate time difference only for pending/late entries (not taken ones)
        val pendingEntries = entries.filter {
            it.scheduleEntry.status == MedicationStatus.PENDING ||
                    it.scheduleEntry.status == MedicationStatus.MISSED
        }

        val firstPendingEntry = pendingEntries.firstOrNull()
        val timeDiff = firstPendingEntry?.let { entry ->
            TimeCalculationUtils.calculateTimeDifference(
                entry.scheduleEntry.scheduledDateTime,
                currentDateTime
            )
        }

        // Next dose indicator - Show time difference only for pending entries
        val statusText = when {
            // If all entries in this time slot are taken
            entries.all { it.scheduleEntry.status == MedicationStatus.TAKEN_ON_TIME ||
                    it.scheduleEntry.status == MedicationStatus.TAKEN_LATE } -> {
                "All medications taken"
            }
            // If there are missed medications
            entries.any { it.scheduleEntry.status == MedicationStatus.MISSED } -> {
                "Missed medications"
            }
            // Show time difference for pending/late entries
            timeDiff != null -> TimeCalculationUtils.formatTimeDifference(timeDiff)
            // Default case
            else -> "Scheduled for this time"
        }

        val statusColor = when {
            entries.any { it.scheduleEntry.status == MedicationStatus.MISSED } ->
                MaterialTheme.colorScheme.error
            timeDiff?.isLate == true ->
                MaterialTheme.colorScheme.error
            entries.all { it.scheduleEntry.status == MedicationStatus.TAKEN_ON_TIME ||
                    it.scheduleEntry.status == MedicationStatus.TAKEN_LATE } ->
                MaterialTheme.colorScheme.primary
            else ->
                MaterialTheme.colorScheme.onSurfaceVariant
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = statusColor,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Medication cards for this time
        entries.forEach { entry ->
            MedicationCard(
                scheduleWithMedicine = entry,
                currentDateTime = currentDateTime,
                currentUser = currentUser,
                onMarkAsTaken = onMarkAsTaken,
                onMarkAsLate = onMarkAsLate
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MedicationCard(
    scheduleWithMedicine: ScheduleWithMedicine,
    currentDateTime: java.time.LocalDateTime,
    currentUser: com.example.dosezy.data.model.User?,
    onMarkAsTaken: (String) -> Unit,
    onMarkAsLate: (String) -> Unit
) {
    val entry = scheduleWithMedicine.scheduleEntry
    val medicine = scheduleWithMedicine.medicine
    val isTaken = entry.status == MedicationStatus.TAKEN_ON_TIME ||
            entry.status == MedicationStatus.TAKEN_LATE
    val isMissed = entry.status == MedicationStatus.MISSED

    // Calculate if it's currently late (only for pending medications)
    val isLate = !isTaken && !isMissed && currentDateTime.isAfter(entry.scheduledDateTime)

    // Calculate if it should be marked as missed (only for pending medications)
    val shouldBeMissed = !isTaken && currentUser?.let { user ->
        TimeCalculationUtils.isMissed(
            entry.scheduledDateTime,
            currentDateTime,
            user.considerMissedAfter
        )
    } ?: false

    // Determine button properties
    val buttonText = when {
        isTaken -> "Taken"
        isMissed -> "Missed"
        isLate -> "Mark Late"
        else -> "Take"
    }

    val buttonColor = when {
        isTaken -> MaterialTheme.colorScheme.surfaceVariant
        isMissed -> MaterialTheme.colorScheme.error
        isLate -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val textColor = when {
        isTaken -> MaterialTheme.colorScheme.onSurfaceVariant
        isMissed -> MaterialTheme.colorScheme.onError
        isLate -> MaterialTheme.colorScheme.onTertiary
        else -> MaterialTheme.colorScheme.onPrimary
    }

    val enabled = !isTaken && !isMissed

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Medicine icon with image support
            MedicineImage(
                imageUri = medicine?.imageUri,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Medication info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = medicine?.medicationName ?: "Unknown Medicine",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = medicine?.getDosageDisplay() ?: "Unknown dosage",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Show individual medication status
                if (isTaken) {
                    Text(
                        text = "Taken",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (isMissed) {
                    Text(
                        text = "Missed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (isLate) {
                    val timeDiff = TimeCalculationUtils.calculateTimeDifference(
                        entry.scheduledDateTime,
                        currentDateTime
                    )
                    Text(
                        text = "${timeDiff.hours}h ${timeDiff.minutes}m late",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Action button
            Button(
                onClick = {
                    when {
                        isLate -> onMarkAsLate(entry.entryId)
                        !isTaken && !isMissed -> onMarkAsTaken(entry.entryId)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = textColor
                ),
                enabled = enabled,
                modifier = Modifier
                    .height(48.dp)
                    .width(120.dp)
            ) {
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun NoMedicationsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(Color(0xffe6e6e6)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Medication,
                contentDescription = "No medications",
                tint = Color(0xFF1E293B),
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "No medications yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Description
        Text(
            text = "It looks like you haven't added any medications to your schedule. Get started by tapping the plus button below.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF1E293B),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun AllGoodState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success icon
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "All good",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "All Good!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Description
        Text(
            text = "No medications need to be taken today. If you need to add more, get started by tapping the plus button below.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    DosezyTheme {
        HomeScreen(navController = androidx.navigation.compose.rememberNavController())
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeScreenDarkPreview() {
    DosezyTheme {
        HomeScreen(navController = androidx.navigation.compose.rememberNavController())
    }
}