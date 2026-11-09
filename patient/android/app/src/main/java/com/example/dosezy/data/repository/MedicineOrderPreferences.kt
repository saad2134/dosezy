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

        fun getSortMode(context: Context, userId: String): MedicineSortMode {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString("$KEY_SORT_MODE$userId", MedicineSortMode.DEFAULT.name)
            return try {
                MedicineSortMode.valueOf(raw ?: MedicineSortMode.DEFAULT.name)
            } catch (_: Exception) {
                MedicineSortMode.DEFAULT
            }
        }

        fun setSortMode(context: Context, userId: String, mode: MedicineSortMode) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString("$KEY_SORT_MODE$userId", mode.name).apply()
        }

        fun getCustomOrder(context: Context, userId: String): List<String> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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

        fun setCustomOrder(context: Context, userId: String, orderedIds: List<String>) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val array = JSONArray()
            orderedIds.forEach { array.put(it) }
            prefs.edit().putString("$KEY_CUSTOM_ORDER$userId", array.toString()).apply()
        }

        // Guard: Prune deleted medicine ID from custom order sequence to prevent orphaned ID leaks in storage
        fun removeMedicine(context: Context, userId: String, medicineId: String) {
            val current = getCustomOrder(context, userId)
            if (current.contains(medicineId)) {
                setCustomOrder(context, userId, current.filter { it != medicineId })
            }
        }

        // Guard: Clear all ordering and sorting preferences for a user profile upon deletion or overwrite to prevent preference leaks
        fun clearPreferencesForUser(context: Context, userId: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove("$KEY_SORT_MODE$userId")
                .remove("$KEY_CUSTOM_ORDER$userId")
                .apply()
        }

        // Guard: Export per-profile sorting mode and custom order into JSON to preserve user-defined roster sequence across backup restore
        fun exportUserOrderConfig(context: Context, userId: String): String? {
            val sortMode = getSortMode(context, userId)
            val customOrder = getCustomOrder(context, userId)
            if (sortMode == MedicineSortMode.DEFAULT && customOrder.isEmpty()) {
                return null
            }
            val json = org.json.JSONObject()
            json.put("sortMode", sortMode.name)
            val array = JSONArray()
            customOrder.forEach { array.put(it) }
            json.put("customOrder", array)
            return json.toString()
        }

        // Guard: Restore and remap medicine IDs for custom sequence so restored profiles preserve ordering without broken ID references
        fun restoreUserOrderConfig(
            context: Context,
            userId: String,
            configJson: String,
            medIdMap: Map<String, String>
        ) {
            try {
                val json = org.json.JSONObject(configJson)
                val sortModeStr = json.optString("sortMode", MedicineSortMode.DEFAULT.name)
                val sortMode = try {
                    MedicineSortMode.valueOf(sortModeStr)
                } catch (_: Exception) {
                    MedicineSortMode.DEFAULT
                }
                setSortMode(context, userId, sortMode)

                val array = json.optJSONArray("customOrder")
                if (array != null && array.length() > 0) {
                    val remappedList = mutableListOf<String>()
                    for (i in 0 until array.length()) {
                        val oldId = array.optString(i)
                        if (!oldId.isNullOrBlank()) {
                            val newId = medIdMap[oldId] ?: oldId
                            remappedList.add(newId)
                        }
                    }
                    setCustomOrder(context, userId, remappedList)
                } else {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().remove("$KEY_CUSTOM_ORDER$userId").apply()
                }
            } catch (_: Exception) {}
        }

        // Guard: Merge medicine sorting configuration into target profile without overwriting existing non-default sorting
        fun mergeUserOrderConfig(
            context: Context,
            userId: String,
            configJson: String,
            medIdMap: Map<String, String>
        ) {
            try {
                val json = org.json.JSONObject(configJson)
                val existingMode = getSortMode(context, userId)
                if (existingMode == MedicineSortMode.DEFAULT) {
                    val sortModeStr = json.optString("sortMode", MedicineSortMode.DEFAULT.name)
                    val sortMode = try {
                        MedicineSortMode.valueOf(sortModeStr)
                    } catch (_: Exception) {
                        MedicineSortMode.DEFAULT
                    }
                    setSortMode(context, userId, sortMode)
                }

                val array = json.optJSONArray("customOrder")
                if (array != null && array.length() > 0) {
                    val existingOrder = getCustomOrder(context, userId).toMutableList()
                    for (i in 0 until array.length()) {
                        val oldId = array.optString(i)
                        if (!oldId.isNullOrBlank()) {
                            val newId = medIdMap[oldId] ?: oldId
                            if (!existingOrder.contains(newId)) {
                                existingOrder.add(newId)
                            }
                        }
                    }
                    setCustomOrder(context, userId, existingOrder)
                }
            } catch (_: Exception) {}
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSortMode(userId: String): MedicineSortMode = getSortMode(context, userId)

    fun setSortMode(userId: String, mode: MedicineSortMode) = setSortMode(context, userId, mode)

    fun getCustomOrder(userId: String): List<String> = getCustomOrder(context, userId)

    fun setCustomOrder(userId: String, orderedIds: List<String>) = setCustomOrder(context, userId, orderedIds)

    fun removeMedicine(userId: String, medicineId: String) = removeMedicine(context, userId, medicineId)

    fun clearPreferencesForUser(userId: String) = clearPreferencesForUser(context, userId)

    fun exportUserOrderConfig(userId: String): String? = exportUserOrderConfig(context, userId)

    fun restoreUserOrderConfig(userId: String, configJson: String, medIdMap: Map<String, String>) =
        restoreUserOrderConfig(context, userId, configJson, medIdMap)

    fun mergeUserOrderConfig(userId: String, configJson: String, medIdMap: Map<String, String>) =
        mergeUserOrderConfig(context, userId, configJson, medIdMap)
}
