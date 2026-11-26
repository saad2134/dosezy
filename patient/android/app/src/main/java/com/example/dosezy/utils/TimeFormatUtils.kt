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

    // Guard: Trim and try fallback patterns (unspaced AM/PM, single-digit 24h) to prevent DateTimeParseException (#BUG-TIME-PARSING-FLEXIBLE)
    fun parseTime(timeString: String, timeFormat: TimeFormat, locale: Locale = Locale.getDefault()): Date? {
        val trimmed = timeString.trim()
        val patterns = when (timeFormat) {
            TimeFormat.HOUR_12 -> listOf("h:mm a", "hh:mm a", "h:mma", "hh:mma")
            TimeFormat.HOUR_24 -> listOf("HH:mm", "H:mm")
        }
        for (pattern in patterns) {
            try {
                val formatter = DateTimeFormatter.ofPattern(pattern, locale)
                val localTime = LocalTime.parse(trimmed, formatter)
                return Date.from(localTime.atDate(java.time.LocalDate.now()).atZone(java.time.ZoneId.systemDefault()).toInstant())
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Standard date formatting helper enforcing consistent date display (e.g. "20 Nov" or "20 Nov 2026") (#F15).
     */
    fun formatStandardDate(date: java.time.LocalDate, locale: Locale = Locale.getDefault(), includeYear: Boolean = true): String {
        val pattern = if (includeYear) "d MMM yyyy" else "d MMM"
        return date.format(DateTimeFormatter.ofPattern(pattern, locale))
    }
}