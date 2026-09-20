package com.example.dosezy

import com.example.dosezy.data.converters.Converters
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Medicine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class VariableDosageTest {

    private fun createMedicine(
        baseDosage: Double = 10.0,
        unit: DosageUnit = DosageUnit.MG,
        customDosages: Map<String, Double>? = null
    ): Medicine {
        return Medicine(
            medicineId = "med_var_1",
            userId = "user_1",
            medicationName = "Test Variable Med",
            dosage = baseDosage,
            dosageUnit = unit,
            timesPerDay = 2,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            customDosages = customDosages,
            currentStock = 100,
            refillThreshold = 10,
            autoDeductOnTake = true
        )
    }

    @Test
    fun testFallbackToBaseDosageWhenNoCustomDosages() {
        val med = createMedicine(baseDosage = 50.0, customDosages = null)
        assertEquals(50.0, med.getDosageForTime(LocalTime.of(8, 0)), 0.001)
        assertEquals(50.0, med.getDosageForTime(LocalTime.of(20, 0)), 0.001)
        assertEquals(50.0, med.getDosageForTime(null), 0.001)
        assertEquals("50 mg", med.getDosageDisplay(LocalTime.of(8, 0)))
    }

    @Test
    fun testVariableDosageForDifferentTimes() {
        val custom = mapOf(
            "08:00" to 20.0,
            "20:00" to 10.0
        )
        val med = createMedicine(baseDosage = 15.0, customDosages = custom)

        // Slot 8:00 AM
        assertEquals(20.0, med.getDosageForTime(LocalTime.of(8, 0)), 0.001)
        assertEquals("20 mg", med.getDosageDisplay(LocalTime.of(8, 0)))

        // Slot 8:00 PM (20:00)
        assertEquals(10.0, med.getDosageForTime(LocalTime.of(20, 0)), 0.001)
        assertEquals("10 mg", med.getDosageDisplay(LocalTime.of(20, 0)))

        // Unspecified time or null falls back to base dosage
        assertEquals(15.0, med.getDosageForTime(LocalTime.of(12, 0)), 0.001)
        assertEquals(15.0, med.getDosageForTime(null), 0.001)
        assertEquals("15 mg", med.getDosageDisplay(null))
    }

    @Test
    fun testStockDeductionWithVariableDosage() {
        val customTablets = mapOf(
            "08:00" to 2.0,
            "20:00" to 1.0
        )
        val medTablets = createMedicine(
            baseDosage = 1.0,
            unit = DosageUnit.TABLET,
            customDosages = customTablets
        )

        // Morning: 2 tablets taken -> deducts 2
        assertEquals(2, medTablets.getStockDeductionAmount(LocalTime.of(8, 0)))

        // Evening: 1 tablet taken -> deducts 1
        assertEquals(1, medTablets.getStockDeductionAmount(LocalTime.of(20, 0)))

        // Fallback without time -> base dosage (1 tablet)
        assertEquals(1, medTablets.getStockDeductionAmount(null))
    }

    @Test
    fun testConvertersSerialization() {
        val converters = Converters()
        val originalMap = mapOf(
            "08:00" to 25.5,
            "14:00" to 12.0,
            "21:30" to 50.0
        )

        val json = converters.fromCustomDosagesMap(originalMap)
        assertNotNull(json)

        val deserializedMap = converters.toCustomDosagesMap(json)
        assertNotNull(deserializedMap)
        assertEquals(3, deserializedMap?.size)
        assertEquals(25.5, deserializedMap?.get("08:00") ?: 0.0, 0.001)
        assertEquals(12.0, deserializedMap?.get("14:00") ?: 0.0, 0.001)
        assertEquals(50.0, deserializedMap?.get("21:30") ?: 0.0, 0.001)

        // Null and empty checks
        assertNull(converters.fromCustomDosagesMap(null))
        assertNull(converters.toCustomDosagesMap(null))
        assertNull(converters.toCustomDosagesMap(""))
    }

    @Test
    fun testGenerateScheduleEntriesWithVariableDosages() {
        val custom = mapOf(
            "08:00" to 20.0,
            "20:00" to 10.0
        )
        val med = createMedicine(baseDosage = 10.0, customDosages = custom)
        val tomorrow = LocalDate.now().plusDays(1)
        val entries = med.generateScheduleEntries(
            startDateRange = tomorrow,
            days = 0
        )

        assertEquals(2, entries.size)
        val morningEntry = entries.first { it.scheduledDateTime.toLocalTime() == LocalTime.of(8, 0) }
        val eveningEntry = entries.first { it.scheduledDateTime.toLocalTime() == LocalTime.of(20, 0) }

        assertEquals(20.0, morningEntry.dosage ?: 0.0, 0.001)
        assertEquals(10.0, eveningEntry.dosage ?: 0.0, 0.001)
    }
}
