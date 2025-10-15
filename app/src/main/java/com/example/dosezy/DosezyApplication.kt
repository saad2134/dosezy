package com.example.dosezy

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DosezyApplication : Application()
{

    override fun onCreate() {
        super.onCreate()
        Log.d("DosezyApp", "Application onCreate called")

        // Initialize anything that might crash here
        try {
            // This will trigger database initialization
            // and reveal any Room database issues
        } catch (e: Exception) {
            Log.e("DosezyApp", "Error during initialization", e)
        }
    }
}