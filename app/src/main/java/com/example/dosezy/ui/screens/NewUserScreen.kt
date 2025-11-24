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

    // Handle navigation when current user changes (for profile selection)
    LaunchedEffect(currentUser) {
        if (currentUser != null && showExistingProfiles) {
            // Profile was selected from the modal - navigate to home
            showExistingProfiles = false
            navController.navigate("home") {
                popUpTo("newuser/1") { inclusive = true }
            }
        }
    }

    Scaffold(
        bottomBar = {
            // Show bottom navigation bar for all frames
            NewUserBottomBar(
                currentFrame = currentFrame,
                isCreatingNewProfile = isCreatingNewProfile,
                existingUsers = users,
                onBack = {
                    if (currentFrame > 1) onNext(currentFrame - 1)
                    else navController.popBackStack()
                },
                onNext = { onNext(currentFrame + 1) },
                onSkip = onSkip,
                onShowExistingProfiles = { showExistingProfiles = true },
                isProfileSetupValid = isProfileSetupValid,
                onCompleteProfileSetup = { completeProfileSetup() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            when (currentFrame) {
                1 -> WelcomePage(
                    isCreatingNewProfile = isCreatingNewProfile,
                    existingUsers = users,
                    onSelectUser = { user ->
                        userViewModel.setCurrentUser(user)
                        // Navigation will be handled by the LaunchedEffect above
                    },
                    onCreateNewProfile = { onNext(2) }
                )
                2 -> FeaturesPage()
                3 -> ProfileSetupPage(
                    onComplete = { user ->
                        userViewModel.addUser(user)
                        // Navigate to home after profile creation
                        navController.navigate("home") {
                            popUpTo("newuser/1") { inclusive = true }
                        }
                    },
                    onValidationChange = { isValid ->
                        isProfileSetupValid = isValid
                    },
                    setCompleteAction = { action ->
                        completeProfileSetup = action
                    }
                )
            }

            // Existing Profiles Modal
            if (showExistingProfiles) {
                ExistingProfilesModal(
                    users = users,
                    onSelectUser = { user ->
                        userViewModel.setCurrentUser(user)
                    },
                    onClose = { showExistingProfiles = false },
                    navController = navController // Pass navController
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
            text = "Welcome to Dosezy",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )

        Text(
            text = "Your personal medication assistant.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF64748B),
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
            text = "Your Health, Simplified!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Key features of Dosezy",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Features List
        FeatureItem(
            icon = Icons.Filled.Notifications,
            title = "Never Miss a Dose",
            description = "Receive timely reminders for every medication, ensuring you stay on track."
        )

        Spacer(modifier = Modifier.height(24.dp))

        FeatureItem(
            icon = Icons.Filled.ManageHistory,
            title = "Easy Medication Management",
            description = "Manage your medications with a user-friendly interface, designed for simplicity."
        )

        Spacer(modifier = Modifier.height(24.dp))

        FeatureItem(
            icon = Icons.Filled.Info,
            title = "Emergency Support",
            description = "Quickly access emergency contacts and information, ensuring peace of mind."
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
            text = "Tell us about yourself!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "This helps us personalize your experience.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B),
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
                text = if (profilePicPath != null) "Change Profile Picture" else "Upload Profile Picture",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
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
                label = { Text("Name *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (fullNameError) Color.Red else Color(0xFF2084E4),
                    unfocusedBorderColor = if (fullNameError) Color.Red else Color(0xFFE2E8F0),
                    errorBorderColor = Color.Red
                ),
                isError = fullNameError
            )
            if (fullNameError) {
                Text(
                    text = "Name is required",
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
                    label = { Text("Age *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (ageError) Color.Red else Color(0xFF2084E4),
                        unfocusedBorderColor = if (ageError) Color.Red else Color(0xFFE2E8F0)
                    ),
                    isError = ageError
                )
                if (ageError) {
                    Text(
                        text = "Age is required",
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
                        text = "Gender is required",
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
                label = { Text("Phone Number (optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2084E4),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
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
            onComplete(user) // This will now trigger navigation
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
            value = selectedGender?.displayName ?: "Select",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text("Gender *") },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isError) Color.Red else Color(0xFF2084E4),
                unfocusedBorderColor = if (isError) Color.Red else Color(0xFFE2E8F0),
                errorBorderColor = Color.Red
            ),
            isError = isError
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            Gender.entries.forEach { gender ->
                DropdownMenuItem(
                    text = { Text(gender.displayName) },
                    onClick = {
                        onGenderSelected(gender)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
fun FeatureItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0F2FE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF1193D4),
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
                color = Color(0xFF1E293B)
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B),
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
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentFrame) {
                    1 -> {
                        // Frame 1: Go Back (left) and Get Started (right) buttons
                        if (isCreatingNewProfile && existingUsers.isNotEmpty()) {
                            // Show "Go Back" button on the left when creating new profile from switch screen
                            Button(
                                onClick = onBack, // This will navigate back to switch profile screen
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
                                    text = "Go Back",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (!isCreatingNewProfile && existingUsers.isNotEmpty()) {
                            // Show "Existing Profile" button on the left when in regular onboarding with existing users
                            Button(
                                onClick = { onShowExistingProfiles() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF374151)
                                )
                            ) {
                                Text(
                                    text = "Existing Profile",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            // No existing users or first time setup - show empty space to maintain layout
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        // Get Started button always on the right
                        Button(
                            onClick = onNext,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2084E4)
                            )
                        ) {
                            Text(
                                text = "Get Started",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    2 -> {
                        // Frame 2: Back and Next buttons
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
                                text = "Back",
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
                                containerColor = Color(0xFF2084E4)
                            )
                        ) {
                            Text(
                                text = "Next",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    3 -> {
                        // Frame 3: Back and Complete buttons
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
                                text = "Back",
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
                                    Color(0xFF2084E4)
                                } else {
                                    Color(0xFF9CA3AF)
                                }
                            ),
                            enabled = isProfileSetupValid
                        ) {
                            Text(
                                text = "Complete",
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
            color = Color.White
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
                            // Remove the immediate navigation here - let parent handle it
                            // navController.navigate("home") {
                            //     popUpTo("newuser/1") { inclusive = true }
                            // }
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
                        containerColor = Color(0xFF2084E4)
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
                        color = Color(0xFF6B7280)
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

// Keep all your existing previews the same...
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
                .background(Color.White)
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
                .background(Color.White)
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
                .background(Color.White)
        ) {
            ProfileSetupPage(onComplete = {})
        }
    }
}