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
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.MedicineSortMode
import com.example.dosezy.data.model.User
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

    @Test
    fun removeMedicine_prunesDeletedIdFromCustomOrder() {
        preferences.setCustomOrder("user_a", listOf("med_1", "med_2", "med_3"))
        preferences.removeMedicine("user_a", "med_2")
        assertEquals(listOf("med_1", "med_3"), preferences.getCustomOrder("user_a"))
    }

    @Test
    fun clearPreferencesForUser_removesSortModeAndCustomOrder() {
        preferences.setSortMode("user_a", MedicineSortMode.CUSTOM)
        preferences.setCustomOrder("user_a", listOf("med_1", "med_2"))

        preferences.clearPreferencesForUser("user_a")
        assertEquals(MedicineSortMode.DEFAULT, preferences.getSortMode("user_a"))
        assertTrue(preferences.getCustomOrder("user_a").isEmpty())
    }

    @Test
    fun exportAndRestoreUserOrderConfig_remapsMedicineIdsAccurately() {
        preferences.setSortMode("user_export", MedicineSortMode.CUSTOM)
        preferences.setCustomOrder("user_export", listOf("old_med_1", "old_med_2"))

        val exportedJson = preferences.exportUserOrderConfig("user_export")
        assertTrue(exportedJson != null)

        val idMap = mapOf(
            "old_med_1" to "new_uuid_1",
            "old_med_2" to "new_uuid_2"
        )
        MedicineOrderPreferences.restoreUserOrderConfig(context, "user_import", exportedJson!!, idMap)

        assertEquals(MedicineSortMode.CUSTOM, preferences.getSortMode("user_import"))
        assertEquals(listOf("new_uuid_1", "new_uuid_2"), preferences.getCustomOrder("user_import"))
    }

    @Test
    fun mergeUserOrderConfig_combinesNewIdsWithoutDuplication() {
        preferences.setSortMode("user_merge", MedicineSortMode.TIME_ASC)
        preferences.setCustomOrder("user_merge", listOf("existing_1", "existing_2"))

        val configJson = org.json.JSONObject().apply {
            put("sortMode", MedicineSortMode.NAME_ASC.name)
            put("customOrder", org.json.JSONArray(listOf("old_3", "existing_1")))
        }.toString()

        val idMap = mapOf("old_3" to "remapped_3")
        MedicineOrderPreferences.mergeUserOrderConfig(context, "user_merge", configJson, idMap)

        // Existing non-default sort mode is retained
        assertEquals(MedicineSortMode.TIME_ASC, preferences.getSortMode("user_merge"))
        // New ID is appended, existing ID is not duplicated
        assertEquals(listOf("existing_1", "existing_2", "remapped_3"), preferences.getCustomOrder("user_merge"))
    }

    @Test
    fun deleteMedicinePermanently_repositoryCall_scrubsMedicineFromCustomOrder() = kotlinx.coroutines.runBlocking {
        // Guard: Verify that deleting a medicine directly via MedicineRepository purges it from custom order preferences (#BUG-MED-REPO-DELETE-ORDER)
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, DosezyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val scheduleRepo = com.example.dosezy.data.repository.ScheduleRepository(db)
        val medRepo = com.example.dosezy.data.repository.MedicineRepository(db, scheduleRepo, context)

        val userId = "user_repo_del_test"
        val user = User(
            userId = userId,
            fullName = "Order Test User",
            age = 30,
            gender = Gender.DO_NOT_SPECIFY,
            contactNumber = "1234567890"
        )
        db.userDao().insertUser(user)

        val med1 = createMedicine("med_del_1", "Med One").copy(userId = userId)
        val med2 = createMedicine("med_del_2", "Med Two").copy(userId = userId)
        db.medicineDao().insertMedicine(med1)
        db.medicineDao().insertMedicine(med2)

        preferences.setCustomOrder(userId, listOf("med_del_1", "med_del_2"))
        assertEquals(listOf("med_del_1", "med_del_2"), preferences.getCustomOrder(userId))

        medRepo.deleteMedicinePermanently(med1)

        assertEquals(
            "deleteMedicinePermanently must remove deleted medicine ID from custom order preferences",
            listOf("med_del_2"),
            preferences.getCustomOrder(userId)
        )
        db.close()
    }
}

