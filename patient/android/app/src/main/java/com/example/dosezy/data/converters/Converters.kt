package com.example.dosezy.data.converters

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.TypeConverter
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.Language
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Theme
import com.example.dosezy.data.model.TimeFormat
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
    fun fromMedicationStatus(status: MedicationStatus): String = status.name

    @TypeConverter
    fun toMedicationStatus(status: String): MedicationStatus = enumValueOf(status)

    // LocalDateTime converters
    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): Long? = dateTime?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun toLocalDateTime(value: Long?): LocalDateTime? = value?.let {
        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
    }

    // LocalDate converters
    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? = value?.let {
        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    }

    // LocalTime converters
    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? = time?.format(DateTimeFormatter.ISO_LOCAL_TIME)

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it, DateTimeFormatter.ISO_LOCAL_TIME) }

    // List<LocalTime> converters
    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun fromLocalTimeList(times: List<LocalTime>?): String? {
        return times?.let { list ->
            val jsonArray = JSONArray()
            list.forEach { time ->
                jsonArray.put(time.format(DateTimeFormatter.ISO_LOCAL_TIME))
            }
            jsonArray.toString()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun toLocalTimeList(value: String?): List<LocalTime>? {
        return if (value == null) {
            null
        } else {
            val jsonArray = JSONArray(value)
            val list = mutableListOf<LocalTime>()
            for (i in 0 until jsonArray.length()) {
                val timeString = jsonArray.getString(i)
                list.add(LocalTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_TIME))
            }
            list
        }
    }

    // PillShape converters
    @TypeConverter
    fun fromPillShape(shape: com.example.dosezy.data.model.PillShape?): String = (shape ?: com.example.dosezy.data.model.PillShape.ROUND).name

    @TypeConverter
    fun toPillShape(value: String?): com.example.dosezy.data.model.PillShape = try {
        if (value != null) com.example.dosezy.data.model.PillShape.valueOf(value) else com.example.dosezy.data.model.PillShape.ROUND
    } catch (_: Exception) {
        com.example.dosezy.data.model.PillShape.ROUND
    }

    // Frequency converters
    @TypeConverter
    fun fromFrequency(frequency: Frequency?): String? {
        return frequency?.let { freq ->
            val jsonObject = JSONObject()
            jsonObject.put("pattern", freq.pattern.name)
            freq.daysPerWeek?.let { jsonObject.put("daysPerWeek", it) }
            freq.daysPerMonth?.let { jsonObject.put("daysPerMonth", it) }
            freq.intervalHours?.let { jsonObject.put("intervalHours", it) }
            freq.intervalDays?.let { jsonObject.put("intervalDays", it) }
            freq.selectedDaysOfWeek?.let { days ->
                val arr = JSONArray()
                days.forEach { arr.put(it) }
                jsonObject.put("selectedDaysOfWeek", arr)
            }
            freq.selectedDaysOfMonth?.let { days ->
                val arr = JSONArray()
                days.forEach { arr.put(it) }
                jsonObject.put("selectedDaysOfMonth", arr)
            }
            jsonObject.toString()
        }
    }

    @TypeConverter
    fun toFrequency(value: String?): Frequency? {
        return if (value == null) {
            null
        } else {
            try {
                val jsonObject = JSONObject(value)
                val pattern = try {
                    FrequencyPattern.valueOf(jsonObject.getString("pattern"))
                } catch (_: Exception) {
                    FrequencyPattern.DAILY
                }
                val daysPerWeek = if (jsonObject.has("daysPerWeek") && !jsonObject.isNull("daysPerWeek")) jsonObject.getInt("daysPerWeek") else null
                val daysPerMonth = if (jsonObject.has("daysPerMonth") && !jsonObject.isNull("daysPerMonth")) jsonObject.getInt("daysPerMonth") else null
                val intervalHours = if (jsonObject.has("intervalHours") && !jsonObject.isNull("intervalHours")) jsonObject.getInt("intervalHours") else null
                val intervalDays = if (jsonObject.has("intervalDays") && !jsonObject.isNull("intervalDays")) jsonObject.getInt("intervalDays") else null
                val selectedDaysOfWeek = if (jsonObject.has("selectedDaysOfWeek") && !jsonObject.isNull("selectedDaysOfWeek")) {
                    val arr = jsonObject.getJSONArray("selectedDaysOfWeek")
                    (0 until arr.length()).map { arr.getInt(it) }
                } else null
                val selectedDaysOfMonth = if (jsonObject.has("selectedDaysOfMonth") && !jsonObject.isNull("selectedDaysOfMonth")) {
                    val arr = jsonObject.getJSONArray("selectedDaysOfMonth")
                    (0 until arr.length()).map { arr.getInt(it) }
                } else null
                Frequency(pattern, daysPerWeek, daysPerMonth, selectedDaysOfWeek, selectedDaysOfMonth, intervalHours, intervalDays)
            } catch (_: Exception) {
                Frequency(FrequencyPattern.DAILY)
            }
        }
    }


    @TypeConverter
    fun fromTheme(theme: Theme): String = theme.name

    @TypeConverter
    fun toTheme(themeString: String): Theme = Theme.valueOf(themeString)

    @TypeConverter
    fun fromTimeFormat(timeFormat: TimeFormat): String = timeFormat.name

    @TypeConverter
    fun toTimeFormat(timeFormatString: String): TimeFormat = TimeFormat.valueOf(timeFormatString)

    @TypeConverter
    fun fromLanguage(language: Language): String = language.name

    @TypeConverter
    fun toLanguage(languageString: String): Language = try {
        Language.valueOf(languageString)
    } catch (e: Exception) {
        Language.SYSTEM
    }

    @TypeConverter
    fun fromAlarmSound(alarmSound: com.example.dosezy.data.model.AlarmSound): String = alarmSound.name

    @TypeConverter
    fun toAlarmSound(alarmSoundString: String): com.example.dosezy.data.model.AlarmSound = try {
        com.example.dosezy.data.model.AlarmSound.valueOf(alarmSoundString)
    } catch (_: Exception) {
        com.example.dosezy.data.model.AlarmSound.SYSTEM_DEFAULT
    }
}