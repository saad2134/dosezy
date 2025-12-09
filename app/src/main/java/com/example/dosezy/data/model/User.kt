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
    val theme: Theme = Theme.LIGHT,
    val timeFormat: TimeFormat = TimeFormat.HOUR_12,
    val language: Language = Language.ENGLISH,
    val considerMissedAfter: Int = 3 // hours, default 3
)

enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    DO_NOT_SPECIFY("Do not specify")
}

enum class Theme {
    LIGHT, DARK
}

enum class TimeFormat {
    HOUR_12, HOUR_24
}

enum class Language {
    ENGLISH

}