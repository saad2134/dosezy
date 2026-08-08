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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.dosezy.R
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
    val userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
    val scheduleViewModel: ScheduleViewModel = com.example.dosezy.utils.sharedScheduleViewModel()

    val currentUser by userViewModel.currentUser.collectAsState()
    val scheduleWithMedicine by scheduleViewModel.scheduleWithMedicine.collectAsState()

    // Get current day for subtitle
    val dayOfWeek = DateUtils.getCurrentDayOfWeekLegacy()
    val today = java.time.LocalDate.now()
    val currentDateTime = java.time.LocalDateTime.now()

    // State for real-time updates
    var currentTime by remember { mutableStateOf(java.time.LocalDateTime.now()) }

    // Auto-refresh every minute for real-time updates and run immediately on load
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            scheduleViewModel.autoMarkMissedMedications(
                user.userId,
                user.considerMissedAfter
            )
        }
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
        // Refresh when we come back to home screen (eg from adding medicine)
        if (navBackStackEntry?.destination?.route == "home") {
            currentUser?.let { user ->
                scheduleViewModel.loadScheduleForDate(user.userId, java.time.LocalDate.now())
            }
        }
    }

    val isRefreshing by scheduleViewModel.isRefreshing.collectAsState()

    // Filter today's schedule entries
    val todayEntries = remember(scheduleWithMedicine) {
        scheduleWithMedicine.filter {
            it.scheduleEntry.scheduledDateTime.toLocalDate() == java.time.LocalDate.now()
        }
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
    val activeLocale = remember(currentUser?.language) {
        com.example.dosezy.utils.LocaleHelper.getLocale(currentUser?.language ?: com.example.dosezy.data.model.Language.SYSTEM)
    }
    val groupedEntries = remember(todayEntries, timeFormat, activeLocale) {
        todayEntries.groupBy { entry ->
            TimeFormatUtils.formatTime(entry.scheduleEntry.scheduledDateTime, timeFormat, activeLocale)
        }.toList().sortedBy { (time, _) ->
            TimeFormatUtils.parseTime(time, timeFormat)?.time ?: 0L
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),

    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            TopBar(
                navController = navController,
                currentUser = currentUser,
                title = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.home_todays_meds),
                subtitle = "($dayOfWeek)",
                actions = {}
            )

            if (isRefreshing && !hasMedications) {
                com.example.dosezy.ui.components.HomeSkeletonView()
            } else {
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
            entries.all { it.scheduleEntry.status == MedicationStatus.TAKEN_ON_TIME ||
                    it.scheduleEntry.status == MedicationStatus.TAKEN_LATE } -> {
                "All medications taken"
            }
            timeDiff != null -> TimeCalculationUtils.formatTimeDifference(timeDiff)
            else -> "Scheduled for this time"
        }

        // Always normal color below time header (not transparent, not red, not orange)
        val statusColor = MaterialTheme.colorScheme.onSurfaceVariant

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

    val lateAfter = currentUser?.considerLateAfter ?: 3
    val missedAfter = currentUser?.considerMissedAfter ?: 6

    val isPassed = currentDateTime.isAfter(entry.scheduledDateTime)

    // Calculate isMissed dynamically if it passed the threshold, in addition to DB status
    val isMissed = entry.status == MedicationStatus.MISSED || (!isTaken && isPassed && TimeCalculationUtils.isMissed(
        entry.scheduledDateTime,
        currentDateTime,
        missedAfter
    ))

    // Calculate if it's currently late (only for pending medications)
    val isLate = !isTaken && !isMissed && isPassed && TimeCalculationUtils.isLate(
        entry.scheduledDateTime,
        currentDateTime,
        lateAfter,
        missedAfter
    )

    val orangeColor = Color(0xFFF97316)

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
        isLate -> orangeColor
        else -> MaterialTheme.colorScheme.primary
    }

    val textColor = when {
        isTaken -> MaterialTheme.colorScheme.onSurfaceVariant
        isMissed -> MaterialTheme.colorScheme.onError
        isLate -> Color.White
        else -> MaterialTheme.colorScheme.onPrimary
    }

    val enabled = !isTaken && !isMissed

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Medicine icon
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

                // Show individual medication status under dosage
                if (isTaken) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.status_taken),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    val timeDiff = TimeCalculationUtils.calculateTimeDifference(
                        entry.scheduledDateTime,
                        currentDateTime
                    )
                    val agoText = "${timeDiff.hours}h ${timeDiff.minutes}m ago"

                    if (isMissed) {
                        Text(
                            text = agoText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error // Red if missed
                        )
                    } else if (isLate) {
                        Text(
                            text = agoText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = orangeColor // Orange if late
                        )
                    } else if (isPassed) {
                        Text(
                            text = agoText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant // Normal color if under late time
                        )
                    }
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
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Medication,
                contentDescription = "No medications",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = androidx.compose.ui.res.stringResource(R.string.medicines_empty),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Description
        Text(
            text = androidx.compose.ui.res.stringResource(R.string.medicines_empty_sub),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
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
            text = androidx.compose.ui.res.stringResource(R.string.home_all_good),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Description
        Text(
            text = androidx.compose.ui.res.stringResource(R.string.home_all_good_desc),
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