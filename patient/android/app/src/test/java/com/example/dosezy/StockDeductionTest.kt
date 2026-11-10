package com.example.dosezy

import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Medicine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class StockDeductionTest {

    private fun createMedicine(dosage: Double, unit: DosageUnit, stock: Double = 150.0): Medicine {
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
            refillThreshold = 10.0,
            autoDeductOnTake = true
        )
    }

    @Test
    fun testMilligramDosageDeductsOneUnit() {
        // Metformin 500mg with 150 pills in stock should deduct 1 pill, leaving 149
        val med500mg = createMedicine(dosage = 500.0, unit = DosageUnit.MG, stock = 150.0)
        assertEquals(1.0, med500mg.getStockDeductionAmount(), 0.001)

        // 150mg dosage should also deduct 1 pill, not 150 pills (fixing issue #80)
        val med150mg = createMedicine(dosage = 150.0, unit = DosageUnit.MG, stock = 150.0)
        assertEquals(1.0, med150mg.getStockDeductionAmount(), 0.001)

        // 700mg dosage should deduct 1 pill, not 700 pills (fixing issue #80)
        val med700mg = createMedicine(dosage = 700.0, unit = DosageUnit.MG, stock = 700.0)
        assertEquals(1.0, med700mg.getStockDeductionAmount(), 0.001)
    }

    @Test
    fun testMicrogramDosageDeductsOneUnit() {
        val med25mcg = createMedicine(dosage = 25.0, unit = DosageUnit.MCG, stock = 100.0)
        assertEquals(1.0, med25mcg.getStockDeductionAmount(), 0.001)
    }

    @Test
    fun testTabletDosageDeduction() {
        // 1 tablet dose deducts 1
        val med1Tab = createMedicine(dosage = 1.0, unit = DosageUnit.TABLET, stock = 60.0)
        assertEquals(1.0, med1Tab.getStockDeductionAmount(), 0.001)

        // 2 tablets dose deducts 2
        val med2Tab = createMedicine(dosage = 2.0, unit = DosageUnit.TABLET, stock = 60.0)
        assertEquals(2.0, med2Tab.getStockDeductionAmount(), 0.001)

        // Half tablet dose deducts 0.5 (issue #123)
        val medHalfTab = createMedicine(dosage = 0.5, unit = DosageUnit.TABLET, stock = 60.0)
        assertEquals(0.5, medHalfTab.getStockDeductionAmount(), 0.001)

        // 1.5 tablets dose deducts 1.5 (issue #123)
        val medOneAndHalfTab = createMedicine(dosage = 1.5, unit = DosageUnit.TABLET, stock = 60.0)
        assertEquals(1.5, medOneAndHalfTab.getStockDeductionAmount(), 0.001)

        // Safeguard: If user mistakenly entered 500 as dosage with TABLET unit, cap to 1
        val med500Tab = createMedicine(dosage = 500.0, unit = DosageUnit.TABLET, stock = 60.0)
        assertEquals(1.0, med500Tab.getStockDeductionAmount(), 0.001)
    }

    @Test
    fun testCapsuleDosageDeduction() {
        val med1Cap = createMedicine(dosage = 1.0, unit = DosageUnit.CAPSULE, stock = 30.0)
        assertEquals(1.0, med1Cap.getStockDeductionAmount(), 0.001)

        val med2Cap = createMedicine(dosage = 2.0, unit = DosageUnit.CAPSULE, stock = 30.0)
        assertEquals(2.0, med2Cap.getStockDeductionAmount(), 0.001)

        val medHalfCap = createMedicine(dosage = 0.5, unit = DosageUnit.CAPSULE, stock = 30.0)
        assertEquals(0.5, medHalfCap.getStockDeductionAmount(), 0.001)
    }

    @Test
    fun testLiquidAndDropsDeduction() {
        val medDrops = createMedicine(dosage = 3.0, unit = DosageUnit.DROP, stock = 100.0)
        assertEquals(3.0, medDrops.getStockDeductionAmount(), 0.001)

        val medLiquid = createMedicine(dosage = 10.0, unit = DosageUnit.ML, stock = 200.0)
        assertEquals(10.0, medLiquid.getStockDeductionAmount(), 0.001)

        val medFractionalLiquid = createMedicine(dosage = 2.5, unit = DosageUnit.ML, stock = 100.0)
        assertEquals(2.5, medFractionalLiquid.getStockDeductionAmount(), 0.001)
    }
}
