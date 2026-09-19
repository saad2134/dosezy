package com.example.dosezy.utils

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.dosezy.R

object NotificationUtils {

    /**
     * Check if the app has notification permission
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Get current battery level as percentage
     */
    fun getBatteryLevel(context: Context): Int {
        try {
            val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter)
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    val pct = (level * 100f / scale).toInt()
                    if (pct in 0..100) return pct
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val capacity = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            if (capacity in 0..100) {
                return capacity
            }
        } catch (e: Exception) {
            // ignore
        }

        return 100
    }

    /**
     * Check if battery level is sufficient (above 15%)
     */
    fun isBatterySufficient(context: Context): Boolean {
        return getBatteryLevel(context) >= 15
    }

    /**
     * Get battery status description
     */
    fun getBatteryStatus(context: Context): String {
        val batteryLevel = getBatteryLevel(context)
        return context.getString(com.example.dosezy.R.string.notif_battery_status, batteryLevel)
    }

    /**
     * Get current sound mode status
     */
    fun getSoundStatus(context: Context): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return when (audioManager?.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> context.getString(com.example.dosezy.R.string.notif_sound_normal)
            AudioManager.RINGER_MODE_VIBRATE -> context.getString(com.example.dosezy.R.string.notif_sound_vibrate)
            AudioManager.RINGER_MODE_SILENT -> context.getString(com.example.dosezy.R.string.notif_sound_silent)
            else -> context.getString(com.example.dosezy.R.string.notif_sound_unknown)
        }
    }

    /**
     * Check if phone is not in silent mode
     */
    fun isPhoneNotSilent(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return audioManager?.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }

    /**
     * Check if app is ignoring battery optimizations - IMPROVED VERSION
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } catch (e: Exception) {
                // Log the error for debugging
                e.printStackTrace()
                false
            }
        } else {
            // For older versions, assume optimization is not enabled
            true
        }
    }

    /**
     * Get background permission status description
     */
    fun getBackgroundPermissionStatus(context: Context): String {
        val isIgnoring = isIgnoringBatteryOptimizations(context)
        return if (isIgnoring) {
            context.getString(R.string.notif_status_bg_allowed)
        } else {
            context.getString(R.string.notif_status_bg_restricted)
        }
    }

    /**
     * Direct request for battery optimization exemption (Android 6.0+)
     * This shows a system dialog that returns immediately
     */
    fun requestBatteryOptimizationExemption(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                // Check if the intent can be resolved
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    /**
     * Open battery optimization settings
     */
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            // First try the direct request (shows a dialog)
            val directRequestSuccess = requestBatteryOptimizationExemption(context)

            if (!directRequestSuccess) {
                // Fallback to settings page
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            // Final fallback: open app info page
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(context, context.getString(R.string.err_battery_optimization), Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Request notification permission (opens app settings for Android 13+)
     */
    fun requestNotificationPermission(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } else {
                // For older versions, open general notification settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.err_open_notification_settings), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Open sound settings
     */
    fun openSoundSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.err_open_sound_settings), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Open app notification settings
     */
    fun openAppNotificationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.err_open_app_settings), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Check if core notification & alarm requirements are met
     */
    fun areAllNotificationRequirementsMet(context: Context): Boolean {
        return hasNotificationPermission(context) &&
                canScheduleExactAlarms(context) &&
                isIgnoringBatteryOptimizations(context) &&
                isPhoneNotSilent(context)
    }

    /**
     * Check if Do Not Disturb mode is enabled
     */
    fun isDoNotDisturbEnabled(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager?.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE
        } else {
            false
        }
    }

    /**
     * Get Do Not Disturb status
     */
    fun getDoNotDisturbStatus(context: Context): String {
        return if (isDoNotDisturbEnabled(context)) {
            context.getString(R.string.notif_status_dnd_enabled)
        } else {
            context.getString(R.string.notif_status_dnd_disabled)
        }
    }

    /**
     * Check if app can schedule exact alarms (for Android 12+)
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = ContextCompat.getSystemService(context, android.app.AlarmManager::class.java)
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    /**
     * Request exact alarm permission (for Android 12+)
     */
    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.err_open_exact_alarm_settings), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Check if app has permission to draw overlays / display over other apps (SYSTEM_ALERT_WINDOW)
     */
    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Request overlay / display over other apps permission
     */
    fun requestOverlayPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    Toast.makeText(context, context.getString(R.string.err_open_app_settings), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Check if app can use full screen intents (Android 14+)
     */
    fun canUseFullScreenIntent(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.canUseFullScreenIntent() ?: true
        } else {
            true
        }
    }

    /**
     * Request full screen intent permission (Android 14+)
     */
    fun requestFullScreenIntentPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                openAppNotificationSettings(context)
            }
        }
    }

    /**
     * Detect known aggressive OEM manufacturers
     */
    fun getOemName(): String {
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> "Xiaomi / HyperOS"
            manufacturer.contains("samsung") -> "Samsung"
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> "Huawei / Honor"
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> "OPPO / Realme / OnePlus"
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> "Vivo / iQOO"
            manufacturer.contains("transsion") || manufacturer.contains("infinix") || manufacturer.contains("tecno") || manufacturer.contains("itel") -> "Transsion / Infinix / Tecno"
            else -> Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Device"
        }
    }

    fun isKnownAggressiveOem(): Boolean {
        val m = Build.MANUFACTURER?.lowercase() ?: ""
        return m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") ||
                m.contains("samsung") || m.contains("huawei") || m.contains("honor") ||
                m.contains("oppo") || m.contains("realme") || m.contains("oneplus") ||
                m.contains("vivo") || m.contains("iqoo") || m.contains("transsion") ||
                m.contains("infinix") || m.contains("tecno")
    }

    /**
     * Open OEM-specific background autostart & popup settings
     */
    fun openOemBackgroundSettings(context: Context): Boolean {
        val pkg = context.packageName
        val intentList = mutableListOf<Intent>()

        val m = Build.MANUFACTURER?.lowercase() ?: ""
        when {
            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") -> {
                intentList.add(Intent().setComponent(android.content.ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                intentList.add(Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                    setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                    putExtra("extra_pkgname", pkg)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                intentList.add(Intent().setComponent(android.content.ComponentName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity")).apply {
                    putExtra("extra_pkgname", pkg)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
            m.contains("huawei") || m.contains("honor") -> {
                intentList.add(Intent().setComponent(android.content.ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                intentList.add(Intent().setComponent(android.content.ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                intentList.add(Intent().setComponent(android.content.ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            }
            m.contains("oppo") || m.contains("realme") || m.contains("oneplus") -> {
                intentList.add(Intent().setComponent(android.content.ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                intentList.add(Intent().setComponent(android.content.ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                intentList.add(Intent().setComponent(android.content.ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                intentList.add(Intent().setComponent(android.content.ComponentName("com.oplus.safecenter", "com.oplus.safecenter.startupapp.StartupAppListActivity")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            }
            m.contains("vivo") || m.contains("iqoo") -> {
                intentList.add(Intent().setComponent(android.content.ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                intentList.add(Intent().setComponent(android.content.ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                intentList.add(Intent().setComponent(android.content.ComponentName("com.iqoo.secure", "com.iqoo.secure.MainGuideActivity")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            }
            m.contains("samsung") -> {
                // Let Samsung fall back to standard ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS or ACTION_APPLICATION_DETAILS_SETTINGS
            }
        }

        // Generic fallback intents
        intentList.add(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        intentList.add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$pkg")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })

        for (intent in intentList) {
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (_: Exception) {}
        }
        return false
    }

    /**
     * Get comprehensive notification status summary
     */
    fun getNotificationStatusSummary(context: Context): Map<String, Pair<Boolean, String>> {
        val overlayPassed = canDrawOverlays(context)
        val fullScreenPassed = canUseFullScreenIntent(context)
        val exactAlarmsPassed = canScheduleExactAlarms(context)
        val notifPassed = hasNotificationPermission(context)
        val batteryOptPassed = isIgnoringBatteryOptimizations(context)

        return mapOf(
            "notification_permission" to Pair(
                notifPassed,
                if (notifPassed) "Notification permission granted" else "Notification permission required"
            ),
            "exact_alarms" to Pair(
                exactAlarmsPassed,
                if (exactAlarmsPassed) "Exact alarms permitted" else "Exact alarms permission required"
            ),
            "battery_optimization" to Pair(
                batteryOptPassed,
                if (batteryOptPassed) "Unrestricted background running permitted" else "Battery optimization whitelist required"
            ),
            "overlay_permission" to Pair(
                overlayPassed,
                if (overlayPassed) "Display over other apps permitted" else "Display over other apps / Pop-up recommended"
            ),
            "fullscreen_intent" to Pair(
                fullScreenPassed,
                if (fullScreenPassed) "Full-screen alarm popups permitted" else "Full-screen alarm popup permission required"
            ),
            "sound_mode" to Pair(
                isPhoneNotSilent(context),
                getSoundStatus(context)
            ),
            "battery_level" to Pair(
                isBatterySufficient(context),
                getBatteryStatus(context)
            )
        )
    }

    /**
     * Get the number of failed notification requirements
     */
    fun getFailedRequirementCount(context: Context): Int {
        val status = getNotificationStatusSummary(context)
        return status.count { !it.value.first }
    }

    /**
     * Get the most critical failed requirement (if any)
     */
    fun getMostCriticalFailedRequirement(context: Context): String? {
        val status = getNotificationStatusSummary(context)
        return status.entries
            .filter { !it.value.first }
            .map { it.value.second }
            .firstOrNull()
    }
}