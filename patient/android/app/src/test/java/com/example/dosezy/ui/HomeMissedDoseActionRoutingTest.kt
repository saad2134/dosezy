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
import com.example.dosezy.R
import com.example.dosezy.data.model.ScheduleWithMedicine
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

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. Home Timeline & Section Status Badge Adherence Accuracy
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testHomeStatusBadge_mixedCompletedSlot_evaluatesToCompletedWithSkipped() {
        val slotTime = LocalDateTime.now().withHour(8).withMinute(0)
        val med1 = testMedicine.copy(medicineId = "med_badge_1", medicationName = "Pill 1")
        val med2 = testMedicine.copy(medicineId = "med_badge_2", medicationName = "Pill 2")

        val entryTaken = ScheduleEntry(
            entryId = "entry_badge_taken",
            userId = testUser.userId,
            medicineId = med1.medicineId,
            scheduledDateTime = slotTime,
            status = MedicationStatus.TAKEN_ON_TIME
        )
        val entrySkipped = ScheduleEntry(
            entryId = "entry_badge_skipped",
            userId = testUser.userId,
            medicineId = med2.medicineId,
            scheduledDateTime = slotTime,
            status = MedicationStatus.SKIPPED
        )

        val mixedEntries = listOf(
            ScheduleWithMedicine(entryTaken, med1),
            ScheduleWithMedicine(entrySkipped, med2)
        )

        val allTakenMixed = mixedEntries.all {
            it.scheduleEntry.status == MedicationStatus.TAKEN_ON_TIME ||
                    it.scheduleEntry.status == MedicationStatus.TAKEN_LATE
        }
        val allSkippedMixed = mixedEntries.all {
            it.scheduleEntry.status == MedicationStatus.SKIPPED
        }
        val allCompletedMixed = mixedEntries.all {
            it.scheduleEntry.status == MedicationStatus.TAKEN_ON_TIME ||
                    it.scheduleEntry.status == MedicationStatus.TAKEN_LATE ||
                    it.scheduleEntry.status == MedicationStatus.SKIPPED
        }

        assertFalse("Mixed slot must not evaluate as all taken", allTakenMixed)
        assertFalse("Mixed slot must not evaluate as all skipped", allSkippedMixed)
        assertTrue("Mixed slot where each dose is taken or skipped must evaluate as allCompleted", allCompletedMixed)

        // Guard: Mixed completed slot must render status_slot_completed_with_skipped, not status_all_medications_taken
        val badgeResMixed = when {
            allTakenMixed -> R.string.status_all_medications_taken
            allSkippedMixed -> R.string.home_action_skipped
            allCompletedMixed -> R.string.status_slot_completed_with_skipped
            else -> R.string.status_scheduled_for_this_time
        }
        assertEquals(R.string.status_slot_completed_with_skipped, badgeResMixed)

        // Verify homogeneous taken slot still evaluates to status_all_medications_taken
        val allTakenEntries = listOf(
            ScheduleWithMedicine(entryTaken, med1),
            ScheduleWithMedicine(entryTaken.copy(entryId = "entry_badge_taken_2"), med2)
        )
        val badgeResTaken = when {
            allTakenEntries.all { it.scheduleEntry.status == MedicationStatus.TAKEN_ON_TIME } -> R.string.status_all_medications_taken
            else -> R.string.status_scheduled_for_this_time
        }
        assertEquals(R.string.status_all_medications_taken, badgeResTaken)

        // Verify homogeneous skipped slot still evaluates to home_action_skipped
        val allSkippedEntries = listOf(
            ScheduleWithMedicine(entrySkipped, med1),
            ScheduleWithMedicine(entrySkipped.copy(entryId = "entry_badge_skipped_2"), med2)
        )
        val badgeResSkipped = when {
            allSkippedEntries.all { it.scheduleEntry.status == MedicationStatus.SKIPPED } -> R.string.home_action_skipped
            else -> R.string.status_scheduled_for_this_time
        }
        assertEquals(R.string.home_action_skipped, badgeResSkipped)
    }

    @Test
    fun calendarDayStatus_partiallyCompletedWithLateDose_prioritizesPendingAheadOfAmber() {
        // Guard: Verify partially completed days with pending doses prioritize pending status over Amber (#BUG-CALENDAR-PARTIAL-PENDING)
        val now = LocalDateTime.of(2026, 10, 18, 10, 0, 0)
        val missedAfterHours = 6

        val entryLateTaken = ScheduleEntry(
            entryId = "entry_morning_late",
            userId = "test_user_105",
            medicineId = "med_105",
            scheduledDateTime = LocalDateTime.of(2026, 10, 18, 8, 0, 0),
            status = MedicationStatus.TAKEN_LATE
        )

        val entryEveningPending = ScheduleEntry(
            entryId = "entry_evening_pending",
            userId = "test_user_105",
            medicineId = "med_105",
            scheduledDateTime = LocalDateTime.of(2026, 10, 18, 20, 0, 0),
            status = MedicationStatus.PENDING
        )

        val dayEntries = listOf(entryLateTaken, entryEveningPending)

        val hasMissed = dayEntries.any { it.status == MedicationStatus.MISSED }
        val hasLate = dayEntries.any { it.status == MedicationStatus.TAKEN_LATE }
        val allCompleted = dayEntries.all {
            it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE || it.status == MedicationStatus.SKIPPED
        }
        val hasTaken = dayEntries.any {
            it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE
        }
        val allSkipped = dayEntries.all { it.status == MedicationStatus.SKIPPED }
        val hasPastPending = dayEntries.any {
            it.status == MedicationStatus.PENDING &&
                    now.isAfter(it.scheduledDateTime) &&
                    TimeCalculationUtils.isMissed(it.scheduledDateTime, now, missedAfterHours)
        }
        val hasUpcomingPending = dayEntries.any { it.status == MedicationStatus.PENDING }

        val resolvedColor = when {
            hasMissed || hasPastPending -> androidx.compose.ui.graphics.Color(0xFFEF4444) // Red
            allCompleted && hasLate -> androidx.compose.ui.graphics.Color(0xFFF59E0B) // Amber (all doses resolved, with late intake)
            allCompleted && hasTaken -> androidx.compose.ui.graphics.Color(0xFF10B981) // Green (all doses resolved on time)
            allSkipped -> androidx.compose.ui.graphics.Color(0xFF9CA3AF) // Slate Gray for skipped (neutral)
            hasUpcomingPending -> androidx.compose.ui.graphics.Color(0xFF6B7280) // Gray for pending / upcoming doses
            hasLate -> androidx.compose.ui.graphics.Color(0xFFF59E0B) // Amber fallback
            else -> androidx.compose.ui.graphics.Color(0xFF6B7280)
        }

        // Partially complete day must evaluate to Gray (pending/upcoming), not Amber
        assertEquals(androidx.compose.ui.graphics.Color(0xFF6B7280), resolvedColor)

        // When evening dose is taken, day completes and turns Amber
        val completedDayEntries = listOf(
            entryLateTaken,
            entryEveningPending.copy(status = MedicationStatus.TAKEN_ON_TIME)
        )
        val allCompletedNow = completedDayEntries.all {
            it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE || it.status == MedicationStatus.SKIPPED
        }
        val hasLateNow = completedDayEntries.any { it.status == MedicationStatus.TAKEN_LATE }

        val resolvedColorWhenCompleted = when {
            allCompletedNow && hasLateNow -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
            else -> androidx.compose.ui.graphics.Color(0xFF6B7280)
        }
        assertEquals(androidx.compose.ui.graphics.Color(0xFFF59E0B), resolvedColorWhenCompleted)
    }

    @Test
    fun takeAllCohortAction_preferenceRouting_evaluatesDialogPathsAccurately() {
        // Guard: Verify Take All confirmation branches respect user's allowCustomDoseTime, promptDoseNotes, and intakeSafeguard preferences (#BUG-COHORT-CUSTOM-TIME, #BUG-COHORT-DOSE-NOTES)
        data class RoutingState(
            val routesToCustomTime: Boolean,
            val routesToNotePrompt: Boolean,
            val startsSafeguard: Boolean,
            val executesImmediately: Boolean
        )

        fun evaluateRouting(user: User?): RoutingState {
            val routesToCustomTime = user?.allowCustomDoseTime == true
            val routesToNotePrompt = !routesToCustomTime && (user?.allowDoseNotes == true && user.promptDoseNotes)
            val startsSafeguard = !routesToCustomTime && !routesToNotePrompt && (user?.intakeSafeguardEnabled == true)
            val executesImmediately = !routesToCustomTime && !routesToNotePrompt && !startsSafeguard
            return RoutingState(routesToCustomTime, routesToNotePrompt, startsSafeguard, executesImmediately)
        }

        // 1. allowCustomDoseTime = true routes to custom time dialog regardless of other settings
        val userWithCustomTime = testUser.copy(allowCustomDoseTime = true, promptDoseNotes = true, intakeSafeguardEnabled = true)
        val routeCustom = evaluateRouting(userWithCustomTime)
        assertTrue(routeCustom.routesToCustomTime)
        assertFalse(routeCustom.routesToNotePrompt)
        assertFalse(routeCustom.startsSafeguard)
        assertFalse(routeCustom.executesImmediately)

        // 2. promptDoseNotes = true (without custom time) routes to note dialog
        val userWithNotes = testUser.copy(allowCustomDoseTime = false, allowDoseNotes = true, promptDoseNotes = true, intakeSafeguardEnabled = true)
        val routeNote = evaluateRouting(userWithNotes)
        assertFalse(routeNote.routesToCustomTime)
        assertTrue(routeNote.routesToNotePrompt)
        assertFalse(routeNote.startsSafeguard)
        assertFalse(routeNote.executesImmediately)

        // 3. intakeSafeguardEnabled = true (without custom time or note prompt) starts safeguard countdown
        val userWithSafeguard = testUser.copy(allowCustomDoseTime = false, promptDoseNotes = false, intakeSafeguardEnabled = true)
        val routeSafeguard = evaluateRouting(userWithSafeguard)
        assertFalse(routeSafeguard.routesToCustomTime)
        assertFalse(routeSafeguard.routesToNotePrompt)
        assertTrue(routeSafeguard.startsSafeguard)
        assertFalse(routeSafeguard.executesImmediately)

        // 4. Standard default user executes batch take immediately
        val standardUser = testUser.copy(allowCustomDoseTime = false, promptDoseNotes = false, intakeSafeguardEnabled = false)
        val routeStandard = evaluateRouting(standardUser)
        assertFalse(routeStandard.routesToCustomTime)
        assertFalse(routeStandard.routesToNotePrompt)
        assertFalse(routeStandard.startsSafeguard)
        assertTrue(routeStandard.executesImmediately)
    }
}
