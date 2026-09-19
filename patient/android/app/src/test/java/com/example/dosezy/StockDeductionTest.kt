package com.example.dosezy

import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Medicine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class StockDeductionTest {

    private fun createMedicine(dosage: Double, unit: DosageUnit, stock: Int = 150): Medicine {
        return Medicine(
            medicineId = "test_med",
            userId = "user_1",
            medicationName = "Test Medication",
            dosage = dosage,
            dosageUnit = unit,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0)),
            currentStock = stock,
            refillThreshold = 10,
            autoDeductOnTake = true
        )
    }

    @Test
    fun testMilligramDosageDeductsOneUnit() {
        // Metformin 500mg with 150 pills in stock should deduct 1 pill, leaving 149
        val med500mg = createMedicine(dosage = 500.0, unit = DosageUnit.MG, stock = 150)
        assertEquals(1, med500mg.getStockDeductionAmount())

        // 150mg dosage should also deduct 1 pill, not 150 pills (fixing issue #80)
        val med150mg = createMedicine(dosage = 150.0, unit = DosageUnit.MG, stock = 150)
        assertEquals(1, med150mg.getStockDeductionAmount())

        // 700mg dosage should deduct 1 pill, not 700 pills (fixing issue #80)
        val med700mg = createMedicine(dosage = 700.0, unit = DosageUnit.MG, stock = 700)
        assertEquals(1, med700mg.getStockDeductionAmount())
    }

    @Test
    fun testMicrogramDosageDeductsOneUnit() {
        val med25mcg = createMedicine(dosage = 25.0, unit = DosageUnit.MCG, stock = 100)
        assertEquals(1, med25mcg.getStockDeductionAmount())
    }

    @Test
    fun testTabletDosageDeduction() {
        // 1 tablet dose deducts 1
        val med1Tab = createMedicine(dosage = 1.0, unit = DosageUnit.TABLET, stock = 60)
        assertEquals(1, med1Tab.getStockDeductionAmount())

        // 2 tablets dose deducts 2
        val med2Tab = createMedicine(dosage = 2.0, unit = DosageUnit.TABLET, stock = 60)
        assertEquals(2, med2Tab.getStockDeductionAmount())

        // Safeguard: If user mistakenly entered 500 as dosage with TABLET unit, cap to 1
        val med500Tab = createMedicine(dosage = 500.0, unit = DosageUnit.TABLET, stock = 60)
        assertEquals(1, med500Tab.getStockDeductionAmount())
    }

    @Test
    fun testCapsuleDosageDeduction() {
        val med1Cap = createMedicine(dosage = 1.0, unit = DosageUnit.CAPSULE, stock = 30)
        assertEquals(1, med1Cap.getStockDeductionAmount())

        val med2Cap = createMedicine(dosage = 2.0, unit = DosageUnit.CAPSULE, stock = 30)
        assertEquals(2, med2Cap.getStockDeductionAmount())
    }

    @Test
    fun testLiquidAndDropsDeduction() {
        val medDrops = createMedicine(dosage = 3.0, unit = DosageUnit.DROP, stock = 100)
        assertEquals(3, medDrops.getStockDeductionAmount())

        val medLiquid = createMedicine(dosage = 10.0, unit = DosageUnit.ML, stock = 200)
        assertEquals(10, medLiquid.getStockDeductionAmount())
    }
}
