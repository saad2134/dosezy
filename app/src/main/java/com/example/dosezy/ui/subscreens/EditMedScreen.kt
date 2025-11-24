package com.example.dosezy.ui.subscreens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.navigation.compose.rememberNavController
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
fun EditMedScreen(
    navController: NavController,
    medicineId: String? = null,
    medicineViewModel: MedicineViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val medicines by medicineViewModel.medicines.collectAsState()

    // Find the medicine to edit
    val medicineToEdit = medicines.find { it.medicineId == medicineId }

    // Form state - pre-filled with existing medicine data
    var medicationName by remember {
        mutableStateOf("")
    }
    var dosage by remember {
        mutableStateOf("")
    }
    var selectedDosageUnit by remember {
        mutableStateOf(DosageUnit.MG)
    }
    var selectedTime by remember {
        mutableStateOf(LocalTime.now().withMinute(0))
    }
    var selectedFrequency by remember {
        mutableStateOf(FrequencyPattern.DAILY)
    }
    var medicineImagePath by remember {
        mutableStateOf<String?>(null)
    }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Dropdown states
    var dosageUnitExpanded by remember { mutableStateOf(false) }
    var frequencyExpanded by remember { mutableStateOf(false) }

    // Load existing medicine data when screen loads or medicine changes
    LaunchedEffect(medicineToEdit) {
        if (medicineToEdit != null) {
            medicationName = medicineToEdit.medicationName
            dosage = medicineToEdit.dosage.toString()
            selectedDosageUnit = medicineToEdit.dosageUnit
            selectedTime = medicineToEdit.scheduledTimes.firstOrNull() ?: LocalTime.now().withMinute(0)
            selectedFrequency = medicineToEdit.frequency.pattern
            medicineImagePath = medicineToEdit.imageUri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Medicine",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF0D1B1B)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0D1B1B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()) // Added scroll here
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Medicine Image Selection
                    Text(
                        text = "Medicine Image",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp
                        ),
                        color = Color(0xFF0D1B1B),
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
                            color = Color(0xFF64748B),
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
                        color = Color(0xFF0D1B1B),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = medicationName,
                        onValueChange = { medicationName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        placeholder = {
                            Text(
                                "Enter medication name",
                                color = Color(0xFF94A3B8)
                            )
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF1193D4),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
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
                        color = Color(0xFF0D1B1B),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
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
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF1193D4),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
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
                                        unfocusedBorderColor = Color(0xFFE2E8F0),
                                        focusedLabelColor = Color(0xFF1193D4),
                                        unfocusedLabelColor = Color(0xFF6B7280)
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = dosageUnitExpanded,
                                    onDismissRequest = { dosageUnitExpanded = false }
                                ) {
                                    DosageUnit.values().forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unit.displayName) },
                                            onClick = {
                                                selectedDosageUnit = unit
                                                dosageUnitExpanded = false
                                            }
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
                        color = Color(0xFF0D1B1B),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Time Picker
                    OutlinedTextField(
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
                                color = Color(0xFF94A3B8)
                            )
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF1193D4),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
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
                                    tint = Color(0xFF6B7280)
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
                        color = Color(0xFF0D1B1B),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = frequencyExpanded,
                        onExpandedChange = { frequencyExpanded = !frequencyExpanded }
                    ) {
                        OutlinedTextField(
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
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF1193D4),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedLabelColor = Color(0xFF1193D4),
                                unfocusedLabelColor = Color(0xFF6B7280)
                            ),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded)
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = frequencyExpanded,
                            onDismissRequest = { frequencyExpanded = false }
                        ) {
                            FrequencyPattern.values().forEach { frequency ->
                                DropdownMenuItem(
                                    text = { Text(frequency.displayName) },
                                    onClick = {
                                        selectedFrequency = frequency
                                        frequencyExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Save Changes Button
                    Button(
                        onClick = {
                            if (medicationName.isNotBlank() && dosage.isNotBlank()) {
                                val updatedMedicine = Medicine(
                                    medicineId = medicineToEdit?.medicineId ?: UUID.randomUUID().toString(),
                                    userId = currentUser?.userId ?: "",
                                    medicationName = medicationName,
                                    dosage = dosage.toDoubleOrNull() ?: 0.0,
                                    dosageUnit = selectedDosageUnit,
                                    timesPerDay = when (selectedFrequency) {
                                        FrequencyPattern.DAILY -> 1
                                        FrequencyPattern.WEEKLY -> 7
                                        FrequencyPattern.MONTHLY -> 30
                                        FrequencyPattern.CUSTOM -> 1
                                    },
                                    frequency = com.example.dosezy.data.model.Frequency(
                                        pattern = selectedFrequency
                                    ),
                                    scheduledTimes = listOf(selectedTime),
                                    imageUri = medicineImagePath
                                )

                                if (medicineToEdit != null) {
                                    medicineViewModel.updateMedicine(updatedMedicine)
                                } else {
                                    medicineViewModel.addMedicine(updatedMedicine)
                                }
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2084E4),
                            contentColor = Color.White
                        ),
                        enabled = medicationName.isNotBlank() && dosage.isNotBlank()
                    ) {
                        Text(
                            "Save Changes",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
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
                                text = "Delete Medication",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Add extra space at the bottom to ensure everything is visible
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    )

    // Time Picker Dialog
    if (showTimePicker) {
        CustomTimePickerDialog(
            initialTime = selectedTime,
            onTimeSelected = { time ->
                selectedTime = time
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Medication") },
            text = { Text("Are you sure you want to delete this medication? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        medicineViewModel.deleteMedicine(medicineToEdit!!)
                        showDeleteDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6B7280)
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CustomTimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text(
                    "Selected: ${String.format("%02d:%02d", initialTime.hour, initialTime.minute)}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Simple hour selection
                Text("Hour: ${initialTime.hour}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = initialTime.hour.toFloat(),
                    onValueChange = { newHour ->
                        onTimeSelected(initialTime.withHour(newHour.toInt()))
                    },
                    valueRange = 0f..23f,
                    steps = 22
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Simple minute selection
                Text("Minute: ${initialTime.minute}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = initialTime.minute.toFloat(),
                    onValueChange = { newMinute ->
                        onTimeSelected(initialTime.withMinute(newMinute.toInt()))
                    },
                    valueRange = 0f..59f,
                    steps = 58
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onDismiss() }
            ) {
                Text("OK")
            }
        }
    )
}

// Extension properties for display names
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
fun EditMedScreenPreview() {
    DosezyTheme {
        EditMedScreen(navController = rememberNavController())
    }
}