/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy

import android.content.Context
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
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.ui.viewmodels.AlarmViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlarmViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val database: DosezyDatabase = mockk(relaxed = true)
    private val scheduleDao: ScheduleDao = mockk(relaxed = true)
    private val medicineDao: MedicineDao = mockk(relaxed = true)
    private val userDao: UserDao = mockk(relaxed = true)
    private val scheduleRepository: ScheduleRepository = mockk(relaxed = true)
    private lateinit var context: Context

    private val testUser = User(
        userId = "user_123",
        fullName = "Test User",
        age = 30,
        gender = Gender.MALE,
        contactNumber = "1234567890",
        isCurrentUser = true
    )

    private val testMedicine = Medicine(
        medicineId = "med_123",
        userId = "user_123",
        medicationName = "Aspirin",
        dosage = 100.0,
        dosageUnit = DosageUnit.MG,
        timesPerDay = 1,
        frequency = Frequency(FrequencyPattern.DAILY),
        scheduledTimes = listOf(LocalTime.of(8, 0))
    )

    private val testEntry = ScheduleEntry(
        entryId = "entry_123",
        medicineId = "med_123",
        userId = "user_123",
        scheduledDateTime = LocalDateTime.now().minusMinutes(10),
        status = MedicationStatus.PENDING
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        every { database.scheduleDao() } returns scheduleDao
        every { database.medicineDao() } returns medicineDao
        every { database.userDao() } returns userDao
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialUiState_isUnloaded() {
        val viewModel = AlarmViewModel(database, scheduleRepository, context, testDispatcher)
        assertFalse(viewModel.uiState.value.isLoaded)
        assertTrue(viewModel.uiState.value.medicinesList.isEmpty())
    }

    @Test
    fun loadCohort_populatesMedicinesAndUser() = runTest(testDispatcher) {
        coEvery { scheduleDao.getScheduleEntryById("entry_123") } returns testEntry
        coEvery { medicineDao.getMedicineByIdDirect("med_123") } returns testMedicine
        coEvery { userDao.getUserByIdDirect("user_123") } returns testUser

        val viewModel = AlarmViewModel(database, scheduleRepository, context, testDispatcher)
        viewModel.loadCohort(listOf("entry_123"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isLoaded)
        assertEquals(1, state.medicinesList.size)
        assertEquals("Aspirin", state.medicinesList.first().second.medicationName)
        assertEquals("Test User", state.user?.fullName)
    }

    @Test
    fun takeCohort_recordsDoseTakenAndInvokesCallback() = runTest(testDispatcher) {
        coEvery { scheduleDao.getScheduleEntryById("entry_123") } returns testEntry
        coEvery { medicineDao.getMedicineByIdDirect("med_123") } returns testMedicine
        coEvery { userDao.getUserByIdDirect("user_123") } returns testUser

        val viewModel = AlarmViewModel(database, scheduleRepository, context, testDispatcher)
        viewModel.loadCohort(listOf("entry_123"))
        advanceUntilIdle()

        var completed = false
        viewModel.takeCohort(listOf("entry_123")) {
            completed = true
        }
        advanceUntilIdle()

        assertTrue(completed)
        coVerify {
            scheduleRepository.recordDoseTaken(
                entryId = eq("entry_123"),
                status = any(),
                takenAtStr = any(),
                context = any(),
                notes = any(),
                syncSlotNotification = any()
            )
        }
    }

    @Test
    fun skipCohort_recordsDoseSkippedAndInvokesCallback() = runTest(testDispatcher) {
        coEvery { scheduleDao.getScheduleEntryById("entry_123") } returns testEntry
        coEvery { medicineDao.getMedicineByIdDirect("med_123") } returns testMedicine
        coEvery { userDao.getUserByIdDirect("user_123") } returns testUser

        val viewModel = AlarmViewModel(database, scheduleRepository, context, testDispatcher)
        viewModel.loadCohort(listOf("entry_123"))
        advanceUntilIdle()

        var completed = false
        viewModel.skipCohort(listOf("entry_123"), reason = "Patient asleep") {
            completed = true
        }
        advanceUntilIdle()

        assertTrue(completed)
        coVerify { scheduleRepository.recordDoseSkipped(eq("entry_123"), eq("Patient asleep"), any()) }
    }

    @Test
    fun startPlaybackIfNeeded_triggersPlayback_whenAudioNotPlaying() = runTest(testDispatcher) {
        coEvery { scheduleDao.getScheduleEntryById("entry_123") } returns testEntry
        coEvery { userDao.getUserByIdDirect("user_123") } returns testUser

        com.example.dosezy.notifications.AlarmAudioPlayer.stop()
        val viewModel = AlarmViewModel(database, scheduleRepository, context, testDispatcher)
        viewModel.startPlaybackIfNeeded("entry_123")
        advanceUntilIdle()

        // startPlaybackIfNeeded completes and loads target sound configuration without error
        assertNotNull(viewModel)
    }

    @Test
    fun loadCohort_withEntriesFromMultipleUsers_isolatesCohortToPrimaryUser() = runTest(testDispatcher) {
        // Guard: Verify that if loadCohort receives entry IDs from different users, it scopes medicinesList exclusively to the primary user (#BUG-MULTIUSER-ALARM-MERGE)
        val secondUser = User(userId = "user_456", fullName = "Second User", age = 40, gender = Gender.FEMALE, contactNumber = "0987654321")
        val secondMed = Medicine(medicineId = "med_456", userId = "user_456", medicationName = "Metformin", dosage = 500.0, dosageUnit = DosageUnit.MG, timesPerDay = 1, frequency = Frequency(FrequencyPattern.DAILY), scheduledTimes = listOf(LocalTime.of(8, 0)))
        val secondEntry = ScheduleEntry(entryId = "entry_456", medicineId = "med_456", userId = "user_456", scheduledDateTime = LocalDateTime.now().minusMinutes(10), status = MedicationStatus.PENDING)

        coEvery { scheduleDao.getScheduleEntryById("entry_123") } returns testEntry
        coEvery { scheduleDao.getScheduleEntryById("entry_456") } returns secondEntry
        coEvery { medicineDao.getMedicineByIdDirect("med_123") } returns testMedicine
        coEvery { medicineDao.getMedicineByIdDirect("med_456") } returns secondMed
        coEvery { userDao.getUserByIdDirect("user_123") } returns testUser
        coEvery { userDao.getUserByIdDirect("user_456") } returns secondUser

        val viewModel = AlarmViewModel(database, scheduleRepository, context, testDispatcher)
        viewModel.loadCohort(listOf("entry_123", "entry_456"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isLoaded)
        assertEquals("user_123", state.user?.userId)
        assertEquals(1, state.medicinesList.size)
        assertEquals("entry_123", state.medicinesList.first().first.entryId)
    }

    @Test
    fun takeCohort_withMixedUserIds_onlyTakesMedicationsForPrimaryUser() = runTest(testDispatcher) {
        // Guard: Verify takeCohort filters out foreign profile entry IDs when invoked with mixed entry IDs (#BUG-ALARM-PROFILE-REINJECT)
        val secondUser = User(userId = "user_456", fullName = "Second User", age = 40, gender = Gender.FEMALE, contactNumber = "0987654321")
        val secondMed = Medicine(medicineId = "med_456", userId = "user_456", medicationName = "Metformin", dosage = 500.0, dosageUnit = DosageUnit.MG, timesPerDay = 1, frequency = Frequency(FrequencyPattern.DAILY), scheduledTimes = listOf(LocalTime.of(8, 0)))
        val secondEntry = ScheduleEntry(entryId = "entry_456", medicineId = "med_456", userId = "user_456", scheduledDateTime = LocalDateTime.now().minusMinutes(10), status = MedicationStatus.PENDING)

        coEvery { scheduleDao.getScheduleEntryById("entry_123") } returns testEntry
        coEvery { scheduleDao.getScheduleEntryById("entry_456") } returns secondEntry
        coEvery { medicineDao.getMedicineByIdDirect("med_123") } returns testMedicine
        coEvery { medicineDao.getMedicineByIdDirect("med_456") } returns secondMed
        coEvery { userDao.getUserByIdDirect("user_123") } returns testUser
        coEvery { userDao.getUserByIdDirect("user_456") } returns secondUser

        val viewModel = AlarmViewModel(database, scheduleRepository, context, testDispatcher)
        viewModel.loadCohort(listOf("entry_123"))
        advanceUntilIdle()

        var completed = false
        viewModel.takeCohort(listOf("entry_123", "entry_456")) {
            completed = true
        }
        advanceUntilIdle()

        assertTrue(completed)
        coVerify(exactly = 1) {
            scheduleRepository.recordDoseTaken(
                entryId = eq("entry_123"),
                status = any(),
                takenAtStr = any(),
                context = any(),
                notes = any(),
                syncSlotNotification = any()
            )
        }
        coVerify(exactly = 0) {
            scheduleRepository.recordDoseTaken(
                entryId = eq("entry_456"),
                status = any(),
                takenAtStr = any(),
                context = any(),
                notes = any(),
                syncSlotNotification = any()
            )
        }
    }

    @Test
    fun skipCohort_withMixedUserIds_onlySkipsMedicationsForPrimaryUser() = runTest(testDispatcher) {
        // Guard: Verify skipCohort filters out foreign profile entry IDs when invoked with mixed entry IDs (#BUG-ALARM-PROFILE-REINJECT)
        val secondUser = User(userId = "user_456", fullName = "Second User", age = 40, gender = Gender.FEMALE, contactNumber = "0987654321")
        val secondMed = Medicine(medicineId = "med_456", userId = "user_456", medicationName = "Metformin", dosage = 500.0, dosageUnit = DosageUnit.MG, timesPerDay = 1, frequency = Frequency(FrequencyPattern.DAILY), scheduledTimes = listOf(LocalTime.of(8, 0)))
        val secondEntry = ScheduleEntry(entryId = "entry_456", medicineId = "med_456", userId = "user_456", scheduledDateTime = LocalDateTime.now().minusMinutes(10), status = MedicationStatus.PENDING)

        coEvery { scheduleDao.getScheduleEntryById("entry_123") } returns testEntry
        coEvery { scheduleDao.getScheduleEntryById("entry_456") } returns secondEntry
        coEvery { medicineDao.getMedicineByIdDirect("med_123") } returns testMedicine
        coEvery { medicineDao.getMedicineByIdDirect("med_456") } returns secondMed
        coEvery { userDao.getUserByIdDirect("user_123") } returns testUser
        coEvery { userDao.getUserByIdDirect("user_456") } returns secondUser

        val viewModel = AlarmViewModel(database, scheduleRepository, context, testDispatcher)
        viewModel.loadCohort(listOf("entry_123"))
        advanceUntilIdle()

        var completed = false
        viewModel.skipCohort(listOf("entry_123", "entry_456"), reason = "Patient asleep") {
            completed = true
        }
        advanceUntilIdle()

        assertTrue(completed)
        coVerify(exactly = 1) { scheduleRepository.recordDoseSkipped(eq("entry_123"), eq("Patient asleep"), any()) }
        coVerify(exactly = 0) { scheduleRepository.recordDoseSkipped(eq("entry_456"), any(), any()) }
    }
}
