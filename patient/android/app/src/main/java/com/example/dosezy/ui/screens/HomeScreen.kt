/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dosezy.R
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleWithMedicine
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.notifications.IntakeSafeguardManager
import com.example.dosezy.ui.components.DoseNoteDialog
import com.example.dosezy.ui.components.DosezySnackbarHost
import com.example.dosezy.ui.components.HomeSkeletonView
import com.example.dosezy.ui.components.IntakeSafeguardBanner
import com.example.dosezy.ui.components.RecordDoseTimeDialog
import com.example.dosezy.ui.components.SkipReasonDialog
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.components.UndoConfirmationDialog
import com.example.dosezy.ui.screens.home.AllGoodBanner
import com.example.dosezy.ui.screens.home.HomeTimeSection
import com.example.dosezy.ui.screens.home.HomeTimelineView
import com.example.dosezy.ui.screens.home.NoMedicationsState
import com.example.dosezy.ui.screens.home.NoScheduledDosesToday
import com.example.dosezy.ui.screens.home.homePrnSection
import com.example.dosezy.ui.theme.DosezyTheme
import com.example.dosezy.ui.viewmodels.MedicineViewModel
import com.example.dosezy.ui.viewmodels.ScheduleViewModel
import com.example.dosezy.ui.viewmodels.UserViewModel
import com.example.dosezy.utils.DateUtils
import com.example.dosezy.utils.LocaleHelper
import com.example.dosezy.utils.TimeCalculationUtils
import com.example.dosezy.utils.TimeFormatUtils
import com.example.dosezy.utils.sharedMedicineViewModel
import com.example.dosezy.utils.sharedScheduleViewModel
import com.example.dosezy.utils.sharedUserViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    navController: NavController,
    shouldRefresh: Boolean = false
) {
    val userViewModel: UserViewModel = sharedUserViewModel()
    val scheduleViewModel: ScheduleViewModel = sharedScheduleViewModel()
    val medicineViewModel: MedicineViewModel = sharedMedicineViewModel()

    val userMedicines by medicineViewModel.medicines.collectAsState()
    val currentUser by userViewModel.currentUser.collectAsState()
    val todayEntries by scheduleViewModel.todayScheduleWithMedicine.collectAsState()
    val isTodayLoading by scheduleViewModel.isTodayLoading.collectAsState()

    val context = LocalContext.current
    val database = remember { DosezyDatabase.getInstance(context) }
    val activeIntake by IntakeSafeguardManager.activeCountdown.collectAsState()
    val currentLang = currentUser?.language ?: LocaleHelper.getSavedLanguage(context)
    val activeLocale = remember(currentLang) { LocaleHelper.getLocale(currentLang) }
    // Get current day for subtitle
    val dayOfWeek = remember(activeLocale) { DateUtils.getCurrentDayOfWeekLegacy(activeLocale) }

    // State for real-time updates
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    var isPrnExpanded by remember { mutableStateOf(false) }
    var medToLogConfirm by remember { mutableStateOf<Medicine?>(null) }
    var skippingEntryId by remember { mutableStateOf<String?>(null) }
    var manualRecordEntry by remember { mutableStateOf<ScheduleWithMedicine?>(null) }
    var undoConfirmTarget by remember { mutableStateOf<ScheduleWithMedicine?>(null) }
    var notePromptEntry by remember { mutableStateOf<ScheduleWithMedicine?>(null) }
    var noteEditEntry by remember { mutableStateOf<ScheduleWithMedicine?>(null) }
    var markAllConfirmSlot by remember { mutableStateOf<Pair<String, List<ScheduleWithMedicine>>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Auto-refresh every minute for real-time updates and run immediately on load
    LaunchedEffect(currentUser) {
        // Guard: Reconnect today's schedule and calendar date on midnight transition to avoid displaying yesterday's doses
        scheduleViewModel.checkMidnightDateTransition()
        currentUser?.let { user ->
            scheduleViewModel.autoMarkMissedMedications(
                user.userId,
                user.considerMissedAfter
            )
        }
        while (true) {
            delay(60000) // Update every minute
            currentTime = LocalDateTime.now()

            // Guard: Reconnect today's schedule and calendar date on midnight transition to avoid displaying yesterday's doses
            scheduleViewModel.checkMidnightDateTransition()

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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        val msg = navBackStackEntry?.savedStateHandle?.remove<String>("snackbar_message")
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
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
    val groupedEntries = remember(todayEntries, timeFormat, activeLocale) {
        todayEntries.groupBy { entry ->
            TimeFormatUtils.formatTime(entry.scheduleEntry.scheduledDateTime, timeFormat, activeLocale)
        }.toList().sortedBy { (_, entries) ->
            entries.firstOrNull()?.scheduleEntry?.scheduledDateTime
        }
    }

    val executeTakeDose: (String, LocalDateTime, String?) -> Unit = { entryId, resolvedDateTime, note ->
        val medName = todayEntries.find { it.scheduleEntry.entryId == entryId }?.medicine?.medicationName ?: ""
        val lateAfter = currentUser?.considerLateAfter ?: 3
        val targetScheduleEntry = todayEntries.find { it.scheduleEntry.entryId == entryId }?.scheduleEntry
        // Guard: Use isTakenLate so overdue doses past missedAfter are correctly recorded as TAKEN_LATE instead of reverting to on-time
        val isLate = if (targetScheduleEntry != null) {
            TimeCalculationUtils.isTakenLate(targetScheduleEntry.scheduledDateTime, resolvedDateTime, lateAfter)
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
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed && allowUndo) {
                undoConfirmTarget = todayEntries.find { it.scheduleEntry.entryId == entryId }
            }
        }
    }

    val handleMarkAsTaken: (String) -> Unit = { entryId ->
        val target = todayEntries.find { it.scheduleEntry.entryId == entryId }
        if (currentUser?.allowCustomDoseTime == true) {
            if (target != null) {
                manualRecordEntry = target
            } else {
                executeTakeDose(entryId, LocalDateTime.now(), null)
            }
        } else if (currentUser?.allowDoseNotes == true && currentUser?.promptDoseNotes == true) {
            if (target != null) {
                notePromptEntry = target
            } else {
                executeTakeDose(entryId, LocalDateTime.now(), null)
            }
        } else if (currentUser?.intakeSafeguardEnabled == true) {
            // Guard: Start intake countdown timer with re-alert safeguard for ADHD support (#104)
            val medName = target?.medicine?.medicationName ?: context.getString(R.string.form_med_name)
            val is24 = currentUser?.timeFormat == com.example.dosezy.data.model.TimeFormat.HOUR_24 ||
                android.text.format.DateFormat.is24HourFormat(context)
            val timePattern = if (is24) "HH:mm" else "h:mm a"
            val scheduledTimeStr = target?.scheduleEntry?.scheduledDateTime?.format(
                java.time.format.DateTimeFormatter.ofPattern(timePattern, activeLocale)
            ) ?: ""
            IntakeSafeguardManager.startCountdown(
                context = context,
                entryId = entryId,
                entryIds = listOf(entryId),
                medicineName = medName,
                medicineNames = listOf(medName),
                scheduledTime = scheduledTimeStr,
                durationSeconds = currentUser?.intakeCountdownSeconds ?: 120
            )
        } else {
            executeTakeDose(entryId, LocalDateTime.now(), null)
        }
    }

    val handleMarkAsLate: (String) -> Unit = { entryId ->
        val target = todayEntries.find { it.scheduleEntry.entryId == entryId }
        if (currentUser?.allowCustomDoseTime == true) {
            if (target != null) {
                manualRecordEntry = target
            } else {
                executeTakeDose(entryId, LocalDateTime.now(), null)
            }
        } else if (currentUser?.allowDoseNotes == true && currentUser?.promptDoseNotes == true) {
            if (target != null) {
                notePromptEntry = target
            } else {
                executeTakeDose(entryId, LocalDateTime.now(), null)
            }
        } else if (currentUser?.intakeSafeguardEnabled == true) {
            // Guard: Start intake countdown timer with re-alert safeguard for ADHD support (#104)
            val medName = target?.medicine?.medicationName ?: context.getString(R.string.form_med_name)
            val is24 = currentUser?.timeFormat == com.example.dosezy.data.model.TimeFormat.HOUR_24 ||
                android.text.format.DateFormat.is24HourFormat(context)
            val timePattern = if (is24) "HH:mm" else "h:mm a"
            val scheduledTimeStr = target?.scheduleEntry?.scheduledDateTime?.format(
                java.time.format.DateTimeFormatter.ofPattern(timePattern, activeLocale)
            ) ?: ""
            IntakeSafeguardManager.startCountdown(
                context = context,
                entryId = entryId,
                entryIds = listOf(entryId),
                medicineName = medName,
                medicineNames = listOf(medName),
                scheduledTime = scheduledTimeStr,
                durationSeconds = currentUser?.intakeCountdownSeconds ?: 120
            )
        } else {
            executeTakeDose(entryId, LocalDateTime.now(), null)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { DosezySnackbarHost(snackbarHostState) }
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
                title = stringResource(R.string.home_todays_meds),
                subtitle = "($dayOfWeek)",
                actions = {
                    if (hasMedications) {
                        val isTimeline = currentUser?.timelineModeEnabled == true
                        IconButton(
                            onClick = {
                                currentUser?.let { user ->
                                    userViewModel.updateUser(user.copy(timelineModeEnabled = !isTimeline))
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isTimeline) Icons.Default.ViewAgenda else Icons.Default.ViewTimeline,
                                contentDescription = if (isTimeline) stringResource(R.string.home_view_standard) else stringResource(R.string.home_view_timeline),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )

            if (isTodayLoading && !hasMedications) {
                HomeSkeletonView()
            } else {
                // Main content with virtualized scrolling
                // Guard: Virtualize home content with LazyColumn to eliminate massive initial layout measurement overhead on older devices
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    if (hasMedications) {
                        if (allCompleted && hasTaken) {
                            item(key = "all_good_banner") {
                                AllGoodBanner()
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        if (currentUser?.timelineModeEnabled == true) {
                            item(key = "timeline_view") {
                                HomeTimelineView(
                                    groupedEntries = groupedEntries,
                                    currentDateTime = currentTime,
                                    currentUser = currentUser,
                                    onMarkAsTaken = handleMarkAsTaken,
                                    onMarkAsLate = handleMarkAsLate,
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
                                    },
                                    onMarkAllTaken = { slotEntries ->
                                        val timeSlot = slotEntries.firstOrNull()?.let {
                                            TimeFormatUtils.formatTime(it.scheduleEntry.scheduledDateTime, timeFormat, activeLocale)
                                        } ?: ""
                                        markAllConfirmSlot = Pair(timeSlot, slotEntries)
                                    }
                                )
                            }
                        } else {
                            groupedEntries.forEach { (time, entries) ->
                                item(key = "time_section_$time") {
                                    HomeTimeSection(
                                        time = time,
                                        entries = entries,
                                        currentDateTime = currentTime,
                                        currentUser = currentUser,
                                        onMarkAsTaken = handleMarkAsTaken,
                                        onMarkAsLate = handleMarkAsLate,
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
                                        },
                                        onMarkAllTaken = { slotEntries ->
                                            markAllConfirmSlot = Pair(time, slotEntries)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    } else if (userMedicines.isEmpty()) {
                        item(key = "no_meds_state") {
                            NoMedicationsState(onAddMedicineClick = { navController.navigate("add_med") })
                        }
                    } else {
                        // User has medications, but none scheduled for today (e.g. PRN or non-today)
                        item(key = "no_scheduled_doses") {
                            NoScheduledDosesToday()
                        }
                    }

                    // --- As-Needed (PRN) Medications Section ---
                    val prnMedicines = userMedicines.filter { it.frequency.pattern == FrequencyPattern.AS_NEEDED }
                    homePrnSection(
                        prnMedicines = prnMedicines,
                        isPrnExpanded = isPrnExpanded,
                        onToggleExpand = { isPrnExpanded = !isPrnExpanded },
                        onLogPrnDoseClick = { medToLogConfirm = it }
                    )
                }
            }

            // Guard: Render ADHD intake countdown safeguard banner at the bottom of HomeScreen above navigation (#104)
            IntakeSafeguardBanner(
                state = activeIntake,
                onConfirm = {
                    coroutineScope.launch {
                        val medName = activeIntake?.medicineName ?: ""
                        IntakeSafeguardManager.confirmIntake(context, database)
                        if (medName.isNotBlank()) {
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.home_intake_confirmed, medName),
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                },
                onExtend = {
                    IntakeSafeguardManager.extendCountdown(context, 60)
                },
                onCancel = {
                    IntakeSafeguardManager.cancelCountdown(context)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.home_intake_cancelled),
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            )
        }
    }

    if (medToLogConfirm != null) {
        val med = medToLogConfirm!!
        AlertDialog(
            onDismissRequest = { medToLogConfirm = null },
            title = {
                Text(
                    text = stringResource(R.string.prn_confirm_log_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(
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
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1193D4))
                ) {
                    Text(stringResource(R.string.btn_log_prn_dose))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { medToLogConfirm = null }
                ) {
                    Text(stringResource(android.R.string.cancel))
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
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed && allowUndo) {
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
                        duration = SnackbarDuration.Short
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
                executeTakeDose(entryId, LocalDateTime.now(), note.takeIf { it.isNotBlank() })
            },
            onSkip = {
                notePromptEntry = null
                executeTakeDose(entryId, LocalDateTime.now(), null)
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

    if (markAllConfirmSlot != null) {
        val (slotTime, slotEntries) = markAllConfirmSlot!!
        val medNamesSummary = slotEntries.joinToString(", ") { it.medicine?.medicationName ?: "" }
        AlertDialog(
            onDismissRequest = { markAllConfirmSlot = null },
            title = {
                Text(
                    text = stringResource(R.string.dialog_take_all_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.dialog_take_all_message, slotEntries.size, slotTime),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = medNamesSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val entryIds = slotEntries.map { it.scheduleEntry.entryId }
                        val medNames = slotEntries.mapNotNull { it.medicine?.medicationName }
                        val count = slotEntries.size
                        markAllConfirmSlot = null

                        val user = currentUser
                        if (user?.intakeSafeguardEnabled == true) {
                            // Guard: Start intake countdown timer for ADHD support when cohort take is confirmed (#104, #102)
                            IntakeSafeguardManager.startCountdown(
                                context = context,
                                entryId = entryIds.first(),
                                entryIds = entryIds,
                                medicineName = medNames.firstOrNull() ?: "Medicine",
                                medicineNames = medNames,
                                scheduledTime = slotTime,
                                durationSeconds = user.intakeCountdownSeconds
                            )
                        } else {
                            val now = LocalDateTime.now()
                            val lateAfter = user?.considerLateAfter ?: 3
                            scheduleViewModel.markCohortAsTaken(entryIds, now.toString(), lateAfter)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.take_all_success_toast, count),
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.take_all))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { markAllConfirmSlot = null }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    DosezyTheme {
        HomeScreen(navController = rememberNavController())
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeScreenDarkPreview() {
    DosezyTheme {
        HomeScreen(navController = rememberNavController())
    }
}