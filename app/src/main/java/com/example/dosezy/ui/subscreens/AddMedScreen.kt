package com.example.dosezy.ui.subscreens

import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.ui.theme.DosezyTheme
import com.example.dosezy.ui.viewmodels.MedicineViewModel
import com.example.dosezy.ui.viewmodels.UserViewModel
import com.example.dosezy.utils.rememberImagePicker
import java.time.LocalTime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedScreen(
    navController: NavController,
    medicineViewModel: MedicineViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val currentUser by userViewModel.currentUser.collectAsState()

    // Form state
    var medicationName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var selectedDosageUnit by remember { mutableStateOf(DosageUnit.MG) }
    var selectedTime by remember { mutableStateOf(LocalTime.of(8, 30)) }
    var selectedFrequency by remember { mutableStateOf(FrequencyPattern.DAILY) }
    var medicineImageUri by remember { mutableStateOf<Uri?>(null) }

    // Dropdown states
    var dosageUnitExpanded by remember { mutableStateOf(false) }
    var frequencyExpanded by remember { mutableStateOf(false) }

    // Image Picker - FIXED: Use proper context and handle camera permissions
    val context = LocalContext.current
    val imagePicker = rememberImagePicker(
        context = context,
        onImageSelected = { uri ->
            medicineImageUri = uri
        }
    )

    val cameraLauncher = imagePicker.getCameraLauncher()
    val galleryLauncher = imagePicker.getGalleryLauncher()

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image Preview - FIXED: Rounded square shape
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                Color(0xFFDBEAFE),
                                shape = RoundedCornerShape(12.dp) // Rounded square
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (medicineImageUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(medicineImageUri)
                                        .crossfade(true)
                                        .build()
                                ),
                                contentDescription = "Selected medicine image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Color(0xFFDBEAFE),
                                        shape = RoundedCornerShape(12.dp) // Rounded square
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = "Default medicine icon",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Camera and Gallery Buttons
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Camera Button - FIXED: Handle camera properly
                        Button(
                            onClick = {
                                try {
                                    val tempUri = imagePicker.prepareCameraIntent()
                                    cameraLauncher.launch(tempUri)
                                } catch (e: Exception) {
                                    // Handle camera error (permissions, etc.)
                                    e.printStackTrace()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEFF6FF),
                                contentColor = Color(0xFF3B82F6)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Take photo",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Take Photo",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Gallery Button
                        Button(
                            onClick = {
                                galleryLauncher.launch("image/*")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEFF6FF),
                                contentColor = Color(0xFF3B82F6)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Choose from gallery",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Choose from Gallery",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Rest of the form fields remain the same...
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

                // Time
                Text(
                    text = "Time",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    ),
                    color = Color(0xFF0D1B1B),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = String.format("%02d:%02d", selectedTime.hour, selectedTime.minute),
                    onValueChange = { /* Would open time picker dialog */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
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
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Select time",
                            tint = Color(0xFF6B7280)
                        )
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
                                    else -> 1
                                },
                                frequency = com.example.dosezy.data.model.Frequency(
                                    pattern = selectedFrequency
                                ),
                                scheduledTimes = listOf(selectedTime),
                                imageUri = medicineImageUri?.toString()
                            )
                            medicineViewModel.addMedicine(newMedicine)
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
                        "Add Medication",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                }
            }
        }
    )
}

// Extension properties remain the same...
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