package com.example.dosezy.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.dosezy.data.converters.Converters
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
    indices = [Index("userId")] // Add this index
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
    val scheduledTimes: List<LocalTime> // Multiple times per day
)

enum class DosageUnit {
    MG, MCG, ML, DROP, TABLET, CAPSULE
}

data class Frequency(
    val pattern: FrequencyPattern,
    val daysPerWeek: Int? = null,
    val daysPerMonth: Int? = null
)

enum class FrequencyPattern {
    DAILY, WEEKLY, MONTHLY, CUSTOM
}