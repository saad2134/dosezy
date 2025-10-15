package com.example.dosezy.data.converters

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.TypeConverter
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.DosageUnit
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class Converters {

    // Gender converters
    @TypeConverter
    fun fromGender(gender: Gender): String = gender.name

    @TypeConverter
    fun toGender(gender: String): Gender = enumValueOf(gender)

    // DosageUnit converters
    @TypeConverter
    fun fromDosageUnit(unit: DosageUnit): String = unit.name

    @TypeConverter
    fun toDosageUnit(unit: String): DosageUnit = enumValueOf(unit)

    // MedicationStatus converters
    @TypeConverter
    fun fromStatus(status: MedicationStatus): String = status.name

    @TypeConverter
    fun toStatus(status: String): MedicationStatus = enumValueOf(status)

    // LocalTime list converters
    @TypeConverter
    fun fromLocalTimeList(times: List<LocalTime>): String {
        return times.joinToString(",") { it.toString() }
    }

    @TypeConverter
    fun toLocalTimeList(timesString: String): List<LocalTime> {
        return if (timesString.isEmpty()) emptyList()
        else timesString.split(",").map { LocalTime.parse(it) }
    }

    // LocalDateTime converters
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime): String = dateTime.toString()

    @TypeConverter
    fun toLocalDateTime(dateTimeString: String): LocalDateTime = LocalDateTime.parse(dateTimeString)

    // LocalDate converters
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun toLocalDate(dateString: String): LocalDate = LocalDate.parse(dateString)

    // Frequency converters
    @TypeConverter
    fun fromFrequency(frequency: Frequency): String {
        return "${frequency.pattern.name},${frequency.daysPerWeek},${frequency.daysPerMonth}"
    }

    @TypeConverter
    fun toFrequency(frequencyString: String): Frequency {
        val parts = frequencyString.split(",")
        return Frequency(
            pattern = enumValueOf(parts[0]),
            daysPerWeek = parts[1].toIntOrNull(),
            daysPerMonth = parts[2].toIntOrNull()
        )
    }

    // FrequencyPattern converters
    @TypeConverter
    fun fromFrequencyPattern(pattern: FrequencyPattern): String = pattern.name

    @TypeConverter
    fun toFrequencyPattern(pattern: String): FrequencyPattern = enumValueOf(pattern)
}