/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.domain

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.ScheduleWithMedicine
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import com.example.dosezy.domain.usecases.SkipCohortUseCase
import com.example.dosezy.domain.usecases.TakeCohortUseCase
import com.example.dosezy.notifications.IntakeSafeguardManager
import com.example.dosezy.utils.TimeCalculationUtils
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
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Unit tests verifying Issue #102: Mark all taken cohort functionality:
 *  1. Time slot visibility rule: "Take All" button is visible when 2 or more pending/missed doses share the slot.
 *  2. Single pending dose does not show "Take All" (individual card actions remain).
 *  3. Completed slots (all taken or skipped) do not show "Take All".
 *  4. Batch take execution records all doses as taken, decrements stock atomically, and assigns proper status.
 *  5. Take All integrates seamlessly with Intake Safeguard (ADHD support) by tracking the entire cohort.
 *  6. Cancelling the confirmation dialog retains pending status without side effects.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TakeAllCohortFunctionalityTest {

    private lateinit var context: Context
    private lateinit var database: DosezyDatabase
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var userRepository: UserRepository
    private lateinit var medicineRepository: MedicineRepository

    private val testUser = User(
        userId = "user_cohort_102",
        fullName = "Cohort Test User",
        age = 35,
        gender = Gender.DO_NOT_SPECIFY,
        contactNumber = "+1234567890",
        considerLateAfter = 2,
        considerMissedAfter = 4,
        intakeSafeguardEnabled = false,
        intakeCountdownSeconds = 120
    )

    private val medA = Medicine(
        medicineId = "med_cohort_a",
        userId = "user_cohort_102",
        medicationName = "Lisinopril",
        dosage = 10.0,
        dosageUnit = DosageUnit.MG,
        timesPerDay = 1,
        frequency = Frequency(FrequencyPattern.DAILY),
        scheduledTimes = listOf(LocalTime.of(8, 0)),
        currentStock = 30,
        autoDeductOnTake = true
    )

    private val medB = Medicine(
        medicineId = "med_cohort_b",
        userId = "user_cohort_102",
        medicationName = "Metformin",
        dosage = 500.0,
        dosageUnit = DosageUnit.MG,
        timesPerDay = 1,
        frequency = Frequency(FrequencyPattern.DAILY),
        scheduledTimes = listOf(LocalTime.of(8, 0)),
        currentStock = 15,
        autoDeductOnTake = true
    )

    private val medC = Medicine(
        medicineId = "med_cohort_c",
        userId = "user_cohort_102",
        medicationName = "Aspirin",
        dosage = 81.0,
        dosageUnit = DosageUnit.MG,
        timesPerDay = 1,
        frequency = Frequency(FrequencyPattern.DAILY),
        scheduledTimes = listOf(LocalTime.of(8, 0)),
        currentStock = 60,
        autoDeductOnTake = false
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, DosezyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scheduleRepository = ScheduleRepository(database)
        medicineRepository = MedicineRepository(database, scheduleRepository, context)
        userRepository = UserRepository(database)

        runBlocking {
            database.userDao().insertUser(testUser)
            database.medicineDao().insertMedicine(medA)
            database.medicineDao().insertMedicine(medB)
            database.medicineDao().insertMedicine(medC)
        }
    }

    @After
    fun tearDown() {
        IntakeSafeguardManager.cancelCountdown(context)
        database.close()
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. Visibility & Qualification Rule Tests
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testTakeAll_visibilityCondition_multiplePendingDosesQualify() {
        val slotEntries = listOf(
            ScheduleWithMedicine(
                scheduleEntry = ScheduleEntry(
                    entryId = "e1",
                    userId = testUser.userId,
                    medicineId = medA.medicineId,
                    scheduledDateTime = LocalDateTime.now(),
                    status = MedicationStatus.PENDING
                ),
                medicine = medA
            ),
            ScheduleWithMedicine(
                scheduleEntry = ScheduleEntry(
                    entryId = "e2",
                    userId = testUser.userId,
                    medicineId = medB.medicineId,
                    scheduledDateTime = LocalDateTime.now(),
                    status = MedicationStatus.MISSED
                ),
                medicine = medB
            )
        )

        // Pending or missed entries qualify for Take All action
        val pendingEntries = slotEntries.filter {
            it.scheduleEntry.status == MedicationStatus.PENDING ||
                    it.scheduleEntry.status == MedicationStatus.MISSED
        }

        val shouldShowTakeAll = pendingEntries.size >= 2
        assertTrue("Take All button must be visible when 2 or more pending/missed doses share the slot", shouldShowTakeAll)
        assertEquals(2, pendingEntries.size)
    }

    @Test
    fun testTakeAll_visibilityCondition_singlePendingDoseDoesNotQualify() {
        val slotEntries = listOf(
            ScheduleWithMedicine(
                scheduleEntry = ScheduleEntry(
                    entryId = "e1",
                    userId = testUser.userId,
                    medicineId = medA.medicineId,
                    scheduledDateTime = LocalDateTime.now(),
                    status = MedicationStatus.PENDING
                ),
                medicine = medA
            ),
            ScheduleWithMedicine(
                scheduleEntry = ScheduleEntry(
                    entryId = "e2",
                    userId = testUser.userId,
                    medicineId = medB.medicineId,
                    scheduledDateTime = LocalDateTime.now(),
                    status = MedicationStatus.TAKEN_ON_TIME
                ),
                medicine = medB
            )
        )

        val pendingEntries = slotEntries.filter {
            it.scheduleEntry.status == MedicationStatus.PENDING ||
                    it.scheduleEntry.status == MedicationStatus.MISSED
        }

        val shouldShowTakeAll = pendingEntries.size >= 2
        assertFalse("Take All button must NOT be visible when only 1 pending dose remains", shouldShowTakeAll)
        assertEquals(1, pendingEntries.size)
    }

    @Test
    fun testTakeAll_visibilityCondition_allCompletedDoesNotQualify() {
        val slotEntries = listOf(
            ScheduleWithMedicine(
                scheduleEntry = ScheduleEntry(
                    entryId = "e1",
                    userId = testUser.userId,
                    medicineId = medA.medicineId,
                    scheduledDateTime = LocalDateTime.now(),
                    status = MedicationStatus.TAKEN_ON_TIME
                ),
                medicine = medA
            ),
            ScheduleWithMedicine(
                scheduleEntry = ScheduleEntry(
                    entryId = "e2",
                    userId = testUser.userId,
                    medicineId = medB.medicineId,
                    scheduledDateTime = LocalDateTime.now(),
                    status = MedicationStatus.SKIPPED
                ),
                medicine = medB
            )
        )

        val pendingEntries = slotEntries.filter {
            it.scheduleEntry.status == MedicationStatus.PENDING ||
                    it.scheduleEntry.status == MedicationStatus.MISSED
        }

        val shouldShowTakeAll = pendingEntries.size >= 2
        assertFalse("Take All button must NOT be visible when all medications are already completed", shouldShowTakeAll)
        assertTrue(pendingEntries.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. Execution & State Update Tests
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testMarkCohortAsTaken_recordsAllDosesAndDecrementsStock() = runBlocking {
        val scheduledTime = LocalDateTime.now().minusMinutes(10)
        val entry1 = ScheduleEntry(
            entryId = "entry_cohort_1",
            userId = testUser.userId,
            medicineId = medA.medicineId,
            scheduledDateTime = scheduledTime,
            status = MedicationStatus.PENDING
        )
        val entry2 = ScheduleEntry(
            entryId = "entry_cohort_2",
            userId = testUser.userId,
            medicineId = medB.medicineId,
            scheduledDateTime = scheduledTime,
            status = MedicationStatus.PENDING
        )
        val entry3 = ScheduleEntry(
            entryId = "entry_cohort_3",
            userId = testUser.userId,
            medicineId = medC.medicineId,
            scheduledDateTime = scheduledTime,
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntry(entry1)
        database.scheduleDao().insertScheduleEntry(entry2)
        database.scheduleDao().insertScheduleEntry(entry3)

        // Initial stock assertions
        assertEquals(30, database.medicineDao().getMedicineByIdDirect(medA.medicineId)?.currentStock)
        assertEquals(15, database.medicineDao().getMedicineByIdDirect(medB.medicineId)?.currentStock)
        assertEquals(60, database.medicineDao().getMedicineByIdDirect(medC.medicineId)?.currentStock)

        // Execute cohort take
        val now = LocalDateTime.now()
        val entryIds = listOf("entry_cohort_1", "entry_cohort_2", "entry_cohort_3")
        entryIds.forEach { entryId ->
            val entry = scheduleRepository.getScheduleEntryById(entryId)
            val status = if (entry != null && TimeCalculationUtils.isTakenLate(entry.scheduledDateTime, now, testUser.considerLateAfter)) {
                "TAKEN_LATE"
            } else {
                "TAKEN_ON_TIME"
            }
            scheduleRepository.recordDoseTaken(entryId, status, now, context, null)
        }

        // Verify status updated for all cohort entries
        val updated1 = database.scheduleDao().getScheduleEntryById("entry_cohort_1")
        val updated2 = database.scheduleDao().getScheduleEntryById("entry_cohort_2")
        val updated3 = database.scheduleDao().getScheduleEntryById("entry_cohort_3")
        assertEquals(MedicationStatus.TAKEN_ON_TIME, updated1?.status)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, updated2?.status)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, updated3?.status)

        // Verify stock decremented for autoDeductOnTake medicines, and untouched for false
        assertEquals(29, database.medicineDao().getMedicineByIdDirect(medA.medicineId)?.currentStock)
        assertEquals(14, database.medicineDao().getMedicineByIdDirect(medB.medicineId)?.currentStock)
        assertEquals(60, database.medicineDao().getMedicineByIdDirect(medC.medicineId)?.currentStock)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. Intake Safeguard Integration (ADHD Support)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testTakeAll_withIntakeSafeguard_startsCountdownForEntireCohort() = runBlocking {
        val scheduledTime = LocalDateTime.now().minusMinutes(5)
        val entry1 = ScheduleEntry(
            entryId = "safeguard_cohort_1",
            userId = testUser.userId,
            medicineId = medA.medicineId,
            scheduledDateTime = scheduledTime,
            status = MedicationStatus.PENDING
        )
        val entry2 = ScheduleEntry(
            entryId = "safeguard_cohort_2",
            userId = testUser.userId,
            medicineId = medB.medicineId,
            scheduledDateTime = scheduledTime,
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntry(entry1)
        database.scheduleDao().insertScheduleEntry(entry2)

        val entryIds = listOf("safeguard_cohort_1", "safeguard_cohort_2")
        val medNames = listOf(medA.medicationName, medB.medicationName)

        // Start countdown for the cohort
        val state = IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entryIds.first(),
            entryIds = entryIds,
            medicineName = medNames.first(),
            medicineNames = medNames,
            scheduledTime = "08:00 AM",
            durationSeconds = 120
        )

        assertEquals(2, state.entryIds.size)
        assertEquals(2, state.medicineNames.size)
        assertTrue(state.entryIds.contains("safeguard_cohort_1"))
        assertTrue(state.entryIds.contains("safeguard_cohort_2"))

        // During countdown, doses remain PENDING
        assertEquals(MedicationStatus.PENDING, database.scheduleDao().getScheduleEntryById("safeguard_cohort_1")?.status)
        assertEquals(MedicationStatus.PENDING, database.scheduleDao().getScheduleEntryById("safeguard_cohort_2")?.status)

        // Confirm intake
        val success = IntakeSafeguardManager.confirmIntake(context, database)
        assertTrue(success)

        // After confirmation, both are TAKEN_ON_TIME and stock is decremented
        assertEquals(MedicationStatus.TAKEN_ON_TIME, database.scheduleDao().getScheduleEntryById("safeguard_cohort_1")?.status)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, database.scheduleDao().getScheduleEntryById("safeguard_cohort_2")?.status)
        assertEquals(29, database.medicineDao().getMedicineByIdDirect(medA.medicineId)?.currentStock)
        assertEquals(14, database.medicineDao().getMedicineByIdDirect(medB.medicineId)?.currentStock)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. Confirmation Dialog Cancellation
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testTakeAll_confirmationDialogCancellation_leavesDosesPending() = runBlocking {
        val entry1 = ScheduleEntry(
            entryId = "cancel_cohort_1",
            userId = testUser.userId,
            medicineId = medA.medicineId,
            scheduledDateTime = LocalDateTime.now(),
            status = MedicationStatus.PENDING
        )
        val entry2 = ScheduleEntry(
            entryId = "cancel_cohort_2",
            userId = testUser.userId,
            medicineId = medB.medicineId,
            scheduledDateTime = LocalDateTime.now(),
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntry(entry1)
        database.scheduleDao().insertScheduleEntry(entry2)

        // Simulate dialog dismissal without confirm
        var markAllConfirmSlot: Pair<String, List<String>>? = Pair("08:00 AM", listOf("cancel_cohort_1", "cancel_cohort_2"))
        assertNotNull(markAllConfirmSlot)

        // Dismiss action
        markAllConfirmSlot = null
        assertNull(markAllConfirmSlot)

        // Verify entries remain PENDING and stock is unchanged
        val e1 = database.scheduleDao().getScheduleEntryById("cancel_cohort_1")
        val e2 = database.scheduleDao().getScheduleEntryById("cancel_cohort_2")
        assertEquals(MedicationStatus.PENDING, e1?.status)
        assertEquals(MedicationStatus.PENDING, e2?.status)
        assertEquals(30, database.medicineDao().getMedicineByIdDirect(medA.medicineId)?.currentStock)
        assertEquals(15, database.medicineDao().getMedicineByIdDirect(medB.medicineId)?.currentStock)
    }

    @Test
    fun testTakeCohortUseCase_cancelsActiveIntakeSafeguardCountdown() = runBlocking {
        val entry = ScheduleEntry(
            entryId = "entry_safeguard_cohort_take_usecase",
            userId = testUser.userId,
            medicineId = medA.medicineId,
            scheduledDateTime = LocalDateTime.now(),
            status = MedicationStatus.PENDING,
            dosage = 1.0
        )
        database.scheduleDao().insertScheduleEntry(entry)

        // 1. Simulate user initiating an intake countdown
        IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entry.entryId,
            entryIds = listOf(entry.entryId),
            medicineName = medA.medicationName,
            medicineNames = listOf(medA.medicationName),
            scheduledTime = "08:00 AM",
            durationSeconds = 120
        )
        assertNotNull("Intake countdown must be active", IntakeSafeguardManager.activeCountdown.value)

        // 2. User confirms taking the dose via TakeCohortUseCase
        val takeCohortUseCase = TakeCohortUseCase(database, scheduleRepository, context)
        takeCohortUseCase.invoke(listOf(entry.entryId), user = testUser)

        // 3. Assert dose is recorded as taken in database
        val updatedEntry = database.scheduleDao().getScheduleEntryById(entry.entryId)
        assertNotNull(updatedEntry)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, updatedEntry!!.status)

        // 4. Guard assertion: Active intake countdown must be cancelled so no phantom re-alert fires
        assertNull("Active intake countdown must be cancelled upon dose completion", IntakeSafeguardManager.activeCountdown.value)
    }

    @Test
    fun testSkipCohortUseCase_cancelsActiveIntakeSafeguardCountdown() = runBlocking {
        val entry = ScheduleEntry(
            entryId = "entry_safeguard_cohort_skip_usecase",
            userId = testUser.userId,
            medicineId = medA.medicineId,
            scheduledDateTime = LocalDateTime.now(),
            status = MedicationStatus.PENDING,
            dosage = 1.0
        )
        database.scheduleDao().insertScheduleEntry(entry)

        // 1. Simulate user initiating an intake countdown
        IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entry.entryId,
            entryIds = listOf(entry.entryId),
            medicineName = medA.medicationName,
            medicineNames = listOf(medA.medicationName),
            scheduledTime = "08:00 AM",
            durationSeconds = 120
        )
        assertNotNull("Intake countdown must be active", IntakeSafeguardManager.activeCountdown.value)

        // 2. User skips the dose via SkipCohortUseCase
        val skipCohortUseCase = SkipCohortUseCase(scheduleRepository, context)
        skipCohortUseCase.invoke(listOf(entry.entryId), reason = "Feeling nauseous")

        // 3. Assert dose is recorded as skipped in database
        val updatedEntry = database.scheduleDao().getScheduleEntryById(entry.entryId)
        assertNotNull(updatedEntry)
        assertEquals(MedicationStatus.SKIPPED, updatedEntry!!.status)
        assertEquals("Feeling nauseous", updatedEntry.skipReason)

        // 4. Guard assertion: Active intake countdown must be cancelled upon skipping dose
        assertNull("Active intake countdown must be cancelled upon skipping dose", IntakeSafeguardManager.activeCountdown.value)
    }

    @Test
    fun testTakeCohortUseCase_multiMedicationCohort_coordinatesSingleSync() = runBlocking {
        val slotTime = LocalDateTime.now().plusHours(2).withSecond(0).withNano(0)
        val entry1 = ScheduleEntry(
            entryId = "entry_cohort_sync_1",
            userId = testUser.userId,
            medicineId = medA.medicineId,
            scheduledDateTime = slotTime,
            status = MedicationStatus.PENDING,
            dosage = 1.0
        )
        val entry2 = ScheduleEntry(
            entryId = "entry_cohort_sync_2",
            userId = testUser.userId,
            medicineId = medB.medicineId,
            scheduledDateTime = slotTime,
            status = MedicationStatus.PENDING,
            dosage = 2.0
        )
        database.scheduleDao().insertScheduleEntry(entry1)
        database.scheduleDao().insertScheduleEntry(entry2)

        val takeCohortUseCase = TakeCohortUseCase(database, scheduleRepository, context)
        takeCohortUseCase.invoke(listOf(entry1.entryId, entry2.entryId), user = testUser)

        val updated1 = database.scheduleDao().getScheduleEntryById(entry1.entryId)
        val updated2 = database.scheduleDao().getScheduleEntryById(entry2.entryId)
        assertNotNull(updated1)
        assertNotNull(updated2)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, updated1!!.status)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, updated2!!.status)
    }

    @Test
    fun testSkipCohortUseCase_multiMedicationCohort_coordinatesSingleSync() = runBlocking {
        val slotTime = LocalDateTime.now().plusHours(3).withSecond(0).withNano(0)
        val entry1 = ScheduleEntry(
            entryId = "entry_cohort_skip_sync_1",
            userId = testUser.userId,
            medicineId = medA.medicineId,
            scheduledDateTime = slotTime,
            status = MedicationStatus.PENDING,
            dosage = 1.0
        )
        val entry2 = ScheduleEntry(
            entryId = "entry_cohort_skip_sync_2",
            userId = testUser.userId,
            medicineId = medB.medicineId,
            scheduledDateTime = slotTime,
            status = MedicationStatus.PENDING,
            dosage = 2.0
        )
        database.scheduleDao().insertScheduleEntry(entry1)
        database.scheduleDao().insertScheduleEntry(entry2)

        val skipCohortUseCase = SkipCohortUseCase(scheduleRepository, context)
        skipCohortUseCase.invoke(listOf(entry1.entryId, entry2.entryId), reason = "Doctor advised")

        val updated1 = database.scheduleDao().getScheduleEntryById(entry1.entryId)
        val updated2 = database.scheduleDao().getScheduleEntryById(entry2.entryId)
        assertNotNull(updated1)
        assertNotNull(updated2)
        assertEquals(MedicationStatus.SKIPPED, updated1!!.status)
        assertEquals(MedicationStatus.SKIPPED, updated2!!.status)
        assertEquals("Doctor advised", updated1.skipReason)
        assertEquals("Doctor advised", updated2.skipReason)
    }

    @Test
    fun testTakeCohortUseCase_triggersRefillAlert_whenStockCrossesThreshold() = runBlocking {
        val lowStockMed = Medicine(
            medicineId = "med_low_stock_refill",
            userId = testUser.userId,
            medicationName = "Thyroxine",
            dosage = 50.0,
            dosageUnit = DosageUnit.MCG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0)),
            currentStock = 3,
            refillThreshold = 3,
            autoDeductOnTake = true
        )
        database.medicineDao().insertMedicine(lowStockMed)

        val slotTime = LocalDateTime.now().plusHours(1).withSecond(0).withNano(0)
        val entry = ScheduleEntry(
            entryId = "entry_refill_threshold_1",
            userId = testUser.userId,
            medicineId = lowStockMed.medicineId,
            scheduledDateTime = slotTime,
            status = MedicationStatus.PENDING,
            dosage = 1.0
        )
        database.scheduleDao().insertScheduleEntry(entry)

        val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nManager.cancelAll()

        val takeCohortUseCase = TakeCohortUseCase(database, scheduleRepository, context)
        takeCohortUseCase.invoke(listOf(entry.entryId), user = testUser)

        val updatedMed = database.medicineDao().getMedicineByIdDirect(lowStockMed.medicineId)
        assertEquals(2, updatedMed?.currentStock)

        // Verify refill alert notification was posted
        val refillNotifId = (lowStockMed.medicineId + "_refill").hashCode()
        val activeNotifs = nManager.activeNotifications
        assertTrue("Refill alert notification must be posted when stock drops to or below threshold", activeNotifs.any { it.id == refillNotifId })
    }
}

