package com.example.dosezy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File
import androidx.compose.foundation.clickable

enum class ExportFormat(
    val displayName: String,
    val fileExtension: String
) {
    CSV("CSV Format", ".csv"),
    JSON("JSON Format", ".json"),
    PDF("PDF Format", ".pdf")
}

enum class SaveLocation(
    val displayName: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    DEVICE_STORAGE(
        "Device Storage",
        "Save to your device's Downloads folder",
        Icons.Default.Folder
    ),
    CLOUD_STORAGE(
        "Cloud Storage",
        "Save to Google Drive or other cloud services",
        Icons.Default.CloudDownload
    ),
    SHARE_DIRECTLY(
        "Share Directly",
        "Share immediately without saving",
        Icons.Default.Share
    )
}


@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExportToDevice: (ExportFormat) -> Unit,
    onExportToCloud: () -> Unit,
    onShareDirectly: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var saveLocation by remember { mutableStateOf(SaveLocation.DEVICE_STORAGE) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Export",
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(28.dp),
                        tint = Color(0xFF2084E4)
                    )
                    Text(
                        text = "Export Data",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Format Selection
                Text(
                    text = "Export Format",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column {
                    ExportFormat.entries.forEach { format ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedFormat = format },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedFormat == format,
                                onClick = { selectedFormat = format }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = format.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF374151)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = format.fileExtension,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save Location Selection
                Text(
                    text = "Save To",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column {
                    SaveLocation.entries.forEach { location ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { saveLocation = location },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (saveLocation == location) {
                                    Color(0xFFEFF6FF)
                                } else {
                                    Color(0xFFF9FAFB)
                                }
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (saveLocation == location) 1.dp else 0.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = location.icon,
                                    contentDescription = location.displayName,
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .size(24.dp),
                                    tint = if (saveLocation == location) {
                                        Color(0xFF2084E4)
                                    } else {
                                        Color(0xFF6B7280)
                                    }
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = location.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (saveLocation == location) {
                                            Color(0xFF1E293B)
                                        } else {
                                            Color(0xFF374151)
                                        }
                                    )
                                    Text(
                                        text = location.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            when (saveLocation) {
                                SaveLocation.DEVICE_STORAGE -> onExportToDevice(selectedFormat)
                                SaveLocation.CLOUD_STORAGE -> onExportToCloud()
                                SaveLocation.SHARE_DIRECTLY -> onShareDirectly()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2084E4)
                        )
                    ) {
                        Text(
                            text = "Export",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExportSuccessDialog(
    exportedFile: File? = null,
    onDismiss: () -> Unit,
    onShare: (File) -> Unit,
    onViewInFolder: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Icon
                Card(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFDBEAFE)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Success",
                        modifier = Modifier
                            .padding(16.dp)
                            .size(32.dp),
                        tint = Color(0xFF2084E4)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "Export Successful",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = exportedFile?.let {
                        "Your data has been exported to:\n${it.name}"
                    } ?: "Your data has been successfully exported.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    exportedFile?.let { file ->
                        Button(
                            onClick = { onShare(file) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2084E4)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Share File",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { onViewInFolder(file) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF6B7280)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "View in Folder",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "View in Folder",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF6B7280)
                        )
                    ) {
                        Text(
                            text = "Close",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

