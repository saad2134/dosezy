/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

object StorageUtils {
    private const val TAG = "StorageUtils"

    // Warn if available space is below 25 MB to prevent SQLiteFullException
    const val CRITICAL_STORAGE_THRESHOLD_BYTES = 25L * 1024L * 1024L

    /**
     * Checks if internal flash storage has less than CRITICAL_STORAGE_THRESHOLD_BYTES usable space.
     */
    fun isStorageCriticallyLow(context: Context): Boolean {
        return try {
            val usable = context.filesDir.usableSpace
            usable in 1..CRITICAL_STORAGE_THRESHOLD_BYTES
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check usable space", e)
            false
        }
    }

    /**
     * Checks if internal flash storage is completely full (0 bytes).
     */
    fun isStorageCompletelyFull(context: Context): Boolean {
        return try {
            context.filesDir.usableSpace <= 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if storage is completely full", e)
            false
        }
    }

    /**
     * Opens system storage settings so the user can clean up space.
     */
    fun openStorageSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
        } catch (_: Exception) {}

        // Fallback to general settings
        try {
            val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not open system settings", e)
        }
    }
}
