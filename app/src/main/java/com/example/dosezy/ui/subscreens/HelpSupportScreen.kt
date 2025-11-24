package com.example.dosezy.ui.subscreens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.viewmodels.UserViewModel

@Composable
fun HelpSupportScreen(navController: NavController) {
    val userViewModel: UserViewModel = hiltViewModel()
    val currentUser by userViewModel.currentUser.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopBar(
                navController = navController,
                currentUser = currentUser,
                title = "Help & Support",
                showBackButton = true,
                actions = {}
            )

            HelpSupportContent(navController)
        }

        // Version info in bottom right corner
        CornerVersion()
    }
}

@Composable
fun HelpSupportContent(navController: NavController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Resources Section
        Text(
            text = "Resources",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // FAQ Card
        SupportCard(
            icon = Icons.Outlined.Help,
            title = "FAQs",
            description = "Answers to common questions.",
            onClick = {
                openUrl(context, "https://github.com/saad2134/dosezy/blob/main/resources/FAQs.md")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // User Manual Card
        SupportCard(
            icon = Icons.Outlined.Book,
            title = "User Manual",
            description = "Browse the user guide.",
            onClick = {
                openUrl(context, "https://github.com/saad2134/dosezy/blob/main/resources/UserManual.md")
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Contact Support Section
        Text(
            text = "Contact Support",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Email Support Card
        SupportCard(
            icon = Icons.Outlined.Email,
            title = "Email Support",
            description = "Get help via email.",
            onClick = {
                openEmail(context, "reach.saad@outlook.com", "Help & Support Request")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Phone Support Card
        SupportCard(
            icon = Icons.Outlined.Call,
            title = "Phone Support",
            description = "Call us for immediate help.",
            onClick = {
                openPhone(context, "+91 98765 43210")
            }
        )

        // Add bottom padding for version info
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun CornerVersion() {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "v$versionName",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier
                .align(Alignment.BottomEnd)
        )
    }
}

// If you want a centered version at the bottom instead:
@Composable
fun BottomVersion() {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName
    val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        packageInfo.versionCode.toLong()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Dosezy v$versionName",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Build $versionCode",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportCard(
    icon: ImageVector,
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

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
                    fontSize = 18.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Icon(
                imageVector = Icons.Outlined.ArrowOutward,
                contentDescription = "Open",
                tint = Color(0xFF94A3B8)
            )
        }
    }
}

// Utility functions for opening external apps
private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Cannot open link", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun openEmail(context: android.content.Context, email: String, subject: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:")
            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "No email app available", android.widget.Toast.LENGTH_SHORT).show()
    }
}

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