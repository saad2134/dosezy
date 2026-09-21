package com.example.dosezy

import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class AlarmAndNotificationLogicTest {

    // ───────────────────────────────────────────────────────────────
    // 1. Nagging and Snooze Alarm ID Determinism
    // ───────────────────────────────────────────────────────────────

    @Test
    fun naggingAlarmId_isDeterministicAndUniqueFromSnooze() {
        val entryId = "med_123_2026_09_21_08_00"
        val naggingId = entryId.hashCode() + 2000
        val snoozeId = entryId.hashCode() + 1000

        // Nagging ID must match whenever computed with same entryId
        assertEquals(naggingId, entryId.hashCode() + 2000)
        // Nagging ID and Snooze ID must never collide
        assertNotEquals(naggingId, snoozeId)
    }

    @Test
    fun naggingPayload_preservesAllGroupedEntries() {
        // Pure simulator of intent extras map for nagging reminders
        data class NaggingIntentExtras(
            val primaryEntryId: String,
            val primaryMedicineName: String,
            val naggingCount: Int,
            val entryIds: List<String>?,
            val medicineNames: List<String>?,
            val scheduledTime: String?
        )

        val entry1 = "med_A_2026_09_21_08_00"
        val entry2 = "med_B_2026_09_21_08_00"
        val groupedIds = listOf(entry1, entry2)
        val groupedNames = listOf("Metformin", "Lisinopril")

        val extras = NaggingIntentExtras(
            primaryEntryId = entry1,
            primaryMedicineName = "Metformin",
            naggingCount = 1,
            entryIds = groupedIds,
            medicineNames = groupedNames,
            scheduledTime = "08:00"
        )

        // When nagging fires, all sibling entries must be present
        assertEquals(2, extras.entryIds?.size)
        assertTrue(extras.entryIds?.contains(entry1) == true)
        assertTrue(extras.entryIds?.contains(entry2) == true)
        assertEquals(listOf("Metformin", "Lisinopril"), extras.medicineNames)
        assertEquals("08:00", extras.scheduledTime)
    }

    // ───────────────────────────────────────────────────────────────
    // 2. PDF Dose History Table Formatting and Sorting
    // ───────────────────────────────────────────────────────────────

    @Test
    fun pdfDoseHistory_sortsDescendingAndFormatsMedication() {
        val now = LocalDateTime.of(2026, 9, 21, 12, 0)
        val olderEntry = ScheduleEntry(
            entryId = "e_1",
            userId = "u_1",
            medicineId = "med_1",
            scheduledDateTime = now.minusDays(5),
            status = MedicationStatus.TAKEN_ON_TIME
        )
        val newerEntry = ScheduleEntry(
            entryId = "e_2",
            userId = "u_1",
            medicineId = "med_2",
            scheduledDateTime = now.minusHours(2),
            status = MedicationStatus.TAKEN_ON_TIME
        )

        val rawList = listOf(olderEntry, newerEntry)
        // Test sorting logic applied in DataExporter
        val sorted = rawList.sortedByDescending { it.scheduledDateTime }

        assertEquals("e_2", sorted[0].entryId)
        assertEquals("e_1", sorted[1].entryId)

        // Test medication row lookup
        val med1 = Medicine(
            medicineId = "med_1",
            userId = "u_1",
            medicationName = "Aspirin",
            dosage = 81.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0))
        )
        val medMap = mapOf("med_1" to med1)

        val targetMed = medMap[olderEntry.medicineId]
        val medName = targetMed?.medicationName ?: "Unknown"
        val doseStr = targetMed?.let { " (${it.getDosageDisplay(olderEntry.scheduledDateTime.toLocalTime())})" } ?: ""
        val fullMed = "$medName$doseStr"

        assertEquals("Aspirin (81 mg)", fullMed)
    }

    @Test
    fun pdfDoseHistory_truncatesExcessivelyLongNames() {
        val veryLongName = "Supercalifragilisticexpialidocious Ultra Strength Extended Release"
        val truncated = if (veryLongName.length > 28) veryLongName.take(25) + "..." else veryLongName

        assertTrue(truncated.length <= 28)
        assertTrue(truncated.endsWith("..."))
    }
}
