package com.example.dosezy.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.dosezy.data.model.MedicationStatus
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.O)
object TimeCalculationUtils {

    fun calculateTimeDifference(scheduledTime: LocalDateTime, currentTime: LocalDateTime): TimeDifference {
        val duration = Duration.between(currentTime, scheduledTime)
        val totalMinutes = duration.toMinutes()

        return if (totalMinutes >= 0) {
            // Future time (to be taken)
            val hours = TimeUnit.MINUTES.toHours(totalMinutes)
            val minutes = totalMinutes - TimeUnit.HOURS.toMinutes(hours)
            TimeDifference(hours.toInt(), minutes.toInt(), false)
        } else {
            // Past time (late)
            val absoluteMinutes = -totalMinutes
            val hours = TimeUnit.MINUTES.toHours(absoluteMinutes)
            val minutes = absoluteMinutes - TimeUnit.HOURS.toMinutes(hours)
            TimeDifference(hours.toInt(), minutes.toInt(), true)
        }
    }

    fun isLate(scheduledTime: LocalDateTime, currentTime: LocalDateTime, lateAfterHours: Int, missedAfterHours: Int): Boolean {
        val duration = Duration.between(scheduledTime, currentTime)
        val hours = duration.toHours()
        return hours >= lateAfterHours.toLong() && hours < missedAfterHours.toLong()
    }

    fun isMissed(scheduledTime: LocalDateTime, currentTime: LocalDateTime, missedAfterHours: Int): Boolean {
        val duration = Duration.between(scheduledTime, currentTime)
        return duration.toHours() >= missedAfterHours.toLong()
    }

    fun getStatus(scheduledTime: LocalDateTime, currentTime: LocalDateTime, lateAfterHours: Int, missedAfterHours: Int): MedicationStatus {
        return when {
            currentTime.isBefore(scheduledTime) -> MedicationStatus.PENDING
            isMissed(scheduledTime, currentTime, missedAfterHours) -> MedicationStatus.MISSED
            else -> MedicationStatus.PENDING
        }
    }

    fun formatTimeDifference(timeDiff: TimeDifference): String {
        return if (timeDiff.isLate) {
            "${timeDiff.hours}h ${timeDiff.minutes}m ago"
        } else {
            "To be taken in ${timeDiff.hours}h ${timeDiff.minutes}m"
        }
    }
}

data class TimeDifference(
    val hours: Int,
    val minutes: Int,
    val isLate: Boolean
)