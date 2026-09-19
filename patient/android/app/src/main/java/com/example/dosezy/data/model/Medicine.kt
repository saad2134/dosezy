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
    val imageUri: String? = null,
    val currentStock: Int? = null,
    val refillThreshold: Int? = null,
    val autoDeductOnTake: Boolean = true,
    val notes: String? = null,
    val pillShape: PillShape = PillShape.ROUND,
    val pillColor: String = "#1193D4",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val durationDays: Int? = null,
    val isArchived: Boolean = false
) {

    /**
     * Generates schedule entries for this medicine for a given date range
     * @param startDateRange The start date for generating schedule entries
     * @param days Number of days to generate entries for (default: 30 days)
     * @return List of ScheduleEntry objects
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun generateScheduleEntries(startDateRange: LocalDate, days: Int = 30): List<ScheduleEntry> {
        // As-needed (PRN) medications do not generate automated scheduled reminder slots
        if (frequency.pattern == FrequencyPattern.AS_NEEDED) {
            return emptyList()
        }

        val entries = mutableListOf<ScheduleEntry>()
        val effectiveStart = if (startDate != null && startDate.isAfter(startDateRange)) startDate else startDateRange
        val effectiveEnd = if (endDate != null && endDate.isBefore(startDateRange.plusDays(days.toLong()))) endDate else startDateRange.plusDays(days.toLong())

        if (effectiveStart.isAfter(effectiveEnd)) return emptyList()

        var currentDate = effectiveStart
        while (currentDate.isBefore(effectiveEnd) || currentDate.isEqual(effectiveEnd)) {
            // Check if medicine should be taken on this day based on frequency
            if (shouldTakeOnDate(currentDate)) {
                val now = LocalDateTime.now()
                scheduledTimes.forEach { time ->
                    val cleanTime = time.withSecond(0).withNano(0)
                    val scheduledDateTime = LocalDateTime.of(currentDate, cleanTime)

                    // Skip past reminder times on the current day to avoid immediate missed status
                    if (currentDate.isEqual(now.toLocalDate()) && scheduledDateTime.isBefore(now.minusMinutes(15))) {
                        return@forEach
                    }

                    val entryId = "${medicineId}_${currentDate}_${cleanTime}".replace(":", "_").replace("-", "_")
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
        // Verify within finite course bounds
        if (startDate != null && date.isBefore(startDate)) return false
        if (endDate != null && date.isAfter(endDate)) return false

        return when (frequency.pattern) {
            FrequencyPattern.DAILY -> true
            FrequencyPattern.AS_NEEDED -> false
            FrequencyPattern.EVERY_X_HOURS -> true
            FrequencyPattern.EVERY_X_DAYS -> {
                val interval = (frequency.intervalDays ?: 2).coerceAtLeast(1)
                val baseDate = startDate ?: LocalDate.of(2024, 1, 1)
                val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(baseDate, date)
                daysDiff % interval == 0L
            }
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
            FrequencyPattern.CUSTOM -> true
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
     * Gets a display string for the frequency (e.g., "Daily", "3 times per week")
     */
    fun getFrequencyDisplay(): String {
        return when (frequency.pattern) {
            FrequencyPattern.DAILY -> "Daily"
            FrequencyPattern.AS_NEEDED -> "As Needed (PRN)"
            FrequencyPattern.EVERY_X_HOURS -> "Every ${frequency.intervalHours ?: 4} Hours"
            FrequencyPattern.EVERY_X_DAYS -> "Every ${frequency.intervalDays ?: 2} Days"
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

    /**
     * Calculates the number of inventory stock units to deduct when this medication is taken.
     *
     * - For solid medications measured by chemical strength (MG, MCG): physical stock is tracked
     *   in count of units/pills (e.g. 150 pills in a bottle). Taking a dose consumes 1 unit (not 150/500 units).
     * - For TABLET / CAPSULE: deducts the tablet/capsule quantity (defaulting to 1 if dosage is <= 0 or
     *   if an unusually large number like 500 was entered representing mg strength).
     * - For liquid / drops (ML, DROP): deducts the specified dose quantity (at least 1).
     */
    fun getStockDeductionAmount(): Int {
        return when (dosageUnit) {
            DosageUnit.MG, DosageUnit.MCG -> 1
            DosageUnit.TABLET, DosageUnit.CAPSULE -> {
                val count = dosage.toInt()
                if (count in 1..10) count else 1
            }
            DosageUnit.DROP, DosageUnit.ML -> {
                dosage.toInt().coerceAtLeast(1)
            }
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

enum class PillShape {
    ROUND, CAPSULE, OVAL, LIQUID, INHALER, INJECTION, DROPS, PATCH
}

@androidx.compose.runtime.Composable
fun PillShape.getLocalizedName(): String {
    return when (this) {
        PillShape.ROUND -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.pill_shape_round)
        PillShape.CAPSULE -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.pill_shape_capsule)
        PillShape.OVAL -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.pill_shape_oval)
        PillShape.LIQUID -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.pill_shape_liquid)
        PillShape.INHALER -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.pill_shape_inhaler)
        PillShape.INJECTION -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.pill_shape_injection)
        PillShape.DROPS -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.pill_shape_drops)
        PillShape.PATCH -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.pill_shape_patch)
    }
}

data class Frequency(
    val pattern: FrequencyPattern,
    val daysPerWeek: Int? = null,
    val daysPerMonth: Int? = null,
    val selectedDaysOfWeek: List<Int>? = null,  // 1=Mon, 2=Tue, ..., 7=Sun (ISO)
    val selectedDaysOfMonth: List<Int>? = null,  // 1-31
    val intervalHours: Int? = null,              // for EVERY_X_HOURS (e.g. 4, 6, 8, 12)
    val intervalDays: Int? = null                // for EVERY_X_DAYS (e.g. 2, 3, 5)
)

enum class FrequencyPattern {
    DAILY, WEEKLY, MONTHLY, CUSTOM, AS_NEEDED, EVERY_X_HOURS, EVERY_X_DAYS
}

@androidx.compose.runtime.Composable
fun FrequencyPattern.getLocalizedName(): String {
    return when (this) {
        FrequencyPattern.DAILY -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.freq_daily)
        FrequencyPattern.WEEKLY -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.freq_weekly)
        FrequencyPattern.MONTHLY -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.freq_monthly)
        FrequencyPattern.CUSTOM -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.freq_custom)
        FrequencyPattern.AS_NEEDED -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.freq_as_needed)
        FrequencyPattern.EVERY_X_HOURS -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.freq_every_x_hours)
        FrequencyPattern.EVERY_X_DAYS -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.freq_every_x_days)
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
        FrequencyPattern.AS_NEEDED -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.freq_as_needed)
        FrequencyPattern.EVERY_X_HOURS -> {
            val hours = frequency.intervalHours ?: 4
            androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.freq_every_hours_format, hours)
        }
        FrequencyPattern.EVERY_X_DAYS -> {
            val days = frequency.intervalDays ?: 2
            androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.freq_every_days_format, days)
        }
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