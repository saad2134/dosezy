/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.dosezy.data.converters.Converters
import java.util.UUID

@Entity(
    tableName = "users",
    indices = [Index("userId")]
)
@TypeConverters(Converters::class)
data class User(
    @PrimaryKey val userId: String = UUID.randomUUID().toString(),
    val profilePicPath: String? = null,
    val fullName: String,
    val age: Int,
    val gender: Gender,
    val contactNumber: String,
    val isCurrentUser: Boolean = false,
    val theme: Theme = Theme.SYSTEM,
    val timeFormat: TimeFormat = TimeFormat.HOUR_12,
    val language: Language = Language.SYSTEM,
    val considerLateAfter: Int = 3, // hours, default 3 (options 1-3)
    val considerMissedAfter: Int = 6, // hours, default 6 (options 3-9)
    val snoozeDuration: Int = 10, // minutes, default 10 (options 5, 10, 15, 20, 30)
    val allergies: String? = null,
    val medicalConditions: String? = null,
    val naggingRemindersEnabled: Boolean = false,
    val naggingIntervalMinutes: Int = 5, // 5, 10, 15 min
    val naggingMaxRepeats: Int = 3, // 1, 2, 3 repeats
    val alarmSound: AlarmSound = AlarmSound.SYSTEM_DEFAULT,
    val customAlarmSoundPath: String? = null,
    val customAlarmSoundTitle: String? = null,
    val alarmDurationSeconds: Int = 0, // 0 = Continuous, 30 = 30s, 60 = 1m, 120 = 2m, 300 = 5m
    val allowDoseSkipping: Boolean = false,
    val allowCustomDoseTime: Boolean = false,
    val hideAddMedicineNavButton: Boolean = false,
    val allowDoseUndo: Boolean = false,
    val promptDoseNotes: Boolean = false
)

enum class AlarmSound(val rawResId: Int?) {
    SYSTEM_DEFAULT(null),
    GENTLE_CHIME(com.example.dosezy.R.raw.alarm_gentle_chime),
    MEDICAL_MARIMBA(com.example.dosezy.R.raw.alarm_medical_marimba),
    BRISK_PULSE(com.example.dosezy.R.raw.alarm_brisk_pulse),
    CALM_BELL(com.example.dosezy.R.raw.alarm_calm_bell),
    CUSTOM(null);

    fun getTitleRes(): Int {
        return when (this) {
            SYSTEM_DEFAULT -> com.example.dosezy.R.string.alarm_sound_system_default
            GENTLE_CHIME -> com.example.dosezy.R.string.alarm_sound_gentle_chime
            MEDICAL_MARIMBA -> com.example.dosezy.R.string.alarm_sound_medical_marimba
            BRISK_PULSE -> com.example.dosezy.R.string.alarm_sound_brisk_pulse
            CALM_BELL -> com.example.dosezy.R.string.alarm_sound_calm_bell
            CUSTOM -> com.example.dosezy.R.string.alarm_sound_custom
        }
    }
}

enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    DO_NOT_SPECIFY("Do not specify")
}

@androidx.compose.runtime.Composable
fun Gender.getLocalizedName(): String {
    return when (this) {
        Gender.MALE -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.gender_male)
        Gender.FEMALE -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.gender_female)
        Gender.DO_NOT_SPECIFY -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.gender_other)
    }
}

enum class Theme {
    LIGHT, DARK, SYSTEM
}

enum class TimeFormat {
    HOUR_12, HOUR_24
}

enum class Language {
    SYSTEM, ENGLISH, SPANISH, HINDI, CHINESE, PORTUGUESE, ARABIC, FRENCH, GERMAN, JAPANESE, RUSSIAN, ITALIAN, BENGALI
}