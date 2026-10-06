package com.example.dosezy.ui.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dosezy.R
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.PillShape
import com.example.dosezy.data.model.normalizeArabicDigits
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

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { com.example.dosezy.ui.components.DosezySnackbarHost(snackbarHostState) },
        topBar = {
            TopBar(
                navController = navController,
                currentUser = currentUser,
                title = stringResource(R.string.form_add_medicine),
                showBackButton = true,
                showNotificationStatus = false
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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
                // --- Card 1: Medicine & Identification Card ---
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
                    defaultInitialTime = defaultInitialTime,
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

                Button(
                    onClick = {
                        if (!isFormValid) return@Button

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

                        val newMedicine = Medicine(
                            medicineId = UUID.randomUUID().toString(),
                            userId = currentUser?.userId ?: "",
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
                            customDosages = finalCustomDosages
                        )
                        medicineViewModel.addMedicine(newMedicine)
                        navController.previousBackStackEntry?.savedStateHandle?.set("snackbar_message", context.getString(R.string.medication_added_success))
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
                        stringResource(R.string.form_add_medicine),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                Spacer(modifier = Modifier.height(32.dp))
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