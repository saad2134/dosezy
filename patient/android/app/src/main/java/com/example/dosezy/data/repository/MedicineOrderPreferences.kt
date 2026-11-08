/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.data.repository

import android.content.Context
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.MedicineSortMode
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import java.time.LocalTime
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles persistence and evaluation of user-defined medication ordering and sorting presets.
 */
@Singleton
class MedicineOrderPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "medicine_order_prefs"
        private const val KEY_SORT_MODE = "sort_mode_"
        private const val KEY_CUSTOM_ORDER = "custom_order_"

        /**
         * Pure function to sort a list of medicines according to the active sort mode and custom order.
         */
        fun sortMedicines(
            medicines: List<Medicine>,
            mode: MedicineSortMode,
            customOrder: List<String>
        ): List<Medicine> {
            return when (mode) {
                MedicineSortMode.DEFAULT -> medicines
                MedicineSortMode.NAME_ASC -> medicines.sortedWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { it.medicationName.trim() }
                )
                MedicineSortMode.NAME_DESC -> medicines.sortedWith(
                    compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.medicationName.trim() }
                )
                MedicineSortMode.TIME_ASC -> medicines.sortedWith(
                    compareBy<Medicine> {
                        it.scheduledTimes.minOrNull() ?: LocalTime.MAX
                    }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.medicationName.trim() }
                )
                MedicineSortMode.CUSTOM -> {
                    val orderMap = customOrder.mapIndexed { index, id -> id to index }.toMap()
                    medicines.sortedWith(
                        compareBy<Medicine> {
                            orderMap[it.medicineId] ?: Int.MAX_VALUE
                        }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.medicationName.trim() }
                    )
                }
            }
        }

        /**
         * Helper to reorder a target medicine up or down within a custom sequence.
         */
        fun reorderMedicine(
            currentOrder: List<String>,
            medicineId: String,
            moveUp: Boolean,
            allMedicines: List<Medicine>
        ): List<String> {
            val allIds = allMedicines.map { it.medicineId }
            val existingIds = currentOrder.filter { allIds.contains(it) }
            val missingIds = allIds.filter { !existingIds.contains(it) }
            val mutableList = (existingIds + missingIds).toMutableList()

            val index = mutableList.indexOf(medicineId)
            if (index == -1) return mutableList

            if (moveUp && index > 0) {
                Collections.swap(mutableList, index, index - 1)
            } else if (!moveUp && index < mutableList.size - 1) {
                Collections.swap(mutableList, index, index + 1)
            }
            return mutableList
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSortMode(userId: String): MedicineSortMode {
        val raw = prefs.getString("$KEY_SORT_MODE$userId", MedicineSortMode.DEFAULT.name)
        return try {
            MedicineSortMode.valueOf(raw ?: MedicineSortMode.DEFAULT.name)
        } catch (_: Exception) {
            MedicineSortMode.DEFAULT
        }
    }

    fun setSortMode(userId: String, mode: MedicineSortMode) {
        prefs.edit().putString("$KEY_SORT_MODE$userId", mode.name).apply()
    }

    fun getCustomOrder(userId: String): List<String> {
        val jsonStr = prefs.getString("$KEY_CUSTOM_ORDER$userId", null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val id = array.getString(i)
                if (id.isNotBlank()) list.add(id)
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setCustomOrder(userId: String, orderedIds: List<String>) {
        val array = JSONArray()
        orderedIds.forEach { array.put(it) }
        prefs.edit().putString("$KEY_CUSTOM_ORDER$userId", array.toString()).apply()
    }
}
