package com.example.dosezy.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.dosezy.data.export.ConflictStrategy
import com.example.dosezy.data.export.ProfileImportDecision
import com.example.dosezy.data.export.ZipInspectionResult
import java.io.File

@Composable
fun ProfileImportDialog(
    inspectionResult: ZipInspectionResult,
    onDismiss: () -> Unit,
    onConfirmImport: (List<ProfileImportDecision>) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val dialogBg = if (isDark) Color(0xFF1E2228) else MaterialTheme.colorScheme.surface
    val subCardBg = if (isDark) Color(0xFF15181E) else Color(0xFFF8FAFC)
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val borderStroke = if (isDark) Color(0xFF2D3748) else Color(0xFFE5E7EB)

    val selections = remember {
        mutableStateMapOf<String, Boolean>().apply {
            inspectionResult.profiles.forEach { put(it.originalUserId, true) }
        }
    }

    val strategies = remember {
        mutableStateMapOf<String, ConflictStrategy>().apply {
            inspectionResult.profiles.forEach { 
                put(it.originalUserId, if (it.isConflict) ConflictStrategy.CREATE_NEW else ConflictStrategy.CREATE_NEW) 
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = dialogBg,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Title
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.import_profile_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.import_profile_sub, inspectionResult.profiles.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Profiles List
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    for (profile in inspectionResult.profiles) {
                        val isSelected = selections[profile.originalUserId] ?: true
                        val strategy = strategies[profile.originalUserId] ?: ConflictStrategy.CREATE_NEW

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = subCardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderStroke)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selections[profile.originalUserId] = checked
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF1193D4)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Avatar Preview
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1193D4).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (profile.avatarTempPath != null && File(profile.avatarTempPath).exists()) {
                                            val bitmap = remember(profile.avatarTempPath) {
                                                android.graphics.BitmapFactory.decodeFile(profile.avatarTempPath)?.asImageBitmap()
                                            }
                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF1193D4))
                                            }
                                        } else {
                                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF1193D4))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = profile.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                        Text(
                                            text = "${profile.age} • ${profile.gender} • ${profile.medicineCount}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textSecondary
                                        )
                                    }
                                }

                                // Conflict Resolution Options
                                if (profile.isConflict && isSelected) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isDark) Color(0xFF380D0D) else Color(0xFFFEF2F2))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.import_profile_conflict_warn),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.import_profile_action),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        StrategyPill(
                                            label = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.import_strategy_create_new),
                                            isSelected = strategy == ConflictStrategy.CREATE_NEW,
                                            onClick = { strategies[profile.originalUserId] = ConflictStrategy.CREATE_NEW },
                                            activeColor = Color(0xFF10B981)
                                        )
                                        StrategyPill(
                                            label = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.import_profile_merge),
                                            isSelected = strategy == ConflictStrategy.MERGE,
                                            onClick = { strategies[profile.originalUserId] = ConflictStrategy.MERGE },
                                            activeColor = Color(0xFF3B82F6)
                                        )
                                        StrategyPill(
                                            label = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.import_profile_replace),
                                            isSelected = strategy == ConflictStrategy.OVERWRITE,
                                            onClick = { strategies[profile.originalUserId] = ConflictStrategy.OVERWRITE },
                                            activeColor = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.cancel))
                    }

                    val selectedCount = inspectionResult.profiles.count { selections[it.originalUserId] == true }
                    Button(
                        onClick = {
                            val decisions = inspectionResult.profiles.map { summary ->
                                ProfileImportDecision(
                                    summary = summary,
                                    isSelected = selections[summary.originalUserId] ?: true,
                                    conflictStrategy = strategies[summary.originalUserId] ?: ConflictStrategy.CREATE_NEW
                                )
                            }
                            onConfirmImport(decisions)
                        },
                        enabled = selectedCount > 0,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1193D4)
                        )
                    ) {
                        Text(
                            text = "${androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.import_profile_btn)} ($selectedCount)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StrategyPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    activeColor: Color
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bg = if (isSelected) activeColor.copy(alpha = 0.2f) else (if (isDark) Color(0xFF272A30) else Color(0xFFE5E7EB))
    val textCol = if (isSelected) activeColor else (if (isDark) Color(0xFF9CA3AF) else Color(0xFF4B5563))

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, if (isSelected) activeColor else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textCol
        )
    }
}
