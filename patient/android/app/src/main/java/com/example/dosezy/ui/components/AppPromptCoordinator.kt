/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dosezy.R
import com.example.dosezy.data.model.User
import com.example.dosezy.data.preferences.UserPreferencesDataStore
import com.example.dosezy.utils.NotificationUtils
import com.example.dosezy.utils.StorageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Coordinates system permissions, storage warnings, and optimization exemption prompts.
 * Decouples onboarding and permission dialog orchestrations from the host activity.
 */
@Composable
fun AppPromptCoordinator(
    currentUser: User?,
    preferencesDataStore: UserPreferencesDataStore? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefsStore = remember(preferencesDataStore, context) {
        preferencesDataStore ?: UserPreferencesDataStore(context)
    }

    val batteryPromptDismissed by prefsStore.batteryPromptDismissedFlow.collectAsState(initial = false)
    val overlayPromptDismissed by prefsStore.overlayPromptDismissedFlow.collectAsState(initial = false)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    var showBatteryPrompt by remember { mutableStateOf(false) }
    var showOverlayPrompt by remember { mutableStateOf(false) }
    var showLowStorageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (StorageUtils.isStorageCriticallyLow(context)) {
            showLowStorageDialog = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (currentUser != null) {
            // Guard: Check legacy SharedPreferences as immediate synchronous fallback
            val legacyBatteryDismissed = try {
                context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    .getBoolean("battery_prompt_dismissed", false)
            } catch (_: Exception) { false }

            val isBatteryDismissed = batteryPromptDismissed || legacyBatteryDismissed

            if (!isBatteryDismissed && !NotificationUtils.isIgnoringBatteryOptimizations(context)) {
                delay(1200)
                showBatteryPrompt = true
            } else if (!NotificationUtils.canDrawOverlays(context)) {
                val legacyOverlayDismissed = try {
                    context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                        .getBoolean("overlay_prompt_dismissed", false)
                } catch (_: Exception) { false }

                val isOverlayDismissed = overlayPromptDismissed || legacyOverlayDismissed
                if (!isOverlayDismissed) {
                    delay(1200)
                    showOverlayPrompt = true
                }
            }
        }
    }

    if (showBatteryPrompt) {
        AlertDialog(
            onDismissRequest = {
                coroutineScope.launch { prefsStore.setBatteryPromptDismissed(true) }
                showBatteryPrompt = false
            },
            title = {
                Text(
                    text = stringResource(R.string.notif_bg_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.notif_bg_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch { prefsStore.setBatteryPromptDismissed(true) }
                        showBatteryPrompt = false
                        NotificationUtils.requestBatteryOptimizationExemption(context)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.dialog_enable),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1193D4)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch { prefsStore.setBatteryPromptDismissed(true) }
                        showBatteryPrompt = false
                    }
                ) {
                    Text(stringResource(R.string.dialog_later))
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showOverlayPrompt) {
        AlertDialog(
            onDismissRequest = { showOverlayPrompt = false },
            title = {
                Text(
                    text = stringResource(R.string.notif_overlay_prompt_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.notif_overlay_prompt_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch { prefsStore.setOverlayPromptDismissed(true) }
                        showOverlayPrompt = false
                        NotificationUtils.requestOverlayPermission(context)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.dialog_enable),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1193D4)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch { prefsStore.setOverlayPromptDismissed(true) }
                        showOverlayPrompt = false
                    }
                ) {
                    Text(stringResource(R.string.dialog_later))
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showLowStorageDialog) {
        LowStorageDialog(
            onDismiss = { showLowStorageDialog = false }
        )
    }
}
