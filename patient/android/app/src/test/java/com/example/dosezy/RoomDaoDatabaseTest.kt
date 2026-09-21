/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.dao.MedicineDao
import com.example.dosezy.data.dao.ScheduleDao
import com.example.dosezy.data.dao.UserDao
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomDaoDatabaseTest {

    private lateinit var db: DosezyDatabase
    private lateinit var userDao: UserDao
    private lateinit var medicineDao: MedicineDao
    private lateinit var scheduleDao: ScheduleDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DosezyDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA foreign_keys = ON;")
                }
            })
            .build()
        userDao = db.userDao()
        medicineDao = db.medicineDao()
        scheduleDao = db.scheduleDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    private fun sampleUser(userId: String = "u_1", name: String = "Alice"): User {
        return User(
            userId = userId,
            fullName = name,
            age = 30,
            gender = Gender.FEMALE,
            contactNumber = "555-1234",
            isCurrentUser = true
        )
    }

    private fun sampleMedicine(
        medicineId: String = "m_1",
        userId: String = "u_1",
        name: String = "Amoxicillin",
        isArchived: Boolean = false
    ): Medicine {
        return Medicine(
            medicineId = medicineId,
            userId = userId,
            medicationName = name,
            dosage = 500.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(9, 0)),
            isArchived = isArchived
        )
    }

    private fun sampleScheduleEntry(
        entryId: String = "s_1",
        userId: String = "u_1",
        medicineId: String = "m_1",
        dateTime: LocalDateTime = LocalDateTime.of(2026, 9, 21, 9, 0),
        status: MedicationStatus = MedicationStatus.PENDING,
        takenAt: LocalDateTime? = null,
        skipReason: String? = null
    ): ScheduleEntry {
        return ScheduleEntry(
            entryId = entryId,
            userId = userId,
            medicineId = medicineId,
            scheduledDateTime = dateTime,
            status = status,
            takenAt = takenAt,
            skipReason = skipReason
        )
    }

    // ───────────────────────────────────────────────────────────────
    // 1. UserDao Tests
    // ───────────────────────────────────────────────────────────────

    @Test
    fun userDao_insertAndRetrieveUser() = runBlocking {
        val user = sampleUser("u_100", "Charlie")
        userDao.insertUser(user)

        val direct = userDao.getUserByIdDirect("u_100")
        assertNotNull(direct)
        assertEquals("Charlie", direct?.fullName)
        assertEquals(30, direct?.age)

        val allUsers = userDao.getAllUsersDirect()
        assertEquals(1, allUsers.size)
        assertEquals("u_100", allUsers[0].userId)
    }

    @Test
    fun userDao_updateUser_reflectsChanges() = runBlocking {
        val user = sampleUser("u_200", "David")
        userDao.insertUser(user)

        val updated = user.copy(fullName = "David Updated", age = 35)
        userDao.updateUser(updated)

        val retrieved = userDao.getUserByIdDirect("u_200")
        assertEquals("David Updated", retrieved?.fullName)
        assertEquals(35, retrieved?.age)
    }

    @Test
    fun userDao_deleteUserById_removesUser() = runBlocking {
        val user = sampleUser("u_300", "Eve")
        userDao.insertUser(user)
        assertNotNull(userDao.getUserByIdDirect("u_300"))

        userDao.deleteUserById("u_300")
        assertNull(userDao.getUserByIdDirect("u_300"))
    }

    // ───────────────────────────────────────────────────────────────
    // 2. MedicineDao Active vs Archived Tests
    // ───────────────────────────────────────────────────────────────

    @Test
    fun medicineDao_activeAndArchivedFiltering() = runBlocking {
        userDao.insertUser(sampleUser("u_1"))
        medicineDao.insertMedicine(sampleMedicine("m_act_1", "u_1", "Ibuprofen", isArchived = false))
        medicineDao.insertMedicine(sampleMedicine("m_act_2", "u_1", "Paracetamol", isArchived = false))
        medicineDao.insertMedicine(sampleMedicine("m_arc_1", "u_1", "Old Med", isArchived = true))

        val active = medicineDao.getActiveMedicinesByUser("u_1").first()
        assertEquals(2, active.size)
        assertTrue(active.any { it.medicineId == "m_act_1" })
        assertTrue(active.any { it.medicineId == "m_act_2" })

        val archived = medicineDao.getArchivedMedicinesByUser("u_1").first()
        assertEquals(1, archived.size)
        assertEquals("m_arc_1", archived[0].medicineId)
    }

    @Test
    fun medicineDao_setArchivedStatus_movesMedicineBetweenLists() = runBlocking {
        userDao.insertUser(sampleUser("u_1"))
        val med = sampleMedicine("m_toggle", "u_1", "Metformin", isArchived = false)
        medicineDao.insertMedicine(med)

        assertEquals(1, medicineDao.getActiveMedicinesByUser("u_1").first().size)
        assertEquals(0, medicineDao.getArchivedMedicinesByUser("u_1").first().size)

        medicineDao.setArchivedStatus("m_toggle", true)
        assertEquals(0, medicineDao.getActiveMedicinesByUser("u_1").first().size)
        assertEquals(1, medicineDao.getArchivedMedicinesByUser("u_1").first().size)

        medicineDao.setArchivedStatus("m_toggle", false)
        assertEquals(1, medicineDao.getActiveMedicinesByUser("u_1").first().size)
        assertEquals(0, medicineDao.getArchivedMedicinesByUser("u_1").first().size)
    }

    // ───────────────────────────────────────────────────────────────
    // 3. ScheduleDao Date Range and Query Filtering
    // ───────────────────────────────────────────────────────────────

    @Test
    fun scheduleDao_getScheduleForDateRange_filtersCorrectly() = runBlocking {
        userDao.insertUser(sampleUser("u_1"))
        medicineDao.insertMedicine(sampleMedicine("m_1", "u_1"))

        val targetDate = LocalDate.of(2026, 9, 21)
        val startOfDay = targetDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endOfDay = targetDate.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant().toEpochMilli()

        // Yesterday
        val entryYesterday = sampleScheduleEntry(
            "s_prev", "u_1", "m_1",
            LocalDateTime.of(2026, 9, 20, 22, 0)
        )
        // Today morning
        val entryTodayMorning = sampleScheduleEntry(
            "s_today_1", "u_1", "m_1",
            LocalDateTime.of(2026, 9, 21, 8, 0)
        )
        // Today evening
        val entryTodayEvening = sampleScheduleEntry(
            "s_today_2", "u_1", "m_1",
            LocalDateTime.of(2026, 9, 21, 20, 0)
        )
        // Tomorrow
        val entryTomorrow = sampleScheduleEntry(
            "s_next", "u_1", "m_1",
            LocalDateTime.of(2026, 9, 22, 8, 0)
        )

        scheduleDao.insertScheduleEntries(listOf(entryYesterday, entryTodayMorning, entryTodayEvening, entryTomorrow))

        val results = scheduleDao.getScheduleForDateRangeDirect("u_1", startOfDay, endOfDay)
        assertEquals(2, results.size)
        assertEquals("s_today_1", results[0].entryId)
        assertEquals("s_today_2", results[1].entryId)
    }

    @Test
    fun scheduleDao_updateMedicationStatusWithReason() = runBlocking {
        userDao.insertUser(sampleUser("u_1"))
        medicineDao.insertMedicine(sampleMedicine("m_1", "u_1"))
        scheduleDao.insertScheduleEntry(sampleScheduleEntry("s_skip", "u_1", "m_1"))

        val takenAtMillis = LocalDateTime.of(2026, 9, 21, 10, 0)
            .atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
        scheduleDao.updateMedicationStatusWithReason(
            entryId = "s_skip",
            status = "SKIPPED",
            takenAt = takenAtMillis,
            skipReason = "Fasting for blood test"
        )

        val updated = scheduleDao.getScheduleEntryById("s_skip")
        assertNotNull(updated)
        assertEquals(MedicationStatus.SKIPPED, updated?.status)
        assertEquals("Fasting for blood test", updated?.skipReason)
        assertNotNull(updated?.takenAt)
    }

    @Test
    fun scheduleDao_deleteUntakenScheduleEntriesFrom_preservesTakenDoses() = runBlocking {
        userDao.insertUser(sampleUser("u_1"))
        medicineDao.insertMedicine(sampleMedicine("m_1", "u_1"))

        val cutoff = LocalDateTime.of(2026, 9, 21, 12, 0)
        val cutoffMillis = cutoff.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()

        // Future pending dose (should be deleted)
        val futurePending = sampleScheduleEntry(
            "s_fut_pending", "u_1", "m_1",
            LocalDateTime.of(2026, 9, 21, 14, 0),
            status = MedicationStatus.PENDING
        )
        // Future taken dose (should be preserved)
        val futureTaken = sampleScheduleEntry(
            "s_fut_taken", "u_1", "m_1",
            LocalDateTime.of(2026, 9, 21, 18, 0),
            status = MedicationStatus.TAKEN_ON_TIME,
            takenAt = LocalDateTime.of(2026, 9, 21, 18, 1)
        )
        // Past pending dose (before cutoff, should be preserved)
        val pastPending = sampleScheduleEntry(
            "s_past_pending", "u_1", "m_1",
            LocalDateTime.of(2026, 9, 21, 8, 0),
            status = MedicationStatus.PENDING
        )

        scheduleDao.insertScheduleEntries(listOf(futurePending, futureTaken, pastPending))

        scheduleDao.deleteUntakenScheduleEntriesFrom("m_1", cutoffMillis)

        assertNull(scheduleDao.getScheduleEntryById("s_fut_pending"))
        assertNotNull(scheduleDao.getScheduleEntryById("s_fut_taken"))
        assertNotNull(scheduleDao.getScheduleEntryById("s_past_pending"))
    }

    // ───────────────────────────────────────────────────────────────
    // 4. ScheduleWithMedicine Join / Relation (@Transaction)
    // ───────────────────────────────────────────────────────────────

    @Test
    fun scheduleWithMedicine_relationResolvesParentMedicine() = runBlocking {
        userDao.insertUser(sampleUser("u_1", "Test User"))
        medicineDao.insertMedicine(sampleMedicine("m_1", "u_1", "Atorvastatin"))

        val targetDate = LocalDate.of(2026, 9, 21)
        val startOfDay = targetDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endOfDay = targetDate.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant().toEpochMilli()

        scheduleDao.insertScheduleEntry(
            sampleScheduleEntry("s_join", "u_1", "m_1", LocalDateTime.of(2026, 9, 21, 9, 0))
        )

        val scheduleWithMedicineList = scheduleDao
            .getScheduleWithMedicineForDateRange("u_1", startOfDay, endOfDay)
            .first()

        assertEquals(1, scheduleWithMedicineList.size)
        val item = scheduleWithMedicineList[0]
        assertEquals("s_join", item.scheduleEntry.entryId)
        assertNotNull(item.medicine)
        assertEquals("Atorvastatin", item.medicine?.medicationName)
        assertEquals(500.0, item.medicine?.dosage ?: 0.0, 0.001)
    }

    // ───────────────────────────────────────────────────────────────
    // 5. Foreign Key Cascading Deletes (Data Integrity)
    // ───────────────────────────────────────────────────────────────

    @Test
    fun foreignKeyCascade_deleteMedicine_cascadesToScheduleEntries() = runBlocking {
        userDao.insertUser(sampleUser("u_1"))
        medicineDao.insertMedicine(sampleMedicine("m_del", "u_1"))

        scheduleDao.insertScheduleEntry(
            sampleScheduleEntry("s_cascade_1", "u_1", "m_del", LocalDateTime.of(2026, 9, 21, 8, 0))
        )
        scheduleDao.insertScheduleEntry(
            sampleScheduleEntry("s_cascade_2", "u_1", "m_del", LocalDateTime.of(2026, 9, 21, 20, 0))
        )

        assertEquals(2, scheduleDao.getScheduleEntriesByMedicine("m_del").size)

        // Delete the medicine directly
        medicineDao.deleteMedicineById("m_del")

        // Medicine must be deleted
        assertNull(medicineDao.getMedicineByIdDirect("m_del"))
        // Associated schedule entries must be cascade deleted by SQLite
        assertEquals(0, scheduleDao.getScheduleEntriesByMedicine("m_del").size)
        assertNull(scheduleDao.getScheduleEntryById("s_cascade_1"))
        assertNull(scheduleDao.getScheduleEntryById("s_cascade_2"))
    }

    @Test
    fun foreignKeyCascade_deleteUser_cascadesToMedicinesAndScheduleEntries() = runBlocking {
        userDao.insertUser(sampleUser("u_del"))
        medicineDao.insertMedicine(sampleMedicine("m_u_1", "u_del"))
        medicineDao.insertMedicine(sampleMedicine("m_u_2", "u_del"))

        scheduleDao.insertScheduleEntry(
            sampleScheduleEntry("s_u_1", "u_del", "m_u_1", LocalDateTime.of(2026, 9, 21, 8, 0))
        )
        scheduleDao.insertScheduleEntry(
            sampleScheduleEntry("s_u_2", "u_del", "m_u_2", LocalDateTime.of(2026, 9, 21, 12, 0))
        )

        // Delete the user
        userDao.deleteUserById("u_del")

        // User must be deleted
        assertNull(userDao.getUserByIdDirect("u_del"))
        // All medicines belonging to the user must be cascade deleted
        assertEquals(0, medicineDao.getMedicinesByUserDirect("u_del").size)
        // All schedule entries belonging to the user must be cascade deleted
        assertEquals(0, scheduleDao.getAllScheduleEntries("u_del").size)
    }
}
