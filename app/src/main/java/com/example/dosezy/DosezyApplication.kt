package com.example.dosezy

import android.app.Application
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.dosezy.notifications.MedicineNotificationManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DosezyApplication : Application(), ImageLoaderFactory
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

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }

    @Inject
    lateinit var medicineNotificationManager: MedicineNotificationManager // UPDATED

}