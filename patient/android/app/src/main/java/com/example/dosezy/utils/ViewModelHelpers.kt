/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.example.dosezy.ui.viewmodels.AnalyticsViewModel
import com.example.dosezy.ui.viewmodels.MedicineViewModel
import com.example.dosezy.ui.viewmodels.ScheduleViewModel
import com.example.dosezy.ui.viewmodels.UserViewModel

/**
 * Traverses the [Context] hierarchy unwrapping [ContextWrapper] instances to locate the hosting [ComponentActivity].
 */
fun Context.findActivity(): ComponentActivity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Scopes a Hilt ViewModel to the hosting Activity if available, falling back to NavBackStackEntry/LocalViewModelStoreOwner.
 */
@Composable
inline fun <reified VM : ViewModel> sharedActivityViewModel(): VM {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    return if (activity != null) {
        hiltViewModel(activity)
    } else {
        hiltViewModel()
    }
}

@Composable
fun sharedUserViewModel(): UserViewModel = sharedActivityViewModel()

@Composable
fun sharedScheduleViewModel(): ScheduleViewModel = sharedActivityViewModel()

@Composable
fun sharedMedicineViewModel(): MedicineViewModel = sharedActivityViewModel()

@Composable
fun sharedAnalyticsViewModel(): AnalyticsViewModel = sharedActivityViewModel()
