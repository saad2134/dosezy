// ComposeNotificationUtils.kt
package com.example.dosezy.notifications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

object ComposeNotificationUtils {

    enum class NotificationType {
        MEDICINE_REMINDER,
        MISSED_MEDICINE,
        UPCOMING_MEDICINE,
        SYSTEM_ALERT
    }

    fun getIconForNotification(type: NotificationType): ImageVector {
        return when (type) {
            NotificationType.MEDICINE_REMINDER -> Icons.Filled.Medication
            NotificationType.MISSED_MEDICINE -> Icons.Filled.Warning
            NotificationType.UPCOMING_MEDICINE -> Icons.Filled.Notifications
            NotificationType.SYSTEM_ALERT -> Icons.Filled.Info
        }
    }

    fun getColorForNotification(type: NotificationType): Long {
        return when (type) {
            NotificationType.MEDICINE_REMINDER -> 0xFF4CAF50 // Green
            NotificationType.MISSED_MEDICINE -> 0xFFF44336 // Red
            NotificationType.UPCOMING_MEDICINE -> 0xFF2196F3 // Blue
            NotificationType.SYSTEM_ALERT -> 0xFFFF9800 // Orange
        }
    }

    fun getChannelName(type: NotificationType): String {
        return when (type) {
            NotificationType.MEDICINE_REMINDER -> "Medicine Reminders"
            NotificationType.MISSED_MEDICINE -> "Missed Medicine Alerts"
            NotificationType.UPCOMING_MEDICINE -> "Upcoming Medicine Notifications"
            NotificationType.SYSTEM_ALERT -> "System Alerts"
        }
    }

    fun getChannelDescription(type: NotificationType): String {
        return when (type) {
            NotificationType.MEDICINE_REMINDER -> "Notifications for scheduled medicine reminders"
            NotificationType.MISSED_MEDICINE -> "Alerts for missed medications"
            NotificationType.UPCOMING_MEDICINE -> "Notifications for upcoming medicine schedules"
            NotificationType.SYSTEM_ALERT -> "Important system and app notifications"
        }
    }
}