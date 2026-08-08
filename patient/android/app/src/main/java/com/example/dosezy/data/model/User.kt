// User.kt
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
    val snoozeDuration: Int = 10 // minutes, default 10 (options 5, 10, 15, 20, 30)
)

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