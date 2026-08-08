package com.example.dosezy.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.dosezy.data.converters.Converters
import java.time.LocalDateTime

@Entity(
    tableName = "schedule_entries",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Medicine::class,
            parentColumns = ["medicineId"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        Index("medicineId"),
        Index("scheduledDateTime")
    ]
)
@TypeConverters(Converters::class)
data class ScheduleEntry(
    @PrimaryKey val entryId: String,
    val userId: String,
    val medicineId: String,
    val scheduledDateTime: LocalDateTime,
    val status: MedicationStatus,
    val takenAt: LocalDateTime? = null
)

enum class MedicationStatus {
    PENDING, TAKEN_ON_TIME, TAKEN_LATE, MISSED
}