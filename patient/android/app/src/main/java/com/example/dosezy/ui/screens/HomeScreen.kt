/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.screens

import androidx.compose.ui.graphics.luminance

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import com.example.dosezy.ui.components.SkipReasonDialog
import com.example.dosezy.ui.components.RecordDoseTimeDialog
import com.example.dosezy.ui.components.UndoConfirmationDialog
import com.example.dosezy.ui.components.DoseNoteDialog
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Medication
import com.example.dosezy.data.model.Medicine
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
import com.example.dosezy.ui.viewmodels.MedicineViewModel
import com.example.dosezy.ui.viewmodels.UserViewModel
import com.example.dosezy.utils.DateUtils
import com.example.dosezy.utils.TimeCalculationUtils
import com.example.dosezy.utils.TimeFormatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    navController: NavController,
    shouldRefresh: Boolean = false
) {
    val userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
    val scheduleViewModel: ScheduleViewModel = com.example.dosezy.utils.sharedScheduleViewModel()
    val medicineViewModel: MedicineViewModel = com.example.dosezy.utils.sharedMedicineViewModel()

    val userMedicines by medicineViewModel.medicines.collectAsState()

    val currentUser by userViewModel.currentUser.collectAsState()
    val todayEntries by scheduleViewModel.todayScheduleWithMedicine.collectAsState()
    val isTodayLoading by scheduleViewModel.isTodayLoading.collectAsState()

    // Get current day for subtitle
    val dayOfWeek = DateUtils.getCurrentDayOfWeekLegacy()

    // State for real-time updates
    var currentTime by remember { mutableStateOf(java.time.LocalDateTime.now()) }
    var isPrnExpanded by remember { mutableStateOf(false) }
    var medToLogConfirm by remember { mutableStateOf<Medicine?>(null) }
    var skippingEntryId by remember { mutableStateOf<String?>(null) }
    var manualRecordEntry by remember { mutableStateOf<ScheduleWithMedicine?>(null) }
    var undoConfirmTarget by remember { mutableStateOf<ScheduleWithMedicine?>(null) }
    var notePromptEntry by remember { mutableStateOf<ScheduleWithMedicine?>(null) }
    var noteEditEntry by remember { mutableStateOf<ScheduleWithMedicine?>(null) }
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

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


    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            scheduleViewModel.refreshAfterMedicineAdded()
        }
    }

    // Check if all medications are completed (taken or skipped) and at least one was actually taken
    val allCompleted = todayEntries.isNotEmpty() && todayEntries.all {
        it.scheduleEntry.status == MedicationStatus.TAKEN_ON_TIME ||
                it.scheduleEntry.status == MedicationStatus.TAKEN_LATE ||
                it.scheduleEntry.status == MedicationStatus.SKIPPED
    }
    val hasTaken = todayEntries.any {
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

    val executeTakeDose: (String, java.time.LocalDateTime, String?) -> Unit = { entryId, resolvedDateTime, note ->
        val medName = todayEntries.find { it.scheduleEntry.entryId == entryId }?.medicine?.medicationName ?: ""
        val lateAfter = currentUser?.considerLateAfter ?: 3
        val missedAfter = currentUser?.considerMissedAfter ?: 6
        val targetScheduleEntry = todayEntries.find { it.scheduleEntry.entryId == entryId }?.scheduleEntry
        val isLate = if (targetScheduleEntry != null) {
            TimeCalculationUtils.isLate(targetScheduleEntry.scheduledDateTime, resolvedDateTime, lateAfter, missedAfter)
        } else false

        val takenAt = resolvedDateTime.toString()
        if (isLate) {
            scheduleViewModel.markAsLate(entryId, takenAt, note)
        } else {
            scheduleViewModel.markAsTaken(entryId, takenAt, note)
        }

        coroutineScope.launch {
            val message = if (medName.isNotBlank()) {
                context.getString(R.string.home_dose_recorded, medName)
            } else {
                context.getString(R.string.home_action_taken)
            }
            val allowUndo = currentUser?.allowDoseUndo == true
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (allowUndo) context.getString(R.string.btn_undo) else null,
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed && allowUndo) {
                undoConfirmTarget = todayEntries.find { it.scheduleEntry.entryId == entryId }
            }
        }
    }

    androidx.compose.material3.Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { com.example.dosezy.ui.components.DosezySnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TopBar(
                navController = navController,
                currentUser = currentUser,
                title = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.home_todays_meds),
                subtitle = "($dayOfWeek)",
                actions = {}
            )

            if (isTodayLoading && !hasMedications) {
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
                        if (allCompleted && hasTaken) {
                            AllGoodBanner()
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        groupedEntries.forEach { (time, entries) ->
                            TimeSection(
                                time = time,
                                entries = entries,
                                currentDateTime = currentTime,
                                currentUser = currentUser,
                                onMarkAsTaken = { entryId ->
                                    val target = todayEntries.find { it.scheduleEntry.entryId == entryId }
                                    if (currentUser?.allowCustomDoseTime == true) {
                                        if (target != null) {
                                            manualRecordEntry = target
                                        } else {
                                            executeTakeDose(entryId, java.time.LocalDateTime.now(), null)
                                        }
                                    } else if (currentUser?.allowDoseNotes == true && currentUser?.promptDoseNotes == true) {
                                        if (target != null) {
                                            notePromptEntry = target
                                        } else {
                                            executeTakeDose(entryId, java.time.LocalDateTime.now(), null)
                                        }
                                    } else {
                                        executeTakeDose(entryId, java.time.LocalDateTime.now(), null)
                                    }
                                },
                                onMarkAsLate = { entryId ->
                                    val target = todayEntries.find { it.scheduleEntry.entryId == entryId }
                                    if (currentUser?.allowCustomDoseTime == true) {
                                        if (target != null) {
                                            manualRecordEntry = target
                                        } else {
                                            executeTakeDose(entryId, java.time.LocalDateTime.now(), null)
                                        }
                                    } else if (currentUser?.allowDoseNotes == true && currentUser?.promptDoseNotes == true) {
                                        if (target != null) {
                                            notePromptEntry = target
                                        } else {
                                            executeTakeDose(entryId, java.time.LocalDateTime.now(), null)
                                        }
                                    } else {
                                        executeTakeDose(entryId, java.time.LocalDateTime.now(), null)
                                    }
                                },
                                onUndo = { entryId ->
                                    val target = todayEntries.find { it.scheduleEntry.entryId == entryId }
                                    if (currentUser?.allowDoseUndo == true && target != null) {
                                        undoConfirmTarget = target
                                    }
                                },
                                onEditNote = { entryId ->
                                    val target = todayEntries.find { it.scheduleEntry.entryId == entryId }
                                    if (target != null) {
                                        noteEditEntry = target
                                    }
                                },
                                onSkip = { entryId ->
                                    skippingEntryId = entryId
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    } else if (userMedicines.isEmpty()) {
                        NoMedicationsState(onAddMedicineClick = { navController.navigate("add_med") })
                    } else {
                        // User has medications, but none scheduled for today (e.g. PRN or non-today)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = androidx.compose.ui.res.stringResource(R.string.no_scheduled_doses_today),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // --- v2.4.0 As-Needed (PRN) Medications Section ---
                    val prnMedicines = userMedicines.filter { it.frequency.pattern == com.example.dosezy.data.model.FrequencyPattern.AS_NEEDED }
                    if (prnMedicines.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isPrnExpanded = !isPrnExpanded },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Medication,
                                        contentDescription = null,
                                        tint = Color(0xFF1193D4),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "${androidx.compose.ui.res.stringResource(R.string.prn_section_title)} (${prnMedicines.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = if (isPrnExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isPrnExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            prnMedicines.forEach { prnMed ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowElevation = 3.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            MedicineImage(
                                                imageUri = prnMed.imageUri,
                                                pillShape = prnMed.pillShape,
                                                pillColor = prnMed.pillColor,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = prnMed.medicationName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = prnMed.getDosageDisplay(),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (!prnMed.notes.isNullOrBlank()) {
                                                    Text(
                                                        text = "📝 ${prnMed.notes}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFFF59E0B),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                medToLogConfirm = prnMed
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF1193D4),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = androidx.compose.ui.res.stringResource(R.string.btn_log_prn_dose),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (medToLogConfirm != null) {
        val med = medToLogConfirm!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { medToLogConfirm = null },
            title = {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.prn_confirm_log_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        R.string.prn_confirm_log_msg,
                        med.medicationName,
                        med.getDosageDisplay()
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val medName = med.medicationName
                        scheduleViewModel.logAsNeededDose(med)
                        medToLogConfirm = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.prn_logged_success, medName),
                                duration = androidx.compose.material3.SnackbarDuration.Short
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1193D4))
                ) {
                    Text(androidx.compose.ui.res.stringResource(R.string.btn_log_prn_dose))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { medToLogConfirm = null }
                ) {
                    Text(androidx.compose.ui.res.stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (skippingEntryId != null) {
        SkipReasonDialog(
            onConfirm = { reason ->
                val targetId = skippingEntryId ?: return@SkipReasonDialog
                val medName = todayEntries.find { it.scheduleEntry.entryId == targetId }?.medicine?.medicationName ?: ""
                scheduleViewModel.markAsSkipped(targetId, reason)
                skippingEntryId = null
                val allowUndo = currentUser?.allowDoseUndo == true
                coroutineScope.launch {
                    val message = if (medName.isNotBlank()) {
                        context.getString(R.string.home_dose_skipped, medName)
                    } else {
                        context.getString(R.string.home_action_skipped)
                    }
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = if (allowUndo) context.getString(R.string.btn_undo) else null,
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed && allowUndo) {
                        undoConfirmTarget = todayEntries.find { it.scheduleEntry.entryId == targetId }
                    }
                }
            },
            onDismiss = {
                skippingEntryId = null
            }
        )
    }

    if (manualRecordEntry != null) {
        val target = manualRecordEntry!!
        RecordDoseTimeDialog(
            entry = target.scheduleEntry,
            medicineName = target.medicine?.medicationName ?: "",
            timeFormat = currentUser?.timeFormat ?: TimeFormat.HOUR_12,
            allowDoseNotes = currentUser?.allowDoseNotes == true,
            onDismiss = { manualRecordEntry = null },
            onConfirm = { resolvedDateTime, note ->
                val targetId = target.scheduleEntry.entryId
                manualRecordEntry = null
                executeTakeDose(targetId, resolvedDateTime, note)
            }
        )
    }

    if (undoConfirmTarget != null) {
        val target = undoConfirmTarget!!
        val medName = target.medicine?.medicationName ?: ""
        val entryId = target.scheduleEntry.entryId
        UndoConfirmationDialog(
            medicationName = medName,
            onConfirm = {
                undoConfirmTarget = null
                scheduleViewModel.undoDoseTaken(entryId)
                coroutineScope.launch {
                    val message = if (medName.isNotBlank()) {
                        context.getString(R.string.home_dose_reverted, medName)
                    } else {
                        context.getString(R.string.home_action_taken)
                    }
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                }
            },
            onDismiss = {
                undoConfirmTarget = null
            }
        )
    }

    if (notePromptEntry != null) {
        val target = notePromptEntry!!
        val medName = target.medicine?.medicationName ?: ""
        val entryId = target.scheduleEntry.entryId
        DoseNoteDialog(
            medicationName = medName,
            isEditing = false,
            onConfirm = { note ->
                notePromptEntry = null
                executeTakeDose(entryId, java.time.LocalDateTime.now(), note.takeIf { it.isNotBlank() })
            },
            onSkip = {
                notePromptEntry = null
                executeTakeDose(entryId, java.time.LocalDateTime.now(), null)
            },
            onDismiss = {
                notePromptEntry = null
            }
        )
    }

    if (noteEditEntry != null) {
        val target = noteEditEntry!!
        val medName = target.medicine?.medicationName ?: ""
        val entryId = target.scheduleEntry.entryId
        DoseNoteDialog(
            medicationName = medName,
            initialNote = target.scheduleEntry.doseNotes ?: "",
            isEditing = true,
            onConfirm = { newNote ->
                noteEditEntry = null
                scheduleViewModel.updateDoseNotes(entryId, newNote.takeIf { it.isNotBlank() })
            },
            onDismiss = {
                noteEditEntry = null
            }
        )
    }
}

@Composable
private fun TimeSection(
    time: String,
    entries: List<ScheduleWithMedicine>,
    currentDateTime: java.time.LocalDateTime,
    currentUser: com.example.dosezy.data.model.User?,
    onMarkAsTaken: (String) -> Unit,
    onMarkAsLate: (String) -> Unit,
    onUndo: (String) -> Unit = {},
    onSkip: (String) -> Unit = {},
    onEditNote: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Time Header
        Text(
            text = time,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

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

        val allTaken = entries.all {
            it.scheduleEntry.status == MedicationStatus.TAKEN_ON_TIME ||
                    it.scheduleEntry.status == MedicationStatus.TAKEN_LATE ||
                    it.scheduleEntry.status == MedicationStatus.SKIPPED
        }

        val statusText = when {
            allTaken -> {
                androidx.compose.ui.res.stringResource(R.string.status_all_medications_taken)
            }
            timeDiff != null -> {
                if (timeDiff.isLate) {
                    androidx.compose.ui.res.stringResource(R.string.time_diff_ago, timeDiff.hours, timeDiff.minutes)
                } else {
                    androidx.compose.ui.res.stringResource(R.string.to_be_taken_in, timeDiff.hours, timeDiff.minutes)
                }
            }
            else -> androidx.compose.ui.res.stringResource(R.string.status_scheduled_for_this_time)
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
                onMarkAsLate = onMarkAsLate,
                onUndo = onUndo,
                onSkip = onSkip,
                onEditNote = onEditNote
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
    onMarkAsLate: (String) -> Unit,
    onUndo: (String) -> Unit = {},
    onSkip: (String) -> Unit = {},
    onEditNote: (String) -> Unit = {}
) {
    val entry = scheduleWithMedicine.scheduleEntry
    val medicine = scheduleWithMedicine.medicine
    val isTaken = entry.status == MedicationStatus.TAKEN_ON_TIME ||
            entry.status == MedicationStatus.TAKEN_LATE
    val isSkipped = entry.status == MedicationStatus.SKIPPED

    val lateAfter = currentUser?.considerLateAfter ?: 3
    val missedAfter = currentUser?.considerMissedAfter ?: 6

    val isPassed = currentDateTime.isAfter(entry.scheduledDateTime)

    // Calculate isMissed dynamically if it passed the threshold, in addition to DB status
    val isMissed = entry.status == MedicationStatus.MISSED || (!isTaken && !isSkipped && isPassed && TimeCalculationUtils.isMissed(
        entry.scheduledDateTime,
        currentDateTime,
        missedAfter
    ))

    // Calculate if it's currently late (only for pending medications)
    val isLate = !isTaken && !isSkipped && !isMissed && isPassed && TimeCalculationUtils.isLate(
        entry.scheduledDateTime,
        currentDateTime,
        lateAfter,
        missedAfter
    )

    val orangeColor = Color(0xFFF97316)

    // Determine button properties
    val buttonText = when {
        isTaken -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.home_action_taken)
        isSkipped -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.home_action_skipped)
        isMissed -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.home_action_missed)
        isLate -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.home_action_mark_late)
        else -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.home_action_take)
    }

    val buttonColor = when {
        isTaken || isSkipped -> MaterialTheme.colorScheme.surfaceVariant
        isMissed -> MaterialTheme.colorScheme.error
        isLate -> orangeColor
        else -> MaterialTheme.colorScheme.primary
    }

    val textColor = when {
        isTaken || isSkipped -> MaterialTheme.colorScheme.onSurfaceVariant
        isMissed -> MaterialTheme.colorScheme.onError
        isLate -> Color.White
        else -> MaterialTheme.colorScheme.onPrimary
    }

    val enabled = when {
        isTaken || isSkipped -> currentUser?.allowDoseUndo == true
        isMissed -> currentUser?.allowCustomDoseTime == true
        else -> true
    }

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
            // Medicine icon with pill visual support
            MedicineImage(
                imageUri = medicine?.imageUri,
                pillShape = medicine?.pillShape,
                pillColor = medicine?.pillColor,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Medication info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 1. Name
                Text(
                    text = medicine?.medicationName ?: "Unknown Medicine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 2. Qty / Dosage
                Text(
                    text = medicine?.getDosageDisplay(entry.scheduledDateTime.toLocalTime()) ?: "Unknown dosage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 3. Note
                if (!medicine?.notes.isNullOrBlank()) {
                    Text(
                        text = "📝 ${medicine?.notes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF59E0B),
                        maxLines = 2
                    )
                }

                // 3b. Skip reason if skipped
                if (isSkipped && !entry.skipReason.isNullOrBlank()) {
                    Text(
                        text = "⏭️ ${androidx.compose.ui.res.stringResource(R.string.home_action_skipped)}: ${entry.skipReason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                // 3c. Dose note if taken
                if (isTaken) {
                    if (!entry.doseNotes.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .then(
                                    if (currentUser?.allowDoseNotes == true) {
                                        Modifier.clickable { onEditNote(entry.entryId) }
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "💬 " + androidx.compose.ui.res.stringResource(R.string.home_dose_note_label, entry.doseNotes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 2
                            )
                        }
                    } else if (currentUser?.allowDoseNotes == true) {
                        androidx.compose.material3.TextButton(
                            onClick = { onEditNote(entry.entryId) },
                            modifier = Modifier.height(24.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "💬 " + androidx.compose.ui.res.stringResource(R.string.btn_add_note),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // 4. Refill warning / stock badge
                medicine?.currentStock?.let { stock ->
                    val isLow = medicine.refillThreshold != null && stock <= medicine.refillThreshold
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val containerBg = if (isLow) {
                        if (isDark) Color(0xFF3F1313) else Color(0xFFFEE2E2)
                    } else {
                        if (isDark) Color(0xFF0F2D14) else Color(0xFFD1FAE5)
                    }
                    val contentColor = if (isLow) {
                        if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                    } else {
                        if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                    }
                    androidx.compose.material3.Surface(
                        color = containerBg,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = if (isLow) {
                                androidx.compose.ui.res.stringResource(R.string.med_stock_refill_warning_badge, stock)
                            } else {
                                androidx.compose.ui.res.stringResource(R.string.med_stock_badge, stock)
                            },
                            color = contentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Action button area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = {
                        when {
                            isTaken || isSkipped -> {
                                if (currentUser?.allowDoseUndo == true) {
                                    onUndo(entry.entryId)
                                }
                            }
                            isLate -> onMarkAsLate(entry.entryId)
                            else -> onMarkAsTaken(entry.entryId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = textColor,
                        disabledContainerColor = buttonColor.copy(alpha = 0.5f),
                        disabledContentColor = textColor.copy(alpha = 0.5f)
                    ),
                    enabled = enabled,
                    modifier = Modifier
                        .height(44.dp)
                        .width(120.dp)
                ) {
                    Text(
                        text = buttonText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                if (currentUser?.allowDoseSkipping == true && !isTaken && !isSkipped && !isMissed) {
                    androidx.compose.material3.TextButton(
                        onClick = { onSkip(entry.entryId) },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.home_action_skip),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoMedicationsState(onAddMedicineClick: () -> Unit = {}) {
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

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onAddMedicineClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1193D4),
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.form_add_medicine),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AllGoodBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.home_all_good),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.home_all_good_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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