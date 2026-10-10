// ScheduleWithMedicine.kt
package com.example.dosezy.data.model

import androidx.room.Embedded
import androidx.room.Relation

// Guard: Mark Immutable so Compose skips recomposing unchanged items in LazyColumn on slower devices
@androidx.compose.runtime.Immutable
data class ScheduleWithMedicine(
    @Embedded val scheduleEntry: ScheduleEntry,
    @Relation(
        parentColumn = "medicineId",
        entityColumn = "medicineId"
    )
    val medicine: Medicine?
)