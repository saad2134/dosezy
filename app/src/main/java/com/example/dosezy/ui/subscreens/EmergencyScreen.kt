package com.example.dosezy.ui.subscreens

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.viewmodels.UserViewModel

@Composable
fun EmergencyScreen(navController: NavController) {
    val userViewModel: UserViewModel = hiltViewModel()
    val currentUser by userViewModel.currentUser.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopBar(
            navController = navController,
            currentUser = currentUser,
            title = "Emergency",
            showBackButton = true,
            titleColor = Color(0xFFEF4444),
            actions = {}
        )

        EmergencyContent(navController)
    }
}

@Composable
fun EmergencyContent(navController: NavController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showEmergencyInfo by remember { mutableStateOf(false) }
    var showEditEmergencyInfo by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Information Section
        /*Text(
            text = "Information",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // View Information Card
        EmergencyCard(
            icon = Icons.Outlined.ContactEmergency,
            iconColor = Color(0xFF3B82F6),
            title = "View Information",
            description = "See saved emergency information.",
            onClick = {
                showEmergencyInfo = true
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Edit Information Card
        EmergencyCard(
            icon = Icons.Outlined.Edit,
            iconColor = Color(0xFF3B82F6),
            title = "Edit Information",
            description = "Edit emergency information.",
            onClick = {
                showEditEmergencyInfo = true
            }
        )

        Spacer(modifier = Modifier.height(32.dp))*/

        // Emergency Services Section
        Text(
            text = "Emergency Services (India)",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Ambulance Card
        EmergencyServiceCard(
            icon = Icons.Outlined.LocalHospital,
            iconColor = Color(0xFFEF4444),
            title = "Ambulance",
            phoneNumber = "108",
            onClick = {
                openPhone(context, "108")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Fire Department Card
        EmergencyServiceCard(
            icon = Icons.Outlined.LocalFireDepartment,
            iconColor = Color(0xFFF97316),
            title = "Fire",
            phoneNumber = "101",
            onClick = {
                openPhone(context, "101")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Police Card
        EmergencyServiceCard(
            icon = Icons.Outlined.LocalPolice,
            iconColor = Color(0xFF3B82F6),
            title = "Police",
            phoneNumber = "100",
            onClick = {
                openPhone(context, "100")
            }
        )
    }

    // Dialogs
    if (showEmergencyInfo) {
        EmergencyInfoDialog(
            onDismiss = { showEmergencyInfo = false },
            onEdit = {
                showEmergencyInfo = false
                showEditEmergencyInfo = true
            }
        )
    }

    if (showEditEmergencyInfo) {
        EditEmergencyInfoDialog(
            onDismiss = { showEditEmergencyInfo = false },
            onSave = { showEditEmergencyInfo = false }
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick
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
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = description,
                    fontSize = 16.sp,
                    color = Color(0xFF64748B)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick
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
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = phoneNumber,
                    fontSize = 16.sp,
                    color = Color(0xFF64748B)
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
                color = Color(0xFF1E293B)
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
                color = Color(0xFF1E293B)
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