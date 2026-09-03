package com.example.dosezy.ui.subscreens

import androidx.compose.material3.MaterialTheme
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalPolice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.example.dosezy.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.viewmodels.UserViewModel

data class CountryEmergency(
    val code: String,
    val flag: String,
    val name: String,
    val ambulance: String,
    val fire: String,
    val police: String,
    val universal: String? = null
)

val emergencyCountries = listOf(
    CountryEmergency("IN", "🇮🇳", "India", ambulance = "108", fire = "101", police = "100", universal = "112"),
    CountryEmergency("US", "🇺🇸", "United States / Canada", ambulance = "911", fire = "911", police = "911", universal = "911"),
    CountryEmergency("GB", "🇬🇧", "United Kingdom", ambulance = "999", fire = "999", police = "999", universal = "112"),
    CountryEmergency("EU", "🇪🇺", "European Union", ambulance = "112", fire = "112", police = "112", universal = "112"),
    CountryEmergency("CN", "🇨🇳", "China", ambulance = "120", fire = "119", police = "110"),
    CountryEmergency("JP", "🇯🇵", "Japan", ambulance = "119", fire = "119", police = "110"),
    CountryEmergency("RU", "🇷🇺", "Russia", ambulance = "103", fire = "101", police = "102", universal = "112"),
    CountryEmergency("BR", "🇧🇷", "Brazil", ambulance = "192", fire = "193", police = "190"),
    CountryEmergency("BD", "🇧🇩", "Bangladesh", ambulance = "999", fire = "999", police = "999", universal = "999"),
    CountryEmergency("AU", "🇦🇺", "Australia", ambulance = "000", fire = "000", police = "000", universal = "000")
)

fun detectDeviceEmergencyCountry(context: android.content.Context, userLanguage: com.example.dosezy.data.model.Language): CountryEmergency {
    // 1. Try Telephony Network or SIM Country ISO
    val tm = context.getSystemService(android.content.Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
    val telephonyIso = try {
        val netIso = tm?.networkCountryIso?.uppercase()
        val simIso = tm?.simCountryIso?.uppercase()
        if (!netIso.isNullOrBlank()) netIso else if (!simIso.isNullOrBlank()) simIso else null
    } catch (_: Exception) { null }

    // 2. Try System Locale Country ISO
    val localeIso = try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]?.country?.uppercase()
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale?.country?.uppercase()
        }
    } catch (_: Exception) { null }

    val detectedIso = telephonyIso ?: localeIso

    if (!detectedIso.isNullOrBlank()) {
        val matched = matchEmergencyCountryByIso(detectedIso)
        if (matched != null) return matched
    }

    // 3. Fallback based on user app language setting if explicitly set
    return when (userLanguage) {
        com.example.dosezy.data.model.Language.BENGALI -> emergencyCountries.find { it.code == "BD" }
        com.example.dosezy.data.model.Language.CHINESE -> emergencyCountries.find { it.code == "CN" }
        com.example.dosezy.data.model.Language.JAPANESE -> emergencyCountries.find { it.code == "JP" }
        com.example.dosezy.data.model.Language.RUSSIAN -> emergencyCountries.find { it.code == "RU" }
        com.example.dosezy.data.model.Language.SPANISH,
        com.example.dosezy.data.model.Language.FRENCH,
        com.example.dosezy.data.model.Language.GERMAN,
        com.example.dosezy.data.model.Language.ITALIAN,
        com.example.dosezy.data.model.Language.PORTUGUESE -> emergencyCountries.find { it.code == "EU" }
        com.example.dosezy.data.model.Language.HINDI -> emergencyCountries.find { it.code == "IN" }
        else -> emergencyCountries.find { it.code == "US" }
    } ?: emergencyCountries.find { it.code == "GB" } ?: emergencyCountries.first()
}

