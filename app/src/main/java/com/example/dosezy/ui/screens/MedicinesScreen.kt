package com.example.dosezy.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.theme.DosezyTheme
import com.example.dosezy.ui.viewmodels.MedicineViewModel
import com.example.dosezy.ui.viewmodels.UserViewModel

@Composable
fun MedicinesScreen(
    navController: NavController,
    medicineViewModel: MedicineViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val medicines by medicineViewModel.medicines.collectAsState()
    val isLoading by medicineViewModel.isLoading.collectAsState()

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            medicineViewModel.setCurrentUser(user.userId)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopBar(
            navController = navController,
            currentUser = currentUser,
            title = "Medicines",
            actions = {} // Add empty actions
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            MedicinesContent(
                medicines = medicines,
                onMedicineClick = { medicine ->
                    navController.navigate("edit_med/${medicine.medicineId}")
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun MedicinesContent(
    medicines: List<Medicine>,
    onMedicineClick: (Medicine) -> Unit,
    modifier: Modifier = Modifier
) {
    if (medicines.isEmpty()) {
        EmptyMedicinesState()
    } else {
        LazyColumn(
            modifier = modifier
        ) {
            items(medicines) { medicine ->
                MedicineItem(
                    medicine = medicine,
                    onClick = { onMedicineClick(medicine) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineItem(
    medicine: Medicine,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick
    ) {
        androidx.compose.material3.ListItem(
            headlineContent = {
                Text(
                    text = medicine.medicationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
            },
            supportingContent = {
                Text(
                    text = formatDosageInfo(medicine),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
            },
            leadingContent = {
                MedicineImage(
                    imageUri = medicine.imageUri,
                    modifier = Modifier.size(48.dp)
                )
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Edit medicine",
                    tint = Color(0xFF94A3B8)
                )
            },
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
fun MedicineImage(
    imageUri: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Color(0xFFDBEAFE),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp) // Changed to rounded square
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUri.isNullOrEmpty()) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(imageUri))
                        .crossfade(true)
                        .build()
                ),
                contentDescription = "Medicine image",
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color(0xFFDBEAFE),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp) // Rounded square
                    ),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Medication,
                contentDescription = "Default medicine icon",
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun EmptyMedicinesState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    Color(0xFFE5E7EB),
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Medication,
                contentDescription = "No medications",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No medications yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "It looks like you haven't added any medications. Get started by tapping the plus button below.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

private fun formatDosageInfo(medicine: Medicine): String {
    val dosageText = "${medicine.dosage} ${medicine.dosageUnit.displayName.toLowerCase()}"
    val timesText = if (medicine.timesPerDay == 1) "1 time a day" else "${medicine.timesPerDay} times a day"
    return "$dosageText, $timesText"
}

private val DosageUnit.displayName: String
    get() = when (this) {
        DosageUnit.MG -> "mg"
        DosageUnit.MCG -> "mcg"
        DosageUnit.ML -> "ml"
        DosageUnit.DROP -> "drop"
        DosageUnit.TABLET -> "tablet"
        DosageUnit.CAPSULE -> "capsule"
    }

@Preview(showBackground = true)
@Composable
fun MedicinesScreenPreview() {
    DosezyTheme {
        MedicinesScreen(navController = androidx.navigation.compose.rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun MedicineItemPreview() {
    DosezyTheme {
        val sampleMedicine = Medicine(
            medicineId = "1",
            userId = "user1",
            medicationName = "Metformin",
            dosage = 1.0,
            dosageUnit = DosageUnit.TABLET,
            timesPerDay = 2,
            frequency = com.example.dosezy.data.model.Frequency(
                pattern = com.example.dosezy.data.model.FrequencyPattern.DAILY
            ),
            scheduledTimes = listOf()
        )

        MedicineItem(
            medicine = sampleMedicine,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyMedicinesStatePreview() {
    DosezyTheme {
        EmptyMedicinesState()
    }
}