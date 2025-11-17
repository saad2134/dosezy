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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationUtils {

    /**
     * Check if the app has notification permission
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            // For older versions, notification permission is granted by default
            true
        }
    }

    /**
     * Get current battery level as percentage
     */
    fun getBatteryLevel(context: Context): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        return batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
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
        return "Battery is at $batteryLevel%"
    }

    /**
     * Get current sound mode status
     */
    fun getSoundStatus(context: Context): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return when (audioManager?.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> "Phone is not silent."
            AudioManager.RINGER_MODE_VIBRATE -> "Phone is in vibrate mode."
            AudioManager.RINGER_MODE_SILENT -> "Phone is silent."
            else -> "Unknown sound mode."
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
     * Check if app is ignoring battery optimizations
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            // For older versions, assume permission is granted
            true
        }
    }

    /**
     * Get background permission status description
     */
    fun getBackgroundPermissionStatus(context: Context): String {
        return if (isIgnoringBatteryOptimizations(context)) {
            "App can run in background."
        } else {
            "No permission to run in background."
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
     * Request notification permission (opens app settings for Android 13+)
     */
    fun requestNotificationPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        } else {
            // For older versions, open general notification settings
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Check if battery optimization exemption can be requested
     */
    fun canRequestBatteryOptimizationExemption(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        return intent.resolveActivity(context.packageManager) != null
    }

    /**
     * Request battery optimization exemption with fallback
     */
    fun requestBatteryOptimizationExemption(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    true
                } else {
                    // Fallback to battery optimization settings
                    openBatteryOptimizationSettings(context)
                    false
                }
            } catch (e: Exception) {
                openBatteryOptimizationSettings(context)
                false
            }
        }
        return false
    }

    /**
     * Open battery optimization settings
     */
    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }

    /**
     * Open sound settings
     */
    fun openSoundSettings(context: Context) {
        val intent = Intent(Settings.ACTION_SOUND_SETTINGS)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }

    /**
     * Open app notification settings
     */
    fun openAppNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
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
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
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