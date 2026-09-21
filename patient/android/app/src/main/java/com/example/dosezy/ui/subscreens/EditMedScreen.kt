package com.example.dosezy.ui.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CalendarMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.example.dosezy.ui.components.GridTimePickerDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Divider
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.stringResource
import com.example.dosezy.R
import kotlin.OptIn
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dosezy.ui.components.MedicinePhotoVisualPicker
import com.example.dosezy.ui.components.ProfilePicturePicker
import androidx.navigation.compose.rememberNavController
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.PillShape
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.data.model.getLocalizedName
import com.example.dosezy.data.model.normalizeArabicDigits
import com.example.dosezy.ui.components.PillColorSelector
import com.example.dosezy.ui.components.PillShapeSelector
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.example.dosezy.ui.components.EditMedSkeletonView
import com.example.dosezy.ui.theme.DosezyTheme
import com.example.dosezy.ui.viewmodels.MedicineViewModel
import com.example.dosezy.ui.viewmodels.UserViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditMedScreen(
    navController: NavController,
    medicineId: String? = null,
    medicineViewModel: MedicineViewModel = com.example.dosezy.utils.sharedMedicineViewModel(),
    userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
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

    // v2.4.0 New States: Pill Visual, Notes, Course Duration & Interval
    var selectedPillShape by remember { mutableStateOf(PillShape.ROUND) }
    var selectedPillColor by remember { mutableStateOf("#1193D4") }
    var doctorNotes by remember { mutableStateOf("") }
    var isFiniteCourse by remember { mutableStateOf(false) }
    var selectedStartDate by remember { mutableStateOf(medicineToEdit?.startDate ?: LocalDate.now()) }
    var durationDaysText by remember { mutableStateOf("") }
    var intervalHoursText by remember { mutableStateOf("4") }
    var intervalDaysText by remember { mutableStateOf("2") }

    // Dropdown states
    var dosageUnitExpanded by remember { mutableStateOf(false) }
    var frequencyExpanded by remember { mutableStateOf(false) }
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
            // Small subtle delay to allow smooth animation transition from skeleton to loaded content
            kotlinx.coroutines.delay(120)
            isDataLoaded = true
        }
    }

    val activeLocale = remember(currentUser?.language) {
        com.example.dosezy.utils.LocaleHelper.getLocale(currentUser?.language ?: com.example.dosezy.data.model.Language.SYSTEM)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.form_edit_medicine),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                            .verticalScroll(rememberScrollState())
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                    // Medicine Image Selection
                    Text(
                        text = stringResource(R.string.form_medicine_image),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Medicine Image Selection with Pill Visual
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MedicinePhotoVisualPicker(
                            imagePath = medicineImagePath,
                            pillShape = selectedPillShape,
                            pillColor = selectedPillColor,
                            onImageSelected = { path -> medicineImagePath = path },
                            onVisualSelected = { shape, color ->
                                selectedPillShape = shape
                                selectedPillColor = color
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (medicineImagePath != null) stringResource(R.string.form_change_img) else stringResource(R.string.form_upload_img),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Medication Name
                    Text(
                        text = stringResource(R.string.form_med_name),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        shape = RoundedCornerShape(16.dp),
                        value = medicationName,
                        onValueChange = { medicationName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp),
                        placeholder = {
                            Text(
                                stringResource(R.string.form_med_name_placeholder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1193D4),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = Color(0xFF1193D4),
                            unfocusedLabelColor = Color(0xFF6B7280),
                            cursorColor = Color(0xFF1193D4)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dosage
                    Text(
                        text = stringResource(R.string.form_dosage),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            shape = RoundedCornerShape(16.dp),
                            value = dosage,
                            onValueChange = {
                                if (it.all { char -> char.isDigit() || char == '.' }) {
                                    dosage = it
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 56.dp),
                            placeholder = {
                                Text(
                                    "0",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1193D4),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = Color(0xFF1193D4),
                                unfocusedLabelColor = Color(0xFF6B7280),
                                cursorColor = Color(0xFF1193D4)
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Dosage Unit Dropdown
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = dosageUnitExpanded,
                                onExpandedChange = { dosageUnitExpanded = !dosageUnitExpanded }
                            ) {
                                OutlinedTextField(
                                    shape = RoundedCornerShape(16.dp),
                                    value = selectedDosageUnit.getLocalizedName(),
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dosageUnitExpanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 56.dp)
                                        .menuAnchor(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF1193D4),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedLabelColor = Color(0xFF1193D4),
                                        unfocusedLabelColor = Color(0xFF6B7280)
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = dosageUnitExpanded,
                                    onDismissRequest = { dosageUnitExpanded = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    DosageUnit.values().forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unit.getLocalizedName()) },
                                            onClick = {
                                                selectedDosageUnit = unit
                                                dosageUnitExpanded = false
                                            },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))



                    // --- Unified Schedule & Dosing Times Card ---
                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = stringResource(R.string.scheduled_dosing_times_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 1. Frequency Dropdown
                            Text(
                                text = stringResource(R.string.form_frequency),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            ExposedDropdownMenuBox(
                                expanded = frequencyExpanded,
                                onExpandedChange = { frequencyExpanded = !frequencyExpanded }
                            ) {
                                OutlinedTextField(
                                    shape = RoundedCornerShape(16.dp),
                                    value = selectedFrequency.getLocalizedName(),
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 56.dp)
                                        .menuAnchor(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF1193D4),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded)
                                    }
                                )

                                ExposedDropdownMenu(
                                    expanded = frequencyExpanded,
                                    onDismissRequest = { frequencyExpanded = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    FrequencyPattern.values().forEach { frequency ->
                                        DropdownMenuItem(
                                            text = { Text(frequency.getLocalizedName()) },
                                            onClick = {
                                                selectedFrequency = frequency
                                                frequencyExpanded = false
                                            },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                        )
                                    }
                                }
                            }

                            // Specific frequency configurations
                            if (selectedFrequency == FrequencyPattern.AS_NEEDED) {
                                Spacer(modifier = Modifier.height(12.dp))
                                androidx.compose.material3.Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.freq_as_needed_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            if (selectedFrequency == FrequencyPattern.EVERY_X_HOURS) {
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedTextField(
                                    value = intervalHoursText,
                                    onValueChange = { if (it.all { c -> c.isDigit() }) intervalHoursText = it },
                                    label = { Text(stringResource(R.string.interval_hours_label)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF1193D4),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                                val hrs = intervalHoursText.toIntOrNull()
                                if (hrs != null && hrs > 24) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(R.string.hours_limit_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFF59E0B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (selectedFrequency == FrequencyPattern.EVERY_X_DAYS) {
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedTextField(
                                    value = intervalDaysText,
                                    onValueChange = { if (it.all { c -> c.isDigit() }) intervalDaysText = it },
                                    label = { Text(stringResource(R.string.interval_days_label)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF1193D4),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }

                            if (selectedFrequency == FrequencyPattern.WEEKLY) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = stringResource(R.string.form_select_days_week),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val daysOfWeekNames = listOf(
                                    stringResource(R.string.day_mon),
                                    stringResource(R.string.day_tue),
                                    stringResource(R.string.day_wed),
                                    stringResource(R.string.day_thu),
                                    stringResource(R.string.day_fri),
                                    stringResource(R.string.day_sat),
                                    stringResource(R.string.day_sun)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    daysOfWeekNames.forEachIndexed { index, name ->
                                        val dayValue = index + 1
                                        val isSelected = selectedDaysOfWeek.contains(dayValue)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary 
                                                    else MaterialTheme.colorScheme.surface
                                                )
                                                .clickable {
                                                    selectedDaysOfWeek = if (isSelected) {
                                                        selectedDaysOfWeek - dayValue
                                                    } else {
                                                        selectedDaysOfWeek + dayValue
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            if (selectedFrequency == FrequencyPattern.MONTHLY) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = stringResource(R.string.form_select_days_month),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val chunkedDays = (1..31).chunked(7)
                                    chunkedDays.forEach { rowDays ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            rowDays.forEach { day ->
                                                val isSelected = selectedDaysOfMonth.contains(day)
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.primary 
                                                            else MaterialTheme.colorScheme.surface
                                                        )
                                                        .clickable {
                                                            selectedDaysOfMonth = if (isSelected) {
                                                                selectedDaysOfMonth - day
                                                            } else {
                                                                selectedDaysOfMonth + day
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = day.toString(),
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                            if (rowDays.size < 7) {
                                                repeat(7 - rowDays.size) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Presets and custom times (only for scheduled frequencies, not AS_NEEDED)
                            if (selectedFrequency != FrequencyPattern.AS_NEEDED) {
                                Spacer(modifier = Modifier.height(18.dp))

                                // 2. Dosing Presets (1x, 2x, 3x, 4x) - only for DAILY
                                if (selectedFrequency == FrequencyPattern.DAILY) {
                                    Text(
                                        text = stringResource(R.string.daily_frequency_presets_title),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    val presets = listOf(
                                        "1x" to listOf(LocalTime.of(8, 0)),
                                        "2x" to listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
                                        "3x" to listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0)),
                                        "4x" to listOf(LocalTime.of(8, 0), LocalTime.of(12, 0), LocalTime.of(16, 0), LocalTime.of(20, 0))
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        presets.forEach { (label, times) ->
                                            val isSelected = selectedDosePreset == label
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = 48.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        selectedDosePreset = label
                                                        scheduledTimesList = times
                                                        selectedTime = times.first()
                                                    },
                                                color = if (isSelected) Color(0xFF1193D4) else MaterialTheme.colorScheme.surface,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isSelected) Color(0xFF1193D4) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 12.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 15.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                }

                                // 3. Custom Dosing Times Chips - 2-items-per-row expanded layout
                                Text(
                                    text = stringResource(R.string.scheduled_dosing_times_title),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                val totalItems = scheduledTimesList.size + 1
                                val rowCount = (totalItems + 1) / 2
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (rowIdx in 0 until rowCount) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            for (colIdx in 0..1) {
                                                val itemIndex = rowIdx * 2 + colIdx
                                                if (itemIndex < scheduledTimesList.size) {
                                                    val time = scheduledTimesList[itemIndex]
                                                    val formatted = com.example.dosezy.utils.TimeFormatUtils.formatLocalTime(
                                                        time,
                                                        currentUser?.timeFormat ?: TimeFormat.HOUR_12,
                                                        activeLocale
                                                    )
                                                    val isSelected = selectedTime == time
                                                    Surface(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .heightIn(min = 48.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .clickable {
                                                                selectedTime = time
                                                                editingTimeIndex = itemIndex
                                                                showTimePicker = true
                                                            },
                                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                                        border = androidx.compose.foundation.BorderStroke(
                                                            1.dp,
                                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                                        ),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 10.dp, vertical = 10.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Schedule,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Text(
                                                                text = formatted,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .padding(horizontal = 6.dp)
                                                            )
                                                            if (scheduledTimesList.size > 1) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(24.dp)
                                                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                                                        .clickable {
                                                                            scheduledTimesList = scheduledTimesList - time
                                                                            if (selectedTime == time && scheduledTimesList.isNotEmpty()) {
                                                                                selectedTime = scheduledTimesList.first()
                                                                            }
                                                                            selectedDosePreset = "Custom"
                                                                        },
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Close,
                                                                        contentDescription = stringResource(R.string.med_time_remove_cd),
                                                                        modifier = Modifier.size(16.dp),
                                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else if (itemIndex == scheduledTimesList.size) {
                                                    // + Add Time Button
                                                    Surface(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .heightIn(min = 48.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .clickable {
                                                                editingTimeIndex = null
                                                                showTimePicker = true
                                                            },
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                                        border = androidx.compose.foundation.BorderStroke(
                                                            1.dp,
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                        ),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 10.dp, vertical = 10.dp),
                                                            horizontalArrangement = Arrangement.Center,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Add,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = stringResource(R.string.med_time_add_btn),
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }

                                // --- Variable Dosages Per Scheduled Time (Issue #82) ---
                                if (scheduledTimesList.size > 1) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                text = stringResource(R.string.med_different_dosages_toggle),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = stringResource(R.string.med_different_dosages_subtitle),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = hasDifferentDosages,
                                            onCheckedChange = { isChecked ->
                                                hasDifferentDosages = isChecked
                                                if (isChecked) {
                                                    val updated = perTimeDosages.toMutableMap()
                                                    scheduledTimesList.forEach { t ->
                                                        val key = String.format(java.util.Locale.US, "%02d:%02d", t.hour, t.minute)
                                                        if (updated[key].isNullOrBlank()) {
                                                            updated[key] = dosage
                                                        }
                                                    }
                                                    perTimeDosages = updated
                                                }
                                            }
                                        )
                                    }

                                    if (hasDifferentDosages) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                             scheduledTimesList.forEach { time ->
                                                 val key = String.format(java.util.Locale.US, "%02d:%02d", time.hour, time.minute)
                                                 val formattedTime = time.format(DateTimeFormatter.ofPattern("hh:mm a"))

                                                Surface(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.surface,
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Schedule,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Text(
                                                                text = formattedTime,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }

                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            OutlinedTextField(
                                                                value = perTimeDosages[key] ?: (if (dosage.isNotBlank()) dosage else ""),
                                                                onValueChange = { newVal ->
                                                                    if (newVal.all { it.isDigit() || it == '.' || it == ',' || it == '\u066B' }) {
                                                                        perTimeDosages = perTimeDosages + (key to newVal)
                                                                    }
                                                                },
                                                                modifier = Modifier.width(85.dp),
                                                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                                singleLine = true,
                                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                                shape = RoundedCornerShape(10.dp)
                                                            )
                                                            Text(
                                                                text = selectedDosageUnit.getLocalizedName(),
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Medium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- v2.4.0 Prescription Duration & End Date Card ---
                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = stringResource(R.string.course_duration_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (!isFiniteCourse) Color(0xFF1193D4) else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clickable { isFiniteCourse = false }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = stringResource(R.string.course_ongoing),
                                            color = if (!isFiniteCourse) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isFiniteCourse) Color(0xFF1193D4) else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clickable { isFiniteCourse = true }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = stringResource(R.string.course_finite),
                                            color = if (isFiniteCourse) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }

                            // Start Date Picker Row (placed before course duration for logical workflow)
                            val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        android.app.DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                selectedStartDate = LocalDate.of(y, m + 1, d)
                                            },
                                            selectedStartDate.year,
                                            selectedStartDate.monthValue - 1,
                                            selectedStartDate.dayOfMonth
                                        ).show()
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.med_start_date_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF1193D4)
                                        )
                                        Text(
                                            text = selectedStartDate.format(dateFormatter),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            if (isFiniteCourse) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = durationDaysText,
                                    onValueChange = { if (it.all { c -> c.isDigit() }) durationDaysText = it },
                                    label = { Text(stringResource(R.string.course_duration_days_label)) },
                                    placeholder = { Text(stringResource(R.string.course_duration_days_placeholder)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF1193D4),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                 val days = durationDaysText.toIntOrNull()
                                if (days != null && days > 0) {
                                    val endDate = selectedStartDate.plusDays(days.toLong())
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(R.string.course_end_date_label, endDate.toString()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Medicine Stock & Inventory UI ---
                    Text(
                        text = stringResource(R.string.stock_inventory_title),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = currentStockText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) currentStockText = it },
                            label = { Text(stringResource(R.string.stock_current)) },
                            placeholder = { Text(stringResource(R.string.med_stock_current_placeholder)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 56.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1193D4),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        OutlinedTextField(
                            value = refillThresholdText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) refillThresholdText = it },
                            label = { Text(stringResource(R.string.stock_refill_threshold)) },
                            placeholder = { Text(stringResource(R.string.med_stock_refill_placeholder)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 56.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1193D4),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- v2.4.0 Doctor & Pharmacy Notes ---
                    Text(
                        text = stringResource(R.string.med_notes_label),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = doctorNotes,
                        onValueChange = { doctorNotes = it },
                        placeholder = { Text(stringResource(R.string.med_notes_placeholder)) },
                        shape = RoundedCornerShape(16.dp),
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1193D4),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val durationDaysInt = durationDaysText.toIntOrNull()
                    val calcStartDate = selectedStartDate
                    val calcEndDate = if (isFiniteCourse && durationDaysInt != null) {
                        selectedStartDate.plusDays((durationDaysInt - 1).toLong().coerceAtLeast(0L))
                    } else null

                    val isFormValid = medicationName.isNotBlank() && dosage.isNotBlank() && (
                        selectedFrequency == FrequencyPattern.DAILY ||
                        selectedFrequency == FrequencyPattern.AS_NEEDED ||
                        selectedFrequency == FrequencyPattern.EVERY_X_HOURS ||
                        selectedFrequency == FrequencyPattern.EVERY_X_DAYS ||
                        (selectedFrequency == FrequencyPattern.WEEKLY && selectedDaysOfWeek.isNotEmpty()) ||
                        (selectedFrequency == FrequencyPattern.MONTHLY && selectedDaysOfMonth.isNotEmpty()) ||
                        selectedFrequency == FrequencyPattern.CUSTOM
                    )

                    Button(
                        onClick = {
                            if (!isFormValid) {
                                val errorMsg = when {
                                    medicationName.isBlank() -> context.getString(R.string.validation_enter_med_name)
                                    dosage.isBlank() -> context.getString(R.string.validation_enter_dosage)
                                    selectedFrequency == FrequencyPattern.WEEKLY && selectedDaysOfWeek.isEmpty() -> context.getString(R.string.validation_select_days_week)
                                    selectedFrequency == FrequencyPattern.MONTHLY && selectedDaysOfMonth.isEmpty() -> context.getString(R.string.validation_select_days_month)
                                    else -> context.getString(R.string.validation_enter_med_name)
                                }
                                android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val finalCustomDosages: Map<String, Double>? = if (hasDifferentDosages && scheduledTimesList.size > 1) {
                                val map = mutableMapOf<String, Double>()
                                val baseDose = dosage.normalizeArabicDigits().toDoubleOrNull() ?: 0.0
                                scheduledTimesList.forEach { t ->
                                    val key = String.format(java.util.Locale.US, "%02d:%02d", t.hour, t.minute)
                                    val entered = perTimeDosages[key]?.normalizeArabicDigits()?.toDoubleOrNull() ?: baseDose
                                    map[key] = entered
                                }
                                if (map.isNotEmpty()) map else null
                            } else null

                            val updatedMedicine = Medicine(
                                medicineId = medicineToEdit?.medicineId ?: UUID.randomUUID().toString(),
                                userId = currentUser?.userId ?: "",
                                medicationName = medicationName,
                                dosage = dosage.toDoubleOrNull() ?: 0.0,
                                dosageUnit = selectedDosageUnit,
                                timesPerDay = if (selectedFrequency == FrequencyPattern.AS_NEEDED) 0 else scheduledTimesList.size,
                                frequency = com.example.dosezy.data.model.Frequency(
                                    pattern = selectedFrequency,
                                    daysPerWeek = if (selectedFrequency == FrequencyPattern.WEEKLY) selectedDaysOfWeek.size else null,
                                    daysPerMonth = if (selectedFrequency == FrequencyPattern.MONTHLY) selectedDaysOfMonth.size else null,
                                    selectedDaysOfWeek = if (selectedFrequency == FrequencyPattern.WEEKLY) selectedDaysOfWeek else null,
                                    selectedDaysOfMonth = if (selectedFrequency == FrequencyPattern.MONTHLY) selectedDaysOfMonth else null,
                                    intervalHours = if (selectedFrequency == FrequencyPattern.EVERY_X_HOURS) intervalHoursText.toIntOrNull() else null,
                                    intervalDays = if (selectedFrequency == FrequencyPattern.EVERY_X_DAYS) intervalDaysText.toIntOrNull() else null
                                ),
                                scheduledTimes = if (selectedFrequency == FrequencyPattern.AS_NEEDED) emptyList() else scheduledTimesList,
                                imageUri = medicineImagePath,
                                currentStock = currentStockText.toIntOrNull(),
                                refillThreshold = refillThresholdText.toIntOrNull(),
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
                            } else {
                                medicineViewModel.addMedicine(updatedMedicine)
                            }
                            navController.popBackStack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFormValid) Color(0xFF2084E4) else Color(0xFF2084E4).copy(alpha = 0.6f),
                            contentColor = Color.White
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

                    Spacer(modifier = Modifier.height(32.dp))
                        }
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
                    val mutable = scheduledTimesList.toMutableList()
                    mutable[editingTimeIndex!!] = cleanTime
                    scheduledTimesList = mutable.sorted().distinct()
                } else if (!scheduledTimesList.contains(cleanTime)) {
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


@Preview(showBackground = true)
@Composable
fun EditMedScreenPreview() {
    DosezyTheme {
        EditMedScreen(navController = rememberNavController())
    }
}
