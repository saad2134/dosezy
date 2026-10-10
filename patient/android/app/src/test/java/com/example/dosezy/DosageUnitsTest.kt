package com.example.dosezy

import android.content.Context
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.converters.Converters
import com.example.dosezy.data.export.BackupRestoreManager
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.PillShape
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.ui.components.displayName
import io.mockk.mockk
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class DosageUnitsTest {

    private fun createMedicine(
        dosage: Double,
        unit: DosageUnit,
        timesPerDay: Int = 1,
        stock: Int = 100,
        pattern: FrequencyPattern = FrequencyPattern.DAILY
    ): Medicine {
        return Medicine(
            medicineId = "test_med_${unit.name.lowercase()}",
            userId = "test_user",
            medicationName = "Test ${unit.name}",
            dosage = dosage,
            dosageUnit = unit,
            timesPerDay = timesPerDay,
            frequency = Frequency(pattern = pattern),
            scheduledTimes = (1..timesPerDay).map { LocalTime.of(8, 0) },
            currentStock = stock,
            pillShape = PillShape.ROUND
        )
    }

    @Test
    fun testDosageUnitEnumCompleteness() {
        val expectedUnits = listOf("MG", "MCG", "ML", "DROP", "TABLET", "CAPSULE", "MEQ", "PUFF", "AMPULE")
        val actualUnits = DosageUnit.values().map { it.name }
        assertEquals(expectedUnits.size, actualUnits.size)
        assertTrue(actualUnits.containsAll(expectedUnits))
    }

    @Test
    fun testDisplayNameMappings() {
        assertEquals("mEq", DosageUnit.MEQ.displayName)
        assertEquals("puff", DosageUnit.PUFF.displayName)
        assertEquals("ampule", DosageUnit.AMPULE.displayName)
        assertEquals("mg", DosageUnit.MG.displayName)
        assertEquals("mcg", DosageUnit.MCG.displayName)
        assertEquals("ml", DosageUnit.ML.displayName)
        assertEquals("drop", DosageUnit.DROP.displayName)
        assertEquals("tablet", DosageUnit.TABLET.displayName)
        assertEquals("capsule", DosageUnit.CAPSULE.displayName)
    }

    @Test
    fun testDosageDisplayFormats() {
        val meqMed = createMedicine(20.0, DosageUnit.MEQ)
        assertEquals("20 mEq", meqMed.getDosageDisplay())

        val meqFractional = createMedicine(10.5, DosageUnit.MEQ)
        assertEquals("10.5 mEq", meqFractional.getDosageDisplay())

        val puffMed = createMedicine(2.0, DosageUnit.PUFF)
        assertEquals("2 puff", puffMed.getDosageDisplay())

        val ampuleMed = createMedicine(1.0, DosageUnit.AMPULE)
        assertEquals("1 ampule", ampuleMed.getDosageDisplay())
    }

    @Test
    fun testStockDeductionAmount() {
        // MEQ: physical stock is measured in discrete tablets/packets, consume 1 unit per intake
        val meqMed10 = createMedicine(10.0, DosageUnit.MEQ)
        assertEquals(1, meqMed10.getStockDeductionAmount())
        val meqMed20 = createMedicine(20.0, DosageUnit.MEQ)
        assertEquals(1, meqMed20.getStockDeductionAmount())

        // PUFF: inhaler canister tracks total actuations, consume intake dosage count
        val puffMed1 = createMedicine(1.0, DosageUnit.PUFF)
        assertEquals(1, puffMed1.getStockDeductionAmount())
        val puffMed2 = createMedicine(2.0, DosageUnit.PUFF)
        assertEquals(2, puffMed2.getStockDeductionAmount())
        val puffMed4 = createMedicine(4.0, DosageUnit.PUFF)
        assertEquals(4, puffMed4.getStockDeductionAmount())

        // AMPULE: single-use containers, consume 1..10 or clamp to 1
        val ampuleMed1 = createMedicine(1.0, DosageUnit.AMPULE)
        assertEquals(1, ampuleMed1.getStockDeductionAmount())
        val ampuleMed2 = createMedicine(2.0, DosageUnit.AMPULE)
        assertEquals(2, ampuleMed2.getStockDeductionAmount())
        val ampuleMedLarge = createMedicine(50.0, DosageUnit.AMPULE)
        assertEquals(1, ampuleMedLarge.getStockDeductionAmount())
    }

    @Test
    fun testRefillCalculations() {
        // 30 days, 2 times daily
        val meqMed = createMedicine(20.0, DosageUnit.MEQ, timesPerDay = 2)
        // MEQ: dosesPerIntake is 1, so 60 intakes = 60 units
        assertEquals(60, meqMed.calculateRefillQuantity(30))

        val puffMed = createMedicine(2.0, DosageUnit.PUFF, timesPerDay = 2)
        // PUFF: 2 puffs per intake * 2 intakes * 30 days = 120 puffs
        assertEquals(120, puffMed.calculateRefillQuantity(30))

        val ampuleMed = createMedicine(1.0, DosageUnit.AMPULE, timesPerDay = 2)
        // AMPULE: 1 ampule per intake * 2 intakes * 30 days = 60 ampules
        assertEquals(60, ampuleMed.calculateRefillQuantity(30))
    }

    @Test
    fun testRoomTypeConverter() {
        val converters = Converters()
        DosageUnit.values().forEach { unit ->
            val converted = converters.fromDosageUnit(unit)
            val restored = converters.toDosageUnit(converted)
            assertEquals(unit, restored)
        }

        // Defensive fallback for legacy or invalid string
        assertEquals(DosageUnit.TABLET, converters.toDosageUnit("INVALID_UNIT_XYZ"))
    }

    @Test
    fun testBackupRestoreManagerParsingAndAliases() {
        val manager = BackupRestoreManager(
            context = mockk<Context>(relaxed = true),
            database = mockk<DosezyDatabase>(relaxed = true),
            scheduleRepository = mockk<ScheduleRepository>(relaxed = true)
        )

        fun createMedJson(unitStr: String): String {
            val arr = JSONArray()
            val obj = JSONObject().apply {
                put("medicineId", "med_test")
                put("userId", "user_1")
                put("medicationName", "Test Inhaler")
                put("dosage", 2.0)
                put("dosageUnit", unitStr)
                put("timesPerDay", 1)
                put("scheduledTimes", JSONArray(listOf("08:00")))
            }
            arr.put(obj)
            return arr.toString()
        }

        // Test standard enum names
        assertEquals(DosageUnit.MEQ, manager.parseMedicinesFromJson(createMedJson("MEQ")).first().dosageUnit)
        assertEquals(DosageUnit.PUFF, manager.parseMedicinesFromJson(createMedJson("PUFF")).first().dosageUnit)
        assertEquals(DosageUnit.AMPULE, manager.parseMedicinesFromJson(createMedJson("AMPULE")).first().dosageUnit)

        // Test aliases
        assertEquals(DosageUnit.MEQ, manager.parseMedicinesFromJson(createMedJson("mEq")).first().dosageUnit)
        assertEquals(DosageUnit.MEQ, manager.parseMedicinesFromJson(createMedJson("milliequivalents")).first().dosageUnit)
        assertEquals(DosageUnit.PUFF, manager.parseMedicinesFromJson(createMedJson("puffs")).first().dosageUnit)
        assertEquals(DosageUnit.PUFF, manager.parseMedicinesFromJson(createMedJson("actuation")).first().dosageUnit)
        assertEquals(DosageUnit.PUFF, manager.parseMedicinesFromJson(createMedJson("actuations")).first().dosageUnit)
        assertEquals(DosageUnit.AMPULE, manager.parseMedicinesFromJson(createMedJson("ampules")).first().dosageUnit)
        assertEquals(DosageUnit.AMPULE, manager.parseMedicinesFromJson(createMedJson("amp")).first().dosageUnit)
        assertEquals(DosageUnit.AMPULE, manager.parseMedicinesFromJson(createMedJson("ampoule")).first().dosageUnit)

        // Test fallback on unknown string
        assertEquals(DosageUnit.TABLET, manager.parseMedicinesFromJson(createMedJson("unknown_unit")).first().dosageUnit)
    }
}
