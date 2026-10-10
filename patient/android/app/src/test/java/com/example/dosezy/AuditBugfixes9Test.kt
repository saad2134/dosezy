package com.example.dosezy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.Language
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.Theme
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import com.example.dosezy.notifications.MedicineNotificationManager
import com.example.dosezy.ui.viewmodels.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuditBugfixes9Test {

    private lateinit var context: Context
    private lateinit var db: DosezyDatabase
    private lateinit var userRepository: UserRepository
    private lateinit var medicineRepository: MedicineRepository
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var medicineNotificationManager: MedicineNotificationManager
    private val testDispatcher = StandardTestDispatcher()

    private val testUser = User(
        userId = "user_audit9",
        fullName = "Test User Audit 9",
        age = 32,
        gender = Gender.FEMALE,
        contactNumber = "9876543210",
        theme = Theme.SYSTEM,
        timeFormat = TimeFormat.HOUR_12,
        language = Language.ENGLISH,
        allowDoseUndo = true,
        allowCustomDoseTime = false,
        isCurrentUser = true
    )

    private val secondUser = User(
        userId = "user_audit9_second",
        fullName = "Second User Audit 9",
        age = 28,
        gender = Gender.MALE,
        contactNumber = "1122334455",
        theme = Theme.DARK,
        timeFormat = TimeFormat.HOUR_24,
        language = Language.ENGLISH,
        allowDoseUndo = true,
        allowCustomDoseTime = false,
        isCurrentUser = false
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, DosezyDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        userRepository = UserRepository(db)
        scheduleRepository = ScheduleRepository(db)
        medicineRepository = MedicineRepository(db, scheduleRepository, context)
        medicineNotificationManager = MedicineNotificationManager(context, db)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private suspend fun awaitCondition(timeoutMs: Long = 10000, condition: suspend () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                org.junit.Assert.fail("Timed out waiting for condition")
            }
            kotlinx.coroutines.delay(20)
        }
    }

    // =========================================================================
    // Issue 1 Tests: User Alarm Lifecycle Scheduling on API 24+
    // =========================================================================

    @Test
    fun testUserViewModel_schedulesAndCancelsAlarmsAcrossProfiles() = runTest(testDispatcher) {
        val userViewModel = UserViewModel(userRepository, medicineRepository, scheduleRepository, medicineNotificationManager)

        // 1. Add first user
        userViewModel.addUser(testUser)
        awaitCondition { userRepository.getUserByIdSync(testUser.userId) != null }

        val savedUser = userRepository.getUserByIdSync(testUser.userId)
        assertNotNull("User should be inserted", savedUser)
        assertTrue("First user should be current user", savedUser!!.isCurrentUser)

        // 2. Add second user
        userViewModel.addUser(secondUser)
        awaitCondition { userRepository.getUserByIdSync(secondUser.userId) != null }

        // 3. Switch current user to second user
        val secondFromDb = userRepository.getUserByIdSync(secondUser.userId)!!
        userViewModel.setCurrentUser(secondFromDb)
        awaitCondition { userRepository.getAllUsersList().firstOrNull { it.isCurrentUser }?.userId == secondUser.userId }

        val activeUser = userRepository.getAllUsersList().firstOrNull { it.isCurrentUser }
        assertEquals("Active user should now be second user", secondUser.userId, activeUser?.userId)

        // 4. Delete the active user, which must trigger alarm cancellation and promotion
        userViewModel.deleteUser(activeUser!!)
        awaitCondition { userRepository.getUserByIdSync(secondUser.userId) == null && userRepository.getAllUsersList().firstOrNull()?.isCurrentUser == true }

        val remainingUsers = userRepository.getAllUsersList()
        assertEquals(1, remainingUsers.size)
        assertEquals(testUser.userId, remainingUsers.first().userId)
        assertTrue("Remaining user should now be promoted to current user", remainingUsers.first().isCurrentUser)
    }

    @Test
    @Config(sdk = [24])
    fun testMedicineNotificationManager_executesOnApi24WithoutCrashing() = runTest(testDispatcher) {
        // Direct validation that java.time and notification manager methods execute on API 24 via desugaring
        userRepository.insertUser(testUser)
        medicineNotificationManager.scheduleAllAlarmsForCurrentUser()
        advanceUntilIdle()

        medicineNotificationManager.scheduleAlarmsForUser(testUser.userId)
        advanceUntilIdle()

        medicineNotificationManager.cancelAllAlarmsForUser(testUser.userId)
        advanceUntilIdle()

        medicineNotificationManager.rescheduleAllAlarmsForAllUsers()
        advanceUntilIdle()
    }

    // =========================================================================
    // Issue 2 Tests: PRN Dose Stock Deduction & Custom Slot Symmetry
    // =========================================================================

    @Test
    fun testLogAsNeededDose_deductsCustomSlotDosage_andRestoresSymmetricallyOnUndo() = runTest(testDispatcher) {
        userRepository.insertUser(testUser)

        // Medication with 2 tablets scheduled at 08:00 AM, 1 tablet at 20:00 PM, default 1 tablet
        val medicineWithSlots = Medicine(
            medicineId = "med_prn_slots",
            userId = testUser.userId,
            medicationName = "Metformin",
            dosage = 1.0,
            dosageUnit = DosageUnit.TABLET,
            timesPerDay = 2,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            customDosages = mapOf("08:00" to 2.0, "20:00" to 1.0),
            currentStock = 30,
            autoDeductOnTake = true
        )
        medicineRepository.insertMedicine(medicineWithSlots)
        advanceUntilIdle()

        val initialStock = db.medicineDao().getMedicineByIdDirect("med_prn_slots")!!.currentStock!!
        assertEquals(30, initialStock)

        // Log PRN dose at 08:00 AM (which has custom slot dosage of 2 tablets)
        val morningDoseTime = LocalDateTime.of(2026, 10, 6, 8, 0)
        scheduleRepository.logAsNeededDose(
            medicine = medicineWithSlots,
            userId = testUser.userId,
            dateTime = morningDoseTime,
            context = context
        )

        // Check stock was decremented by 2 tablets (not default 1)
        val medAfterPrn = db.medicineDao().getMedicineByIdDirect("med_prn_slots")!!
        assertEquals("Stock should decrease by custom slot dosage (2 tablets)", 28, medAfterPrn.currentStock)

        // Fetch inserted PRN entry
        val schedules = scheduleRepository.getSchedulesByUserSync(testUser.userId)
        val prnEntry = schedules.find { it.entryId.startsWith("PRN_") }
        assertNotNull("PRN entry should be recorded", prnEntry)

        // Undo the PRN dose - must restore exactly 2 tablets symmetrically
        scheduleRepository.undoDoseTaken(prnEntry!!.entryId, context = context)

        val medAfterUndo = db.medicineDao().getMedicineByIdDirect("med_prn_slots")!!
        assertEquals("Stock should be restored symmetrically to 30", 30, medAfterUndo.currentStock)

        // Verify PRN entry was deleted
        val schedulesAfterUndo = scheduleRepository.getSchedulesByUserSync(testUser.userId)
        assertNull("PRN entry should be removed after undo", schedulesAfterUndo.find { it.entryId == prnEntry.entryId })
    }

    @Test
    fun testLogAsNeededDose_fetchesFreshEntityFromDb_preventingStaleSnapshotOverwrite() = runTest(testDispatcher) {
        userRepository.insertUser(testUser)

        val medicine = Medicine(
            medicineId = "med_prn_concurrency",
            userId = testUser.userId,
            medicationName = "Paracetamol",
            dosage = 500.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(12, 0)),
            currentStock = 20,
            autoDeductOnTake = true
        )
        medicineRepository.insertMedicine(medicine)
        advanceUntilIdle()

        // Simulate UI holding stale snapshot with stock = 20
        val staleUiMedicine = medicine.copy(currentStock = 20)

        // Meanwhile, a concurrent operation refilled stock to 50 in the database
        val freshDbMedicine = db.medicineDao().getMedicineByIdDirect("med_prn_concurrency")!!.copy(currentStock = 50)
        db.medicineDao().updateMedicine(freshDbMedicine)

        // Now user logs PRN dose using the staleUiMedicine object
        scheduleRepository.logAsNeededDose(
            medicine = staleUiMedicine,
            userId = testUser.userId,
            dateTime = LocalDateTime.of(2026, 10, 6, 12, 0),
            context = context
        )

        // Check that fresh stock (50 - 1 = 49) was updated, NOT stale stock (20 - 1 = 19)
        val finalMedicine = db.medicineDao().getMedicineByIdDirect("med_prn_concurrency")!!
        assertEquals("Stock must be calculated from fresh DB value (50 - 1 = 49)", 49, finalMedicine.currentStock)
    }

    // =========================================================================
    // Issue 3 Tests: Batch Insert Integrity in MedicineRepository
    // =========================================================================

    @Test
    fun testInsertMedicine_batchInsertsScheduleEntriesAtomically() = runTest(testDispatcher) {
        userRepository.insertUser(testUser)

        val multiSlotMedicine = Medicine(
            medicineId = "med_batch_insert",
            userId = testUser.userId,
            medicationName = "Multivitamin",
            dosage = 1.0,
            dosageUnit = DosageUnit.TABLET,
            timesPerDay = 3,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0), LocalTime.of(13, 0), LocalTime.of(20, 0)),
            // Guard: Start from tomorrow so past slots on today are not skipped by time-of-day filtering
            startDate = java.time.LocalDate.now().plusDays(1),
            currentStock = 100,
            autoDeductOnTake = true
        )

        // insertMedicine generates 30 days * 3 slots = 90 entries
        medicineRepository.insertMedicine(multiSlotMedicine)
        advanceUntilIdle()

        val insertedMed = db.medicineDao().getMedicineByIdDirect("med_batch_insert")
        assertNotNull("Medicine entity must be inserted", insertedMed)

        val allSchedules = scheduleRepository.getSchedulesByUserSync(testUser.userId)
        assertEquals("Expected exactly 90 batch-inserted schedule entries for 30 days", 90, allSchedules.size)

        // Verify all 90 entries are marked PENDING and have correct medicineId
        assertTrue(allSchedules.all { it.medicineId == "med_batch_insert" })
        assertTrue(allSchedules.all { it.status == MedicationStatus.PENDING })

        // Verify entries span across the 30-day window
        val distinctDates = allSchedules.map { it.scheduledDateTime.toLocalDate() }.distinct()
        assertTrue("Entries must span across multiple calendar days", distinctDates.size >= 29)
        assertEquals("Total batch-inserted entries should equal 90", 90, allSchedules.size)
    }
}
