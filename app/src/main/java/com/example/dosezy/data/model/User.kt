package com.example.dosezy.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.dosezy.data.converters.Converters
import java.util.UUID

@Entity(
    tableName = "users",
    indices = [Index("userId")] // Add index for primary key (good practice)
)

@TypeConverters(Converters::class)
data class User(
    @PrimaryKey val userId: String = UUID.randomUUID().toString(),
    val profilePicPath: String? = null,
    val fullName: String,
    val age: Int,
    val gender: Gender,
    val contactNumber: String,
    val isCurrentUser: Boolean = false
)

enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    DO_NOT_SPECIFY("Do not specify")
}