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
            "App can run in background."
        } else {
            "Background restrictions may prevent notifications."
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
                Toast.makeText(context, "Please enable 'Don't optimize' in Battery settings", Toast.LENGTH_LONG).show()
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
            Toast.makeText(context, "Cannot open notification settings", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Cannot open sound settings", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Cannot open app settings", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Check if all notification requirements are met
     */
    fun areAllNotificationRequirementsMet(context: Context): Boolean {
        return hasNotificationPermission(context) &&
                isBatterySufficient(context) &&
                isPhoneNotSilent(context) &&
                isIgnoringBatteryOptimizations(context)
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
            "Do Not Disturb is enabled."
        } else {
            "Do Not Disturb is disabled."
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
                Toast.makeText(context, "Cannot open exact alarm settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Get comprehensive notification status summary
     */
    fun getNotificationStatusSummary(context: Context): Map<String, Pair<Boolean, String>> {
        return mapOf(
            "notification_permission" to Pair(
                hasNotificationPermission(context),
                if (hasNotificationPermission(context)) "Notification permission granted" else "Notification permission required"
            ),
            "battery_level" to Pair(
                isBatterySufficient(context),
                getBatteryStatus(context)
            ),
            "sound_mode" to Pair(
                isPhoneNotSilent(context),
                getSoundStatus(context)
            ),
            "battery_optimization" to Pair(
                isIgnoringBatteryOptimizations(context),
                getBackgroundPermissionStatus(context)
            ),
            "exact_alarms" to Pair(
                canScheduleExactAlarms(context),
                if (canScheduleExactAlarms(context)) "Can schedule exact alarms" else "Cannot schedule exact alarms"
            ),
            "do_not_disturb" to Pair(
                !isDoNotDisturbEnabled(context),
                getDoNotDisturbStatus(context)
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