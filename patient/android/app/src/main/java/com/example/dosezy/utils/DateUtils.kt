// DateUtils.kt
package com.example.dosezy.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentDayOfWeek(): String {
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
    }

    fun getCurrentDayOfWeekLegacy(): String {
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
    }

    fun formatTimeLegacy(date: Date): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
    }
}