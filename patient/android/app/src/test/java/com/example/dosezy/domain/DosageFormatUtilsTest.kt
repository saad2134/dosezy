package com.example.dosezy.domain

import android.content.Context
import com.example.dosezy.R
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.PillShape
import com.example.dosezy.utils.DosageFormatUtils
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class DosageFormatUtilsTest {

    @Test
    fun testFormatDosageAmount_discreteUnits_vulgarFractions() {
        // Discrete units: TABLET and CAPSULE use vulgar fractions
        assertEquals("½", DosageFormatUtils.formatDosageAmount(0.5, DosageUnit.TABLET))
        assertEquals("1½", DosageFormatUtils.formatDosageAmount(1.5, DosageUnit.TABLET))
        assertEquals("2½", DosageFormatUtils.formatDosageAmount(2.5, DosageUnit.TABLET))
        assertEquals("¼", DosageFormatUtils.formatDosageAmount(0.25, DosageUnit.TABLET))
        assertEquals("¾", DosageFormatUtils.formatDosageAmount(0.75, DosageUnit.TABLET))
        assertEquals("1¼", DosageFormatUtils.formatDosageAmount(1.25, DosageUnit.TABLET))
        assertEquals("1¾", DosageFormatUtils.formatDosageAmount(1.75, DosageUnit.TABLET))
        assertEquals("1", DosageFormatUtils.formatDosageAmount(1.0, DosageUnit.TABLET))
        assertEquals("2", DosageFormatUtils.formatDosageAmount(2.0, DosageUnit.TABLET))
        assertEquals("0.33", DosageFormatUtils.formatDosageAmount(0.33, DosageUnit.TABLET))

        assertEquals("½", DosageFormatUtils.formatDosageAmount(0.5, DosageUnit.CAPSULE))
        assertEquals("1½", DosageFormatUtils.formatDosageAmount(1.5, DosageUnit.CAPSULE))
    }

    @Test
    fun testFormatDosageAmount_chemicalAndContinuousUnits_standardDecimals() {
        // Clinical Safety: MG, MCG, MEQ, ML, DROP, PUFF, SPRAY, AMPULE must strictly preserve decimal format
        assertEquals("0.5", DosageFormatUtils.formatDosageAmount(0.5, DosageUnit.MG))
        assertEquals("1.5", DosageFormatUtils.formatDosageAmount(1.5, DosageUnit.MG))
        assertEquals("500", DosageFormatUtils.formatDosageAmount(500.0, DosageUnit.MG))

        assertEquals("0.5", DosageFormatUtils.formatDosageAmount(0.5, DosageUnit.MCG))
        assertEquals("10.5", DosageFormatUtils.formatDosageAmount(10.5, DosageUnit.MEQ))

        assertEquals("2.5", DosageFormatUtils.formatDosageAmount(2.5, DosageUnit.ML))
        assertEquals("15", DosageFormatUtils.formatDosageAmount(15.0, DosageUnit.ML))

        assertEquals("0.5", DosageFormatUtils.formatDosageAmount(0.5, DosageUnit.DROP))
        assertEquals("2", DosageFormatUtils.formatDosageAmount(2.0, DosageUnit.PUFF))
        assertEquals("1", DosageFormatUtils.formatDosageAmount(1.0, DosageUnit.AMPULE))
        assertEquals("2", DosageFormatUtils.formatDosageAmount(2.0, DosageUnit.SPRAY))
    }

    @Test
    fun testFormatDosageDisplay_englishLabelsAndPluralization() {
        // Discrete units
        assertEquals("½ tablet", DosageFormatUtils.formatDosageDisplay(0.5, DosageUnit.TABLET))
        assertEquals("1 tablet", DosageFormatUtils.formatDosageDisplay(1.0, DosageUnit.TABLET))
        assertEquals("1½ tablet", DosageFormatUtils.formatDosageDisplay(1.5, DosageUnit.TABLET))
        assertEquals("2 tablet", DosageFormatUtils.formatDosageDisplay(2.0, DosageUnit.TABLET))

        assertEquals("½ capsule", DosageFormatUtils.formatDosageDisplay(0.5, DosageUnit.CAPSULE))
        assertEquals("1 capsule", DosageFormatUtils.formatDosageDisplay(1.0, DosageUnit.CAPSULE))
        assertEquals("1½ capsule", DosageFormatUtils.formatDosageDisplay(1.5, DosageUnit.CAPSULE))
        assertEquals("2 capsule", DosageFormatUtils.formatDosageDisplay(2.0, DosageUnit.CAPSULE))

        // Chemical strengths
        assertEquals("0.5 mg", DosageFormatUtils.formatDosageDisplay(0.5, DosageUnit.MG))
        assertEquals("500 mg", DosageFormatUtils.formatDosageDisplay(500.0, DosageUnit.MG))
        assertEquals("25 mcg", DosageFormatUtils.formatDosageDisplay(25.0, DosageUnit.MCG))
        assertEquals("10.5 mEq", DosageFormatUtils.formatDosageDisplay(10.5, DosageUnit.MEQ))

        // Liquid volume: standard mL
        assertEquals("2.5 mL", DosageFormatUtils.formatDosageDisplay(2.5, DosageUnit.ML))
        assertEquals("1 mL", DosageFormatUtils.formatDosageDisplay(1.0, DosageUnit.ML))

        // Drops, puffs, sprays, ampules
        assertEquals("1 drop", DosageFormatUtils.formatDosageDisplay(1.0, DosageUnit.DROP))
        assertEquals("3 drop", DosageFormatUtils.formatDosageDisplay(3.0, DosageUnit.DROP))
        assertEquals("1 puff", DosageFormatUtils.formatDosageDisplay(1.0, DosageUnit.PUFF))
        assertEquals("2 puff", DosageFormatUtils.formatDosageDisplay(2.0, DosageUnit.PUFF))
        assertEquals("1 ampule", DosageFormatUtils.formatDosageDisplay(1.0, DosageUnit.AMPULE))
        assertEquals("2 ampule", DosageFormatUtils.formatDosageDisplay(2.0, DosageUnit.AMPULE))
        assertEquals("1 spray", DosageFormatUtils.formatDosageDisplay(1.0, DosageUnit.SPRAY))
        assertEquals("2 spray", DosageFormatUtils.formatDosageDisplay(2.0, DosageUnit.SPRAY))
    }

    @Test
    fun testFormatStockQuantity() {
        assertEquals("10", DosageFormatUtils.formatStockQuantity(10.0))
        assertEquals("10.5", DosageFormatUtils.formatStockQuantity(10.5))
        assertEquals("10.25", DosageFormatUtils.formatStockQuantity(10.25))
        assertEquals("0", DosageFormatUtils.formatStockQuantity(0.0))
        assertEquals("", DosageFormatUtils.formatStockQuantity(null))
        // Guard: Verify floating point precision drift trims trailing zeros cleanly
        assertEquals("2", DosageFormatUtils.formatStockQuantity(2.0000000000000004))
        assertEquals("0", DosageFormatUtils.formatStockQuantity(0.0001))
    }

    @Test
    fun testFormatDosageAmount_floatPrecisionAndUpperBoundary() {
        // Discrete units: upper fraction boundary rounds up to next whole number
        assertEquals("1", DosageFormatUtils.formatDosageAmount(0.99999999, DosageUnit.TABLET))
        assertEquals("2", DosageFormatUtils.formatDosageAmount(1.99999999, DosageUnit.TABLET))
        assertEquals("1 tablet", DosageFormatUtils.formatDosageDisplay(0.99999999, DosageUnit.TABLET))

        // Chemical units with float precision
        assertEquals("2", DosageFormatUtils.formatDosageAmount(2.0000000000000004, DosageUnit.MG))
        assertEquals("0", DosageFormatUtils.formatDosageAmount(0.0001, DosageUnit.MG))
    }

    @Test
    fun testFormatStockUnit_chemicalStrengthsUseCountableUnitsAndBottles() {
        val context = mockk<Context>()
        every { context.getString(R.string.unit_units) } returns "units"
        every { context.getString(R.string.unit_bottle) } returns "bottle"
        every { context.getString(R.string.unit_bottles) } returns "bottles"
        every { context.getString(R.string.unit_tablet) } returns "tablet"
        every { context.getString(R.string.unit_capsule) } returns "capsule"

        // Guard: Verify chemical strength units return "units" instead of displaying "mg", "mcg", or "mEq" for inventory stock (#129)
        assertEquals("units", DosageFormatUtils.formatStockUnit(context, DosageUnit.MG, PillShape.ROUND, 10.0))
        assertEquals("units", DosageFormatUtils.formatStockUnit(context, DosageUnit.MCG, PillShape.CAPSULE, 25.0))
        assertEquals("units", DosageFormatUtils.formatStockUnit(context, DosageUnit.MEQ, PillShape.ROUND, 15.0))

        // Discrete countable units return their localized name
        assertEquals("tablet", DosageFormatUtils.formatStockUnit(context, DosageUnit.TABLET, PillShape.ROUND, 40.0))
        assertEquals("capsule", DosageFormatUtils.formatStockUnit(context, DosageUnit.CAPSULE, PillShape.CAPSULE, 12.0))

        // Bottle formulations return bottle/bottles
        assertEquals("bottle", DosageFormatUtils.formatStockUnit(context, DosageUnit.DROP, PillShape.DROPS, 1.0))
        assertEquals("bottles", DosageFormatUtils.formatStockUnit(context, DosageUnit.DROP, PillShape.DROPS, 2.0))
        assertEquals("bottle", DosageFormatUtils.formatStockUnit(context, DosageUnit.SPRAY, PillShape.SPRAY, 1.0))
        assertEquals("bottles", DosageFormatUtils.formatStockUnit(context, DosageUnit.SPRAY, PillShape.SPRAY, 3.0))
    }

    @Test
    fun testFormatOrderUnit_chemicalStrengthsUseCountableUnitsAndBottles() {
        val context = mockk<Context>()
        every { context.getString(R.string.unit_units) } returns "units"
        every { context.getString(R.string.unit_bottle) } returns "bottle"
        every { context.getString(R.string.unit_bottles) } returns "bottles"
        every { context.getString(R.string.unit_tablet) } returns "tablet"

        // Guard: Verify order unit maps chemical strengths to units and drops/sprays to bottles (#129)
        assertEquals("units", DosageFormatUtils.formatOrderUnit(context, DosageUnit.MG, PillShape.ROUND, 60))
        assertEquals("units", DosageFormatUtils.formatOrderUnit(context, DosageUnit.MCG, PillShape.ROUND, 30))
        assertEquals("units", DosageFormatUtils.formatOrderUnit(context, DosageUnit.MEQ, PillShape.ROUND, 15))
        assertEquals("tablet", DosageFormatUtils.formatOrderUnit(context, DosageUnit.TABLET, PillShape.ROUND, 30))
        assertEquals("bottle", DosageFormatUtils.formatOrderUnit(context, DosageUnit.DROP, PillShape.DROPS, 1))
        assertEquals("bottles", DosageFormatUtils.formatOrderUnit(context, DosageUnit.DROP, PillShape.DROPS, 2))
    }
}
