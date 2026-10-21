package com.example.dosezy.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dosezy.R
import com.example.dosezy.data.model.ScheduleWithMedicine
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.ui.components.DoseNoteDialog
import com.example.dosezy.ui.components.DosezySnackbarHost
import com.example.dosezy.ui.components.RecordDoseTimeDialog
import com.example.dosezy.ui.components.ScheduleActionDialog
import com.example.dosezy.ui.components.ScheduleCalendar
import com.example.dosezy.ui.components.ScheduleListItem
import com.example.dosezy.ui.components.ScheduleSkeletonList
import com.example.dosezy.ui.components.SkipReasonDialog
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.components.UndoConfirmationDialog
import com.example.dosezy.ui.theme.DosezyTheme
import com.example.dosezy.ui.viewmodels.ScheduleViewModel
import com.example.dosezy.ui.viewmodels.UserViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScheduleScreen(navController: NavController) {
    val userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
    val scheduleViewModel: ScheduleViewModel = com.example.dosezy.utils.sharedScheduleViewModel()

    val currentUser by userViewModel.currentUser.collectAsState()
    val selectedDate by scheduleViewModel.selectedDate.collectAsState()
    val scheduleWithMedicine by scheduleViewModel.scheduleWithMedicine.collectAsState()
    val isRefreshing by scheduleViewModel.isRefreshing.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentLocale = remember(context) { com.example.dosezy.utils.LocaleHelper.getCurrentLocale(context) }

    // State for interactive dose actions on past and current schedule items (#109)
    var actionDialogEntry by remember { mutableStateOf<ScheduleWithMedicine?>(null) }
    var manualRecordEntry by remember { mutableStateOf<ScheduleWithMedicine?>(null) }
    var skippingEntryId by remember { mutableStateOf<String?>(null) }
    var notePromptEntry by remember { mutableStateOf<ScheduleWithMedicine?>(null) }
    var noteEditEntry by remember { mutableStateOf<ScheduleWithMedicine?>(null) }
    var undoConfirmTarget by remember { mutableStateOf<ScheduleWithMedicine?>(null) }

    // Get user's time format preference
    val timeFormat = currentUser?.timeFormat ?: TimeFormat.HOUR_12
    val considerMissedAfter = currentUser?.considerMissedAfter ?: 6

    // Guard: Helper function to execute dose taking on the schedule screen with retrospective audit tagging (#109)
    fun executeTakeScheduleDose(
        swm: ScheduleWithMedicine,
        takenDateTime: LocalDateTime,
        userNote: String?
    ) {
        val entryId = swm.scheduleEntry.entryId
        val medName = swm.medicine?.medicationName ?: ""
        val isPast = swm.scheduleEntry.scheduledDateTime.toLocalDate().isBefore(LocalDate.now())

        // Guard: Attach [Logged retrospectively] audit tag to historical doses for medical report transparency (#109)
        val effectiveNote = if (isPast) {
            val tag = context.getString(R.string.dose_retrospective_tag)
            if (!userNote.isNullOrBlank()) "$userNote | $tag" else tag
        } else {
            userNote
        }

        scheduleViewModel.markAsTaken(entryId, takenDateTime.toString(), effectiveNote)

        coroutineScope.launch {
            val message = if (medName.isNotBlank()) {
                context.getString(R.string.schedule_dose_marked_taken, medName)
            } else {
                context.getString(R.string.home_action_taken)
            }
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    // Auto-refresh every minute for real-time updates and check midnight date transition
    LaunchedEffect(currentUser) {
        // Guard: Check midnight date transition on launch and user change to avoid displaying yesterday's doses
        scheduleViewModel.checkMidnightDateTransition()
        currentUser?.let { user ->
            scheduleViewModel.autoMarkMissedMedications(
                user.userId,
                user.considerMissedAfter
            )
        }
        while (true) {
            delay(60000)
            // Guard: Reconnect schedule and calendar date on midnight transition to avoid displaying yesterday's doses
            scheduleViewModel.checkMidnightDateTransition()
            currentUser?.let { user ->
                scheduleViewModel.autoMarkMissedMedications(
                    user.userId,
                    user.considerMissedAfter
                )
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                TopBar(
                    navController = navController,
                    currentUser = currentUser,
                    title = stringResource(R.string.schedule_title),
                    showBackButton = false,
                    actions = {}
                )

                // Guard: Virtualize Schedule list with LazyColumn to prevent O(N) simultaneous layout measurements on older devices
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
                ) {
                    // Calendar Section wrapped in Card-styled Surface
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            shadowElevation = 2.dp
                        ) {
                            val rawScheduleEntries by scheduleViewModel.scheduleEntries.collectAsState()

                            ScheduleCalendar(
                                selectedDate = selectedDate,
                                scheduleEntries = rawScheduleEntries,
                                onDateSelected = { date ->
                                    scheduleViewModel.setSelectedDate(date)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Date Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedDate.format(
                                    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(currentLocale)
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedDate != LocalDate.now()) {
                                androidx.compose.material3.TextButton(
                                    onClick = { scheduleViewModel.setSelectedDate(LocalDate.now()) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.btn_jump_to_today),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }

                    // Schedule Items
                    if (isRefreshing && scheduleWithMedicine.isEmpty()) {
                        item {
                            ScheduleSkeletonList(count = 3)
                        }
                    } else if (scheduleWithMedicine.isNotEmpty()) {
                        items(
                            items = scheduleWithMedicine,
                            key = { it.scheduleEntry.entryId }
                        ) { swm ->
                            // Guard: Wrap schedule item in a clickable surface allowing retrospective status updates (#109)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                shadowElevation = 1.dp
                            ) {
                                ScheduleListItem(
                                    scheduleWithMedicine = swm,
                                    timeFormat = timeFormat,
                                    targetLocale = currentLocale,
                                    missedAfter = considerMissedAfter,
                                    onClick = {
                                        actionDialogEntry = swm
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        item {
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

            // Snackbar host for dose management confirmations
            DosezySnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // Modal dialog to manage dose and retrospectively update missed items (#109)
    if (actionDialogEntry != null) {
        val targetSwm = actionDialogEntry!!
        ScheduleActionDialog(
            scheduleWithMedicine = targetSwm,
            currentUser = currentUser,
            timeFormat = timeFormat,
            targetLocale = currentLocale,
            onTakeDose = {
                val swm = targetSwm
                actionDialogEntry = null
                if (currentUser?.allowCustomDoseTime == true) {
                    manualRecordEntry = swm
                } else if (currentUser?.promptDoseNotes == true && currentUser?.allowDoseNotes == true) {
                    notePromptEntry = swm
                } else {
                    executeTakeScheduleDose(swm, swm.scheduleEntry.scheduledDateTime, null)
                }
            },
            onSkipDose = {
                val swm = targetSwm
                actionDialogEntry = null
                skippingEntryId = swm.scheduleEntry.entryId
            },
            onEditNote = {
                val swm = targetSwm
                actionDialogEntry = null
                noteEditEntry = swm
            },
            onUndoDose = {
                val swm = targetSwm
                actionDialogEntry = null
                undoConfirmTarget = swm
            },
            onDismiss = {
                actionDialogEntry = null
            }
        )
    }

    // Record Dose Time Dialog for manual timestamp and notes (#109)
    if (manualRecordEntry != null) {
        val target = manualRecordEntry!!
        RecordDoseTimeDialog(
            entry = target.scheduleEntry,
            medicineName = target.medicine?.medicationName ?: "",
            timeFormat = timeFormat,
            allowDoseNotes = currentUser?.allowDoseNotes == true,
            onDismiss = { manualRecordEntry = null },
            onConfirm = { resolvedDateTime, note ->
                manualRecordEntry = null
                executeTakeScheduleDose(target, resolvedDateTime, note)
            }
        )
    }

    // Skip Reason Dialog for marking past or current doses skipped
    if (skippingEntryId != null) {
        val targetId = skippingEntryId!!
        val targetSwm = scheduleWithMedicine.find { it.scheduleEntry.entryId == targetId }
        SkipReasonDialog(
            onConfirm = { reason ->
                skippingEntryId = null
                scheduleViewModel.markAsSkipped(targetId, reason)
                val medName = targetSwm?.medicine?.medicationName ?: ""
                coroutineScope.launch {
                    val message = if (medName.isNotBlank()) {
                        context.getString(R.string.schedule_dose_marked_skipped, medName)
                    } else {
                        context.getString(R.string.home_action_skipped)
                    }
                    snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
                }
            },
            onDismiss = {
                skippingEntryId = null
            }
        )
    }

    // Dose Note Dialog for prompt on take
    if (notePromptEntry != null) {
        val target = notePromptEntry!!
        DoseNoteDialog(
            medicationName = target.medicine?.medicationName ?: "",
            isEditing = false,
            onConfirm = { note ->
                notePromptEntry = null
                executeTakeScheduleDose(target, target.scheduleEntry.scheduledDateTime, note.takeIf { it.isNotBlank() })
            },
            onSkip = {
                notePromptEntry = null
                executeTakeScheduleDose(target, target.scheduleEntry.scheduledDateTime, null)
            },
            onDismiss = {
                notePromptEntry = null
            }
        )
    }

    // Dose Note Dialog for editing existing note
    if (noteEditEntry != null) {
        val target = noteEditEntry!!
        DoseNoteDialog(
            medicationName = target.medicine?.medicationName ?: "",
            initialNote = target.scheduleEntry.doseNotes ?: "",
            isEditing = true,
            onConfirm = { note ->
                val entryId = target.scheduleEntry.entryId
                noteEditEntry = null
                scheduleViewModel.updateDoseNotes(entryId, note.takeIf { it.isNotBlank() })
            },
            onDismiss = {
                noteEditEntry = null
            }
        )
    }

    // Undo Confirmation Dialog for reverting doses
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
                        context.getString(R.string.schedule_dose_reverted, medName)
                    } else {
                        context.getString(R.string.home_action_taken)
                    }
                    snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
                }
            },
            onDismiss = {
                undoConfirmTarget = null
            }
        )
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