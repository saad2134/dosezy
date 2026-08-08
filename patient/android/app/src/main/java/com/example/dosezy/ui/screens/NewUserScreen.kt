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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ManageHistory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dosezy.R
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.User
import com.example.dosezy.data.model.getLocalizedName
import com.example.dosezy.ui.components.ProfilePicturePicker
import com.example.dosezy.ui.theme.LightBlue40
import com.example.dosezy.ui.viewmodels.UserViewModel
import java.io.File

@Composable
fun NewUserScreen(
    navController: NavController,
    currentFrame: Int,
    onNext: (Int) -> Unit,
    onSkip: () -> Unit,
    isCreatingNewProfile: Boolean = false,
    userViewModel: UserViewModel = hiltViewModel()
) {
    val users by userViewModel.users.collectAsState()
    val currentUser by userViewModel.currentUser.collectAsState()

    var isProfileSetupValid by remember { mutableStateOf(false) }
    var completeProfileSetup by remember { mutableStateOf({}) }
    var showExistingProfiles by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NewUserBottomBar(
                currentFrame = currentFrame,
                isCreatingNewProfile = isCreatingNewProfile,
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
                onSkip = onSkip,
                onShowExistingProfiles = { showExistingProfiles = true },
                isProfileSetupValid = isProfileSetupValid,
                onCompleteProfileSetup = {
                    completeProfileSetup()
                    // Handle navigation immediately after completion
                    if (isCreatingNewProfile) {
                        navController.popBackStack("switch_profile", false)
                    } else {
                        navController.navigate("home") {
                            popUpTo("newuser/1") { inclusive = true }
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentFrame) {
                1 -> WelcomePage(
                    isCreatingNewProfile = isCreatingNewProfile,
                    existingUsers = users,
                    onSelectUser = { user ->
                        userViewModel.setCurrentUser(user)
                    },
                    onCreateNewProfile = { onNext(2) }
                )
                2 -> FeaturesPage()
                3 -> ProfileSetupPage(
                    onComplete = { user ->
                        userViewModel.addUser(user)
                        // User is added to database, navigation handled by bottom bar
                    },
                    onValidationChange = { isValid ->
                        isProfileSetupValid = isValid
                    },
                    setCompleteAction = { action ->
                        completeProfileSetup = action
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
        }
    }
}

@Composable
fun WelcomePage(
    isCreatingNewProfile: Boolean,
    existingUsers: List<User>,
    onSelectUser: (User) -> Unit,
    onCreateNewProfile: () -> Unit
) {
    var showExistingProfiles by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo/Image
        Image(
            painter = painterResource(id = R.drawable.loader_icon),
            contentDescription = "Dosezy Logo",
            modifier = Modifier.size(240.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Welcome Text
        Text(
            text = stringResource(R.string.setup_welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = stringResource(R.string.setup_welcome_sub),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Progress Indicators
        Row(
            modifier = Modifier.padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            ProgressDot(active = true)
            ProgressDot(active = false)
            ProgressDot(active = false)
        }
    }
}

@Composable
fun FeaturesPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Progress Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            ProgressDot(active = false)
            ProgressDot(active = true)
            ProgressDot(active = false)
        }

        // Header
        Text(
            text = stringResource(R.string.setup_features_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.setup_features_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Features List
        FeatureItem(
            icon = Icons.Filled.Notifications,
            title = stringResource(R.string.setup_feat1_title),
            description = stringResource(R.string.setup_feat1_desc)
        )

        Spacer(modifier = Modifier.height(24.dp))

        FeatureItem(
            icon = Icons.Filled.ManageHistory,
            title = stringResource(R.string.setup_feat2_title),
            description = stringResource(R.string.setup_feat2_desc)
        )

        Spacer(modifier = Modifier.height(24.dp))

        FeatureItem(
            icon = Icons.Filled.Info,
            title = stringResource(R.string.setup_feat3_title),
            description = stringResource(R.string.setup_feat3_desc)
        )
    }
}

@Composable
fun ProfileSetupPage(
    onComplete: (User) -> Unit,
    onValidationChange: (Boolean) -> Unit = {},
    setCompleteAction: (() -> Unit) -> Unit = {}
) {
    var fullName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<Gender?>(null) }
    var contactNumber by remember { mutableStateOf("") }
    var profilePicPath by remember { mutableStateOf<String?>(null) }
    var showGenderDropdown by remember { mutableStateOf(false) }

    // Validation states
    var fullNameError by remember { mutableStateOf(false) }
    var ageError by remember { mutableStateOf(false) }
    var genderError by remember { mutableStateOf(false) }

    // Check if form is valid
    val isFormValid = remember(fullName, age, gender) {
        fullName.isNotBlank() && age.isNotBlank() && gender != null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Progress Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            ProgressDot(active = false)
            ProgressDot(active = false)
            ProgressDot(active = true)
        }

        // Header
        Text(
            text = stringResource(R.string.setup_profile_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.setup_profile_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Profile Picture with upload functionality
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            ProfilePicturePicker(
                profilePicPath = profilePicPath,
                onProfilePictureSelected = { path ->
                    profilePicPath = path
                },
                modifier = Modifier.size(128.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (profilePicPath != null) stringResource(R.string.profile_change_pic) else stringResource(R.string.profile_upload_pic),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Form Fields with validation
        // Name Field
        Column {
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    fullNameError = it.isBlank()
                },
                label = { Text(stringResource(R.string.profile_full_name) + " *") },
                placeholder = { Text(stringResource(R.string.profile_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (fullNameError) Color.Red else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (fullNameError) Color.Red else MaterialTheme.colorScheme.outline,
                    errorBorderColor = Color.Red
                ),
                isError = fullNameError
            )
            if (fullNameError) {
                Text(
                    text = stringResource(R.string.profile_name_required),
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Age and Gender Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Age Field
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = age,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() } && it.length <= 3) {
                            age = it
                            ageError = it.isBlank()
                        }
                    },
                    label = { Text(stringResource(R.string.profile_age) + " *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (ageError) Color.Red else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (ageError) Color.Red else MaterialTheme.colorScheme.outline
                    ),
                    isError = ageError
                )
                if (ageError) {
                    Text(
                        text = stringResource(R.string.profile_age_required),
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            // Gender Field
            Column(modifier = Modifier.weight(1f)) {
                GenderDropdown(
                    selectedGender = gender,
                    onGenderSelected = {
                        gender = it
                        genderError = false
                    },
                    expanded = showGenderDropdown,
                    onExpandedChange = { showGenderDropdown = it },
                    modifier = Modifier.fillMaxWidth(),
                    isError = genderError
                )
                if (genderError) {
                    Text(
                        text = stringResource(R.string.profile_gender_required),
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contact Number Field (Optional)
        Column {
            OutlinedTextField(
                value = contactNumber,
                onValueChange = { contactNumber = it },
                label = { Text(stringResource(R.string.profile_contact_number)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }

    // Validation and completion logic
    LaunchedEffect(Unit) {
        // Reset errors when component first loads
        fullNameError = false
        ageError = false
        genderError = false
    }

    // Notify parent about validation state
    LaunchedEffect(isFormValid) {
        onValidationChange(isFormValid)
    }

    val completeSetup: () -> Unit = {
        // Validate all fields
        fullNameError = fullName.isBlank()
        ageError = age.isBlank()
        genderError = gender == null

        if (!fullNameError && !ageError && !genderError) {
            val user = User(
                profilePicPath = profilePicPath,
                fullName = fullName.trim(),
                age = age.toIntOrNull() ?: 0,
                gender = gender!!,
                contactNumber = contactNumber.trim(),
                isCurrentUser = true
            )
            onComplete(user)
        }
    }

    // Set the complete action for the bottom bar
    LaunchedEffect(Unit) {
        setCompleteAction(completeSetup)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderDropdown(
    selectedGender: Gender?,
    onGenderSelected: (Gender) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedGender?.getLocalizedName() ?: stringResource(R.string.profile_gender),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text(stringResource(R.string.profile_gender) + " *") },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isError) Color.Red else MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (isError) Color.Red else MaterialTheme.colorScheme.outline,
                errorBorderColor = Color.Red
            ),
            isError = isError
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            Gender.entries.forEach { gender ->
                DropdownMenuItem(
                    text = { Text(gender.getLocalizedName()) },
                    onClick = {
                        onGenderSelected(gender)
                        onExpandedChange(false)
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }
}

@Composable
fun FeatureItem(icon: ImageVector, title: String, description: String) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val iconBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val iconTint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ProgressDot(active: Boolean) {
    Box(
        modifier = Modifier
            .height(12.dp)
            .width(if (active) 32.dp else 12.dp)
            .clip(CircleShape)
            .background(
                if (active) LightBlue40
                else Color.Gray.copy(alpha = 0.3f)
            )
    )
}

@Composable
fun NewUserBottomBar(
    currentFrame: Int,
    isCreatingNewProfile: Boolean,
    existingUsers: List<User>,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onShowExistingProfiles: () -> Unit,
    isProfileSetupValid: Boolean = true,
    onCompleteProfileSetup: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            when (currentFrame) {
                1 -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Get Started button
                        Button(
                            onClick = onNext,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.setup_get_started),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val showLeftButton = existingUsers.isNotEmpty()

                        if (showLeftButton) {
                            Button(
                                onClick = onBack,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.cancel),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                2 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Frame 2: Back and Next buttons (both always visible)
                        Button(
                            onClick = onBack,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF1F5F9),
                                contentColor = Color(0xFF475569)
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onNext,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.setup_next),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                3 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Frame 3: Back and Complete buttons (both always visible)
                        Button(
                            onClick = onBack,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF1F5F9),
                                contentColor = Color(0xFF475569)
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (isProfileSetupValid) {
                                    onCompleteProfileSetup()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isProfileSetupValid) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color(0xFF9CA3AF)
                                }
                            ),
                            enabled = isProfileSetupValid
                        ) {
                            Text(
                                text = stringResource(R.string.save),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExistingProfilesModal(
    users: List<User>,
    onSelectUser: (User) -> Unit,
    onClose: () -> Unit,
    navController: NavController // Add navController parameter
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Existing Profiles",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Profiles List
                users.forEach { user ->
                    ProfileItem(
                        user = user,
                        onClick = {
                            onSelectUser(user)
                            navController.navigate("home") {
                                popUpTo("newuser/1") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Close Button
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun ProfileItem(user: User, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = Color(0xFFF8FAFC)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile picture with fallback
                if (!user.profilePicPath.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(user.profilePicPath))
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp)),
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
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${user.age} years • ${user.gender.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Select Profile",
                tint = Color(0xFF9CA3AF)
            )
        }
    }
}

@Preview(name = "Welcome Page")
@Composable
fun PreviewWelcomePage() {
    MaterialTheme {
        WelcomePage(
            isCreatingNewProfile = false,
            existingUsers = listOf(
                User(
                    userId = "1",
                    fullName = "John Doe",
                    age = 30,
                    gender = Gender.MALE,
                    contactNumber = "+1234567890",
                    isCurrentUser = true
                ),
                User(
                    userId = "2",
                    fullName = "Jane Smith",
                    age = 25,
                    gender = Gender.FEMALE,
                    contactNumber = "+0987654321",
                    isCurrentUser = false
                )
            ),
            onSelectUser = {},
            onCreateNewProfile = {}
        )
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

@Preview(name = "New User Screen - Frame 1")
@Composable
fun PreviewNewUserScreenFrame1() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            WelcomePage(
                isCreatingNewProfile = false,
                existingUsers = emptyList(),
                onSelectUser = {},
                onCreateNewProfile = {}
            )
        }
    }
}

@Preview(name = "New User Screen - Frame 2")
@Composable
fun PreviewNewUserScreenFrame2() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            FeaturesPage()
        }
    }
}

@Preview(name = "New User Screen - Frame 3")
@Composable
fun PreviewNewUserScreenFrame3() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            ProfileSetupPage(onComplete = {})
        }
    }
}