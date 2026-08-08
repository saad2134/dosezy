package com.example.dosezy.ui.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import com.example.dosezy.R
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.getLocalizedName
import com.example.dosezy.ui.components.ProfilePicturePicker
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.viewmodels.UserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProfileScreen(navController: NavController) {
    val userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
    val currentUser by userViewModel.currentUser.collectAsState()
    val users by userViewModel.users.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    // Form state
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

    // Initialize form with current user data
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            fullName = user.fullName
            age = user.age.toString()
            gender = user.gender
            contactNumber = user.contactNumber
            profilePicPath = user.profilePicPath
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                navController = navController,
                currentUser = currentUser,
                title = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.manage_profile_title),
                showBackButton = true,
                actions = {}
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    // Profile Picture Section
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.size(128.dp)
                        ) {
                            ProfilePicturePicker(
                                profilePicPath = profilePicPath,
                                onProfilePictureSelected = { path ->
                                    profilePicPath = path
                                },
                                modifier = Modifier.size(128.dp)
                            )

                            // Edit icon overlay
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .align(Alignment.BottomEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile Picture",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (profilePicPath != null) stringResource(R.string.profile_change_pic) else stringResource(R.string.profile_upload_pic),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Form Fields
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

                    // Age Field
                    Column {
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gender Field
                    Column {

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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Contact Number Field
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

                    Spacer(modifier = Modifier.height(32.dp))

                    val updatedSuccessStr = stringResource(R.string.profile_updated_success)
                    val cannotDeleteStr = stringResource(R.string.profile_cannot_delete_only)

                    // Save Changes Button
                    Button(
                        onClick = {
                            // Validate form
                            fullNameError = fullName.isBlank()
                            ageError = age.isBlank()
                            genderError = gender == null

                            if (!fullNameError && !ageError && !genderError) {
                                currentUser?.let { user ->
                                    val updatedUser = user.copy(
                                        profilePicPath = profilePicPath,
                                        fullName = fullName.trim(),
                                        age = age.toIntOrNull() ?: 0,
                                        gender = gender!!,
                                        contactNumber = contactNumber.trim()
                                    )
                                    userViewModel.updateUser(updatedUser)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(updatedSuccessStr)
                                    }
                                    navController.popBackStack()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.form_save_changes),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Delete Profile Button
                    Button(
                        onClick = {
                            showDeleteDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFEE2E2),
                            contentColor = Color(0xFFDC2626)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.profile_delete_btn),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        val cannotDeleteMsg = stringResource(R.string.profile_cannot_delete_only)
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            tonalElevation = 0.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.profile_delete_btn)) },
            text = { Text(stringResource(R.string.profile_delete_confirm_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        currentUser?.let { user ->
                            if (users.size > 1) {
                                userViewModel.deleteUser(user)
                                navController.popBackStack()
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(cannotDeleteMsg)
                                }
                            }
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    )
                ) {
                    Text(stringResource(R.string.form_delete))
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6B7280)
                    )
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Success Message
    if (showSuccessMessage) {
        val updatedStr = stringResource(R.string.profile_updated_success)
        LaunchedEffect(showSuccessMessage) {
            snackbarHostState.showSnackbar(updatedStr)
            showSuccessMessage = false
        }
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