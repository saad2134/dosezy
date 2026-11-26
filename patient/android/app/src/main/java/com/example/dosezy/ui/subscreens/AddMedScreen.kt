package com.example.dosezy.ui.subscreens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dosezy.R
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.PillShape
import com.example.dosezy.data.model.ReminderType
import com.example.dosezy.data.model.normalizeArabicDigits
import com.example.dosezy.ui.components.GridTimePickerDialog
import com.example.dosezy.ui.components.MedicineCourseDurationCard
import com.example.dosezy.ui.components.MedicineDosageCard
import com.example.dosezy.ui.components.MedicineFormStepProgress
import com.example.dosezy.ui.components.MedicineIdentificationCard
import com.example.dosezy.ui.components.MedicineNotesCard
import com.example.dosezy.ui.components.MedicineReminderStyleCard
import com.example.dosezy.ui.components.MedicineStockInventoryCard
import com.example.dosezy.ui.components.MedicineTimingsAndDosagesCard
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.components.validateMedicineForm
import com.example.dosezy.ui.viewmodels.MedicineViewModel
import com.example.dosezy.ui.viewmodels.UserViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedScreen(
    navController: NavController,
    medicineViewModel: MedicineViewModel = hiltViewModel(),
    userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val context = LocalContext.current

    val defaultInitialTime = remember {
        val now = LocalTime.now()
        LocalTime.of(now.hour, 0).plusHours(1)
    }

    var selectedTime by remember { mutableStateOf(defaultInitialTime) }
    var medicationName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var selectedDosageUnit by remember { mutableStateOf(DosageUnit.MG) }
    var selectedFrequency by remember { mutableStateOf(FrequencyPattern.DAILY) }
    var medicineImagePath by remember { mutableStateOf<String?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var editingTimeIndex by remember { mutableStateOf<Int?>(null) }
    var selectedDaysOfWeek by remember { mutableStateOf(listOf<Int>()) }
    var selectedDaysOfMonth by remember { mutableStateOf(listOf<Int>()) }

    // Multi-Dose Presets & Stock Inventory State
    var selectedDosePreset by remember { mutableStateOf("1x") }
    var scheduledTimesList by remember { mutableStateOf(listOf(defaultInitialTime)) }
    var hasDifferentDosages by remember { mutableStateOf(false) }
    var perTimeDosages by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var currentStockText by remember { mutableStateOf("") }
    var refillThresholdText by remember { mutableStateOf("") }

    // Pill Visual, Notes, Course Duration & Interval
    var selectedPillShape by remember { mutableStateOf(PillShape.ROUND) }
    var selectedPillColor by remember { mutableStateOf("#1193D4") }
    var doctorNotes by remember { mutableStateOf("") }
    var isFiniteCourse by remember { mutableStateOf(false) }
    var selectedStartDate by remember { mutableStateOf(LocalDate.now()) }
    var durationDaysText by remember { mutableStateOf("") }
    var intervalHoursText by remember { mutableStateOf("4") }
    var intervalDaysText by remember { mutableStateOf("2") }
    var intervalWeeksText by remember { mutableStateOf("2") }
    var isRollingInterval by remember { mutableStateOf(false) }
    var cycleActiveDaysText by remember { mutableStateOf("21") }
    var cycleRestDaysText by remember { mutableStateOf("7") }
    var asNeededIntervalHoursText by remember { mutableStateOf("") }
    var asNeededNotifyEligible by remember { mutableStateOf(true) }
    var selectedReminderType by remember { mutableStateOf<ReminderType?>(null) }

    // Multi-Step Form Wizard State (#145, F13)
    var currentStep by remember { mutableStateOf(1) }
    var maxStepUnlocked by remember { mutableStateOf(1) }

    // Guard: Intercept system back gestures to step backward through the form wizard before popping back stack (#145)
    BackHandler(enabled = currentStep > 1) {
        currentStep -= 1
    }

    var showValidationErrors by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val validation = validateMedicineForm(
        medicationName = medicationName,
        dosage = dosage,
        hasDifferentDosages = hasDifferentDosages,
        scheduledTimesList = scheduledTimesList,
        perTimeDosages = perTimeDosages,
        selectedFrequency = selectedFrequency,
        selectedDaysOfWeek = selectedDaysOfWeek,
        selectedDaysOfMonth = selectedDaysOfMonth,
        intervalWeeksText = intervalWeeksText,
        isFiniteCourse = isFiniteCourse,
        durationDaysText = durationDaysText,
        cycleActiveDaysText = cycleActiveDaysText,
        cycleRestDaysText = cycleRestDaysText
    )
    val isFormValid = validation.isValid
    val isNameError = showValidationErrors && !validation.isNameValid
    val nameErrorMessage = if (isNameError) stringResource(R.string.form_error_med_name_required) else null

    val isDosageError = showValidationErrors && !validation.isDosageValid
    val dosageErrorMessage = when {
        !isDosageError -> null
        dosage.isBlank() && (!hasDifferentDosages || scheduledTimesList.size <= 1) -> stringResource(R.string.form_error_dosage_required)
        else -> stringResource(R.string.form_error_dosage_invalid)
    }

    val isFrequencyError = showValidationErrors && !validation.isFrequencyValid
    val frequencyErrorMessage = if (isFrequencyError) stringResource(R.string.form_error_select_days) else null

    val isDurationError = showValidationErrors && !validation.isFiniteDurationValid
    val durationErrorMessage = if (isDurationError) stringResource(R.string.form_error_course_duration) else null

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { com.example.dosezy.ui.components.DosezySnackbarHost(snackbarHostState) },
        topBar = {
            TopBar(
                navController = navController,
                currentUser = currentUser,
                title = stringResource(R.string.form_add_medicine),
                showBackButton = true,
                showNotificationStatus = false,
                onBackClick = {
                    if (currentStep > 1) {
                        currentStep -= 1
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = {
                                currentStep -= 1
                                coroutineScope.launch { scrollState.scrollTo(0) }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Text(
                                stringResource(R.string.btn_back),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (currentStep < 3) {
                        Button(
                            onClick = {
                                if (currentStep == 1) {
                                    if (!validation.isNameValid || !validation.isDosageValid) {
                                        showValidationErrors = true
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.form_error_fix_fields))
                                        }
                                        return@Button
                                    }
                                    currentStep = 2
                                    maxStepUnlocked = maxOf(maxStepUnlocked, 2)
                                    coroutineScope.launch { scrollState.scrollTo(0) }
                                } else if (currentStep == 2) {
                                    val hasSlotErrors = hasDifferentDosages && scheduledTimesList.size > 1 && validation.invalidTimeSlotKeys.isNotEmpty()
                                    if (!validation.isFrequencyValid || !validation.isFiniteDurationValid || hasSlotErrors) {
                                        showValidationErrors = true
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.form_error_fix_fields))
                                        }
                                        return@Button
                                    }
                                    currentStep = 3
                                    maxStepUnlocked = maxOf(maxStepUnlocked, 3)
                                    coroutineScope.launch { scrollState.scrollTo(0) }
                                }
                            },
                            modifier = Modifier
                                .weight(if (currentStep > 1) 1.5f else 1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                stringResource(R.string.btn_next),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                if (!isFormValid) {
                                    showValidationErrors = true
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(context.getString(R.string.form_error_fix_fields))
                                    }
                                    return@Button
                                }
                                val targetUserId = currentUser?.userId?.takeIf { it.isNotBlank() } ?: return@Button

                                val baseDose = if (hasDifferentDosages && scheduledTimesList.size > 1) {
                                    val firstKey = scheduledTimesList.firstOrNull()?.let { String.format(java.util.Locale.US, "%02d:%02d", it.hour, it.minute) }
                                    (firstKey?.let { perTimeDosages[it] })?.normalizeArabicDigits()?.toDoubleOrNull() ?: (dosage.normalizeArabicDigits().toDoubleOrNull() ?: 0.0)
                                } else {
                                    dosage.normalizeArabicDigits().toDoubleOrNull() ?: 0.0
                                }
                                val finalCustomDosages: Map<String, Double>? = if (hasDifferentDosages && scheduledTimesList.size > 1) {
                                    val map = mutableMapOf<String, Double>()
                                    scheduledTimesList.forEach { t ->
                                        val key = String.format(java.util.Locale.US, "%02d:%02d", t.hour, t.minute)
                                        val entered = perTimeDosages[key]?.normalizeArabicDigits()?.toDoubleOrNull()
                                            ?: (if (dosage.isNotBlank()) dosage.normalizeArabicDigits().toDoubleOrNull() else null)
                                            ?: baseDose
                                        map[key] = entered
                                    }
                                    if (map.isNotEmpty()) map else null
                                } else null

                                val durationDaysInt = durationDaysText.normalizeArabicDigits().toIntOrNull()
                                val calcEndDate = if (isFiniteCourse && durationDaysInt != null && durationDaysInt > 0) {
                                    selectedStartDate.plusDays((durationDaysInt - 1).toLong().coerceAtLeast(0L))
                                } else null

                                val newMedicine = Medicine(
                                    medicineId = UUID.randomUUID().toString(),
                                    userId = targetUserId,
                                    medicationName = medicationName.trim(),
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
                                        intervalWeeks = if (selectedFrequency == FrequencyPattern.CUSTOM) (intervalWeeksText.normalizeArabicDigits().toIntOrNull() ?: 1).coerceAtLeast(1) else null,
                                        isRollingInterval = isRollingInterval && (selectedFrequency == FrequencyPattern.DAILY || selectedFrequency == FrequencyPattern.EVERY_X_HOURS || selectedFrequency == FrequencyPattern.EVERY_X_DAYS),
                                        cycleActiveDays = if (selectedFrequency == FrequencyPattern.CYCLICAL) (cycleActiveDaysText.normalizeArabicDigits().toIntOrNull() ?: 21).coerceAtLeast(1) else null,
                                        cycleRestDays = if (selectedFrequency == FrequencyPattern.CYCLICAL) (cycleRestDaysText.normalizeArabicDigits().toIntOrNull() ?: 7).coerceAtLeast(0) else null,
                                        asNeededIntervalHours = if (selectedFrequency == FrequencyPattern.AS_NEEDED) asNeededIntervalHoursText.normalizeArabicDigits().toIntOrNull() else null,
                                        asNeededNotifyEligible = if (selectedFrequency == FrequencyPattern.AS_NEEDED) asNeededNotifyEligible else true
                                    ),
                                    scheduledTimes = if (selectedFrequency == FrequencyPattern.AS_NEEDED) emptyList() else scheduledTimesList,
                                    imageUri = medicineImagePath,
                                    currentStock = currentStockText.normalizeArabicDigits().toDoubleOrNull(),
                                    refillThreshold = refillThresholdText.normalizeArabicDigits().toDoubleOrNull(),
                                    autoDeductOnTake = true,
                                    notes = com.example.dosezy.utils.DosageFormatUtils.normalizeNoteCapitalization(doctorNotes),
                                    pillShape = selectedPillShape,
                                    pillColor = selectedPillColor,
                                    startDate = selectedStartDate,
                                    endDate = calcEndDate,
                                    durationDays = if (isFiniteCourse && durationDaysInt != null && durationDaysInt > 0) durationDaysInt else null,
                                    customDosages = if (selectedFrequency == FrequencyPattern.AS_NEEDED) null else finalCustomDosages,
                                    reminderType = selectedReminderType
                                )
                                medicineViewModel.addMedicine(newMedicine)
                                navController.previousBackStackEntry?.savedStateHandle?.set("snackbar_message", context.getString(R.string.medication_added_success))
                                navController.popBackStack()
                            },
                            enabled = !currentUser?.userId.isNullOrBlank(),
                            modifier = Modifier
                                .weight(if (currentStep > 1) 1.5f else 1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(
                                stringResource(R.string.form_add_medicine),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }
                }
            }
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Pinned Step Progress Header
                MedicineFormStepProgress(
                    currentStep = currentStep,
                    onStepClick = { step ->
                        if (step <= maxStepUnlocked) {
                            currentStep = step
                        }
                    },
                    maxStepUnlocked = maxStepUnlocked
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .imePadding()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                        }
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    when (currentStep) {
                        1 -> {
                            // --- Step 1: What are you taking? ---
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
                                onMedicationNameChange = { medicationName = it },
                                isNameError = isNameError,
                                nameErrorMessage = nameErrorMessage
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            MedicineDosageCard(
                                dosage = dosage,
                                onDosageChange = { dosage = it },
                                selectedDosageUnit = selectedDosageUnit,
                                onDosageUnitChange = { selectedDosageUnit = it },
                                isDosageError = isDosageError,
                                dosageErrorMessage = dosageErrorMessage
                            )
                        }
                        2 -> {
                            // --- Step 2: When are you taking it? ---
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
                                defaultInitialTime = defaultInitialTime,
                                onOpenTimePicker = { index, time ->
                                    editingTimeIndex = index
                                    selectedTime = time
                                    showTimePicker = true
                                },
                                isDosageError = isDosageError,
                                dosageErrorMessage = dosageErrorMessage,
                                invalidTimeSlotKeys = validation.invalidTimeSlotKeys,
                                isFrequencyError = isFrequencyError,
                                frequencyErrorMessage = frequencyErrorMessage,
                                isRollingInterval = isRollingInterval,
                                onRollingIntervalChange = { isRollingInterval = it },
                                cycleActiveDaysText = cycleActiveDaysText,
                                onCycleActiveDaysChange = { cycleActiveDaysText = it },
                                cycleRestDaysText = cycleRestDaysText,
                                onCycleRestDaysChange = { cycleRestDaysText = it },
                                asNeededIntervalHoursText = asNeededIntervalHoursText,
                                onAsNeededIntervalHoursChange = { asNeededIntervalHoursText = it },
                                asNeededNotifyEligible = asNeededNotifyEligible,
                                onAsNeededNotifyEligibleChange = { asNeededNotifyEligible = it },
                                showSingleDosageRow = false
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            val durationDaysInt = durationDaysText.normalizeArabicDigits().toIntOrNull()
                            // Guard: Calculate finite course end date only when duration is positive to avoid creating invalid zero-day or negative courses
                            val calcEndDate = if (isFiniteCourse && durationDaysInt != null && durationDaysInt > 0) {
                                selectedStartDate.plusDays((durationDaysInt - 1).toLong().coerceAtLeast(0L))
                            } else null

                            MedicineCourseDurationCard(
                                isFiniteCourse = isFiniteCourse,
                                onIsFiniteCourseChange = { isFiniteCourse = it },
                                selectedStartDate = selectedStartDate,
                                onStartDateChange = { selectedStartDate = it },
                                durationDaysText = durationDaysText,
                                onDurationDaysChange = { durationDaysText = it },
                                calculatedEndDate = calcEndDate,
                                isDurationError = isDurationError,
                                durationErrorMessage = durationErrorMessage
                            )
                        }
                        3 -> {
                            // --- Step 3: Extras (Stock, Notes, Reminders) ---
                            MedicineStockInventoryCard(
                                currentStockText = currentStockText,
                                onCurrentStockChange = { currentStockText = it },
                                refillThresholdText = refillThresholdText,
                                onRefillThresholdChange = { refillThresholdText = it }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            MedicineNotesCard(
                                doctorNotes = doctorNotes,
                                onDoctorNotesChange = { doctorNotes = it }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            MedicineReminderStyleCard(
                                selectedReminderType = selectedReminderType,
                                onReminderTypeChange = { selectedReminderType = it },
                                profileDefaultType = currentUser?.reminderType ?: ReminderType.ALARM
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
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
}