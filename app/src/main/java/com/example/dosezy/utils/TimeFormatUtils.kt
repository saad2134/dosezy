package com.example.dosezy.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.dosezy.data.model.TimeFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
object TimeFormatUtils {

    fun formatTime(localDateTime: java.time.LocalDateTime, timeFormat: TimeFormat): String {
        val formatter = when (timeFormat) {
            TimeFormat.HOUR_12 -> SimpleDateFormat("h:mm a", Locale.getDefault())
            TimeFormat.HOUR_24 -> SimpleDateFormat("HH:mm", Locale.getDefault())
        }

        return formatter.format(
            Date.from(localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant())
        )
    }

    fun parseTime(timeString: String, timeFormat: TimeFormat): Date? {
        val formatter = when (timeFormat) {
            TimeFormat.HOUR_12 -> SimpleDateFormat("h:mm a", Locale.getDefault())
            TimeFormat.HOUR_24 -> SimpleDateFormat("HH:mm", Locale.getDefault())
        }

        return try {
            formatter.parse(timeString)
        } catch (e: Exception) {
            null
        }
    }
}