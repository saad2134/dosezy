/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dosezy.R
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.export.BackupRestoreManager
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.ui.components.ImportSourceDialog
import com.example.dosezy.ui.components.ProfileImportDialog
import com.example.dosezy.ui.screens.newuser.ExistingProfilesModal
import com.example.dosezy.ui.screens.newuser.FeaturesPage
import com.example.dosezy.ui.screens.newuser.NewUserBottomBar
import com.example.dosezy.ui.screens.newuser.ProfileSetupPage
import com.example.dosezy.ui.screens.newuser.WelcomePage
import com.example.dosezy.ui.viewmodels.NewUserViewModel
import com.example.dosezy.ui.viewmodels.UserViewModel
import com.example.dosezy.utils.sharedUserViewModel

/**
 * Screen coordinating the multi-step user onboarding and profile creation flow.
 * Refactored to delegate state management to NewUserViewModel and modular step components.
 */
@Composable
fun NewUserScreen(
    navController: NavController,
    currentFrame: Int,
    onNext: (Int) -> Unit,
    @Suppress("UNUSED_PARAMETER") onSkip: () -> Unit = {},
    isCreatingNewProfile: Boolean = false,
    // Guard: Use activity-scoped sharedUserViewModel() so onboarding and profile creation mutations sync immediately across the entire app
    userViewModel: UserViewModel = sharedUserViewModel(),
    newUserViewModel: NewUserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val users by userViewModel.users.collectAsState()

    val database = remember { DosezyDatabase.getInstance(context) }
    val scheduleRepository = remember { ScheduleRepository(database) }
    val backupManager = remember { BackupRestoreManager(context, database, scheduleRepository) }

    val isProfileSetupValid by newUserViewModel.isProfileValid.collectAsState()
    val isImporting by newUserViewModel.isImporting.collectAsState()
    val loadingMessage by newUserViewModel.loadingMessage.collectAsState()
    val inspectionResult by newUserViewModel.inspectionResult.collectAsState()

    var completeProfileSetup by remember { mutableStateOf({}) }
    var showExistingProfiles by remember { mutableStateOf(false) }
    var showImportSourceDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            newUserViewModel.inspectBackup(
                uri = uri,
                backupManager = backupManager,
                inspectingMessage = context.getString(R.string.backup_status_inspecting)
            ) { inspect ->
                if (!inspect.success || inspect.profiles.isEmpty()) {
                    Toast.makeText(
                        context,
                        inspect.message.ifEmpty { context.getString(R.string.backup_status_no_profiles) },
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NewUserBottomBar(
                currentFrame = currentFrame,
                existingUsers = users,
                onBack = {
                    if (currentFrame > 1) {
                        navController.popBackStack()
                    } else {
                        if (users.isNotEmpty()) {
                            navController.navigate("switch_profile") {
                                popUpTo(navController.currentBackStackEntry?.destination?.route ?: "newuser/1") { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                },
                onNext = { onNext(currentFrame + 1) },
                isProfileSetupValid = isProfileSetupValid,
                onCompleteProfileSetup = {
                    if (isProfileSetupValid) {
                        completeProfileSetup()
                        if (isCreatingNewProfile) {
                            navController.popBackStack("switch_profile", false)
                        } else {
                            navController.navigate("home") {
                                popUpTo("newuser/1") { inclusive = true }
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentFrame) {
                1 -> WelcomePage(
                    onImportBackup = { showImportSourceDialog = true }
                )
                2 -> FeaturesPage()
                3 -> ProfileSetupPage(
                    isDefaultUser = !isCreatingNewProfile && (users.isEmpty() || users.none { it.isCurrentUser }),
                    onComplete = { user ->
                        newUserViewModel.saveUser(user) {
                            userViewModel.addUser(user)
                        }
                    },
                    onValidationChange = { isValid ->
                        newUserViewModel.setProfileValid(isValid)
                    },
                    setCompleteAction = { action ->
                        completeProfileSetup = action
                    }
                )
            }

            if (showImportSourceDialog) {
                ImportSourceDialog(
                    onDismiss = { showImportSourceDialog = false },
                    onSelectLocalFile = {
                        importLauncher.launch(
                            arrayOf(
                                "application/zip",
                                "application/x-zip-compressed",
                                "application/octet-stream",
                                "*/*"
                            )
                        )
                    }
                )
            }

            if (inspectionResult != null) {
                ProfileImportDialog(
                    inspectionResult = inspectionResult!!,
                    onDismiss = {
                        inspectionResult?.tempDir?.deleteRecursively()
                        newUserViewModel.setInspectionResult(null)
                    },
                    onConfirmImport = { decisions ->
                        val tempDir = inspectionResult!!.tempDir!!
                        newUserViewModel.setInspectionResult(null)
                        newUserViewModel.executeRestore(
                            tempDir = tempDir,
                            decisions = decisions,
                            backupManager = backupManager,
                            restoringMessage = context.getString(R.string.backup_status_restoring)
                        ) { success, message, targetUser ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            if (success && targetUser != null) {
                                userViewModel.setCurrentUser(targetUser)
                                navController.navigate("home") {
                                    popUpTo("newuser/1") { inclusive = true }
                                }
                            }
                        }
                    }
                )
            }

            if (showExistingProfiles) {
                ExistingProfilesModal(
                    users = users,
                    onSelectUser = { user ->
                        userViewModel.setCurrentUser(user)
                    },
                    onClose = { showExistingProfiles = false },
                    navController = navController
                )
            }

            // In-Front Modal Loading Dialog to eliminate awkward pause / freeze perception
            if (isImporting) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = {},
                    properties = androidx.compose.ui.window.DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false
                    )
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF1193D4),
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = loadingMessage ?: stringResource(R.string.backup_status_inspecting),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Backward-compatible composable delegates & previews
// -------------------------------------------------------------------------------------------------

@Composable
fun WelcomePage(onImportBackup: () -> Unit = {}) =
    com.example.dosezy.ui.screens.newuser.WelcomePage(onImportBackup)

@Composable
fun FeaturesPage() =
    com.example.dosezy.ui.screens.newuser.FeaturesPage()

@Composable
fun ProfileSetupPage(
    onComplete: (User) -> Unit,
    isDefaultUser: Boolean = false,
    onValidationChange: (Boolean) -> Unit = {},
    setCompleteAction: (() -> Unit) -> Unit = {}
) = com.example.dosezy.ui.screens.newuser.ProfileSetupPage(
    onComplete = onComplete,
    isDefaultUser = isDefaultUser,
    onValidationChange = onValidationChange,
    setCompleteAction = setCompleteAction
)

@Composable
fun NewUserBottomBar(
    currentFrame: Int,
    existingUsers: List<User>,
    onBack: () -> Unit,
    onNext: () -> Unit,
    isProfileSetupValid: Boolean = true,
    onCompleteProfileSetup: () -> Unit = {}
) = com.example.dosezy.ui.screens.newuser.NewUserBottomBar(
    currentFrame = currentFrame,
    existingUsers = existingUsers,
    onBack = onBack,
    onNext = onNext,
    isProfileSetupValid = isProfileSetupValid,
    onCompleteProfileSetup = onCompleteProfileSetup
)

@Composable
fun ExistingProfilesModal(
    users: List<User>,
    onSelectUser: (User) -> Unit,
    onClose: () -> Unit,
    navController: NavController
) = com.example.dosezy.ui.screens.newuser.ExistingProfilesModal(
    users = users,
    onSelectUser = onSelectUser,
    onClose = onClose,
    navController = navController
)

@Composable
fun ProfileItem(user: User, onClick: () -> Unit, modifier: Modifier = Modifier) =
    com.example.dosezy.ui.screens.newuser.ProfileItem(user = user, onClick = onClick, modifier = modifier)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderDropdown(
    selectedGender: Gender?,
    onGenderSelected: (Gender) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) = com.example.dosezy.ui.screens.newuser.GenderDropdown(
    selectedGender = selectedGender,
    onGenderSelected = onGenderSelected,
    expanded = expanded,
    onExpandedChange = onExpandedChange,
    modifier = modifier,
    isError = isError
)

@Composable
fun FeatureItem(icon: ImageVector, title: String, description: String) =
    com.example.dosezy.ui.screens.newuser.FeatureItem(icon = icon, title = title, description = description)

@Composable
fun ProgressDot(active: Boolean) =
    com.example.dosezy.ui.screens.newuser.ProgressDot(active = active)

@Preview(name = "Welcome Page")
@Composable
fun PreviewWelcomePage() {
    MaterialTheme {
        WelcomePage()
    }
}

@Preview(name = "Features Page")
@Composable
fun PreviewFeaturesPage() {
    MaterialTheme {
        FeaturesPage()
    }
}

@Preview(name = "Profile Setup Page")
@Composable
fun PreviewProfileSetupPage() {
    MaterialTheme {
        ProfileSetupPage(onComplete = {})
    }
}