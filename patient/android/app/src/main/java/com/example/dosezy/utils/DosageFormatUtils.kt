/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.dosezy.R
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.PillShape
import com.example.dosezy.data.model.getLocalizedName
import java.util.Locale

/**
 * Utility for formatting medication dosages and inventory stock quantities.
 * Supports vulgar fraction display for discrete units (e.g. 0.5 -> "½ tablet", 1.5 -> "1½ tablets")
 * while strictly maintaining decimal notation for chemical and liquid units (e.g. "0.5 mg", "2.5 mL")
 * in accordance with ISMP clinical medication safety guidelines (#123).
 */
object DosageFormatUtils {

    /**
     * Formats a dosage numeric amount according to the dosage unit.
     * Uses vulgar fractions (½, 1½, ¼, ¾) for discrete units (TABLET, CAPSULE).
     * Uses standard decimals (e.g. 0.5, 1.5, 2.5) for chemical/continuous units (MG, ML, etc.).
     */
    fun formatDosageAmount(dosage: Double, unit: DosageUnit): String {
        if (dosage <= 0.0) return "0"

        // Discrete units: format common fractional tablet/capsule doses with vulgar fractions
        if (unit == DosageUnit.TABLET || unit == DosageUnit.CAPSULE) {
            val whole = dosage.toInt()
            val frac = dosage - whole

            // Tolerance check for floating point inaccuracies
            val isHalf = Math.abs(frac - 0.5) < 0.001
            val isQuarter = Math.abs(frac - 0.25) < 0.001
            val isThreeQuarters = Math.abs(frac - 0.75) < 0.001
            // Guard: Handle upper fraction boundary (0.999+) due to IEEE 754 precision drift to prevent showing "1.0 tablet" or "2.0 tablets"
            val isNextWhole = Math.abs(frac - 1.0) < 0.001

            return when {
                frac < 0.001 -> whole.toString()
                isNextWhole -> (whole + 1).toString()
                isHalf -> if (whole == 0) "½" else "$whole½"
                isQuarter -> if (whole == 0) "¼" else "$whole¼"
                isThreeQuarters -> if (whole == 0) "¾" else "$whole¾"
                else -> formatDecimalClean(dosage)
            }
        }

        // Chemical strength and continuous/liquid units (MG, MCG, ML, etc.): strictly use decimal format
        return formatDecimalClean(dosage)
    }

    /**
     * Formats dosage amount with standard unit abbreviation (e.g. "0.5 mg", "½ tablet", "1½ tablets").
     */
    fun formatDosageDisplay(dosage: Double, unit: DosageUnit): String {
        val amountStr = formatDosageAmount(dosage, unit)
        val unitStr = getUnitAbbreviation(unit)
        return "$amountStr $unitStr"
    }

    /**
     * Composable helper formatting dosage amount with localized unit name.
     */
    @Composable
    fun formatLocalizedDosageDisplay(dosage: Double, unit: DosageUnit): String {
        val amountStr = formatDosageAmount(dosage, unit)
        val unitName = unit.getLocalizedName()
        return "$amountStr $unitName"
    }

    /**
     * Context-based helper formatting dosage amount with localized unit name.
     */
    fun formatLocalizedDosageDisplay(context: Context, dosage: Double, unit: DosageUnit): String {
        val amountStr = formatDosageAmount(dosage, unit)
        val unitName = unit.getLocalizedName(context)
        return "$amountStr $unitName"
    }

    /**
     * Formats inventory stock numbers so integer stocks display without decimals ("30", "15")
     * and fractional stock balances retain exact single decimal precision ("29.5", "14.5").
     */
    fun formatStockQuantity(stock: Double?): String {
        if (stock == null) return ""
        return formatDecimalClean(stock)
    }

