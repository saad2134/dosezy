package com.example.dosezy

import android.app.Application
import android.os.Build
import android.util.Log
import com.example.dosezy.notifications.MedicineNotificationManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DosezyApplication : Application()
{

    override fun onCreate() {
        super.onCreate()
        Log.d("DosezyApp", "Application onCreate called")

        // Initialize anything that might crash here
        try {
            // trigger database initialization
            // reveal any Room database issues
        } catch (e: Exception) {
            Log.e("DosezyApp", "Error during initialization", e)
        }

        // Schedule alarms for current user on app start
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CoroutineScope(Dispatchers.IO).launch {
                medicineNotificationManager.scheduleAllAlarmsForCurrentUser() // UPDATED
            }
        }
    }

    @Inject
    lateinit var medicineNotificationManager: MedicineNotificationManager // UPDATED

}