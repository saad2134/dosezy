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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.dosezy.R
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.User
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.viewmodels.UserViewModel
import java.io.File

@Composable
fun MenuScreen(navController: NavController) {
    val userViewModel: UserViewModel = hiltViewModel()
    val currentUser by userViewModel.currentUser.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
    ) {
        TopBar(
            navController = navController,
            currentUser = currentUser,
            title = "Menu",
            showBackButton = false,
            actions = {}
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    title = "Preferences (Format, etc)",
                    color = Color(0xFF1E293B),
                    onClick = { navController.navigate("preferences") }
                )
            }

            // Emergency
            item {
                MenuItem(
                    icon = Icons.Default.Emergency,
                    title = "Emergency",
                    color = Color(0xFFEF4444),
                    onClick = { navController.navigate("emergency") }
                )
            }

            // Help and Support
            item {
                MenuItem(
                    icon = Icons.Default.HelpOutline,
                    title = "Help and Support",
                    color = Color(0xFF1E293B),
                    onClick = { navController.navigate("help_support") }
                )
            }

            // Divider
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color(0xFFD1D5DB))
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Export Data
            item {
                MenuItem(
                    icon = Icons.Default.Download,
                    title = "Export Data as CSV",
                    color = Color(0xFF1E293B),
                    onClick = { showExportDialog = true }
                )
            }
        }
    }

    // Export Data Dialog
    if (showExportDialog) {
        ExportDataDialog(
            onDismiss = { showExportDialog = false },
            onShare = {
                // Handle share functionality
                showExportDialog = false
            }
        )
    }
}

@Composable
fun ProfileCard(
    currentUser: User?,
    onManageProfile: () -> Unit,
    onSwitchProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
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
                    color = Color(0xFF1E293B)
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
                    text = "Manage",
                    onClick = onManageProfile,
                    modifier = Modifier.weight(1f)
                )

                // Switch Profile Button
                ProfileActionButton(
                    icon = Icons.Default.SwitchAccount,
                    text = "Switch",
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
        color = Color(0xFFF3F4F6),
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
                color = Color(0xFF374151)
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
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
                    text = "Data Exported",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = "Your medication data has been successfully saved as a CSV file to your local device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280),
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
                        imageVector = Icons.Default.AccountCircle, // Using account circle as share icon placeholder
                        contentDescription = "Share",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share to another app",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF6B7280)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(

                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight, // Using chevron as close icon placeholder
                        contentDescription = "Close",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Close",
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
                contactNumber = TODO(),
                isCurrentUser = TODO()
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
            color = Color(0xFF1E293B),
            onClick = {}
        )
    }
}