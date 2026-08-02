package com.example.dosezy.ui.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.ui.components.ProfilePicturePicker
import com.example.dosezy.ui.theme.DosezyTheme
import com.example.dosezy.ui.viewmodels.MedicineViewModel
import com.example.dosezy.ui.viewmodels.UserViewModel
import java.time.LocalTime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedScreen(
    navController: NavController,
    medicineViewModel: MedicineViewModel = com.example.dosezy.utils.sharedMedicineViewModel(),
    userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
) {
    val currentUser by userViewModel.currentUser.collectAsState()

    // Form state
    var medicationName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var selectedDosageUnit by remember { mutableStateOf(DosageUnit.MG) }
    var selectedTime by remember { mutableStateOf(LocalTime.now().withMinute(0)) }
    var selectedFrequency by remember { mutableStateOf(FrequencyPattern.DAILY) }
    var medicineImagePath by remember { mutableStateOf<String?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDaysOfWeek by remember { mutableStateOf(listOf<Int>()) }
    var selectedDaysOfMonth by remember { mutableStateOf(listOf<Int>()) }

    // Dropdown states
    var dosageUnitExpanded by remember { mutableStateOf(false) }
    var frequencyExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Add Medication",
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
                            contentDescription = "Back",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Medicine Image Selection
                Text(
                    text = "Medicine Image",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Use ProfilePicturePicker for medicine image selection
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .clip(RoundedCornerShape(16.dp)) // Squircle shape
                    ) {
                        ProfilePicturePicker(
                            profilePicPath = medicineImagePath,
                            onProfilePictureSelected = { path ->
                                medicineImagePath = path
                            },
                            modifier = Modifier.size(128.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (medicineImagePath != null) "Change Medicine Image" else "Upload Medicine Image",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Medication Name
                Text(
                    text = "Medication Name",
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
                        .height(64.dp),
                    placeholder = {
                        Text(
                            "Enter medication name",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
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
                    text = "Dosage",
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
                            .height(64.dp),
                        placeholder = {
                            Text(
                                "0",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
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
                                value = selectedDosageUnit.displayName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dosageUnitExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .menuAnchor(),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
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
                                        text = { Text(unit.displayName) },
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

                Spacer(modifier = Modifier.height(16.dp))

                // Time Selection
                Text(
                    text = "Time",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Time Picker
                OutlinedTextField(
                    shape = RoundedCornerShape(16.dp),
                    value = String.format("%02d:%02d", selectedTime.hour, selectedTime.minute),
                    onValueChange = { /* Read-only, opens time picker */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clickable { showTimePicker = true },
                    readOnly = true,
                    placeholder = {
                        Text(
                            "Select time",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1193D4),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = Color(0xFF1193D4),
                        unfocusedLabelColor = Color(0xFF6B7280)
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = { showTimePicker = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Select time",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Frequency
                Text(
                    text = "Frequency",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = frequencyExpanded,
                    onExpandedChange = { frequencyExpanded = !frequencyExpanded }
                ) {
                    OutlinedTextField(
                    shape = RoundedCornerShape(16.dp),
                        value = selectedFrequency.displayName,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .menuAnchor(),
                        placeholder = {
                            Text(
                                "Select frequency",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF1193D4),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = Color(0xFF1193D4),
                            unfocusedLabelColor = Color(0xFF6B7280)
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
                                text = { Text(frequency.displayName) },
                                onClick = {
                                    selectedFrequency = frequency
                                    frequencyExpanded = false
                                },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            )
                        }
                    }
                }

                if (selectedFrequency == FrequencyPattern.WEEKLY) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Select Days of the Week",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val daysOfWeekNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysOfWeekNames.forEachIndexed { index, name ->
                            val dayValue = index + 1
                            val isSelected = selectedDaysOfWeek.contains(dayValue)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.surfaceVariant
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
                                    text = name.take(2), // Mo, Tu, We, etc.
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Select Days of the Month",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val chunkedDays = (1..31).chunked(7)
                        chunkedDays.forEach { rowDays ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rowDays.forEach { day ->
                                    val isSelected = selectedDaysOfMonth.contains(day)
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.surfaceVariant
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
                                        Spacer(modifier = Modifier.size(42.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Add Medication Button
                Button(
                    onClick = {
                        if (medicationName.isNotBlank() && dosage.isNotBlank()) {
                            val newMedicine = Medicine(
                                medicineId = UUID.randomUUID().toString(),
                                userId = currentUser?.userId ?: "",
                                medicationName = medicationName,
                                dosage = dosage.toDoubleOrNull() ?: 0.0,
                                dosageUnit = selectedDosageUnit,
                                timesPerDay = when (selectedFrequency) {
                                    FrequencyPattern.DAILY -> 1
                                    FrequencyPattern.WEEKLY -> selectedDaysOfWeek.size
                                    FrequencyPattern.MONTHLY -> selectedDaysOfMonth.size
                                    FrequencyPattern.CUSTOM -> 1
                                },
                                frequency = com.example.dosezy.data.model.Frequency(
                                    pattern = selectedFrequency,
                                    daysPerWeek = if (selectedFrequency == FrequencyPattern.WEEKLY) selectedDaysOfWeek.size else null,
                                    daysPerMonth = if (selectedFrequency == FrequencyPattern.MONTHLY) selectedDaysOfMonth.size else null,
                                    selectedDaysOfWeek = if (selectedFrequency == FrequencyPattern.WEEKLY) selectedDaysOfWeek else null,
                                    selectedDaysOfMonth = if (selectedFrequency == FrequencyPattern.MONTHLY) selectedDaysOfMonth else null
                                ),
                                scheduledTimes = listOf(selectedTime),
                                imageUri = medicineImagePath
                            )
                            medicineViewModel.addMedicine(newMedicine)
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2084E4),
                        contentColor = Color.White
                    ),
                    enabled = medicationName.isNotBlank() && dosage.isNotBlank() && (
                        selectedFrequency == FrequencyPattern.DAILY ||
                        (selectedFrequency == FrequencyPattern.WEEKLY && selectedDaysOfWeek.isNotEmpty()) ||
                        (selectedFrequency == FrequencyPattern.MONTHLY && selectedDaysOfMonth.isNotEmpty()) ||
                        selectedFrequency == FrequencyPattern.CUSTOM
                    )
                ) {
                    Text(
                        "Add Medication",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                }
            }
        }
    )

    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            initialTime = selectedTime,
            onTimeSelected = { time ->
                selectedTime = time
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
fun TimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTimeState by remember { mutableStateOf(initialTime) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        tonalElevation = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Select Time", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text(
                    "Selected: ${String.format("%02d:%02d", selectedTimeState.hour, selectedTimeState.minute)}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Simple hour selection
                Text("Hour: ${selectedTimeState.hour}", style = MaterialTheme.typography.bodyMedium)
                androidx.compose.material3.Slider(
                    value = selectedTimeState.hour.toFloat(),
                    onValueChange = { newHour ->
                        selectedTimeState = selectedTimeState.withHour(newHour.toInt())
                    },
                    valueRange = 0f..23f,
                    steps = 22
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Simple minute selection
                Text("Minute: ${selectedTimeState.minute}", style = MaterialTheme.typography.bodyMedium)
                androidx.compose.material3.Slider(
                    value = selectedTimeState.minute.toFloat(),
                    onValueChange = { newMinute ->
                        selectedTimeState = selectedTimeState.withMinute(newMinute.toInt())
                    },
                    valueRange = 0f..59f,
                    steps = 58
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onTimeSelected(selectedTimeState) }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

// Extension properties
private val DosageUnit.displayName: String
    get() = when (this) {
        DosageUnit.MG -> "mg"
        DosageUnit.MCG -> "mcg"
        DosageUnit.ML -> "ml"
        DosageUnit.DROP -> "drop"
        DosageUnit.TABLET -> "tablet"
        DosageUnit.CAPSULE -> "capsule"
    }

private val FrequencyPattern.displayName: String
    get() = when (this) {
        FrequencyPattern.DAILY -> "Daily"
        FrequencyPattern.WEEKLY -> "Weekly"
        FrequencyPattern.MONTHLY -> "Monthly"
        FrequencyPattern.CUSTOM -> "Custom"
    }

@Preview(showBackground = true)
@Composable
fun AddMedScreenPreview() {
    DosezyTheme {
        AddMedScreen(navController = androidx.navigation.compose.rememberNavController())
    }
}