package com.example.dosezy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dosezy.R
import com.example.dosezy.data.model.User
import com.example.dosezy.utils.NotificationUtils
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun TopBar(
    navController: NavController,
    currentUser: User? = null,
    title: String,
    subtitle: String? = null,
    showBackButton: Boolean = false,
    showNotificationStatus: Boolean = true,
    titleColor: Color = Color(0xFF1E293B),
    onBackClick: (() -> Unit)? = null,
    actions: @Composable () -> Unit
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    // Calculate notification status (red if any checks fail)
    val allNotificationsPassed = false

    Column(
        modifier = Modifier
            .fillMaxWidth()

            .background(Color.White)
    ) {
        // First Row: Profile info and Notification button
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Profile picture and user info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Picture
                ProfilePicture(
                    currentUser = currentUser,
                    onClick = { showProfileDialog = true },
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // User Info
                Column {
                    Text(
                        text = "Hi, ${currentUser?.fullName?.split(" ")?.get(0) ?: "User"}!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "\"Health is a priceless wealth.\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            // Right: Notification Status Button
            if (showNotificationStatus) {
                NotificationStatusButton(

                    onClick = { showNotificationDialog = true }
                )
            }
        }

        //Spacer(modifier = Modifier.height(32.dp))

        // Second Row: Screen Title
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom=16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showBackButton) {
                // For subscreens: Back button + Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onBackClick?.invoke() ?: navController.popBackStack() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            textAlign = TextAlign.Center
                        )
                        subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF6B7280),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Empty space to balance the back button
                    Spacer(modifier = Modifier.size(48.dp))
                }
            } else {
                // For main screens: Centered title only
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        textAlign = TextAlign.Center
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Dialogs
        if (showProfileDialog) {
            ProfileDialog(
                onDismiss = { showProfileDialog = false },
                navController = navController
            )
        }

        if (showNotificationDialog) {
            NotificationStatusDialog(
                onDismiss = { showNotificationDialog = false }
            )
        }
    }
}

@Composable
private fun ProfilePicture(
    currentUser: User?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
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
                placeholder = androidx.compose.ui.res.painterResource(id = R.drawable.default_profile),
                error = androidx.compose.ui.res.painterResource(id = R.drawable.default_profile),
                contentScale = ContentScale.Crop
            )
        } else {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.default_profile),
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
private fun NotificationStatusButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var refreshCounter by remember { mutableStateOf(0) }

    // Auto-refresh status every 5 seconds
    LaunchedEffect(refreshCounter) {
        // ensures the status is rechecked periodically
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000) // Refresh every 5 seconds
            refreshCounter++
        }
    }

    val allPassed = NotificationUtils.areAllNotificationRequirementsMet(context)

    val buttonColor = if (allPassed) Color(0xFF10B981) else Color(0xFFEF4444)
    val iconColor = Color.White

    Box(modifier = modifier) {

        // Main button (background + icon)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(buttonColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notification Status",
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        // Status indicator ABOVE, overlayed on top-right
        Box(
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.TopEnd)           // Position relative to parent Box
                .offset(x = 4.dp, y = (-4).dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (allPassed) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (allPassed) "All Good" else "Issues",
                tint = buttonColor,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}