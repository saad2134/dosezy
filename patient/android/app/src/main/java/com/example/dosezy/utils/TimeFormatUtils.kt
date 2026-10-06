package com.example.dosezy.utils

import com.example.dosezy.data.model.TimeFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

// Guard: Core library desugaring enables java.time on API 24+; do not restrict to Android O (API 26+) so Android 7.0/7.1 devices format time correctly
object TimeFormatUtils {

    fun formatTime(localDateTime: LocalDateTime, timeFormat: TimeFormat, locale: Locale = Locale.getDefault()): String {
        val formatter = when (timeFormat) {
            TimeFormat.HOUR_12 -> DateTimeFormatter.ofPattern("h:mm a", locale)
            TimeFormat.HOUR_24 -> DateTimeFormatter.ofPattern("HH:mm", locale)
        }
        return localDateTime.format(formatter)
    }

    fun formatLocalTime(localTime: LocalTime, timeFormat: TimeFormat, locale: Locale = Locale.getDefault()): String {
        val formatter = when (timeFormat) {
            TimeFormat.HOUR_12 -> DateTimeFormatter.ofPattern("h:mm a", locale)
            TimeFormat.HOUR_24 -> DateTimeFormatter.ofPattern("HH:mm", locale)
        }
        return localTime.format(formatter)
    }

    fun parseTime(timeString: String, timeFormat: TimeFormat, locale: Locale = Locale.getDefault()): Date? {
        val formatter = when (timeFormat) {
            TimeFormat.HOUR_12 -> DateTimeFormatter.ofPattern("h:mm a", locale)
            TimeFormat.HOUR_24 -> DateTimeFormatter.ofPattern("HH:mm", locale)
        }
        return try {
            val localTime = LocalTime.parse(timeString, formatter)
            Date.from(localTime.atDate(java.time.LocalDate.now()).atZone(java.time.ZoneId.systemDefault()).toInstant())
        } catch (e: Exception) {
            null
        }
    }
}