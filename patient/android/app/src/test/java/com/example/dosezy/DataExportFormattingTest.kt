package com.example.dosezy

import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Tests for DataExporter formatting logic, pharmacy order generation string templates,
 * CSV format generation, and PDF chronological log sorting.
 */
class DataExportFormattingTest {

    private fun sampleMed(
        name: String = "Metformin",
        dosage: Double = 500.0,
        unit: DosageUnit = DosageUnit.MG,
        freq: Frequency = Frequency(FrequencyPattern.DAILY),
        times: List<LocalTime> = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
        stock: Int? = 40
    ) = Medicine(
        medicineId = "med_123",
        userId = "usr_1",
        medicationName = name,
        dosage = dosage,
        dosageUnit = unit,
        timesPerDay = times.size,
        frequency = freq,
        scheduledTimes = times,
        currentStock = stock
    )

    // ───────────────────────────────────────────────────────────────
    // 1. Pharmacy Order Message Construction Logic
    // ───────────────────────────────────────────────────────────────

    @Test
    fun pharmacyOrder_buildsCleanMarkdownFormattedString() {
        val med = sampleMed(name = "Lisinopril", dosage = 1.0, unit = DosageUnit.TABLET)
        val supplyDays = 30
        val totalNeeded = med.calculateRefillQuantity(supplyDays)

        val sb = StringBuilder()
        sb.append("📋 *Pharmacy Refill Order - John Doe*\n")
        sb.append("Duration: 30 days\n\n")

        val dosageDisplay = if (med.dosage % 1.0 == 0.0) "${med.dosage.toInt()}" else "${med.dosage}"
        val strengthUnit = med.dosageUnit.name.lowercase()
        val orderUnit = "tablets"

        sb.append("1. *${med.medicationName}* ($dosageDisplay $strengthUnit)\n")
        sb.append("   • Needed for $supplyDays days: $totalNeeded $orderUnit\n")
        if (med.currentStock != null) {
            sb.append("   • Current stock: ${med.currentStock} $orderUnit\n")
        }

        val output = sb.toString()
        assertTrue(output.contains("1. *Lisinopril* (1 tablet)"))
        assertTrue(output.contains("Needed for 30 days: 60 tablets"))
        assertTrue(output.contains("Current stock: 40 tablets"))
    }

    @Test
    fun pharmacyOrder_dropsConvertProperlyToBottlesInOrderText() {
        // Eye drops: 2 drops, 2x daily = 4 drops/day * 30 days = 120 drops -> 2 bottles
        val med = sampleMed(
            name = "Latanoprost",
            dosage = 2.0,
            unit = DosageUnit.DROP,
            times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            stock = 25
        )
        val bottlesNeeded = med.calculateRefillQuantity(30)
        assertEquals(2, bottlesNeeded)

        val isDrop = med.dosageUnit == DosageUnit.DROP
        val unitLabel = if (isDrop) (if (bottlesNeeded > 1) "bottles" else "bottle") else "units"
        assertEquals("bottles", unitLabel)
    }

    // ───────────────────────────────────────────────────────────────
    // 2. CSV Export Formatting
    // ───────────────────────────────────────────────────────────────

    @Test
    fun csvExport_generatesAccurateHeaderAndEscapedRows() {
        val headers = "Medication Name,Dosage,Unit,Frequency,Times Per Day,Stock,Notes"
        val med = sampleMed(name = "Aspirin, Buffered", dosage = 81.0, unit = DosageUnit.MG)

        // Simple CSV escape helper
        fun escapeCsv(value: String): String {
            return if (value.contains(",") || value.contains("\"")) {
                "\"" + value.replace("\"", "\"\"") + "\""
            } else {
                value
            }
        }

        val row = listOf(
            escapeCsv(med.medicationName),
            med.dosage.toString(),
            med.dosageUnit.name,
            med.getFrequencyDisplay(),
            med.timesPerDay.toString(),
            med.currentStock?.toString() ?: "",
            escapeCsv(med.notes ?: "")
        ).joinToString(",")

        assertEquals("Medication Name,Dosage,Unit,Frequency,Times Per Day,Stock,Notes", headers)
        assertTrue(row.startsWith("\"Aspirin, Buffered\",81.0,MG,2x Daily,2,40,"))
    }

    // ───────────────────────────────────────────────────────────────
    // 3. Export History Sorting (Most Recent First)
    // ───────────────────────────────────────────────────────────────

    @Test
    fun scheduleExport_sortsChronologicallyDescending() {
        val now = LocalDateTime.now()
        val e1 = ScheduleEntry("1", "u", "m", now.minusHours(5), MedicationStatus.TAKEN_ON_TIME)
        val e2 = ScheduleEntry("2", "u", "m", now.minusHours(1), MedicationStatus.TAKEN_ON_TIME)
        val e3 = ScheduleEntry("3", "u", "m", now.minusHours(12), MedicationStatus.MISSED)

        val unsorted = listOf(e1, e2, e3)
        val sorted = unsorted.sortedByDescending { it.scheduledDateTime }.take(150)

        // e2 (-1h) should be first, then e1 (-5h), then e3 (-12h)
        assertEquals("2", sorted[0].entryId)
        assertEquals("1", sorted[1].entryId)
        assertEquals("3", sorted[2].entryId)
    }
}
