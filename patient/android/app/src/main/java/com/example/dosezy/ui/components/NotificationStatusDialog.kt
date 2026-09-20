package com.example.dosezy.ui.components

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Alarm
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
import androidx.compose.ui.graphics.luminance
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
    val isWarning: Boolean = false,
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
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with rotating sync icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.notif_check_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RotatingSyncIcon()
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2084E4)
                    )
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.ok),
                        fontSize = 17.sp,
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
    val notificationPermStatus = NotificationUtils.hasNotificationPermission(context)
    val exactAlarmsStatus = NotificationUtils.canScheduleExactAlarms(context)
    val overlayStatus = NotificationUtils.canDrawOverlays(context)
    val backgroundPermStatus = NotificationUtils.isIgnoringBatteryOptimizations(context)
    val soundStatus = NotificationUtils.isPhoneNotSilent(context)
    val batteryStatus = NotificationUtils.isBatterySufficient(context)

    return listOf(
        // Notification Permission Item
        NotificationStatus(
            title = context.getString(com.example.dosezy.R.string.notif_perm_title),
            description = if (notificationPermStatus) {
                context.getString(com.example.dosezy.R.string.notif_perm_granted)
            } else {
                context.getString(com.example.dosezy.R.string.notif_perm_desc)
            },
            isPassed = notificationPermStatus,
            isWarning = false,
            icon = {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notification Permission",
                    modifier = Modifier.size(30.dp),
                    tint = if (notificationPermStatus) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            },
            onFixClick = {
                if (!notificationPermStatus) {
                    NotificationUtils.requestNotificationPermission(context)
                }
            }
        ),

        // Exact Alarms Item (Crucial for alarms to trigger reliably on time)
        NotificationStatus(
            title = context.getString(com.example.dosezy.R.string.notif_exact_alarm_title),
            description = if (exactAlarmsStatus) {
                context.getString(com.example.dosezy.R.string.notif_exact_alarm_granted)
            } else {
                context.getString(com.example.dosezy.R.string.notif_exact_alarm_desc)
            },
            isPassed = exactAlarmsStatus,
            isWarning = false,
            icon = {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = "Exact Alarms",
                    modifier = Modifier.size(30.dp),
                    tint = if (exactAlarmsStatus) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            },
            onFixClick = {
                if (!exactAlarmsStatus) {
                    NotificationUtils.requestExactAlarmPermission(context)
                }
            }
        ),

        // Display Over Apps
        NotificationStatus(
            title = context.getString(com.example.dosezy.R.string.notif_overlay_title),
            description = if (overlayStatus) {
                context.getString(com.example.dosezy.R.string.notif_overlay_granted)
            } else {
                context.getString(com.example.dosezy.R.string.notif_overlay_desc)
            },
            isPassed = overlayStatus,
            isWarning = false,
            icon = {
                Icon(
                    imageVector = Icons.Default.WorkOutline,
                    contentDescription = "Display Over Apps",
                    modifier = Modifier.size(30.dp),
                    tint = if (overlayStatus) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            },
            onFixClick = {
                if (!overlayStatus) {
                    NotificationUtils.requestOverlayPermission(context)
                }
            }
        ),

        // Battery Optimization / Background Settings (Takes user directly to OEM background settings if Samsung, etc.)
        NotificationStatus(
            title = context.getString(com.example.dosezy.R.string.notif_bg_title),
            description = if (backgroundPermStatus) {
                context.getString(com.example.dosezy.R.string.notif_bg_granted)
            } else {
                context.getString(com.example.dosezy.R.string.notif_oem_bg_item_desc)
            },
            isPassed = backgroundPermStatus,
            isWarning = false,
            icon = {
                Icon(
                    imageVector = Icons.Default.BatteryStd,
                    contentDescription = "Battery Optimization",
                    modifier = Modifier.size(30.dp),
                    tint = if (backgroundPermStatus) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            },
            onFixClick = {
                if (!backgroundPermStatus) {
                    val directSuccess = NotificationUtils.requestBatteryOptimizationExemption(context)
                    if (!directSuccess) {
                        NotificationUtils.openBatteryOptimizationSettings(context)
                    }
                }
            }
        ),

        // Phone Sound Mode
        NotificationStatus(
            title = context.getString(com.example.dosezy.R.string.notif_sound_title),
            description = NotificationUtils.getSoundStatus(context),
            isPassed = soundStatus,
            isWarning = false,
            icon = {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Phone Sound",
                    modifier = Modifier.size(30.dp),
                    tint = if (soundStatus) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            },
            onFixClick = {
                if (!soundStatus) {
                    NotificationUtils.openSoundSettings(context)
                }
            }
        ),

        // Phone Battery
        NotificationStatus(
            title = context.getString(com.example.dosezy.R.string.notif_battery_title),
            description = NotificationUtils.getBatteryStatus(context),
            isPassed = batteryStatus,
            isWarning = false,
            icon = {
                Icon(
                    imageVector = Icons.Default.BatteryStd,
                    contentDescription = "Phone Battery",
                    modifier = Modifier.size(30.dp),
                    tint = if (batteryStatus) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            },
            onFixClick = {
                NotificationUtils.openBatteryOptimizationSettings(context)
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = when {
        status.isWarning -> if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7) // Warm Orange / Amber
        status.isPassed -> if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5) // Emerald Green
        else -> if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2) // Rose Red
    }
    val textColor = when {
        status.isWarning -> if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309) // Orange Amber
        status.isPassed -> if (isDark) Color(0xFF34D399) else Color(0xFF065F46) // Emerald Green
        else -> if (isDark) Color(0xFFF87171) else Color(0xFF991B1B) // Rose Red
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                enabled = status.onFixClick != null && (!status.isPassed || status.isWarning)
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
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                status.icon()
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = status.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                    Text(
                        text = status.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.85f)
                    )
                }
            }
            if (status.onFixClick != null && (!status.isPassed || status.isWarning)) {
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