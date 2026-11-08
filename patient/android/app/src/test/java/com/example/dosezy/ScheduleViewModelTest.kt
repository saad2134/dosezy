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
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
        coEvery { scheduleRepository.getScheduleEntryById("entry_1") } returns testScheduleEntry
        coEvery { userRepository.getUserByIdSync("user_123") } returns testUser
        coEvery { userRepository.getAllUsersList() } returns listOf(testUser)
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
    fun markAsTaken_pastLateThreshold_recordsTakenLate() = runTest {
        // Guard: Verify that doses taken past considerLateAfter threshold (3 hours) record TAKEN_LATE status (#BUG-VM-LATE-STATUS)
        val viewModel = createViewModel()
        val takenAt = "2026-09-21T12:15:00" // 4 hours 15 mins after 08:00 AM scheduled dose

        viewModel.markAsTaken("entry_1", takenAt)

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
    fun markAsMissed_callsRecordDoseMissed() = runTest {
        val viewModel = createViewModel()

        viewModel.markAsMissed("entry_1")

        coVerify {
            scheduleRepository.recordDoseMissed(
                entryId = "entry_1",
                context = context
            )
        }
    }

    @Test
    fun refreshAfterMedicineAdded_reloadsSchedule() = runTest {
        val viewModel = createViewModel()

        viewModel.refreshAfterMedicineAdded()

        coVerify(atLeast = 2) { scheduleRepository.getScheduleWithMedicineForDate("user_123", any()) }
    }

    @Test
    fun userLoggedOutOrDeleted_clearsScheduleAndStateFlows() = runTest {
        // Guard: Verify schedule entries and flows are flushed and background jobs cancelled when user becomes null
        val usersFlow = MutableStateFlow(listOf(testUser))
        every { userRepository.getAllUsers() } returns usersFlow
        val viewModel = createViewModel()

        assertEquals(1, viewModel.todayScheduleWithMedicine.value.size)

        // Simulate user logged out or profile deleted
        usersFlow.value = emptyList()

        assertEquals(0, viewModel.todayScheduleWithMedicine.value.size)
        assertEquals(0, viewModel.scheduleWithMedicine.value.size)
        assertEquals(0, viewModel.scheduleEntries.value.size)
    }

    @Test
    fun markCohortAsTaken_fallbackBranch_forwardsNotesToRepository() = runTest {
        // Guard: Verify markCohortAsTaken passes clinical dose notes to scheduleRepository in fallback branch (#BUG-COHORT-DOSE-NOTES)
        val viewModel = createViewModel()
        val takenAt = "2026-09-21T08:05:00"
        val notes = "Took with breakfast and glass of water"

        viewModel.markCohortAsTaken(listOf("entry_1"), takenAt, lateAfter = 3, notes = notes)

        coVerify {
            scheduleRepository.recordDoseTaken(
                entryId = "entry_1",
                status = "TAKEN_ON_TIME",
                takenAtStr = takenAt,
                context = context,
                notes = notes
            )
        }
    }

    @Test
    fun markCohortAsTaken_withUseCase_forwardsNotesToUseCase() = runTest {
        // Guard: Verify markCohortAsTaken passes clinical dose notes to takeCohortUseCase (#BUG-COHORT-DOSE-NOTES)
        val mockUseCase: com.example.dosezy.domain.usecases.TakeCohortUseCase = mockk(relaxed = true)
        val viewModel = ScheduleViewModel(
            scheduleRepository = scheduleRepository,
            userRepository = userRepository,
            context = context,
            takeCohortUseCase = mockUseCase
        )
        val takenAt = "2026-09-21T08:05:00"
        val notes = "Mild nausea observed"

        viewModel.markCohortAsTaken(listOf("entry_1"), takenAt, lateAfter = 3, notes = notes)

        coVerify {
            mockUseCase.invoke(
                entryIds = listOf("entry_1"),
                customTakenAt = LocalDateTime.parse(takenAt),
                notes = notes
            )
        }
    }

    @Test
    fun logAsNeededDose_invokesRepositoryAndRefreshesCalendarIndicatorCache() = runTest {
        // Guard: Verify that logging an as-needed dose re-queries schedule entries to update month-wide calendar indicators
        val viewModel = createViewModel()
        coEvery { scheduleRepository.logAsNeededDose(any(), any(), any(), any()) } returns mockk(relaxed = true)

        viewModel.logAsNeededDose(testMedicine)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            scheduleRepository.logAsNeededDose(testMedicine, "user_123", any(), context)
        }
        verify(atLeast = 1) {
            scheduleRepository.getScheduleForUser("user_123")
        }
    }

    @Test
    fun markAsTaken_onSelectedHistoricalDate_reloadsSelectedDateSchedule() = runTest {
        // Guard: Verify marking a retrospective missed dose as taken refreshes schedule for the selected past calendar date (#109)
        val viewModel = createViewModel()
        val yesterday = LocalDate.now().minusDays(1)
        viewModel.setSelectedDate(yesterday)

        val scheduledTime = LocalDateTime.of(yesterday, LocalTime.of(8, 0)).toString()
        val auditNote = "[Logged retrospectively] Took with breakfast"

        viewModel.markAsTaken("entry_1", scheduledTime, auditNote)

        coVerify {
            scheduleRepository.recordDoseTaken(
                entryId = "entry_1",
                status = any(),
                takenAtStr = scheduledTime,
                context = context,
                notes = auditNote
            )
        }
        coVerify(atLeast = 1) {
            scheduleRepository.getScheduleWithMedicineForDate("user_123", yesterday)
        }
    }

    @Test
    fun markAsSkipped_callsRecordDoseSkippedAndRefreshesSelectedDateSchedule() = runTest {
        // Guard: Verify skipping a retrospective dose from calendar records skip reason and refreshes the selected date (#109)
        val viewModel = createViewModel()
        val yesterday = LocalDate.now().minusDays(1)
        viewModel.setSelectedDate(yesterday)

        viewModel.markAsSkipped("entry_1", "Doctor instructed to hold dose")

        coVerify {
            scheduleRepository.recordDoseSkipped(
                entryId = "entry_1",
                skipReason = "Doctor instructed to hold dose",
                context = context
            )
        }
        coVerify(atLeast = 1) {
            scheduleRepository.getScheduleWithMedicineForDate("user_123", yesterday)
        }
    }

    @Test
    fun undoDoseTaken_onSelectedHistoricalDate_reloadsSelectedDateSchedule() = runTest {
        // Guard: Verify reverting a historical dose triggers undo and reloads the active calendar date schedule (#109)
        val viewModel = createViewModel()
        val yesterday = LocalDate.now().minusDays(1)
        viewModel.setSelectedDate(yesterday)

        viewModel.undoDoseTaken("entry_1")

        coVerify {
            scheduleRepository.undoDoseTaken(
                entryId = "entry_1",
                context = context
            )
        }
        coVerify(atLeast = 1) {
            scheduleRepository.getScheduleWithMedicineForDate("user_123", yesterday)
        }
    }

    @Test
    fun markAsTaken_whenDoseTakenLate_recordsStatusAsTakenLate() = runTest {
        // Guard: Verify overdue dose marked taken in ScheduleViewModel is recorded as TAKEN_LATE rather than TAKEN_ON_TIME (#BUG-SCHEDULE-LATE-STATUS)
        val viewModel = createViewModel()
        val scheduled = LocalDateTime.of(2026, 10, 30, 8, 0)
        val overdueEntry = testScheduleEntry.copy(scheduledDateTime = scheduled)
        coEvery { scheduleRepository.getScheduleEntryById("entry_1") } returns overdueEntry

        val takenAtOverdue = "2026-10-30T14:00:00"
        viewModel.markAsTaken("entry_1", takenAtOverdue, null)

        coVerify {
            scheduleRepository.recordDoseTaken(
                entryId = "entry_1",
                status = "TAKEN_LATE",
                takenAtStr = takenAtOverdue,
                context = context,
                notes = null
            )
        }
    }

    @Test
    fun markAsTaken_whenDoseTakenOnTime_recordsStatusAsTakenOnTime() = runTest {
        // Guard: Verify on-time dose marked taken in ScheduleViewModel is recorded as TAKEN_ON_TIME (#BUG-SCHEDULE-LATE-STATUS)
        val viewModel = createViewModel()
        val scheduled = LocalDateTime.of(2026, 10, 30, 8, 0)
        val onTimeEntry = testScheduleEntry.copy(scheduledDateTime = scheduled)
        coEvery { scheduleRepository.getScheduleEntryById("entry_1") } returns onTimeEntry

        val takenAtOnTime = "2026-10-30T08:30:00"
        viewModel.markAsTaken("entry_1", takenAtOnTime, null)

        coVerify {
            scheduleRepository.recordDoseTaken(
                entryId = "entry_1",
                status = "TAKEN_ON_TIME",
                takenAtStr = takenAtOnTime,
                context = context,
                notes = null
            )
        }
    }
}

