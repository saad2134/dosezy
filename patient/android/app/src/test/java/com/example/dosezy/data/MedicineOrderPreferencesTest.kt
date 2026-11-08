/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.MedicineSortMode
import com.example.dosezy.data.repository.MedicineOrderPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MedicineOrderPreferencesTest {

    private lateinit var context: Context
    private lateinit var preferences: MedicineOrderPreferences

    private fun createMedicine(
        id: String,
        name: String,
        times: List<LocalTime> = listOf(LocalTime.of(8, 0))
    ): Medicine {
        return Medicine(
            medicineId = id,
            userId = "test_user",
            medicationName = name,
            dosage = 100.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = times.size,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = times
        )
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("medicine_order_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        preferences = MedicineOrderPreferences(context)
    }

    @Test
    fun sortMedicines_defaultMode_preservesOriginalOrder() {
        val med1 = createMedicine("1", "Zoloft")
        val med2 = createMedicine("2", "Aspirin")
        val med3 = createMedicine("3", "Metformin")
        val list = listOf(med1, med2, med3)

        val result = MedicineOrderPreferences.sortMedicines(list, MedicineSortMode.DEFAULT, emptyList())
        assertEquals(listOf("1", "2", "3"), result.map { it.medicineId })
    }

    @Test
    fun sortMedicines_nameAsc_sortsAlphabeticallyCaseInsensitive() {
        val med1 = createMedicine("1", "zoloft")
        val med2 = createMedicine("2", "Aspirin")
        val med3 = createMedicine("3", "metformin")
        val list = listOf(med1, med2, med3)

        val result = MedicineOrderPreferences.sortMedicines(list, MedicineSortMode.NAME_ASC, emptyList())
        assertEquals(listOf("Aspirin", "metformin", "zoloft"), result.map { it.medicationName })
    }

    @Test
    fun sortMedicines_nameDesc_sortsReverseAlphabeticallyCaseInsensitive() {
        val med1 = createMedicine("1", "zoloft")
        val med2 = createMedicine("2", "Aspirin")
        val med3 = createMedicine("3", "metformin")
        val list = listOf(med1, med2, med3)

        val result = MedicineOrderPreferences.sortMedicines(list, MedicineSortMode.NAME_DESC, emptyList())
        assertEquals(listOf("zoloft", "metformin", "Aspirin"), result.map { it.medicationName })
    }

    @Test
    fun sortMedicines_timeAsc_sortsByEarliestScheduledTimeThenName() {
        val medNoon = createMedicine("1", "Z-Med", listOf(LocalTime.of(12, 0)))
        val medMorningB = createMedicine("2", "B-Med", listOf(LocalTime.of(8, 0)))
        val medMorningA = createMedicine("3", "A-Med", listOf(LocalTime.of(8, 0)))
        val medNight = createMedicine("4", "Night-Med", listOf(LocalTime.of(20, 0)))
        val list = listOf(medNoon, medNight, medMorningB, medMorningA)

        val result = MedicineOrderPreferences.sortMedicines(list, MedicineSortMode.TIME_ASC, emptyList())
        assertEquals(listOf("3", "2", "1", "4"), result.map { it.medicineId })
    }

    @Test
    fun sortMedicines_customMode_respectsCustomOrderAndAppendsNewMedicines() {
        val med1 = createMedicine("id_1", "Zoloft")
        val med2 = createMedicine("id_2", "Aspirin")
        val med3 = createMedicine("id_3", "Metformin")
        val medNew = createMedicine("id_new", "Antibiotic")
        val list = listOf(med1, med2, med3, medNew)

        // Custom order specified: id_3, id_1, id_2. id_new is missing and should be placed at end.
        val customOrder = listOf("id_3", "id_1", "id_2")
        val result = MedicineOrderPreferences.sortMedicines(list, MedicineSortMode.CUSTOM, customOrder)
        assertEquals(listOf("id_3", "id_1", "id_2", "id_new"), result.map { it.medicineId })
    }

    @Test
    fun reorderMedicine_moveDown_swapsWithNextItem() {
        val meds = listOf(
            createMedicine("1", "A"),
            createMedicine("2", "B"),
            createMedicine("3", "C")
        )
        val initialOrder = listOf("1", "2", "3")

        val newOrder = MedicineOrderPreferences.reorderMedicine(initialOrder, "1", moveUp = false, meds)
        assertEquals(listOf("2", "1", "3"), newOrder)
    }

    @Test
    fun reorderMedicine_moveUp_swapsWithPreviousItem() {
        val meds = listOf(
            createMedicine("1", "A"),
            createMedicine("2", "B"),
            createMedicine("3", "C")
        )
        val initialOrder = listOf("1", "2", "3")

        val newOrder = MedicineOrderPreferences.reorderMedicine(initialOrder, "3", moveUp = true, meds)
        assertEquals(listOf("1", "3", "2"), newOrder)
    }

    @Test
    fun reorderMedicine_moveTopUp_and_moveBottomDown_areNoOps() {
        val meds = listOf(
            createMedicine("1", "A"),
            createMedicine("2", "B"),
            createMedicine("3", "C")
        )
        val initialOrder = listOf("1", "2", "3")

        val topUp = MedicineOrderPreferences.reorderMedicine(initialOrder, "1", moveUp = true, meds)
        assertEquals(initialOrder, topUp)

        val bottomDown = MedicineOrderPreferences.reorderMedicine(initialOrder, "3", moveUp = false, meds)
        assertEquals(initialOrder, bottomDown)
    }

    @Test
    fun preferences_sortMode_persistsPerUserId() {
        assertEquals(MedicineSortMode.DEFAULT, preferences.getSortMode("user_a"))

        preferences.setSortMode("user_a", MedicineSortMode.NAME_ASC)
        preferences.setSortMode("user_b", MedicineSortMode.TIME_ASC)

        assertEquals(MedicineSortMode.NAME_ASC, preferences.getSortMode("user_a"))
        assertEquals(MedicineSortMode.TIME_ASC, preferences.getSortMode("user_b"))
    }

    @Test
    fun preferences_customOrder_persistsAndHandlesCorruptedJson() {
        assertTrue(preferences.getCustomOrder("user_a").isEmpty())

        val ids = listOf("id_9", "id_2", "id_5")
        preferences.setCustomOrder("user_a", ids)
        assertEquals(ids, preferences.getCustomOrder("user_a"))

        // Simulate malformed JSON in preferences
        context.getSharedPreferences("medicine_order_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("custom_order_user_a", "not_a_valid_json")
            .commit()

        assertTrue(preferences.getCustomOrder("user_a").isEmpty())
    }
}

