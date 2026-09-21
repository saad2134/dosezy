/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.screens

import androidx.compose.ui.graphics.luminance

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.draw.alpha

@Composable
fun MedicinesScreen(
    navController: NavController,
    userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel(),
    medicineViewModel: MedicineViewModel = com.example.dosezy.utils.sharedMedicineViewModel()
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val medicines by medicineViewModel.medicines.collectAsState()
    val archivedMedicines by medicineViewModel.archivedMedicines.collectAsState()
    val isLoading by medicineViewModel.isLoading.collectAsState()

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            medicineViewModel.setCurrentUser(user.userId)
        }
    }


    var medicineToRefill by remember { mutableStateOf<Medicine?>(null) }
    var medicineToPermanentlyDelete by remember { mutableStateOf<Medicine?>(null) }

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
                title = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.medicines_title)
            )

            if (isLoading && medicines.isEmpty() && archivedMedicines.isEmpty()) {
                com.example.dosezy.ui.components.MedicineListSkeleton(count = 4)
            } else {
                MedicinesContent(
                    medicines = medicines,
                    archivedMedicines = archivedMedicines,
                    onMedicineClick = { medicine ->
                        navController.navigate("edit_med/${medicine.medicineId}")
                    },
                    onRefillClick = { medicine ->
                        medicineToRefill = medicine
                    },
                    onReactivateClick = { medicine ->
                        medicineViewModel.unarchiveMedicine(medicine)
                    },
                    onPermanentDeleteClick = { medicine ->
                        medicineToPermanentlyDelete = medicine
                    },
                    onAddMedicineClick = {
                        navController.navigate("add_med")
                    },
                    currentUser = currentUser,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Quick Refill Dialog
    val medToRefill = medicineToRefill
    if (medToRefill != null) {
        com.example.dosezy.ui.components.QuickRefillDialog(
            medicine = medToRefill,
            onConfirmRefill = { added ->
                val newStock = (medToRefill.currentStock ?: 0) + added
                medicineViewModel.updateMedicine(medToRefill.copy(currentStock = newStock))
                medicineToRefill = null
            },
            onDismiss = { medicineToRefill = null }
        )
    }

    // Permanent Delete Confirmation Dialog
    val medToDelete = medicineToPermanentlyDelete
    if (medToDelete != null) {
        var deleteCountdown by remember(medToDelete) { mutableStateOf(5) }
        LaunchedEffect(medToDelete) {
            deleteCountdown = 5
            while (deleteCountdown > 0) {
                kotlinx.coroutines.delay(1000L)
                deleteCountdown--
            }
        }
        AlertDialog(
            onDismissRequest = { medicineToPermanentlyDelete = null },
            title = {
                Text(
                    text = stringResource(com.example.dosezy.R.string.dialog_delete_permanent_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(com.example.dosezy.R.string.dialog_delete_permanent_msg, medToDelete.medicationName))
            },
            confirmButton = {
                Button(
                    onClick = {
                        medicineViewModel.deleteMedicinePermanently(medToDelete)
                        medicineToPermanentlyDelete = null
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
                            "${stringResource(com.example.dosezy.R.string.btn_delete_permanently)} (${deleteCountdown}s)"
                        } else {
                            stringResource(com.example.dosezy.R.string.btn_delete_permanently)
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { medicineToPermanentlyDelete = null }) {
                    Text(stringResource(com.example.dosezy.R.string.cancel))
                }
            }
        )
    }


}

@Composable
fun MedicinesContent(
    medicines: List<Medicine>,
    archivedMedicines: List<Medicine> = emptyList(),
    onMedicineClick: (Medicine) -> Unit,
    onRefillClick: (Medicine) -> Unit,
    onReactivateClick: (Medicine) -> Unit = {},
    onPermanentDeleteClick: (Medicine) -> Unit = {},
    onAddMedicineClick: () -> Unit = {},
    currentUser: com.example.dosezy.data.model.User? = null,
    modifier: Modifier = Modifier
) {
    var isArchivedExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredMedicines = remember(medicines, searchQuery) {
        if (searchQuery.isBlank()) medicines
        else medicines.filter { it.medicationName.contains(searchQuery.trim(), ignoreCase = true) }
    }

    if (medicines.isEmpty() && archivedMedicines.isEmpty()) {
        EmptyMedicinesState(onAddMedicineClick = onAddMedicineClick)
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // Search bar for medications roster
            if (medicines.size >= 2 || searchQuery.isNotBlank()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_medicines_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.search_clear),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            if (filteredMedicines.isEmpty() && searchQuery.isNotBlank()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.search_no_results, searchQuery),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { searchQuery = "" }
                        ) {
                            Text(
                                text = stringResource(R.string.search_clear),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                items(filteredMedicines, key = { it.medicineId }) { medicine ->
                    MedicineItem(
                        medicine = medicine,
                        onClick = { onMedicineClick(medicine) },
                        onRefillClick = onRefillClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }

            // Primary Add Medicine button (when center navigation button is hidden)
            if (currentUser?.hideAddMedicineNavButton == true) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onAddMedicineClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2084E4)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.form_add_medicine),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                    if (archivedMedicines.isEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            // Discontinued / Archived Medications Section
            if (archivedMedicines.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isArchivedExpanded = !isArchivedExpanded },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Medication,
                                    contentDescription = null,
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "${stringResource(com.example.dosezy.R.string.discontinued_medications_section)} (${archivedMedicines.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = if (isArchivedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isArchivedExpanded) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    items(archivedMedicines, key = { "archived_${it.medicineId}" }) { medicine ->
                        ArchivedMedicineItem(
                            medicine = medicine,
                            onReactivate = { onReactivateClick(medicine) },
                            onDeletePermanently = { onPermanentDeleteClick(medicine) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun ArchivedMedicineItem(
    medicine: Medicine,
    onReactivate: () -> Unit,
    onDeletePermanently: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MedicineImage(
                imageUri = medicine.imageUri,
                pillShape = medicine.pillShape,
                pillColor = medicine.pillColor,
                modifier = Modifier
                    .size(50.dp)
                    .alpha(0.7f)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medicine.medicationName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
                Text(
                    text = medicine.getDosageDisplay(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!medicine.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📝 ${medicine.notes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF59E0B).copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Reactivate Button
            FilledTonalButton(
                onClick = onReactivate,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = stringResource(com.example.dosezy.R.string.btn_reactivate_med),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Permanent Delete Button
            IconButton(
                onClick = onDeletePermanently,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(com.example.dosezy.R.string.btn_delete_permanently),
                    tint = Color(0xFFDC2626).copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
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
    onRefillClick: ((Medicine) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 3.dp
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
                // Medicine Image in Squircle Frame with Pill Visual fallback
                MedicineImage(
                    imageUri = medicine.imageUri,
                    pillShape = medicine.pillShape,
                    pillColor = medicine.pillColor,
                    modifier = Modifier.size(54.dp)
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

                    // Notes / Warnings preview
                    if (!medicine.notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "📝 ${medicine.notes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF59E0B),
                            maxLines = 1,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Finite course end date indicator
                    if (medicine.endDate != null) {
                        val isFinished = java.time.LocalDate.now().isAfter(medicine.endDate)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isFinished) "🏁 ${stringResource(R.string.course_completed_badge)}" else "📅 ${stringResource(R.string.course_end_date_label, medicine.endDate.toString())}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isFinished) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    val stock = medicine.currentStock
                    if (stock != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
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
                                    text = if (isLow) {
                                        stringResource(R.string.med_stock_refill_warning_badge, stock)
                                    } else {
                                        stringResource(R.string.med_stock_badge, stock)
                                    },
                                    color = contentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            if (onRefillClick != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { onRefillClick(medicine) }
                                ) {
                                    Text(
                                        text = stringResource(R.string.btn_quick_refill_action),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Chevron Pill
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1193D4).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = stringResource(R.string.action_edit_medicine_cd),
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
    pillShape: com.example.dosezy.data.model.PillShape? = null,
    pillColor: String? = null,
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
            val modelData = remember(imageUri) {
                val cleanPath = imageUri.removePrefix("file://")
                val file = java.io.File(cleanPath)
                if (file.exists()) file else Uri.parse(imageUri)
            }
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(modelData)
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
        } else if (pillShape != null) {
            com.example.dosezy.ui.components.PillShapeVisual(
                shape = pillShape,
                colorHex = pillColor ?: "#1193D4",
                size = iconSize
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
fun EmptyMedicinesState(onAddMedicineClick: () -> Unit = {}) {
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

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddMedicineClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1193D4),
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.btn_add_first_medicine),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun formatDosageInfo(medicine: Medicine): String {
    val dosageText = medicine.getLocalizedDosageDisplay()
    if (medicine.frequency.pattern == com.example.dosezy.data.model.FrequencyPattern.AS_NEEDED) {
        return "$dosageText, ${stringResource(R.string.freq_as_needed)}"
    }
    val timesText = stringResource(R.string.times_per_day_format, medicine.timesPerDay)
    return "$dosageText, $timesText"
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