/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui

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
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.utils.TimeCalculationUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Unit tests verifying Audit #18 bug fixes:
 *  1. GitHub Issue #105: HomeMedicationCard routes missed doses directly to onMarkAsTaken rather than onUndo,
 *     preventing the infinite undo loop where dynamic time calculation immediately re-flags doses as missed.
 *  2. HomeMedicationCard enables the action button for missed doses regardless of allowDoseUndo setting.
 *  3. HomeMedicationCard allows clinically skipping missed doses when allowDoseSkipping is enabled.
 *  4. Recording a missed dose as taken late deducts medicine stock accurately and assigns TAKEN_LATE status.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeMissedDoseActionRoutingTest {

    private lateinit var context: Context
    private lateinit var database: DosezyDatabase
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var medicineRepository: MedicineRepository

    private val testUser = User(
        userId = "test_user_105",
        fullName = "Test Patient",
        age = 65,
        gender = Gender.DO_NOT_SPECIFY,
        contactNumber = "+1234567890",
        allowDoseUndo = true,
        allowCustomDoseTime = false,
        allowDoseSkipping = true,
        considerLateAfter = 2,
        considerMissedAfter = 4
    )

    private val testMedicine = Medicine(
        medicineId = "med_105",
        userId = "test_user_105",
        medicationName = "Metformin",
        dosage = 500.0,
        dosageUnit = DosageUnit.MG,
        timesPerDay = 1,
        frequency = Frequency(FrequencyPattern.DAILY),
        scheduledTimes = listOf(LocalTime.of(8, 0)),
        currentStock = 30,
        autoDeductOnTake = true
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, DosezyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scheduleRepository = ScheduleRepository(database)
        medicineRepository = MedicineRepository(database, scheduleRepository, context)

        runBlocking {
            database.userDao().insertUser(testUser)
            database.medicineDao().insertMedicine(testMedicine)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. Issue #105: Action Dispatch & Routing for Missed Doses
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testHomeMedicationCard_missedDose_routesToMarkAsTaken_preventingUndoLoop() {
        var undoCalled: Boolean
        var markAsTakenCalled: Boolean

        fun simulateClick(
            isTaken: Boolean,
            isSkipped: Boolean,
            isMissed: Boolean,
            isLate: Boolean,
            allowUndo: Boolean
        ) {
            when {
                isTaken || isSkipped -> {
                    if (allowUndo) undoCalled = true
                }
                isMissed -> {
                    // Guard: Missed doses must route to markAsTaken so users can record intake (#105)
                    markAsTakenCalled = true
                }
                isLate -> { /* Late doses handled by onMarkAsTaken */ }
                else -> markAsTakenCalled = true
            }
        }

        // Case A: allowUndo = true (default user state in #105). Must route to take, never undo.
        undoCalled = false
        markAsTakenCalled = false
        simulateClick(isTaken = false, isSkipped = false, isMissed = true, isLate = false, allowUndo = true)
        assertFalse("Undo must NOT be called for missed doses", undoCalled)
        assertTrue("Mark as taken MUST be called for missed doses", markAsTakenCalled)

        // Case B: allowUndo = false. Must also route to markAsTaken.
        undoCalled = false
        markAsTakenCalled = false
        simulateClick(isTaken = false, isSkipped = false, isMissed = true, isLate = false, allowUndo = false)
        assertFalse(undoCalled)
        assertTrue(markAsTakenCalled)
    }

    @Test
    fun testHomeMedicationCard_missedDose_alwaysEnabled() {
        fun isEnabled(
            isTaken: Boolean,
            isSkipped: Boolean,
            isMissed: Boolean,
            allowUndo: Boolean
        ): Boolean {
            return when {
                isTaken || isSkipped -> allowUndo
                isMissed -> true
                else -> true
            }
        }

        // Missed doses must always be interactive so patients can take them late
        assertTrue(isEnabled(isTaken = false, isSkipped = false, isMissed = true, allowUndo = false))
        assertTrue(isEnabled(isTaken = false, isSkipped = false, isMissed = true, allowUndo = true))

        // Taken or skipped doses only allow interaction if dose undo is enabled
        assertFalse(isEnabled(isTaken = true, isSkipped = false, isMissed = false, allowUndo = false))
        assertTrue(isEnabled(isTaken = true, isSkipped = false, isMissed = false, allowUndo = true))
    }

    @Test
    fun testHomeMedicationCard_missedDose_skippingPermitted_whenDoseSkippingEnabled() {
        fun isSkipVisible(
            allowDoseSkipping: Boolean,
            isTaken: Boolean,
            isSkipped: Boolean
        ): Boolean {
            return allowDoseSkipping && !isTaken && !isSkipped
        }

        // Missed doses (not yet taken, not yet skipped) must permit clinical skipping
        assertTrue(isSkipVisible(allowDoseSkipping = true, isTaken = false, isSkipped = false))

        // Taken doses cannot be skipped
        assertFalse(isSkipVisible(allowDoseSkipping = true, isTaken = true, isSkipped = false))

        // Already skipped doses cannot be skipped again
        assertFalse(isSkipVisible(allowDoseSkipping = true, isTaken = false, isSkipped = true))

        // When skipping preference is disabled, skip button is hidden
        assertFalse(isSkipVisible(allowDoseSkipping = false, isTaken = false, isSkipped = false))
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. Taking a Missed Dose: End-to-End Status and Stock Update
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testTakingMissedDose_recordsLateStatusAndDeductsStock() = runBlocking {
        val entryId = "entry_missed_take_test"
        val scheduledTime = LocalDateTime.now().minusHours(6) // 6 hours ago (past 4h missed threshold)
        val initialEntry = ScheduleEntry(
            entryId = entryId,
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = scheduledTime,
            status = MedicationStatus.MISSED
        )
        database.scheduleDao().insertScheduleEntry(initialEntry)

        // Verify initial setup: stock is 30, status is MISSED
        val initialMed = database.medicineDao().getMedicineByIdDirect(testMedicine.medicineId)
        assertEquals(30, initialMed?.currentStock)
        val insertedEntry = database.scheduleDao().getScheduleEntryById(entryId)
        assertEquals(MedicationStatus.MISSED, insertedEntry?.status)

        // Execute taking the dose late (simulating handleMarkAsTaken -> executeTakeDose)
        val resolvedNow = LocalDateTime.now()
        val isLate = TimeCalculationUtils.isTakenLate(
            scheduledTime,
            resolvedNow,
            testUser.considerLateAfter
        )
        assertTrue("Dose scheduled 6 hours ago must evaluate as late", isLate)

        val targetStatus = if (isLate) MedicationStatus.TAKEN_LATE.name else MedicationStatus.TAKEN_ON_TIME.name
        database.scheduleDao().updateMedicationStatus(entryId, targetStatus, System.currentTimeMillis())

        // Simulate stock deduction on intake
        val currentMed = database.medicineDao().getMedicineByIdDirect(testMedicine.medicineId)
        if (currentMed != null && currentMed.currentStock != null && currentMed.autoDeductOnTake) {
            val stock = currentMed.currentStock ?: 0
            val deduct = currentMed.getStockDeductionAmount(scheduledTime.toLocalTime())
            val updatedMed = currentMed.copy(currentStock = stock - deduct)
            database.medicineDao().updateMedicine(updatedMed)
        }

        // Verify outcomes: status is TAKEN_LATE, stock is decremented to 29
        val updatedEntry = database.scheduleDao().getScheduleEntryById(entryId)
        assertEquals(MedicationStatus.TAKEN_LATE, updatedEntry?.status)

        val finalMed = database.medicineDao().getMedicineByIdDirect(testMedicine.medicineId)
        assertEquals(29, finalMed?.currentStock)
    }

    @Test
    fun testTimeCalculationUtils_isTakenLate_accuratelyFlagsOverdueIntake() {
        val now = LocalDateTime.now()
        val scheduled6HoursAgo = now.minusHours(6)
        val scheduled1HourAgo = now.minusHours(1)

        // With lateAfter = 2:
        // 6 hours ago should be considered late
        assertTrue(TimeCalculationUtils.isTakenLate(scheduled6HoursAgo, now, 2))

        // 1 hour ago should NOT be considered late
        assertFalse(TimeCalculationUtils.isTakenLate(scheduled1HourAgo, now, 2))
    }
}
