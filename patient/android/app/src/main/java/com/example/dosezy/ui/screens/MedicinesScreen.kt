package com.example.dosezy.ui.screens

import androidx.compose.ui.graphics.luminance

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import com.example.dosezy.R
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.getLocalizedDosageDisplay
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.theme.DosezyTheme
import com.example.dosezy.ui.viewmodels.MedicineViewModel
import com.example.dosezy.ui.viewmodels.UserViewModel

@Composable
fun MedicinesScreen(
    navController: NavController,
    medicineViewModel: MedicineViewModel = com.example.dosezy.utils.sharedMedicineViewModel(),
    userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val medicines by medicineViewModel.medicines.collectAsState()
    val isLoading by medicineViewModel.isLoading.collectAsState()

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            medicineViewModel.setCurrentUser(user.userId)
        }
    }

    // light theme
    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            TopBar(
                navController = navController,
                currentUser = currentUser,
                title = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.medicines_title),
                actions = {}
            )

            if (isLoading && medicines.isEmpty()) {
                com.example.dosezy.ui.components.MedicineListSkeleton(count = 4)
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
            items(medicines, key = { it.medicineId }) { medicine ->
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Medicine Image in Squircle Frame (matching Home Page style)
                MedicineImage(
                    imageUri = medicine.imageUri,
                    modifier = Modifier.size(52.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medicine.medicationName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = formatDosageInfo(medicine),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val stock = medicine.currentStock
                    if (stock != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val isLow = medicine.refillThreshold != null && stock <= medicine.refillThreshold
                        val containerBg = if (isLow) {
                            if (isDark) Color(0xFF3F1313) else Color(0xFFFEE2E2)
                        } else {
                            if (isDark) Color(0xFF0F2D14) else Color(0xFFD1FAE5)
                        }
                        val contentColor = if (isLow) {
                            if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                        } else {
                            if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                        }
                        Surface(
                            color = containerBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isLow) "⚠️ Refill Warning: $stock left" else "📦 Stock: $stock",
                                color = contentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Chevron Pill (Matching Home Page item action button style)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1193D4).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Edit medicine",
                    tint = Color(0xFF1193D4),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun MedicineImage(
    imageUri: String?,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 36.dp
) {
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp) // Squircle shape
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
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Medication,
                contentDescription = "Default medicine icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun EmptyMedicinesState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Empty state icon
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Medication,
                contentDescription = "No medications",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = stringResource(R.string.medicines_empty),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.medicines_empty_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun formatDosageInfo(medicine: Medicine): String {
    val dosageText = medicine.getLocalizedDosageDisplay()
    val timesText = stringResource(R.string.times_per_day_format, medicine.timesPerDay)
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
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MedicinesScreen(navController = androidx.navigation.compose.rememberNavController())
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MedicineItemPreview() {
    DosezyTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
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
}

@Preview(showBackground = true)
@Composable
fun EmptyMedicinesStatePreview() {
    DosezyTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            EmptyMedicinesState()
        }
    }
}