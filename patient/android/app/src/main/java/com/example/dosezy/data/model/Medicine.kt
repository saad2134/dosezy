package com.example.dosezy.data.model

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.dosezy.data.converters.Converters
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(
    tableName = "medicines",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
@TypeConverters(Converters::class)
data class Medicine(
    @PrimaryKey val medicineId: String,
    val userId: String, // Foreign key to User
    val medicationName: String,
    val dosage: Double,
    val dosageUnit: DosageUnit,
    val timesPerDay: Int,
    val frequency: Frequency,
    val scheduledTimes: List<LocalTime>, // Multiple times per day
    val imageUri: String? = null
) {

    /**
     * Generates schedule entries for this medicine for a given date range
     * @param startDate The start date for generating schedule entries
     * @param days Number of days to generate entries for (default: 30 days)
     * @return List of ScheduleEntry objects
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun generateScheduleEntries(startDate: LocalDate, days: Int = 30): List<ScheduleEntry> {
        val entries = mutableListOf<ScheduleEntry>()
        val endDate = startDate.plusDays(days.toLong())

        var currentDate = startDate
        while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
            // Check if medicine should be taken on this day based on frequency
            if (shouldTakeOnDate(currentDate)) {
                scheduledTimes.forEach { time ->
                    val scheduledDateTime = LocalDateTime.of(currentDate, time)

                    val entryId = "${medicineId}_${currentDate}_${time}".replace(":", "_").replace("-", "_")
                    val entry = ScheduleEntry(
                        entryId = entryId,
                        userId = userId,
                        medicineId = medicineId,
                        scheduledDateTime = scheduledDateTime,
                        status = MedicationStatus.PENDING
                    )
                    entries.add(entry)
                }
            }
            currentDate = currentDate.plusDays(1)
        }

        return entries
    }

    /**
     * Determines if the medicine should be taken on the given date based on frequency pattern
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun shouldTakeOnDate(date: LocalDate): Boolean {
        return when (frequency.pattern) {
            FrequencyPattern.DAILY -> true
            FrequencyPattern.WEEKLY -> {
                val selectedDays = frequency.selectedDaysOfWeek
                if (!selectedDays.isNullOrEmpty()) {
                    // Use explicitly selected days (1=Mon ... 7=Sun, ISO standard)
                    date.dayOfWeek.value in selectedDays
                } else {
                    // Fallback: treat daysPerWeek as "first N days of the week"
                    val daysPerWeek = frequency.daysPerWeek ?: 7
                    date.dayOfWeek.value <= daysPerWeek
                }
            }
            FrequencyPattern.MONTHLY -> {
                val selectedDays = frequency.selectedDaysOfMonth
                if (!selectedDays.isNullOrEmpty()) {
                    // Use explicitly selected days of month
                    val maxDayInMonth = date.lengthOfMonth()
                    selectedDays.any { targetDay ->
                        val effectiveDay = targetDay.coerceAtMost(maxDayInMonth)
                        date.dayOfMonth == effectiveDay
                    }
                } else {
                    // Fallback: treat daysPerMonth as "first N days of the month"
                    val daysPerMonth = frequency.daysPerMonth ?: 30
                    date.dayOfMonth <= daysPerMonth
                }
            }
            FrequencyPattern.CUSTOM -> {
                // For custom frequency, default to daily
                true
            }
        }
    }

    /**
     * Gets a display string for the dosage (e.g., "100mg", "5mL")
     */
    fun getDosageDisplay(): String {
        val unitAbbr = when (dosageUnit) {
            DosageUnit.MG -> "mg"
            DosageUnit.MCG -> "mcg"
            DosageUnit.ML -> "mL"
            DosageUnit.DROP -> "drop"
            DosageUnit.TABLET -> "tablet"
            DosageUnit.CAPSULE -> "capsule"
        }

        // Remove decimal if it's a whole number
        return if (dosage % 1 == 0.0) {
            "${dosage.toInt()} $unitAbbr"
        } else {
            "$dosage $unitAbbr"
        }
    }

    /**
     * Gets the abbreviation for the dosage unit
     */
    private fun getDosageUnitAbbreviation(): String {
        return when (dosageUnit) {
            DosageUnit.MG -> "mg"
            DosageUnit.MCG -> "mcg"
            DosageUnit.ML -> "mL"
            DosageUnit.DROP -> "drop"
            DosageUnit.TABLET -> "tablet"
            DosageUnit.CAPSULE -> "capsule"
        }
    }

    /**
     * Gets a display string for the frequency (e.g., "Daily", "3 times per week")
     */
    fun getFrequencyDisplay(): String {
        return when (frequency.pattern) {
            FrequencyPattern.DAILY -> "Daily"
            FrequencyPattern.WEEKLY -> {
                val days = frequency.daysPerWeek ?: 7
                "$days times per week"
            }
            FrequencyPattern.MONTHLY -> {
                val days = frequency.daysPerMonth ?: 30
                "$days times per month"
            }
            FrequencyPattern.CUSTOM -> "Custom"
        }
    }

    /**
     * Gets the scheduled times as formatted strings (e.g., ["8:00 AM", "2:00 PM"])
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getFormattedScheduledTimes(): List<String> {
        return scheduledTimes.map { time ->
            val hour = time.hour
            val minute = time.minute
            val amPm = if (hour < 12) "AM" else "PM"
            val displayHour = if (hour % 12 == 0) 12 else hour % 12
            String.format("%d:%02d %s", displayHour, minute, amPm)
        }
    }
}

enum class DosageUnit {
    MG, MCG, ML, DROP, TABLET, CAPSULE
}

@androidx.compose.runtime.Composable
fun DosageUnit.getLocalizedName(): String {
    return when (this) {
        DosageUnit.MG -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.unit_mg)
        DosageUnit.MCG -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.unit_mcg)
        DosageUnit.ML -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.unit_ml)
        DosageUnit.DROP -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.unit_drop)
        DosageUnit.TABLET -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.unit_tablet)
        DosageUnit.CAPSULE -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.unit_capsule)
    }
}

data class Frequency(
    val pattern: FrequencyPattern,
    val daysPerWeek: Int? = null,
    val daysPerMonth: Int? = null,
    val selectedDaysOfWeek: List<Int>? = null,  // 1=Mon, 2=Tue, ..., 7=Sun (ISO)
    val selectedDaysOfMonth: List<Int>? = null   // 1-31
)

enum class FrequencyPattern {
    DAILY, WEEKLY, MONTHLY, CUSTOM
}

@androidx.compose.runtime.Composable
fun FrequencyPattern.getLocalizedName(): String {
    return when (this) {
        FrequencyPattern.DAILY -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.freq_daily)
        FrequencyPattern.WEEKLY -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.freq_weekly)
        FrequencyPattern.MONTHLY -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.freq_monthly)
        FrequencyPattern.CUSTOM -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.freq_custom)
    }
}

@androidx.compose.runtime.Composable
fun Medicine.getLocalizedDosageDisplay(): String {
    val countStr = if (dosage % 1.0 == 0.0) dosage.toInt().toString() else dosage.toString()
    return "$countStr ${dosageUnit.getLocalizedName()}"
}

@androidx.compose.runtime.Composable
fun Medicine.getLocalizedFrequencyDisplay(): String {
    return when (frequency.pattern) {
        FrequencyPattern.DAILY -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.freq_daily)
        FrequencyPattern.WEEKLY -> {
            val days = frequency.daysPerWeek ?: 7
            androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.times_per_week_format, days)
        }
        FrequencyPattern.MONTHLY -> {
            val days = frequency.daysPerMonth ?: 30
            androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.times_per_month_format, days)
        }
        FrequencyPattern.CUSTOM -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.freq_custom)
    }
}