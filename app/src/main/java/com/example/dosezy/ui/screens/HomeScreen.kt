package com.example.dosezy.ui.screens

import android.os.Build
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
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.theme.DosezyTheme
import com.example.dosezy.ui.viewmodels.ScheduleViewModel
import com.example.dosezy.ui.viewmodels.UserViewModel
import com.example.dosezy.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(navController: NavController) {
    val userViewModel: UserViewModel = hiltViewModel()
    val scheduleViewModel: ScheduleViewModel = hiltViewModel()

    val currentUser by userViewModel.currentUser.collectAsState()
    val scheduleEntries by scheduleViewModel.scheduleEntries.collectAsState()

    // Get current day for subtitle using legacy method for compatibility
    val dayOfWeek = DateUtils.getCurrentDayOfWeekLegacy()

    // Load today's schedule on composition
    LaunchedEffect(Unit) {
        scheduleViewModel.setSelectedDate(java.time.LocalDate.now())
    }

    // Filter today's schedule entries
    val todayEntries = scheduleEntries

    // Check if all medications are taken
    val allTaken = todayEntries.all {
        it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE
    }

    // Check if there are any medications
    val hasMedications = todayEntries.isNotEmpty()

    // Group entries by time
    val groupedEntries = todayEntries.groupBy { entry ->
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(
            Date.from(entry.scheduledDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant())
        )
    }.toList().sortedBy { (time, _) ->
        SimpleDateFormat("h:mm a", Locale.getDefault()).parse(time)?.time ?: 0L
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
                        // All medications taken state
                        AllGoodState()
                    } else {
                        // Show medications grouped by time
                        groupedEntries.forEach { (time, entries) ->
                            TimeSection(
                                time = time,
                                entries = entries,
                                onMarkAsTaken = { entryId ->
                                    val takenAt = java.time.LocalDateTime.now().toString()
                                    scheduleViewModel.markAsTaken(entryId, takenAt)
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                } else {
                    // No medications state
                    NoMedicationsState()
                }
            }
        }
    }
}

@Composable
private fun TimeSection(
    time: String,
    entries: List<ScheduleEntry>,
    onMarkAsTaken: (String) -> Unit
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

        // Next dose indicator
        Text(
            text = "Next dose in 30 minutes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Medication cards for this time
        entries.forEach { entry ->
            MedicationCard(
                entry = entry,
                onMarkAsTaken = onMarkAsTaken
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MedicationCard(
    entry: ScheduleEntry,
    onMarkAsTaken: (String) -> Unit
) {
    val isTaken = entry.status == MedicationStatus.TAKEN_ON_TIME ||
            entry.status == MedicationStatus.TAKEN_LATE

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
            // Medication icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = "Medication",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Medication info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.medicineId,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "100mg", // Default dosage - in real app, get from medicine data
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Action button
            Button(
                onClick = {
                    if (!isTaken) {
                        onMarkAsTaken(entry.entryId)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTaken) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (isTaken) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                ),
                enabled = !isTaken,
                modifier = Modifier
                    .height(48.dp)
                    .width(100.dp)
            ) {
                Text(
                    text = if (isTaken) "Taken" else "Take",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
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
        // Empty state icon
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
            text = "No medications yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Description
        Text(
            text = "It looks like you haven't added any medications to your schedule. Get started by tapping the plus button below.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                .background(Color(0xFFC6F6D5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "All good",
                tint = Color(0xFF059669),
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "All Good!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF065F46),
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
@Preview
@Composable
fun HomeScreenPreview() {
    DosezyTheme {
        HomeScreen(navController = androidx.navigation.compose.rememberNavController())
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun HomeScreenWithMedicationsPreview() {
    DosezyTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                TimeSection(
                    time = "8:00 AM",
                    entries = listOf(
                        ScheduleEntry(
                            entryId = "1",
                            userId = "user1",
                            medicineId = "Aspirin",
                            scheduledDateTime = java.time.LocalDateTime.now(),
                            status = MedicationStatus.PENDING
                        ),
                        ScheduleEntry(
                            entryId = "2",
                            userId = "user1",
                            medicineId = "Lisinopril",
                            scheduledDateTime = java.time.LocalDateTime.now(),
                            status = MedicationStatus.TAKEN_ON_TIME
                        )
                    ),
                    onMarkAsTaken = {}
                )
            }
        }
    }
}