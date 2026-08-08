package com.example.dosezy.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.example.dosezy.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dosezy.data.export.DataExporter
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.User
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.viewmodels.UserViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MenuScreen(navController: NavController) {
    val userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
    val users by userViewModel.users.collectAsState()
    val currentUser by userViewModel.currentUser.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }
    var showExportProgress by remember { mutableStateOf(false) }
    var showExportScopeDialog by remember { mutableStateOf(false) }
    var exportFile by remember { mutableStateOf<File?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Initialize DataExporter using the repositories from UserViewModel
    val dataExporter = remember {
        DataExporter(
            context = context,
            userRepository = userViewModel.userRepository,
            medicineRepository = userViewModel.medicineRepository,
            scheduleRepository = userViewModel.scheduleRepository
        )
    }

    fun startExport(exportAll: Boolean) {
        showExportProgress = true
        exportError = null
        coroutineScope.launch {
            try {
                exportFile = if (exportAll) {
                    dataExporter.exportAllUsersToCsv()
                } else {
                    currentUser?.userId?.let { uid ->
                        dataExporter.exportUserToCsv(uid)
                    } ?: dataExporter.exportAllUsersToCsv()
                }
                showExportProgress = false
                showExportDialog = true
            } catch (e: Exception) {
                exportError = e.message ?: "Export failed"
                showExportProgress = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopBar(
            navController = navController,
            currentUser = currentUser,
            title = androidx.compose.ui.res.stringResource(R.string.menu_title),
            showBackButton = false,
            actions = {}
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Profile Card
            item {
                ProfileCard(
                    currentUser = currentUser,
                    onManageProfile = { navController.navigate("manage_profile") },
                    onSwitchProfile = { navController.navigate("switch_profile") }
                )
            }

            // Preferences
            item {
                MenuItem(
                    icon = Icons.Default.Tune,
                    title = androidx.compose.ui.res.stringResource(R.string.menu_preferences),
                    color = MaterialTheme.colorScheme.onSurface,
                    onClick = { navController.navigate("preferences") }
                )
            }

            // Emergency
            item {
                MenuItem(
                    icon = Icons.Default.Emergency,
                    title = androidx.compose.ui.res.stringResource(R.string.menu_emergency),
                    color = Color(0xFFEF4444),
                    onClick = { navController.navigate("emergency") }
                )
            }

            // Help and Support
            item {
                MenuItem(
                    icon = Icons.Default.HelpOutline,
                    title = androidx.compose.ui.res.stringResource(R.string.menu_help_support),
                    color = MaterialTheme.colorScheme.onSurface,
                    onClick = { navController.navigate("help_support") }
                )
            }

            // Data Section Header
            item {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.menu_data),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Export Data
            item {
                MenuItem(
                    icon = Icons.Default.Download,
                    title = androidx.compose.ui.res.stringResource(R.string.menu_export_data),
                    color = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        if (users.size > 1) {
                            showExportScopeDialog = true
                        } else {
                            startExport(exportAll = false)
                        }
                    }
                )
            }
        }
    }

    // Export Scope Selection Dialog (Current Profile vs All Profiles)
    if (showExportScopeDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExportScopeDialog = false },
            tonalElevation = 0.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.export_choose_title), style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.export_choose_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            showExportScopeDialog = false
                            startExport(exportAll = false)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("${stringResource(R.string.export_current_profile)} (${currentUser?.fullName ?: ""})")
                    }

                    OutlinedButton(
                        onClick = {
                            showExportScopeDialog = false
                            startExport(exportAll = true)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("${stringResource(R.string.export_all_profiles)} (${users.size})")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showExportScopeDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Export Progress Dialog
    if (showExportProgress) {
        Dialog(onDismissRequest = { showExportProgress = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color(0xFF2084E4)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.export_progress_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.export_progress_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Export Success Dialog
    if (showExportDialog) {
        ExportDataDialog(
            onDismiss = { showExportDialog = false },
            onShare = {
                exportFile?.let { file ->
                    dataExporter.shareFile(file)
                }
                showExportDialog = false
            }
        )
    }

    // Export Error Dialog
    exportError?.let { error ->
        Dialog(onDismissRequest = { exportError = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Error",
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFFEE2E2), RoundedCornerShape(24.dp))
                            .padding(12.dp),
                        tint = Color(0xFFDC2626)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Export Failed",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { exportError = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2084E4)
                        )
                    ) {
                        Text(
                            text = "OK",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCard(
    currentUser: User?,
    onManageProfile: () -> Unit,
    onSwitchProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Profile row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileImage(
                    currentUser = currentUser,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = currentUser?.fullName ?: "User",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Manage Profile Button
                ProfileActionButton(
                    icon = Icons.Default.AccountCircle,
                    text = stringResource(R.string.manage),
                    onClick = onManageProfile,
                    modifier = Modifier.weight(1f)
                )

                // Switch Profile Button
                ProfileActionButton(
                    icon = Icons.Default.SwitchAccount,
                    text = stringResource(R.string.profile_switch),
                    onClick = onSwitchProfile,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ProfileImage(
    currentUser: User?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
    ) {
        if (currentUser?.profilePicPath != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(currentUser.profilePicPath))
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp)),
                placeholder = painterResource(id = R.drawable.default_profile),
                error = painterResource(id = R.drawable.default_profile),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.default_profile),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun ProfileActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(24.dp),
                tint = Color(0xFF2084E4)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MenuItem(
    icon: ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(32.dp),
                    tint = color
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = color,
                    fontSize = 18.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ExportDataDialog(
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFFDBEAFE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Success",
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFF2084E4)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = stringResource(R.string.export_success_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = "${stringResource(R.string.export_success_desc)} 'Android/data/${context.packageName}/files'",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Share Button
                Button(
                    onClick = onShare,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
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
                        text = stringResource(R.string.export_share_file),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Close Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF6B7280)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.close),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun MenuScreenPreview() {
    MaterialTheme {
        MenuScreen(navController = rememberNavController())
    }
}

@Preview
@Composable
fun ProfileCardPreview() {
    MaterialTheme {
        ProfileCard(
            currentUser = User(
                userId = "1",
                fullName = "John Doe",
                age = 30,
                gender = Gender.MALE,
                profilePicPath = null,
                contactNumber = "+1234567890",
                isCurrentUser = true
            ),
            onManageProfile = {},
            onSwitchProfile = {}
        )
    }
}

@Preview
@Composable
fun MenuItemPreview() {
    MaterialTheme {
        MenuItem(
            icon = Icons.Default.Tune,
            title = "Preferences (Theme, etc)",
            color = MaterialTheme.colorScheme.onSurface,
            onClick = {}
        )
    }
}