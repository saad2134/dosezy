package com.example.dosezy.ui.components

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.dosezy.utils.NotificationUtils
import kotlinx.coroutines.delay

data class NotificationStatus(
    val title: String,
    val description: String,
    val isPassed: Boolean,
    val icon: @Composable () -> Unit,
    val onFixClick: (() -> Unit)? = null
)

@Composable
fun NotificationStatusDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var notificationStatuses by remember { mutableStateOf<List<NotificationStatus>>(emptyList()) }
    var refreshCounter by remember { mutableStateOf(0) }

    // Auto-refresh status every 2 seconds
    LaunchedEffect(refreshCounter) {
        notificationStatuses = createNotificationStatuses(context, refreshCounter)
    }

    // Auto-refresh loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000) // Refresh every 2 seconds
            refreshCounter++
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with rotating sync icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Notification Check",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RotatingSyncIcon()
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status List
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    notificationStatuses.forEach { status ->
                        NotificationStatusItem(
                            status = status,
                            onClick = {
                                status.onFixClick?.invoke()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // OK Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2084E4)
                    )
                ) {
                    Text(
                        text = "OK",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Non-composable function to create notification statuses
private fun createNotificationStatuses(
    context: Context,
    refreshCounter: Int
): List<NotificationStatus> {
    // Force recomputation by using refreshCounter
    val backgroundPermStatus = NotificationUtils.isIgnoringBatteryOptimizations(context)
    val notificationPermStatus = NotificationUtils.hasNotificationPermission(context)
    val batteryStatus = NotificationUtils.isBatterySufficient(context)
    val soundStatus = NotificationUtils.isPhoneNotSilent(context)

    return listOf(
        NotificationStatus(
            title = "Notification Permission",
            description = if (notificationPermStatus) {
                "Permission granted to send notifications."
            } else {
                "No permission to send notifications."
            },
            isPassed = notificationPermStatus,
            icon = {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notification Permission",
                    modifier = Modifier.size(32.dp),
                    tint = if (notificationPermStatus) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            },
            onFixClick = {
                if (!notificationPermStatus) {
                    NotificationUtils.requestNotificationPermission(context)
                }
            }
        ),

        NotificationStatus(
            title = "Phone Battery",
            description = NotificationUtils.getBatteryStatus(context),
            isPassed = batteryStatus,
            icon = {
                Icon(
                    imageVector = Icons.Default.BatteryStd,
                    contentDescription = "Phone Battery",
                    modifier = Modifier.size(32.dp),
                    tint = if (batteryStatus) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            },
            onFixClick = {
                // Battery level can't be fixed via settings, show battery optimization
                NotificationUtils.openBatteryOptimizationSettings(context)
            }
        ),

        NotificationStatus(
            title = "Phone Sound",
            description = NotificationUtils.getSoundStatus(context),
            isPassed = soundStatus,
            icon = {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Phone Sound",
                    modifier = Modifier.size(32.dp),
                    tint = if (soundStatus) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            },
            onFixClick = {
                if (!soundStatus) {
                    NotificationUtils.openSoundSettings(context)
                }
            }
        ),

        NotificationStatus(
            title = "Background Permission",
            description = if (backgroundPermStatus) {
                "App can run in background."
            } else {
                "Background restrictions may prevent notifications."
            },
            isPassed = backgroundPermStatus,
            icon = {
                Icon(
                    imageVector = Icons.Default.WorkOutline,
                    contentDescription = "Background Permission",
                    modifier = Modifier.size(32.dp),
                    tint = if (backgroundPermStatus) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            },
            onFixClick = {
                if (!backgroundPermStatus) {
                    NotificationUtils.openBatteryOptimizationSettings(context)
                }
            }
        )
    )
}

@Composable
fun NotificationStatusItem(
    status: NotificationStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (status.isPassed) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
    val textColor = if (status.isPassed) Color(0xFF065F46) else Color(0xFF991B1B)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                enabled = status.onFixClick != null && !status.isPassed
            ),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                status.icon()
                Column {
                    Text(
                        text = status.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                    Text(
                        text = status.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
            }
            if (status.onFixClick != null && !status.isPassed) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Fix issue",
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun RotatingSyncIcon() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Icon(
        imageVector = Icons.Rounded.Autorenew,
        contentDescription = "Syncing",
        modifier = Modifier
            .size(32.dp)
            .rotate(rotation),
        tint = Color(0xFF2084E4)
    )
}