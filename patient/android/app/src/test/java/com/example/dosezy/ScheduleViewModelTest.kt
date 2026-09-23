package com.example.dosezy

import android.content.Context
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.ScheduleWithMedicine
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import com.example.dosezy.ui.viewmodels.ScheduleViewModel
import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val scheduleRepository: ScheduleRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)

    private val testUser = User(
        userId = "user_123",
        fullName = "Jane Doe",
        age = 45,
        gender = Gender.FEMALE,
        contactNumber = "555-0100",
        isCurrentUser = true
    )

    private val testMedicine = Medicine(
        medicineId = "med_1",
        userId = "user_123",
        medicationName = "Atorvastatin",
        dosage = 20.0,
        dosageUnit = DosageUnit.MG,
        timesPerDay = 1,
        frequency = Frequency(FrequencyPattern.DAILY),
        scheduledTimes = listOf(LocalTime.of(8, 0))
    )

    private val testScheduleEntry = ScheduleEntry(
        entryId = "entry_1",
        userId = "user_123",
        medicineId = "med_1",
        scheduledDateTime = LocalDateTime.of(2026, 9, 21, 8, 0),
        status = MedicationStatus.PENDING
    )

    private val testScheduleWithMedicine = ScheduleWithMedicine(
        scheduleEntry = testScheduleEntry,
        medicine = testMedicine
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0

        Dispatchers.setMain(testDispatcher)
        every { userRepository.getAllUsers() } returns flowOf(listOf(testUser))
        every { scheduleRepository.getScheduleForUser("user_123") } returns flowOf(listOf(testScheduleEntry))
        every { scheduleRepository.getScheduleWithMedicineForDate("user_123", any()) } returns flowOf(listOf(testScheduleWithMedicine))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    private fun createViewModel(): ScheduleViewModel {
        return ScheduleViewModel(
            scheduleRepository = scheduleRepository,
            userRepository = userRepository,
            context = context
        )
    }

    @Test
    fun initialization_loadsCurrentUserAndTodaySchedule() = runTest {
        val viewModel = createViewModel()

        assertEquals(1, viewModel.todayScheduleWithMedicine.value.size)
        assertEquals("entry_1", viewModel.todayScheduleWithMedicine.value[0].scheduleEntry.entryId)
        assertEquals("Atorvastatin", viewModel.todayScheduleWithMedicine.value[0].medicine?.medicationName)
        assertFalse(viewModel.isTodayLoading.value)
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun setSelectedDate_updatesSelectedDateAndFetchesSchedule() = runTest {
        val viewModel = createViewModel()
        val futureDate = LocalDate.now().plusDays(3)

        viewModel.setSelectedDate(futureDate)

        assertEquals(futureDate, viewModel.selectedDate.value)
        coVerify { scheduleRepository.getScheduleWithMedicineForDate("user_123", futureDate) }
    }

    @Test
    fun markAsTaken_callsRepositoryAndRefreshesSchedule() = runTest {
        val viewModel = createViewModel()
        val takenAt = "2026-09-21T08:05:00"

        viewModel.markAsTaken("entry_1", takenAt)

        coVerify {
            scheduleRepository.recordDoseTaken(
                entryId = "entry_1",
                status = "TAKEN_ON_TIME",
                takenAtStr = takenAt,
                context = context,
                notes = null
            )
        }
        coVerify(atLeast = 1) { scheduleRepository.getScheduleWithMedicineForDate("user_123", any()) }
    }

    @Test
    fun markAsTaken_withNotes_passesNotesToRepository() = runTest {
        val viewModel = createViewModel()
        val takenAt = "2026-09-21T08:05:00"
        val note = "With breakfast"

        viewModel.markAsTaken("entry_1", takenAt, note)

        coVerify {
            scheduleRepository.recordDoseTaken(
                entryId = "entry_1",
                status = "TAKEN_ON_TIME",
                takenAtStr = takenAt,
                context = context,
                notes = note
            )
        }
    }

    @Test
    fun markAsLate_callsRepositoryWithLateStatus() = runTest {
        val viewModel = createViewModel()
        val takenAt = "2026-09-21T11:45:00"

        viewModel.markAsLate("entry_1", takenAt)

        coVerify {
            scheduleRepository.recordDoseTaken(
                entryId = "entry_1",
                status = "TAKEN_LATE",
                takenAtStr = takenAt,
                context = context,
                notes = null
            )
        }
    }

    @Test
    fun updateDoseNotes_callsRepositoryAndRefreshesSchedule() = runTest {
        val viewModel = createViewModel()

        viewModel.updateDoseNotes("entry_1", "Mild headache")

        coVerify {
            scheduleRepository.updateDoseNotes("entry_1", "Mild headache")
        }
        coVerify(atLeast = 1) { scheduleRepository.getScheduleWithMedicineForDate("user_123", any()) }
    }

    @Test
    fun undoDoseTaken_callsUndoOnRepository() = runTest {
        val viewModel = createViewModel()

        viewModel.undoDoseTaken("entry_1")

        coVerify {
            scheduleRepository.undoDoseTaken(
                entryId = "entry_1",
                context = context
            )
        }
    }

    @Test
    fun markAsMissed_callsUpdateMedicationStatus() = runTest {
        val viewModel = createViewModel()

        viewModel.markAsMissed("entry_1")

        coVerify {
            scheduleRepository.updateMedicationStatus(
                entryId = "entry_1",
                status = "MISSED",
                takenAtMillis = null as Long?
            )
        }
    }

    @Test
    fun refreshAfterMedicineAdded_reloadsSchedule() = runTest {
        val viewModel = createViewModel()

        viewModel.refreshAfterMedicineAdded()

        coVerify(atLeast = 2) { scheduleRepository.getScheduleWithMedicineForDate("user_123", any()) }
    }
}
