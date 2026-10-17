/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.screens.newuser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dosezy.R
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.User
import com.example.dosezy.data.model.getLocalizedName
import com.example.dosezy.ui.components.ProfilePicturePicker

@Composable
fun ProfileSetupPage(
    onComplete: (User) -> Unit,
    isDefaultUser: Boolean = false,
    onValidationChange: (Boolean) -> Unit = {},
    setCompleteAction: (() -> Unit) -> Unit = {}
) {
    var fullName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<Gender?>(null) }
    var contactNumber by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var medicalConditions by remember { mutableStateOf("") }
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

    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
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

        Spacer(modifier = Modifier.height(16.dp))

        // Allergies Field (Optional)
        Column {
            OutlinedTextField(
                value = allergies,
                onValueChange = { allergies = it },
                label = { Text(stringResource(R.string.profile_allergies)) },
                placeholder = { Text(stringResource(R.string.profile_allergies_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Medical Conditions Field (Optional)
        Column {
            OutlinedTextField(
                value = medicalConditions,
                onValueChange = { medicalConditions = it },
                label = { Text(stringResource(R.string.profile_medical_conditions)) },
                placeholder = { Text(stringResource(R.string.profile_medical_conditions_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
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
                allergies = allergies.trim().ifBlank { null },
                medicalConditions = medicalConditions.trim().ifBlank { null },
                isCurrentUser = isDefaultUser
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