    /**
     * Resolves the appropriate localized display unit for inventory stock quantities (#129).
     * When medication dosage is measured in chemical strength (MG, MCG, MEQ), inventory stock counts
     * discrete physical units (e.g. tablets/pills in the prescription bottle), so R.string.unit_units ("units")
     * is returned instead of displaying raw milligram strength as inventory stock.
     * For bottle formulations (DROP, SPRAY), returns bottle or bottles based on quantity.
     * For discrete physical formulations (TABLET, CAPSULE, etc.), returns their respective localized names.
     */
    fun formatStockUnit(context: Context, unit: DosageUnit, shape: PillShape, quantity: Double = 1.0): String {
        val isBottle = unit == DosageUnit.DROP ||
                unit == DosageUnit.SPRAY ||
                shape == PillShape.DROPS ||
                shape == PillShape.SPRAY
        // Guard: Use unit_units for chemical strength to prevent displaying stock as "30 mg" instead of "30 units" (#129)
        return if (isBottle) {
            if (quantity > 1.0) context.getString(R.string.unit_bottles) else context.getString(R.string.unit_bottle)
        } else when (unit) {
            DosageUnit.MG, DosageUnit.MCG, DosageUnit.MEQ -> {
                context.getString(R.string.unit_units)
            }
            else -> unit.getLocalizedName(context)
        }
    }

    /**
     * Composable helper resolving the appropriate localized display unit for inventory stock quantities (#129).
     */
    @Composable
    fun formatLocalizedStockUnit(unit: DosageUnit, shape: PillShape, quantity: Double = 1.0): String {
        val isBottle = unit == DosageUnit.DROP ||
                unit == DosageUnit.SPRAY ||
                shape == PillShape.DROPS ||
                shape == PillShape.SPRAY
        // Guard: Use unit_units for chemical strength to prevent displaying stock as "30 mg" instead of "30 units" (#129)
        return if (isBottle) {
            if (quantity > 1.0) stringResource(R.string.unit_bottles) else stringResource(R.string.unit_bottle)
        } else when (unit) {
            DosageUnit.MG, DosageUnit.MCG, DosageUnit.MEQ -> {
                stringResource(R.string.unit_units)
            }
            else -> unit.getLocalizedName()
        }
    }

    /**
     * Resolves the appropriate localized display unit for pharmacy refill order quantities (#129).
     */
    fun formatOrderUnit(context: Context, unit: DosageUnit, shape: PillShape, quantity: Int = 1): String {
        val isBottle = unit == DosageUnit.DROP ||
                unit == DosageUnit.SPRAY ||
                shape == PillShape.DROPS ||
                shape == PillShape.SPRAY
        return if (isBottle) {
            if (quantity > 1) context.getString(R.string.unit_bottles) else context.getString(R.string.unit_bottle)
        } else when (unit) {
            DosageUnit.MG, DosageUnit.MCG, DosageUnit.MEQ -> {
                context.getString(R.string.unit_units)
            }
            else -> unit.getLocalizedName(context)
        }
    }

    /**
     * Composable helper resolving the appropriate localized display unit for pharmacy refill order quantities (#129).
     */
    @Composable
    fun formatLocalizedOrderUnit(unit: DosageUnit, shape: PillShape, quantity: Int = 1): String {
        val isBottle = unit == DosageUnit.DROP ||
                unit == DosageUnit.SPRAY ||
                shape == PillShape.DROPS ||
                shape == PillShape.SPRAY
        return if (isBottle) {
            if (quantity > 1) stringResource(R.string.unit_bottles) else stringResource(R.string.unit_bottle)
        } else when (unit) {
            DosageUnit.MG, DosageUnit.MCG, DosageUnit.MEQ -> {
                stringResource(R.string.unit_units)
            }
            else -> unit.getLocalizedName()
        }
    }

    // Guard: Round to 2 decimals and strip all trailing zeros/decimals cleanly to prevent "2.0" or "0.0" output from IEEE 754 float imprecision
    private fun formatDecimalClean(value: Double): String {
        val rounded = Math.round(value * 100.0) / 100.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", rounded)
                .trimEnd('0')
                .trimEnd('.')
                .ifEmpty { "0" }
        }
    }

    private fun getUnitAbbreviation(unit: DosageUnit): String {
        return when (unit) {
            DosageUnit.MG -> "mg"
            DosageUnit.MCG -> "mcg"
            DosageUnit.ML -> "mL"
            DosageUnit.DROP -> "drop"
            DosageUnit.TABLET -> "tablet"
            DosageUnit.CAPSULE -> "capsule"
            DosageUnit.MEQ -> "mEq"
            DosageUnit.PUFF -> "puff"
            DosageUnit.AMPULE -> "ampule"
            DosageUnit.SPRAY -> "spray"
        }
    }
}
