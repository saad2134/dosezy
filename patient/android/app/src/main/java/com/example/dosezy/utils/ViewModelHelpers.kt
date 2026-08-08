package com.example.dosezy.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dosezy.ui.viewmodels.UserViewModel
import com.example.dosezy.ui.viewmodels.ScheduleViewModel
import com.example.dosezy.ui.viewmodels.MedicineViewModel

@Composable
fun sharedUserViewModel(): UserViewModel {
    val context = LocalContext.current
    val activity = remember(context) {
        var c = context
        while (c is ContextWrapper) {
            if (c is ComponentActivity) break
            c = c.baseContext
        }
        c as? ComponentActivity
    }
    return if (activity != null) {
        hiltViewModel(activity)
    } else {
        hiltViewModel()
    }
}

@Composable
fun sharedScheduleViewModel(): ScheduleViewModel {
    val context = LocalContext.current
    val activity = remember(context) {
        var c = context
        while (c is ContextWrapper) {
            if (c is ComponentActivity) break
            c = c.baseContext
        }
        c as? ComponentActivity
    }
    return if (activity != null) {
        hiltViewModel(activity)
    } else {
        hiltViewModel()
    }
}

@Composable
fun sharedMedicineViewModel(): MedicineViewModel {
    val context = LocalContext.current
    val activity = remember(context) {
        var c = context
        while (c is ContextWrapper) {
            if (c is ComponentActivity) break
            c = c.baseContext
        }
        c as? ComponentActivity
    }
    return if (activity != null) {
        hiltViewModel(activity)
    } else {
        hiltViewModel()
    }
}
