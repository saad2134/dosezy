package com.example.dosezy.ui.subscreens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dosezy.R
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.PillShape
import com.example.dosezy.data.model.normalizeArabicDigits
import com.example.dosezy.ui.components.EditMedSkeletonView
import com.example.dosezy.ui.components.GridTimePickerDialog
import com.example.dosezy.ui.components.MedicineCourseDurationCard
import com.example.dosezy.ui.components.MedicineIdentificationCard
import com.example.dosezy.ui.components.MedicineNotesCard
import com.example.dosezy.ui.components.MedicineStockInventoryCard
import com.example.dosezy.ui.components.MedicineTimingsAndDosagesCard
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.components.isMedicineFormValid
import com.example.dosezy.ui.viewmodels.MedicineViewModel
import com.example.dosezy.ui.viewmodels.UserViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMedScreen(
    navController: NavController,
    medicineId: String? = null,
    medicineViewModel: MedicineViewModel = com.example.dosezy.utils.sharedMedicineViewModel(),
    userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val medicines by medicineViewModel.medicines.collectAsState()
    val archivedMedicines by medicineViewModel.archivedMedicines.collectAsState()

    // Find the medicine to edit across both active and archived lists
    val medicineToEdit = remember(medicines, archivedMedicines, medicineId) {
        (medicines + archivedMedicines).find { it.medicineId == medicineId }
    }

    // Form state - pre-filled with existing medicine data
    var medicationName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var selectedDosageUnit by remember { mutableStateOf(DosageUnit.MG) }
    var selectedTime by remember { mutableStateOf(LocalTime.of(8, 0)) }
    var selectedFrequency by remember { mutableStateOf(FrequencyPattern.DAILY) }
    var medicineImagePath by remember { mutableStateOf<String?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var editingTimeIndex by remember { mutableStateOf<Int?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPermanentDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedDaysOfWeek by remember { mutableStateOf(listOf<Int>()) }
    var selectedDaysOfMonth by remember { mutableStateOf(listOf<Int>()) }

    // Multi-Dose Presets & Stock Inventory State
    var selectedDosePreset by remember { mutableStateOf("1x") }
    var scheduledTimesList by remember { mutableStateOf(listOf(LocalTime.of(8, 0))) }
    var hasDifferentDosages by remember { mutableStateOf(false) }
    var perTimeDosages by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var currentStockText by remember { mutableStateOf("") }
    var refillThresholdText by remember { mutableStateOf("") }

    // Pill Visual, Notes, Course Duration & Interval
    var selectedPillShape by remember { mutableStateOf(PillShape.ROUND) }
    var selectedPillColor by remember { mutableStateOf("#1193D4") }
    var doctorNotes by remember { mutableStateOf("") }
    var isFiniteCourse by remember { mutableStateOf(false) }
    var selectedStartDate by remember { mutableStateOf(medicineToEdit?.startDate ?: LocalDate.now()) }
    var durationDaysText by remember { mutableStateOf("") }
    var intervalHoursText by remember { mutableStateOf("4") }
    var intervalDaysText by remember { mutableStateOf("2") }
    var intervalWeeksText by remember { mutableStateOf("2") }

    var isDataLoaded by remember { mutableStateOf(false) }

    // Load existing medicine data when screen loads or medicine changes
    LaunchedEffect(medicineToEdit) {
        if (medicineToEdit != null) {
            medicationName = medicineToEdit.medicationName
            dosage = if (medicineToEdit.dosage > 0) medicineToEdit.dosage.toString() else ""
            selectedDosageUnit = medicineToEdit.dosageUnit
            selectedTime = medicineToEdit.scheduledTimes.firstOrNull() ?: LocalTime.of(8, 0)
            scheduledTimesList = if (medicineToEdit.scheduledTimes.isNotEmpty()) medicineToEdit.scheduledTimes else listOf(selectedTime)
            selectedFrequency = medicineToEdit.frequency.pattern
            medicineImagePath = medicineToEdit.imageUri
            selectedDaysOfWeek = medicineToEdit.frequency.selectedDaysOfWeek ?: listOf()
            selectedDaysOfMonth = medicineToEdit.frequency.selectedDaysOfMonth ?: listOf()
            currentStockText = medicineToEdit.currentStock?.toString() ?: ""
            refillThresholdText = medicineToEdit.refillThreshold?.toString() ?: ""
            selectedPillShape = medicineToEdit.pillShape
            selectedPillColor = medicineToEdit.pillColor
            doctorNotes = medicineToEdit.notes ?: ""
            isFiniteCourse = (medicineToEdit.endDate != null || medicineToEdit.durationDays != null)
            selectedStartDate = medicineToEdit.startDate ?: LocalDate.now()
            durationDaysText = medicineToEdit.durationDays?.toString() ?: ""
            intervalHoursText = medicineToEdit.frequency.intervalHours?.toString() ?: "4"
            intervalDaysText = medicineToEdit.frequency.intervalDays?.toString() ?: "2"
            intervalWeeksText = medicineToEdit.frequency.intervalWeeks?.toString() ?: "2"
            hasDifferentDosages = medicineToEdit.customDosages != null && medicineToEdit.customDosages.isNotEmpty()
            perTimeDosages = medicineToEdit.customDosages?.entries?.associate { (k, v) ->
                val normKey = k.normalizeArabicDigits()
                val strVal = if (v % 1 == 0.0) v.toInt().toString() else v.toString()
                normKey to strVal
            } ?: emptyMap()
            selectedDosePreset = when (scheduledTimesList.size) {
                1 -> "1x"
                2 -> "2x"
                3 -> "3x"
                4 -> "4x"
                else -> "Custom"
            }
            kotlinx.coroutines.delay(120)
            isDataLoaded = true
        }
    }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { com.example.dosezy.ui.components.DosezySnackbarHost(snackbarHostState) },
        topBar = {
            TopBar(
                navController = navController,
                currentUser = currentUser,
                title = stringResource(R.string.form_edit_medicine),
                showBackButton = true,
                showNotificationStatus = false
            )
        },
        content = { paddingValues ->
            Crossfade(
                targetState = isDataLoaded,
                animationSpec = tween(durationMillis = 250),
                label = "edit_med_load_crossfade",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) { loaded ->
                if (!loaded) {
                    EditMedSkeletonView()
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                focusManager.clearFocus()
                            }
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        // --- Card 1: Medicine Visual & Name Card ---
                        MedicineIdentificationCard(
                            medicineImagePath = medicineImagePath,
                            selectedPillShape = selectedPillShape,
                            selectedPillColor = selectedPillColor,
                            medicationName = medicationName,
                            onImageSelected = { path -> medicineImagePath = path },
                            onVisualSelected = { shape, color ->
                                selectedPillShape = shape
                                selectedPillColor = color
                            },
                            onMedicationNameChange = { medicationName = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- Card 2: Timings & Dosages Card ---
                        MedicineTimingsAndDosagesCard(
                            selectedFrequency = selectedFrequency,
                            onFrequencyChange = { selectedFrequency = it },
                            intervalHoursText = intervalHoursText,
                            onIntervalHoursChange = { intervalHoursText = it },
                            intervalDaysText = intervalDaysText,
                            onIntervalDaysChange = { intervalDaysText = it },
                            intervalWeeksText = intervalWeeksText,
                            onIntervalWeeksChange = { intervalWeeksText = it },
                            selectedDaysOfWeek = selectedDaysOfWeek,
                            onDaysOfWeekChange = { selectedDaysOfWeek = it },
                            selectedDaysOfMonth = selectedDaysOfMonth,
                            onDaysOfMonthChange = { selectedDaysOfMonth = it },
                            scheduledTimesList = scheduledTimesList,
                            onScheduledTimesChange = { scheduledTimesList = it },
                            selectedDosePreset = selectedDosePreset,
                            onDosePresetChange = { selectedDosePreset = it },
                            hasDifferentDosages = hasDifferentDosages,
                            onHasDifferentDosagesChange = { hasDifferentDosages = it },
                            perTimeDosages = perTimeDosages,
                            onPerTimeDosagesChange = { perTimeDosages = it },
                            dosage = dosage,
                            onDosageChange = { dosage = it },
                            selectedDosageUnit = selectedDosageUnit,
                            onDosageUnitChange = { selectedDosageUnit = it },
                            timeFormat = currentUser?.timeFormat ?: com.example.dosezy.data.model.TimeFormat.HOUR_12,
                            defaultInitialTime = LocalTime.of(8, 0),
                            onOpenTimePicker = { index, time ->
                                editingTimeIndex = index
                                selectedTime = time
                                showTimePicker = true
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- Card 3: Course Duration Card ---
                        val durationDaysInt = durationDaysText.normalizeArabicDigits().toIntOrNull()
                        val calcStartDate = selectedStartDate
                        val calcEndDate = if (isFiniteCourse && durationDaysInt != null) {
                            selectedStartDate.plusDays((durationDaysInt - 1).toLong().coerceAtLeast(0L))
                        } else null

                        MedicineCourseDurationCard(
                            isFiniteCourse = isFiniteCourse,
                            onIsFiniteCourseChange = { isFiniteCourse = it },
                            selectedStartDate = selectedStartDate,
                            onStartDateChange = { selectedStartDate = it },
                            durationDaysText = durationDaysText,
                            onDurationDaysChange = { durationDaysText = it },
                            calculatedEndDate = calcEndDate
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- Card 4: Inventory & Stock Tracking Card ---
                        MedicineStockInventoryCard(
                            currentStockText = currentStockText,
                            onCurrentStockChange = { currentStockText = it },
                            refillThresholdText = refillThresholdText,
                            onRefillThresholdChange = { refillThresholdText = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- Card 5: Doctor / Pharmacist Notes Card ---
                        MedicineNotesCard(
                            doctorNotes = doctorNotes,
                            onDoctorNotesChange = { doctorNotes = it }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        val isDosageValid = if (hasDifferentDosages && scheduledTimesList.size > 1) {
                            scheduledTimesList.all { t ->
                                val key = String.format(java.util.Locale.US, "%02d:%02d", t.hour, t.minute)
                                val valueStr = perTimeDosages[key] ?: (if (dosage.isNotBlank()) dosage else null)
                                val d = valueStr?.normalizeArabicDigits()?.toDoubleOrNull()
                                d != null && d > 0.0
                            }
                        } else {
                            val d = dosage.normalizeArabicDigits().toDoubleOrNull()
                            d != null && d > 0.0
                        }

                        val isFormValid = isMedicineFormValid(
                            medicationName = medicationName,
                            isDosageValid = isDosageValid,
                            selectedFrequency = selectedFrequency,
                            selectedDaysOfWeek = selectedDaysOfWeek,
                            selectedDaysOfMonth = selectedDaysOfMonth,
                            intervalWeeksText = intervalWeeksText
                        )

                        // Save Changes Button
                        Button(
                            onClick = {
                                if (!isFormValid) return@Button

                                val baseDose = if (hasDifferentDosages && scheduledTimesList.size > 1) {
                                    val firstKey = scheduledTimesList.firstOrNull()?.let { String.format(java.util.Locale.US, "%02d:%02d", it.hour, it.minute) }
                                    val firstVal = (firstKey?.let { perTimeDosages[it] }) ?: (if (dosage.isNotBlank()) dosage else null)
                                    firstVal?.normalizeArabicDigits()?.toDoubleOrNull() ?: (dosage.normalizeArabicDigits().toDoubleOrNull() ?: 0.0)
                                } else {
                                    dosage.normalizeArabicDigits().toDoubleOrNull() ?: 0.0
                                }
                                val finalCustomDosages: Map<String, Double>? = if (hasDifferentDosages && scheduledTimesList.size > 1) {
                                    val map = mutableMapOf<String, Double>()
                                    scheduledTimesList.forEach { t ->
                                        val key = String.format(java.util.Locale.US, "%02d:%02d", t.hour, t.minute)
                                        val entered = (perTimeDosages[key] ?: (if (dosage.isNotBlank()) dosage else null))?.normalizeArabicDigits()?.toDoubleOrNull() ?: baseDose
                                        map[key] = entered
                                    }
                                    if (map.isNotEmpty()) map else null
                                } else null

                                val updatedMedicine = Medicine(
                                    medicineId = medicineToEdit?.medicineId ?: UUID.randomUUID().toString(),
                                    userId = currentUser?.userId ?: "",
                                    medicationName = medicationName,
                                    dosage = baseDose,
                                    dosageUnit = selectedDosageUnit,
                                    timesPerDay = if (selectedFrequency == FrequencyPattern.AS_NEEDED) 0 else scheduledTimesList.size,
                                    frequency = com.example.dosezy.data.model.Frequency(
                                        pattern = selectedFrequency,
                                        daysPerWeek = if (selectedFrequency == FrequencyPattern.WEEKLY || selectedFrequency == FrequencyPattern.CUSTOM) selectedDaysOfWeek.size else null,
                                        daysPerMonth = if (selectedFrequency == FrequencyPattern.MONTHLY) selectedDaysOfMonth.size else null,
                                        selectedDaysOfWeek = if (selectedFrequency == FrequencyPattern.WEEKLY || selectedFrequency == FrequencyPattern.CUSTOM) selectedDaysOfWeek else null,
                                        selectedDaysOfMonth = if (selectedFrequency == FrequencyPattern.MONTHLY) selectedDaysOfMonth else null,
                                        intervalHours = if (selectedFrequency == FrequencyPattern.EVERY_X_HOURS) intervalHoursText.normalizeArabicDigits().toIntOrNull() else null,
                                        intervalDays = if (selectedFrequency == FrequencyPattern.EVERY_X_DAYS) intervalDaysText.normalizeArabicDigits().toIntOrNull() else null,
                                        intervalWeeks = if (selectedFrequency == FrequencyPattern.CUSTOM) (intervalWeeksText.normalizeArabicDigits().toIntOrNull() ?: 1).coerceAtLeast(1) else null
                                    ),
                                    scheduledTimes = if (selectedFrequency == FrequencyPattern.AS_NEEDED) emptyList() else scheduledTimesList,
                                    imageUri = medicineImagePath,
                                    currentStock = currentStockText.normalizeArabicDigits().toIntOrNull(),
                                    refillThreshold = refillThresholdText.normalizeArabicDigits().toIntOrNull(),
                                    autoDeductOnTake = true,
                                    notes = doctorNotes.trim().ifBlank { null },
                                    pillShape = selectedPillShape,
                                    pillColor = selectedPillColor,
                                    startDate = calcStartDate,
                                    endDate = calcEndDate,
                                    durationDays = if (isFiniteCourse) durationDaysInt else null,
                                    isArchived = medicineToEdit?.isArchived ?: false,
                                    customDosages = finalCustomDosages
                                )

                                if (medicineToEdit != null) {
                                    medicineViewModel.updateMedicine(updatedMedicine)
                                    navController.previousBackStackEntry?.savedStateHandle?.set(
                                        "snackbar_message",
                                        context.getString(R.string.medication_updated_success)
                                    )
                                } else {
                                    medicineViewModel.addMedicine(updatedMedicine)
                                    navController.previousBackStackEntry?.savedStateHandle?.set(
                                        "snackbar_message",
                                        context.getString(R.string.medication_added_success)
                                    )
                                }
                                navController.popBackStack()
                            },
                            enabled = isFormValid,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2084E4),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF2084E4).copy(alpha = 0.35f),
                                disabledContentColor = Color.White.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(
                                stringResource(R.string.form_save_changes),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Delete Medication Button (only show for existing medicines)
                        if (medicineToEdit != null) {
                            Button(
                                onClick = {
                                    showDeleteDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFEE2E2),
                                    contentColor = Color(0xFFDC2626)
                                )
                            ) {
                                Text(
                                    stringResource(R.string.form_delete_medicine),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    )

    // Time Picker Dialog
    if (showTimePicker) {
        GridTimePickerDialog(
            initialTime = selectedTime,
            timeFormat = currentUser?.timeFormat ?: com.example.dosezy.data.model.TimeFormat.HOUR_12,
            onTimeSelected = { time ->
                val cleanTime = time.withSecond(0).withNano(0)
                selectedTime = cleanTime
                if (editingTimeIndex != null && editingTimeIndex!! in scheduledTimesList.indices) {
                    val oldTime = scheduledTimesList[editingTimeIndex!!]
                    val oldKey = String.format(java.util.Locale.US, "%02d:%02d", oldTime.hour, oldTime.minute)
                    val newKey = String.format(java.util.Locale.US, "%02d:%02d", cleanTime.hour, cleanTime.minute)
                    if (oldKey != newKey) {
                        val existingVal = perTimeDosages[oldKey] ?: (if (dosage.isNotBlank()) dosage else "")
                        val updated = perTimeDosages.toMutableMap()
                        updated.remove(oldKey)
                        if (existingVal.isNotBlank()) {
                            updated[newKey] = existingVal
                        }
                        perTimeDosages = updated
                    }
                    val mutable = scheduledTimesList.toMutableList()
                    mutable[editingTimeIndex!!] = cleanTime
                    scheduledTimesList = mutable.sorted().distinct()
                } else if (!scheduledTimesList.contains(cleanTime)) {
                    val newKey = String.format(java.util.Locale.US, "%02d:%02d", cleanTime.hour, cleanTime.minute)
                    if (!perTimeDosages.containsKey(newKey) && dosage.isNotBlank()) {
                        perTimeDosages = perTimeDosages + (newKey to dosage)
                    }
                    scheduledTimesList = (scheduledTimesList + cleanTime).sorted()
                }
                selectedDosePreset = "Custom"
                editingTimeIndex = null
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    // Discontinue / Delete Choice Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_med_action_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = stringResource(R.string.dialog_med_action_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 1: Discontinue / Archive (Recommended)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                medicineToEdit?.let { med ->
                                    medicineViewModel.archiveMedicine(med)
                                    showDeleteDialog = false
                                    navController.previousBackStackEntry?.savedStateHandle?.set(
                                        "snackbar_message",
                                        context.getString(R.string.medication_archived_success)
                                    )
                                    navController.popBackStack()
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = stringResource(R.string.btn_discontinue_keep_history),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.discontinue_med_explanation),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Option 2: Delete Permanently
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFDC2626).copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDeleteDialog = false
                                showPermanentDeleteConfirmDialog = true
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = stringResource(R.string.btn_delete_permanently),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.delete_permanently_explanation),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Permanent Delete 5-second Safety Confirmation Dialog
    if (showPermanentDeleteConfirmDialog && medicineToEdit != null) {
        val med = medicineToEdit
        var deleteCountdown by remember(med) { mutableStateOf(5) }
        LaunchedEffect(med) {
            deleteCountdown = 5
            while (deleteCountdown > 0) {
                kotlinx.coroutines.delay(1000L)
                deleteCountdown--
            }
        }
        AlertDialog(
            onDismissRequest = { showPermanentDeleteConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_delete_permanent_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.dialog_delete_permanent_msg, med.medicationName))
            },
            confirmButton = {
                Button(
                    onClick = {
                        medicineViewModel.deleteMedicinePermanently(med)
                        showPermanentDeleteConfirmDialog = false
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "snackbar_message",
                            context.getString(R.string.medication_deleted_success)
                        )
                        navController.popBackStack()
                    },
                    enabled = deleteCountdown == 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        disabledContainerColor = Color(0xFFDC2626).copy(alpha = 0.4f),
                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        if (deleteCountdown > 0) {
                            "${stringResource(R.string.btn_delete_permanently)} (${deleteCountdown}s)"
                        } else {
                            stringResource(R.string.btn_delete_permanently)
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermanentDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
