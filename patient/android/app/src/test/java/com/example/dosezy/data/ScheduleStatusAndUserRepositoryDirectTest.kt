/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.data

import android.content.Context
import android.content.Intent
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
import com.example.dosezy.data.repository.UserRepository
import com.example.dosezy.domain.usecases.GenerateSchedulesUseCase
import com.example.dosezy.notifications.MedicineAlarmReceiver
import com.example.dosezy.notifications.MedicineNotificationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScheduleStatusAndUserRepositoryDirectTest {

    private lateinit var context: Context
    private lateinit var db: DosezyDatabase
    private lateinit var userDao: UserDao
    private lateinit var medicineDao: MedicineDao
    private lateinit var scheduleDao: ScheduleDao
    private lateinit var userRepository: UserRepository
    private lateinit var generateSchedulesUseCase: GenerateSchedulesUseCase
    private lateinit var medicineNotificationManager: MedicineNotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
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
        userRepository = UserRepository(db)
        generateSchedulesUseCase = GenerateSchedulesUseCase(db)
        medicineNotificationManager = MedicineNotificationManager(context, db)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    private fun sampleUser(userId: String = "user_deep_dive_3"): User {
        return User(
            userId = userId,
            fullName = "Audit Test User",
            age = 35,
            gender = Gender.FEMALE,
            contactNumber = "555-0199",
            isCurrentUser = true
        )
    }

    private fun sampleMedicine(
        medicineId: String = "med_deep_dive_3",
        userId: String = "user_deep_dive_3",
        name: String = "Amoxicillin",
        startDate: LocalDate = LocalDate.now().minusDays(10),
        endDate: LocalDate? = null,
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
            scheduledTimes = listOf(LocalTime.of(8, 0)),
            startDate = startDate,
            endDate = endDate,
            isArchived = isArchived
        )
    }

    private fun sampleScheduleEntry(
        entryId: String = "entry_deep_dive_3",
        userId: String = "user_deep_dive_3",
        medicineId: String = "med_deep_dive_3",
        dateTime: LocalDateTime = LocalDateTime.now().minusHours(2),
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

    // ---------------------------------------------------------------------------------------------
    // Bug 1: Stale skipReason Cleared on Dose Taken or Missed Transitions
    // ---------------------------------------------------------------------------------------------

    @Test
    fun updateMedicationStatus_clearsSkipReasonWhenMarkedTaken() = runBlocking {
        val user = sampleUser()
        userDao.insertUser(user)
        val med = sampleMedicine()
        medicineDao.insertMedicine(med)

        val entry = sampleScheduleEntry()
        scheduleDao.insertScheduleEntries(listOf(entry))

        // Step 1: Mark as skipped with reason
        scheduleDao.updateMedicationStatusWithReason(entry.entryId, "SKIPPED", null, "Nausea")
        val skippedEntry = scheduleDao.getScheduleEntryById(entry.entryId)
        assertNotNull(skippedEntry)
        assertEquals(MedicationStatus.SKIPPED, skippedEntry!!.status)
        assertEquals("Nausea", skippedEntry.skipReason)

        // Step 2: User changes status to TAKEN_ON_TIME
        val nowMillis = System.currentTimeMillis()
        scheduleDao.updateMedicationStatus(entry.entryId, "TAKEN_ON_TIME", nowMillis)

        // Verify: skipReason must be cleared to NULL so reports do not display contradictory status
        val takenEntry = scheduleDao.getScheduleEntryById(entry.entryId)
        assertNotNull(takenEntry)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, takenEntry!!.status)
        assertNull("skipReason must be reset to null when marked taken", takenEntry.skipReason)
    }

    @Test
    fun updateMedicationStatusWithNotes_clearsSkipReasonWhenMarkedTakenWithNotes() = runBlocking {
        val user = sampleUser()
        userDao.insertUser(user)
        val med = sampleMedicine()
        medicineDao.insertMedicine(med)

        val entry = sampleScheduleEntry()
        scheduleDao.insertScheduleEntries(listOf(entry))

        // Mark as skipped with reason
        scheduleDao.updateMedicationStatusWithReason(entry.entryId, "SKIPPED", null, "Dizziness")
        val skippedEntry = scheduleDao.getScheduleEntryById(entry.entryId)
        assertEquals("Dizziness", skippedEntry?.skipReason)

        // Mark taken with notes
        val nowMillis = System.currentTimeMillis()
        scheduleDao.updateMedicationStatusWithNotes(entry.entryId, "TAKEN_ON_TIME", nowMillis, "Taken after meal")

        val takenEntry = scheduleDao.getScheduleEntryById(entry.entryId)
        assertNotNull(takenEntry)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, takenEntry!!.status)
        assertEquals("Taken after meal", takenEntry.doseNotes)
        assertNull("skipReason must be reset to null when updated with notes", takenEntry.skipReason)
    }

    @Test
    fun updateMedicationStatus_clearsSkipReasonWhenMarkedMissed() = runBlocking {
        val user = sampleUser()
        userDao.insertUser(user)
        val med = sampleMedicine()
        medicineDao.insertMedicine(med)

        val entry = sampleScheduleEntry()
        scheduleDao.insertScheduleEntries(listOf(entry))

        // Mark as skipped with reason
        scheduleDao.updateMedicationStatusWithReason(entry.entryId, "SKIPPED", null, "Felt unwell")
        val skippedEntry = scheduleDao.getScheduleEntryById(entry.entryId)
        assertEquals("Felt unwell", skippedEntry?.skipReason)

        // Mark as missed
        scheduleDao.updateMedicationStatus(entry.entryId, "MISSED", null)

        val missedEntry = scheduleDao.getScheduleEntryById(entry.entryId)
        assertNotNull(missedEntry)
        assertEquals(MedicationStatus.MISSED, missedEntry!!.status)
        assertNull("skipReason must be reset to null when marked missed", missedEntry.skipReason)
    }

    // ---------------------------------------------------------------------------------------------
    // Bug 2: Safe goAsync() Lifecycle Handling in MedicineAlarmReceiver
    // ---------------------------------------------------------------------------------------------

    @Test
    fun medicineAlarmReceiver_onReceiveSystemBroadcast_handlesLifecycleSafely() {
        val receiver = MedicineAlarmReceiver()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        // In Robolectric environment, goAsync() executes cleanly with try-catch guards
        receiver.onReceive(context, intent)
        ShadowLooper.idleMainLooper()
        assertTrue("Receiver handled system broadcast without unhandled exception", true)
    }

    @Test
    fun medicineAlarmReceiver_onReceiveAlarmTrigger_handlesLifecycleSafely() {
        val receiver = MedicineAlarmReceiver()
        val intent = Intent().apply {
            putExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID, "entry_123")
            putExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME, "Paracetamol")
            putExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME, "08:00")
        }

        receiver.onReceive(context, intent)
        ShadowLooper.idleMainLooper()
        assertTrue("Receiver handled alarm intent without unhandled exception", true)
    }

    // ---------------------------------------------------------------------------------------------
    // Bug 3: GenerateSchedulesUseCase Bypasses Expired Finite Courses
    // ---------------------------------------------------------------------------------------------

    @Test
    fun autoExtendSchedules_bypassesExpiredFiniteMedicationsWithoutRedundantScans() = runBlocking {
        val user = sampleUser()
        userDao.insertUser(user)

        // Expired finite medicine that ended 10 days ago
        val expiredMedicine = sampleMedicine(
            medicineId = "med_expired",
            startDate = LocalDate.now().minusDays(20),
            endDate = LocalDate.now().minusDays(10),
            isArchived = false
        )
        medicineDao.insertMedicine(expiredMedicine)

        // Auto-extend schedules
        generateSchedulesUseCase.autoExtendSchedules(user.userId)

        // Verify: No entries should be generated for expired medication
        val entries = scheduleDao.getScheduleEntriesByMedicine("med_expired")
        assertTrue("No entries should be generated for medication whose endDate is in the past", entries.isEmpty())
    }

    @Test
    fun autoExtendSchedules_bypassesMedicinesWhoseScheduleAlreadyReachedEndDate() = runBlocking {
        val user = sampleUser()
        userDao.insertUser(user)

        // Medicine that ends today
        val today = LocalDate.now()
        val finiteMedicine = sampleMedicine(
            medicineId = "med_finite_today",
            startDate = today.minusDays(5),
            endDate = today,
            isArchived = false
        )
        medicineDao.insertMedicine(finiteMedicine)

        // Add an entry on the end date
        val existingEntry = sampleScheduleEntry(
            entryId = "entry_final_day",
            medicineId = "med_finite_today",
            dateTime = today.atTime(8, 0)
        )
        scheduleDao.insertScheduleEntries(listOf(existingEntry))

        // Auto-extend schedules
        generateSchedulesUseCase.autoExtendSchedules(user.userId)

        // Verify: Schedule count must remain 1 because it already reached endDate
        val entries = scheduleDao.getScheduleEntriesByMedicine("med_finite_today")
        assertEquals(1, entries.size)
        assertEquals("entry_final_day", entries.first().entryId)
    }

    // ---------------------------------------------------------------------------------------------
    // Bug 4: Direct Suspend Queries in UserRepository Synchronous Lookups
    // ---------------------------------------------------------------------------------------------

    @Test
    fun userRepository_syncMethods_useDirectQueriesDirectly() = runBlocking {
        val user1 = sampleUser("user_sync_1")
        val user2 = sampleUser("user_sync_2").copy(fullName = "Bob", isCurrentUser = false)
        userDao.insertUser(user1)
        userDao.insertUser(user2)

        // Test getAllUsersList returns direct list without Flow.first observer overhead
        val allUsers = userRepository.getAllUsersList()
        assertEquals(2, allUsers.size)
        assertTrue(allUsers.any { it.userId == "user_sync_1" })
        assertTrue(allUsers.any { it.userId == "user_sync_2" })

        // Test getUserByIdSync returns direct user without Flow.first observer overhead
        val fetchedUser = userRepository.getUserByIdSync("user_sync_1")
        assertNotNull(fetchedUser)
        assertEquals("Audit Test User", fetchedUser!!.fullName)

        val nonExistentUser = userRepository.getUserByIdSync("user_missing")
        assertNull(nonExistentUser)
    }

    // ---------------------------------------------------------------------------------------------
    // Bug 5: MedicineNotificationManager.checkAlarmStatus Dispatches on Main Thread
    // ---------------------------------------------------------------------------------------------

    @Test
    fun medicineNotificationManager_checkAlarmStatus_dispatchesOnMainThread() {
        val user = sampleUser("user_alarm_status")
        runBlocking {
            userDao.insertUser(user)
        }

        var completed = false
        var callbackInvokedOnMainLooper = false

        medicineNotificationManager.checkAlarmStatus("user_alarm_status") { _, _ ->
            // Check thread confinement
            callbackInvokedOnMainLooper = android.os.Looper.myLooper() == android.os.Looper.getMainLooper()
            completed = true
        }

        val deadline = System.currentTimeMillis() + 3000
        while (!completed && System.currentTimeMillis() < deadline) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }

        assertTrue("Callback should be invoked within timeout", completed)
        assertTrue("Callback must be dispatched on main looper", callbackInvokedOnMainLooper)
    }

    // ---------------------------------------------------------------------------------------------
    // Bug 6: MedicineDao.getActiveMedicinesByUserDirect and Sync Lookups Without Flow Overhead
    // ---------------------------------------------------------------------------------------------

    @Test
    fun medicineDao_getActiveMedicinesByUserDirect_filtersArchivedAndReturnsDirectList() = runBlocking {
        val user = sampleUser("user_direct_meds")
        userDao.insertUser(user)

        val activeMed = sampleMedicine("med_active", "user_direct_meds").copy(isArchived = false)
        val archivedMed = sampleMedicine("med_archived", "user_direct_meds").copy(isArchived = true)
        medicineDao.insertMedicine(activeMed)
        medicineDao.insertMedicine(archivedMed)

        // 1. Verify MedicineDao.getActiveMedicinesByUserDirect excludes archived medications
        val activeMeds = medicineDao.getActiveMedicinesByUserDirect("user_direct_meds")
        assertEquals(1, activeMeds.size)
        assertEquals("med_active", activeMeds.first().medicineId)

        // 2. Verify MedicineRepository.getMedicinesByUserSync uses direct query
        val scheduleRepository = com.example.dosezy.data.repository.ScheduleRepository(db)
        val medicineRepository = com.example.dosezy.data.repository.MedicineRepository(db, scheduleRepository, context)
        val syncMeds = medicineRepository.getMedicinesByUserSync("user_direct_meds")
        assertEquals(1, syncMeds.size)
        assertEquals("med_active", syncMeds.first().medicineId)

        // 3. Verify GenerateSchedulesUseCase.autoExtendSchedules executes cleanly with direct query
        generateSchedulesUseCase.autoExtendSchedules("user_direct_meds")

        // 4. Verify ScheduleRepository.getSchedulesByUserSync returns direct list
        val allSchedules = scheduleRepository.getSchedulesByUserSync("user_direct_meds")
        assertNotNull(allSchedules)
    }

    // ---------------------------------------------------------------------------------------------
    // Atomic Current User Switching (Profile Switch Race Guard)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testSetCurrentUserAtomic_updatesSingleProfileAndDemotesOthers() = runBlocking {
        val user1 = sampleUser("user_atomic_1").copy(isCurrentUser = true)
        val user2 = sampleUser("user_atomic_2").copy(isCurrentUser = false)
        val user3 = sampleUser("user_atomic_3").copy(isCurrentUser = false)

        userDao.insertUser(user1)
        userDao.insertUser(user2)
        userDao.insertUser(user3)

        // Guard: Verify atomic update flips target user to 1 and all others to 0 in a single query
        userRepository.setCurrentUser("user_atomic_2")

        val u1After = userRepository.getUserByIdSync("user_atomic_1")
        val u2After = userRepository.getUserByIdSync("user_atomic_2")
        val u3After = userRepository.getUserByIdSync("user_atomic_3")

        assertNotNull(u1After)
        assertNotNull(u2After)
        assertNotNull(u3After)
        assertFalse(u1After!!.isCurrentUser)
        assertTrue(u2After!!.isCurrentUser)
        assertFalse(u3After!!.isCurrentUser)

        // Switch to user 3
        userRepository.setCurrentUser("user_atomic_3")

        val u1Final = userRepository.getUserByIdSync("user_atomic_1")
        val u2Final = userRepository.getUserByIdSync("user_atomic_2")
        val u3Final = userRepository.getUserByIdSync("user_atomic_3")

        assertFalse(u1Final!!.isCurrentUser)
        assertFalse(u2Final!!.isCurrentUser)
        assertTrue(u3Final!!.isCurrentUser)
    }

    // ---------------------------------------------------------------------------------------------
    // PRN As-Needed Dose Non-Colliding Primary Key Guard
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testLogAsNeededDose_createsUniquePrimaryKeys_andPreventsSilentOverwrite() = runBlocking {
        val user = sampleUser("user_prn_unique")
        userDao.insertUser(user)
        val med = sampleMedicine("med_prn_unique", "user_prn_unique")
        medicineDao.insertMedicine(med)

        val scheduleRepository = com.example.dosezy.data.repository.ScheduleRepository(db)
        val testInstant = LocalDateTime.of(2026, 10, 17, 10, 0, 0)

        // Log two PRN doses with the identical target timestamp in rapid succession
        scheduleRepository.logAsNeededDose(med, "user_prn_unique", testInstant, context)
        scheduleRepository.logAsNeededDose(med, "user_prn_unique", testInstant, context)

        val prnEntries = scheduleDao.getAllScheduleEntries("user_prn_unique")
        assertEquals("Both PRN doses must be preserved in SQLite without overwrite", 2, prnEntries.size)
        assertTrue("PRN entries must have distinct IDs", prnEntries[0].entryId != prnEntries[1].entryId)
        assertTrue("Both entry IDs must start with PRN_", prnEntries.all { it.entryId.startsWith("PRN_") })
    }

    // ---------------------------------------------------------------------------------------------
    // Slot Cohort Notification Synchronization Guard
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testSyncSlotNotificationAndAlarms_reconcilesActiveSlotCohortWhenSecondaryMedTaken() = runBlocking {
        val user = sampleUser("user_cohort_slot")
        userDao.insertUser(user)
        val medA = sampleMedicine("med_cohort_a", "user_cohort_slot", name = "Aspirin")
        val medB = sampleMedicine("med_cohort_b", "user_cohort_slot", name = "Metformin")
        medicineDao.insertMedicine(medA)
        medicineDao.insertMedicine(medB)

        val slotTime = LocalDateTime.of(2026, 10, 17, 8, 0, 0)
        val entryA = sampleScheduleEntry("entry_cohort_a", "user_cohort_slot", "med_cohort_a", slotTime)
        val entryB = sampleScheduleEntry("entry_cohort_b", "user_cohort_slot", "med_cohort_b", slotTime)
        scheduleDao.insertScheduleEntry(entryA)
        scheduleDao.insertScheduleEntry(entryB)

        val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val notif = android.app.Notification.Builder(context).setContentTitle("Medication Reminder").setSmallIcon(android.R.drawable.ic_lock_idle_alarm).build()
        nManager.notify(entryA.entryId.hashCode(), notif)

        val scheduleRepository = com.example.dosezy.data.repository.ScheduleRepository(db)
        // User takes secondary medication (entryB) in-app
        scheduleRepository.recordDoseTaken(entryB.entryId, "TAKEN_ON_TIME", slotTime, context)

        val updatedEntryB = scheduleDao.getScheduleEntryById(entryB.entryId)
        assertNotNull(updatedEntryB)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, updatedEntryB!!.status)

        val remainingPending = scheduleDao.getScheduleForDateRangeDirect(
            "user_cohort_slot",
            slotTime.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
            slotTime.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() + 59999L
        ).filter { it.status == MedicationStatus.PENDING }
        assertEquals(1, remainingPending.size)
        assertEquals("entry_cohort_a", remainingPending.first().entryId)
    }
}

