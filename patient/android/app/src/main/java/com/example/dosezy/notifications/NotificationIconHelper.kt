// NotificationIconHelper.kt
package com.example.dosezy.notifications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

object NotificationIconHelper {

    fun getNotificationIconResId(icon: ImageVector): Int {
        return when (icon) {
            Icons.Filled.Check -> android.R.drawable.ic_input_add
            Icons.Filled.Snooze -> android.R.drawable.ic_media_pause
            Icons.Filled.Medication -> android.R.drawable.ic_menu_help
            Icons.Filled.Warning -> android.R.drawable.ic_dialog_alert
            Icons.Filled.Info -> android.R.drawable.ic_dialog_info
            else -> android.R.drawable.ic_dialog_info
        }
    }

    fun getIconName(icon: ImageVector): String {
        return when (icon) {
            Icons.Filled.Check -> "check"
            Icons.Filled.Snooze -> "snooze"
            Icons.Filled.Medication -> "medication"
            Icons.Filled.Warning -> "warning"
            Icons.Filled.Info -> "info"
            else -> "unknown"
        }
    }
}