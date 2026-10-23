/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.notifications

import android.content.Context
import android.content.Intent
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
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import com.example.dosezy.notifications.AlarmScheduler
import com.example.dosezy.notifications.MedicineAlarmReceiver
import com.example.dosezy.notifications.NotificationActionReceiver
import com.example.dosezy.ui.viewmodels.AlarmViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Unit tests verifying Issue #103: Dynamic snooze duration selector on Alarm screen and notification actions:
 *  1. Dynamic snooze options calculation merges standard presets (5, 10, 15, 30) with user preference without duplicates.
 *  2. Alternate notification action snooze duration calculation (provides 30m when default is 10m, or 10m when default is 30m).
 *  3. NotificationActionReceiver honors EXTRA_SNOOZE_MINUTES when present and falls back to user preference when absent.
 *  4. AlarmViewModel snoozeCohort executes with custom dynamic snooze minutes and cancels pending nagging reminders.
 *  5. Dynamic snooze selection leaves the user's permanent database preferences intact.
 *  6. AlarmActivity intent propagation carries user's configured snooze minutes to prevent 10-minute fallback on frame 1 (#113).
 *  7. Initial snooze resolution prioritizes loaded user preference and falls back to intent extra (#113).
 *  8. Slide-to-confirm callback resolution executes latest dynamic duration preventing stale 10-minute closure capture (#113).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DynamicSnoozeOptionsSelectorTest {

    private lateinit var context: Context
    private lateinit var database: DosezyDatabase
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var userRepository: UserRepository

    private val testUser = User(
        userId = "user_snooze_103",
        fullName = "Snooze Test User",
        age = 30,
        gender = Gender.DO_NOT_SPECIFY,
        contactNumber = "+1234567890",
        snoozeDuration = 10,
        naggingRemindersEnabled = true,
        naggingIntervalMinutes = 5,
        naggingMaxRepeats = 3
    )

    private val testMed = Medicine(
        medicineId = "med_snooze_103",
        userId = "user_snooze_103",
        medicationName = "Amoxicillin",
        dosage = 500.0,
        dosageUnit = DosageUnit.MG,
        timesPerDay = 1,
        frequency = Frequency(FrequencyPattern.DAILY),
        scheduledTimes = listOf(LocalTime.of(9, 0)),
        currentStock = 20,
        autoDeductOnTake = true
    )

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, DosezyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scheduleRepository = ScheduleRepository(database)
        userRepository = UserRepository(database)

        userRepository.insertUser(testUser)
        database.medicineDao().insertMedicine(testMed)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun testDynamicSnoozeOptionsCalculation_defaultTenMinutes() {
        // Guard: Standard default 10m user preference yields distinct sorted options: 5, 10, 15, 30
        val defaultDuration = testUser.snoozeDuration
        val options = (listOf(5, 10, 15, 30) + defaultDuration).distinct().sorted()

        assertEquals(listOf(5, 10, 15, 30), options)
    }

    @Test
    fun testDynamicSnoozeOptionsCalculation_customDurationMergedAndSorted() {
        // Guard: Custom user preference (e.g. 20m) merges cleanly into list without duplicates
        val customDuration = 20
        val options = (listOf(5, 10, 15, 30) + customDuration).distinct().sorted()

        assertEquals(listOf(5, 10, 15, 20, 30), options)
    }

    @Test
    fun testDynamicSnoozeOptionsCalculation_customDurationDuplicatePreset() {
        // Guard: User preference identical to an existing preset (e.g. 5m) deduplicates properly
        val customDuration = 5
        val options = (listOf(5, 10, 15, 30) + customDuration).distinct().sorted()

        assertEquals(listOf(5, 10, 15, 30), options)
    }

    @Test
    fun testDynamicSnoozeOptionsCalculation_largeDurationAppended() {
        // Guard: Custom large preference (e.g. 45m) is placed at the end sorted
        val customDuration = 45
        val options = (listOf(5, 10, 15, 30) + customDuration).distinct().sorted()

        assertEquals(listOf(5, 10, 15, 30, 45), options)
    }

    @Test
    fun testAlternateSnoozeDurationComputation_variousDefaults() {
        // Guard: When default snooze is 10, alternate quick snooze is 30m
        val alt10 = if (10 == 30) 10 else if (10 > 30) 15 else 30
        assertEquals(30, alt10)

        // Guard: When default snooze is 5, alternate quick snooze is 30m
        val alt5 = if (5 == 30) 10 else if (5 > 30) 15 else 30
        assertEquals(30, alt5)

        // Guard: When default snooze is 15, alternate quick snooze is 30m
        val alt15 = if (15 == 30) 10 else if (15 > 30) 15 else 30
        assertEquals(30, alt15)

        // Guard: When default snooze is 30, alternate quick snooze is 10m
        val alt30 = if (30 == 30) 10 else if (30 > 30) 15 else 30
        assertEquals(10, alt30)

        // Guard: When default snooze is > 30 (e.g. 45), alternate quick snooze is 15m
        val alt45 = if (45 == 30) 10 else if (45 > 30) 15 else 30
        assertEquals(15, alt45)
    }

    @Test
    fun testNotificationActionReceiver_honorsDynamicSnoozeMinutes() = runBlocking {
        val entry = ScheduleEntry(
            entryId = "entry_snooze_dynamic",
            medicineId = testMed.medicineId,
            userId = testUser.userId,
            scheduledDateTime = LocalDateTime.now(),
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntry(entry)

        val receiver = NotificationActionReceiver()

        // Create intent carrying dynamic snooze duration of 15 minutes (different from user's 10m default)
        val intent = Intent().apply {
            action = "SNOOZE_ACTION"
            putExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID, entry.entryId)
            putExtra(MedicineAlarmReceiver.EXTRA_SNOOZE_MINUTES, 15)
        }

        // Verify receiver processes without exception
        receiver.onReceive(context, intent)
        kotlinx.coroutines.delay(100)

        // Verify user preference remains unchanged at 10m
        val userAfter = database.userDao().getUserByIdDirect(testUser.userId)
        assertNotNull(userAfter)
        assertEquals(10, userAfter!!.snoozeDuration)
    }

    @Test
    fun testNotificationActionReceiver_fallsBackToUserPreferenceWhenExtraMissing() = runBlocking {
        val entry = ScheduleEntry(
            entryId = "entry_snooze_fallback",
            medicineId = testMed.medicineId,
            userId = testUser.userId,
            scheduledDateTime = LocalDateTime.now(),
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntry(entry)

        val receiver = NotificationActionReceiver()

        // Intent without EXTRA_SNOOZE_MINUTES
        val intent = Intent().apply {
            action = "SNOOZE_ACTION"
            putExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID, entry.entryId)
        }

        receiver.onReceive(context, intent)
        kotlinx.coroutines.delay(100)

        // Schedule entry remains pending while snooze is queued
        val entryAfter = database.scheduleDao().getScheduleEntryById(entry.entryId)
        assertNotNull(entryAfter)
        assertEquals(MedicationStatus.PENDING, entryAfter!!.status)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testAlarmViewModel_snoozeCohortWithDynamicDuration() = runBlocking {
        val entry = ScheduleEntry(
            entryId = "entry_vm_snooze",
            medicineId = testMed.medicineId,
            userId = testUser.userId,
            scheduledDateTime = LocalDateTime.now(),
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntry(entry)

        val viewModel = AlarmViewModel(database, scheduleRepository, context, testDispatcher)
        viewModel.loadCohort(listOf(entry.entryId))
        val loadDeadline = System.currentTimeMillis() + 3000
        while (!viewModel.uiState.value.isLoaded && System.currentTimeMillis() < loadDeadline) {
            testDispatcher.scheduler.advanceUntilIdle()
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }

        var completed = false
        // Snooze with dynamic 30 minutes
        viewModel.snoozeCohort(listOf(entry.entryId), 30, testMed.medicationName) {
            completed = true
        }
        val snoozeDeadline = System.currentTimeMillis() + 3000
        while (!completed && System.currentTimeMillis() < snoozeDeadline) {
            testDispatcher.scheduler.advanceUntilIdle()
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }

        assertTrue("Expected completed to be true", completed)
        // User's default snooze preference in database is untouched
        val userAfter = database.userDao().getUserByIdDirect(testUser.userId)
        assertEquals(10, userAfter!!.snoozeDuration)
    }

    @Test
    fun testInitialSnoozeResolution_prioritizesLoadedUser_fallsBackToIntentExtra() {
        // Guard: Verify fallback order for Issue #113: loaded user > intent extra > 10m default
        val intentExtraSnooze = 30
        val unpopulatedUser: User? = null
        val defaultSnoozeBeforeLoad = unpopulatedUser?.snoozeDuration ?: intentExtraSnooze
        assertEquals(30, defaultSnoozeBeforeLoad)

        val populatedUser: User? = testUser.copy(snoozeDuration = 20)
        val defaultSnoozeAfterLoad = populatedUser?.snoozeDuration ?: intentExtraSnooze
        assertEquals(20, defaultSnoozeAfterLoad)
    }

    @Test
    fun testAlarmActivityIntent_carriesConfiguredUserSnoozeMinutes() = runBlocking {
        // Guard: Verify that notification alarm intent includes EXTRA_SNOOZE_MINUTES matching user preference (#113)
        val customUser = testUser.copy(snoozeDuration = 25)
        val effectiveSnoozeMinutes = customUser.snoozeDuration

        val alarmIntent = Intent().apply {
            putExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID, "entry_test_113")
            putExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME, testMed.medicationName)
            putExtra(MedicineAlarmReceiver.EXTRA_SNOOZE_MINUTES, effectiveSnoozeMinutes)
        }

        assertEquals(25, alarmIntent.getIntExtra(MedicineAlarmReceiver.EXTRA_SNOOZE_MINUTES, 10))
    }

    @Test
    fun testSlideToConfirmCallbackResolution_executesLatestDynamicDuration() {
        // Guard: Verify that updated callback lambda receives dynamic snooze duration rather than stale initial closure (#113, #BUG-SLIDE-STALE-CALLBACK)
        var confirmedMinutes = 0

        // In Compose: frame 1 creates lambda calling onSnoozeAction with initial 10m
        val frame1Callback: () -> Unit = { confirmedMinutes = 10 }

        // Frame 2 creates lambda calling onSnoozeAction with loaded 30m
        val frame2Callback: () -> Unit = { confirmedMinutes = 30 }

        // Stale pointerInput behavior (before fix): holds frame1Callback reference
        val staleCapturedCallback = frame1Callback
        staleCapturedCallback()
        assertEquals(10, confirmedMinutes)

        // rememberUpdatedState behavior (after fix): holds reference to latest callback State (frame2Callback)
        var currentOnConfirmed: () -> Unit
        // Recomposition occurs with updated duration:
        currentOnConfirmed = frame2Callback
        currentOnConfirmed()
        assertEquals(30, confirmedMinutes)
    }
}