private fun matchEmergencyCountryByIso(iso: String): CountryEmergency? {
    return when (iso) {
        "GB" -> emergencyCountries.find { it.code == "GB" }
        "US", "CA" -> emergencyCountries.find { it.code == "US" }
        "IN" -> emergencyCountries.find { it.code == "IN" }
        "AU" -> emergencyCountries.find { it.code == "AU" }
        "BD" -> emergencyCountries.find { it.code == "BD" }
        "BR" -> emergencyCountries.find { it.code == "BR" }
        "CN" -> emergencyCountries.find { it.code == "CN" }
        "JP" -> emergencyCountries.find { it.code == "JP" }
        "RU" -> emergencyCountries.find { it.code == "RU" }
        "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE", "NO", "CH", "IS", "LI" -> emergencyCountries.find { it.code == "EU" }
        else -> null
    }
}

@Composable
fun EmergencyScreen(navController: NavController) {
    val userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
    val currentUser by userViewModel.currentUser.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopBar(
            navController = navController,
            currentUser = currentUser,
            title = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.emergency_title),
            showBackButton = true,
            titleColor = Color(0xFFEF4444),
            actions = {}
        )

        EmergencyContent(currentUser?.language ?: com.example.dosezy.data.model.Language.SYSTEM)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContent(userLanguage: com.example.dosezy.data.model.Language) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val defaultCountry = remember(userLanguage, context) {
        detectDeviceEmergencyCountry(context, userLanguage)
    }

    var selectedCountry by remember { mutableStateOf(defaultCountry) }
    var countryDropdownExpanded by remember { mutableStateOf(false) }

    // Persist personal contacts via SharedPreferences
    val prefs = context.getSharedPreferences("emergency_contacts", android.content.Context.MODE_PRIVATE)
    val customContacts = remember {
        val saved = prefs.getString("contacts_json", null)
        val list = androidx.compose.runtime.mutableStateListOf<Pair<String, String>>()
        if (!saved.isNullOrEmpty()) {
            try {
                val arr = org.json.JSONArray(saved)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(Pair(obj.getString("name"), obj.getString("phone")))
                }
            } catch (_: Exception) {}
        }
        list
    }

    // Helper to persist contacts
    fun saveContacts() {
        val arr = org.json.JSONArray()
        customContacts.forEach { (name, phone) ->
            val obj = org.json.JSONObject()
            obj.put("name", name)
            obj.put("phone", phone)
            arr.put(obj)
        }
        prefs.edit().putString("contacts_json", arr.toString()).apply()
    }

    var showAddContactDialog by remember { mutableStateOf(false) }
    var contactIndexToDelete by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Country Dropdown Picker (no redundant header — TopBar already shows title)
        ExposedDropdownMenuBox(
            expanded = countryDropdownExpanded,
            onExpandedChange = { countryDropdownExpanded = !countryDropdownExpanded }
        ) {
            OutlinedTextField(
                value = "${selectedCountry.flag} ${selectedCountry.name}",
                onValueChange = {},
                readOnly = true,
                label = { Text(androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.emergency_select_country)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryDropdownExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            ExposedDropdownMenu(
                expanded = countryDropdownExpanded,
                onDismissRequest = { countryDropdownExpanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                emergencyCountries.forEach { country ->
                    DropdownMenuItem(
                        text = { Text("${country.flag} ${country.name}", color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            selectedCountry = country
                            countryDropdownExpanded = false
                        },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Universal Emergency Card if available
        if (selectedCountry.universal != null) {
            EmergencyServiceCard(
                icon = Icons.Outlined.Call,
                iconColor = Color(0xFF8B5CF6),
                title = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.emergency_universal),
                phoneNumber = selectedCountry.universal!!,
                onClick = {
                    openPhone(context, selectedCountry.universal!!)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Ambulance Card
        EmergencyServiceCard(
            icon = Icons.Outlined.LocalHospital,
            iconColor = Color(0xFFEF4444),
            title = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.emergency_ambulance),
            phoneNumber = selectedCountry.ambulance,
            onClick = {
                openPhone(context, selectedCountry.ambulance)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Fire Department Card
        EmergencyServiceCard(
            icon = Icons.Outlined.LocalFireDepartment,
            iconColor = Color(0xFFF97316),
            title = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.emergency_fire),
            phoneNumber = selectedCountry.fire,
            onClick = {
                openPhone(context, selectedCountry.fire)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Police Card
        EmergencyServiceCard(
            icon = Icons.Outlined.LocalPolice,
            iconColor = Color(0xFF3B82F6),
            title = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.emergency_police),
            phoneNumber = selectedCountry.police,
            onClick = {
                openPhone(context, selectedCountry.police)
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ═══ Personal Emergency Contacts Section ═══
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.personal_contacts),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            androidx.compose.material3.TextButton(
                onClick = { showAddContactDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Contact",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.personal_contacts_add), color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (customContacts.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 1.dp
            ) {
                Text(
                    text = stringResource(R.string.personal_contacts_empty),
                    modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            customContacts.forEachIndexed { index, (name, phone) ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { openPhone(context, phone) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = phone,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.material3.IconButton(onClick = { contactIndexToDelete = index }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Contact Dialog
    if (showAddContactDialog) {
        var contactName by remember { mutableStateOf("") }
        var contactPhone by remember { mutableStateOf("") }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = { Text(stringResource(R.string.personal_contacts_add_title), fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            text = {
                Column {
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text(stringResource(R.string.personal_contacts_name_label)) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text(stringResource(R.string.personal_contacts_phone_label)) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        )
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (contactName.isNotBlank() && contactPhone.isNotBlank()) {
                            customContacts.add(Pair(contactName, contactPhone))
                            saveContacts()
                            showAddContactDialog = false
                        }
                    }
                ) { Text(stringResource(R.string.personal_contacts_save)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showAddContactDialog = false }) {
                    Text(stringResource(R.string.delete_contact_cancel))
                }
            }
        )
    }

    // Delete Contact Confirmation Dialog
    contactIndexToDelete?.let { index ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { contactIndexToDelete = null },
            tonalElevation = 0.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(R.string.delete_contact_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_contact_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (index < customContacts.size) {
                            customContacts.removeAt(index)
                            saveContacts()
                        }
                        contactIndexToDelete = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.delete_contact_confirm),
                        color = Color(0xFFEF4444)
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { contactIndexToDelete = null }
                ) {
                    Text(
                        text = stringResource(R.string.delete_contact_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Chevron Icon
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Navigate",
                tint = Color(0xFF94A3B8)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyServiceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    phoneNumber: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Service Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = phoneNumber,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Call Icon
            Icon(
                imageVector = Icons.Outlined.Call,
                contentDescription = "Call",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun EmergencyInfoDialog(
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Emergency Information",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                Text("Name: John Doe", modifier = Modifier.padding(vertical = 4.dp))
                Text("Emergency Contact: Jane Doe (123-456-7890)", modifier = Modifier.padding(vertical = 4.dp))
                Text("Blood Type: O+", modifier = Modifier.padding(vertical = 4.dp))
                Text("Allergies: Penicillin, Peanuts", modifier = Modifier.padding(vertical = 4.dp))
                Text("Medical Conditions: Asthma", modifier = Modifier.padding(vertical = 4.dp))
                Text("Current Medications: Albuterol", modifier = Modifier.padding(vertical = 4.dp))
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onEdit) {
                Text("Edit")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun EditEmergencyInfoDialog(
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var name by remember { mutableStateOf("John Doe") }
    var emergencyContact by remember { mutableStateOf("Jane Doe (123-456-7890)") }
    var bloodType by remember { mutableStateOf("O+") }
    var allergies by remember { mutableStateOf("Penicillin, Peanuts") }
    var medicalConditions by remember { mutableStateOf("Asthma") }
    var currentMedications by remember { mutableStateOf("Albuterol") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Emergency Information",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = emergencyContact,
                    onValueChange = { emergencyContact = it },
                    label = { Text("Emergency Contact") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = bloodType,
                    onValueChange = { bloodType = it },
                    label = { Text("Blood Type") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = allergies,
                    onValueChange = { allergies = it },
                    label = { Text("Allergies") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = medicalConditions,
                    onValueChange = { medicalConditions = it },
                    label = { Text("Medical Conditions") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = currentMedications,
                    onValueChange = { currentMedications = it },
                    label = { Text("Current Medications") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Utility function for opening phone dialer
private fun openPhone(context: android.content.Context, phoneNumber: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
            data = android.net.Uri.parse("tel:$phoneNumber")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Cannot make call", android.widget.Toast.LENGTH_SHORT).show()
    }
}