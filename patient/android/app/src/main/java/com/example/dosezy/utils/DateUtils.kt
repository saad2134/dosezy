// DateUtils.kt
package com.example.dosezy.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentDayOfWeek(locale: Locale = Locale.getDefault()): String {
        return SimpleDateFormat("EEEE", locale).format(Date())
    }

    fun getCurrentDayOfWeekLegacy(locale: Locale = Locale.getDefault()): String {
        return SimpleDateFormat("EEEE", locale).format(Date())
    }

    fun formatTimeLegacy(date: Date, locale: Locale = Locale.getDefault()): String {
        return SimpleDateFormat("h:mm a", locale).format(date)
    }
}