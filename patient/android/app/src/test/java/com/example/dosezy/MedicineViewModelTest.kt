package com.example.dosezy

import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.MedicineSortMode
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.MedicineOrderPreferences
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.UserRepository
import com.example.dosezy.ui.viewmodels.MedicineViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class MedicineViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val medicineRepository: MedicineRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val orderPreferences: MedicineOrderPreferences = mockk(relaxed = true)

    private val testUser = User(
        userId = "user_456",
        fullName = "Robert Smith",
        age = 52,
        gender = Gender.MALE,
        contactNumber = "555-0200",
        isCurrentUser = true
    )

    private val activeMedicine = Medicine(
        medicineId = "med_active",
        userId = "user_456",
        medicationName = "Metformin",
        dosage = 500.0,
        dosageUnit = DosageUnit.MG,
        timesPerDay = 2,
        frequency = Frequency(FrequencyPattern.DAILY),
        scheduledTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
        isArchived = false
    )

    private val archivedMedicine = Medicine(
        medicineId = "med_archived",
        userId = "user_456",
        medicationName = "Amoxicillin",
        dosage = 250.0,
        dosageUnit = DosageUnit.MG,
        timesPerDay = 3,
        frequency = Frequency(FrequencyPattern.DAILY),
        scheduledTimes = listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0)),
        isArchived = true
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { userRepository.getAllUsers() } returns flowOf(listOf(testUser))
        every { medicineRepository.getMedicinesByUser("user_456") } returns flowOf(listOf(activeMedicine))
        every { medicineRepository.getArchivedMedicinesByUser("user_456") } returns flowOf(listOf(archivedMedicine))
        every { orderPreferences.getSortMode(any()) } returns MedicineSortMode.DEFAULT
        every { orderPreferences.getCustomOrder(any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        prefs: MedicineOrderPreferences? = orderPreferences
    ): MedicineViewModel {
        return MedicineViewModel(
            medicineRepository = medicineRepository,
            userRepository = userRepository,
            orderPreferences = prefs
        )
    }

    @Test
    fun initialization_loadsActiveAndArchivedMedicinesForCurrentUser() = runTest {
        val viewModel = createViewModel()

        assertEquals(1, viewModel.medicines.value.size)
        assertEquals("Metformin", viewModel.medicines.value[0].medicationName)

        assertEquals(1, viewModel.archivedMedicines.value.size)
        assertEquals("Amoxicillin", viewModel.archivedMedicines.value[0].medicationName)

        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun setCurrentUser_switchesUserAndFetchesMedicines() = runTest {
        val viewModel = createViewModel()
        every { medicineRepository.getMedicinesByUser("user_789") } returns flowOf(emptyList())
        every { medicineRepository.getArchivedMedicinesByUser("user_789") } returns flowOf(emptyList())

        viewModel.setCurrentUser("user_789")

        coVerify { medicineRepository.getMedicinesByUser("user_789") }
        coVerify { medicineRepository.getArchivedMedicinesByUser("user_789") }
    }

    @Test
    fun addMedicine_invokesRepositoryInsert() = runTest {
        val viewModel = createViewModel()
        val newMed = activeMedicine.copy(medicineId = "med_new", medicationName = "Lisinopril")

        viewModel.addMedicine(newMed)

        coVerify { medicineRepository.insertMedicine(newMed) }
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun updateMedicine_invokesRepositoryUpdate() = runTest {
        val viewModel = createViewModel()
        val updated = activeMedicine.copy(dosage = 1000.0)

        viewModel.updateMedicine(updated)

        coVerify { medicineRepository.updateMedicine(updated) }
    }

    @Test
    fun archiveMedicine_invokesRepositoryArchive() = runTest {
        val viewModel = createViewModel()

        viewModel.archiveMedicine(activeMedicine)

        coVerify { medicineRepository.archiveMedicine(activeMedicine) }
    }

    @Test
    fun unarchiveMedicine_invokesRepositoryUnarchive() = runTest {
        val viewModel = createViewModel()

        viewModel.unarchiveMedicine(archivedMedicine)

        coVerify { medicineRepository.unarchiveMedicine(archivedMedicine) }
    }

    @Test
    fun deleteMedicine_invokesRepositoryDelete() = runTest {
        val viewModel = createViewModel()

        viewModel.deleteMedicine(activeMedicine)

        coVerify { medicineRepository.deleteMedicine(activeMedicine) }
    }

    @Test
    fun deleteMedicinePermanently_invokesRepositoryDeletePermanently() = runTest {
        val viewModel = createViewModel()

        viewModel.deleteMedicinePermanently(activeMedicine)

        coVerify { medicineRepository.deleteMedicinePermanently(activeMedicine) }
    }

    @Test
    fun userLoggedOutOrDeleted_clearsMedicinesAndCancelsJobs() = runTest {
        // Guard: Verify cached patient medicines are flushed and active jobs cancelled when user becomes null
        val usersFlow = MutableStateFlow(listOf(testUser))
        every { userRepository.getAllUsers() } returns usersFlow
        val viewModel = createViewModel()

        assertEquals(1, viewModel.medicines.value.size)
        assertEquals(1, viewModel.archivedMedicines.value.size)

        // Simulate all profiles deleted or user logged out
        usersFlow.value = emptyList()

        assertEquals(0, viewModel.medicines.value.size)
        assertEquals(0, viewModel.archivedMedicines.value.size)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun setSortMode_updatesSortModeAndSortsMedicines() = runTest {
        val medZ = activeMedicine.copy(medicineId = "med_z", medicationName = "Zoloft")
        val medA = activeMedicine.copy(medicineId = "med_a", medicationName = "Aspirin")
        val medM = activeMedicine.copy(medicineId = "med_m", medicationName = "Metformin")
        every { medicineRepository.getMedicinesByUser("user_456") } returns flowOf(listOf(medZ, medA, medM))

        val viewModel = createViewModel()

        // Default order
        assertEquals(listOf("Zoloft", "Aspirin", "Metformin"), viewModel.medicines.value.map { it.medicationName })

        // Sort Name Ascending
        viewModel.setSortMode(MedicineSortMode.NAME_ASC)
        assertEquals(MedicineSortMode.NAME_ASC, viewModel.sortMode.value)
        assertEquals(listOf("Aspirin", "Metformin", "Zoloft"), viewModel.medicines.value.map { it.medicationName })
        verify { orderPreferences.setSortMode("user_456", MedicineSortMode.NAME_ASC) }

        // Sort Name Descending
        viewModel.setSortMode(MedicineSortMode.NAME_DESC)
        assertEquals(MedicineSortMode.NAME_DESC, viewModel.sortMode.value)
        assertEquals(listOf("Zoloft", "Metformin", "Aspirin"), viewModel.medicines.value.map { it.medicationName })
        verify { orderPreferences.setSortMode("user_456", MedicineSortMode.NAME_DESC) }
    }

    @Test
    fun setSortMode_timeAsc_sortsByEarliestScheduledTime() = runTest {
        val medNoon = activeMedicine.copy(
            medicineId = "med_noon",
            medicationName = "NoonMed",
            scheduledTimes = listOf(LocalTime.of(14, 0))
        )
        val medMorning = activeMedicine.copy(
            medicineId = "med_morning",
            medicationName = "MorningMed",
            scheduledTimes = listOf(LocalTime.of(8, 0))
        )
        val medNight = activeMedicine.copy(
            medicineId = "med_night",
            medicationName = "NightMed",
            scheduledTimes = listOf(LocalTime.of(20, 0))
        )
        every { medicineRepository.getMedicinesByUser("user_456") } returns flowOf(listOf(medNoon, medMorning, medNight))

        val viewModel = createViewModel()

        viewModel.setSortMode(MedicineSortMode.TIME_ASC)
        assertEquals(MedicineSortMode.TIME_ASC, viewModel.sortMode.value)
        assertEquals(listOf("MorningMed", "NoonMed", "NightMed"), viewModel.medicines.value.map { it.medicationName })
    }

    @Test
    fun moveMedicineUp_and_moveMedicineDown_reordersAndSwitchesToCustomMode() = runTest {
        val medA = activeMedicine.copy(medicineId = "med_a", medicationName = "Med A")
        val medB = activeMedicine.copy(medicineId = "med_b", medicationName = "Med B")
        val medC = activeMedicine.copy(medicineId = "med_c", medicationName = "Med C")
        every { medicineRepository.getMedicinesByUser("user_456") } returns flowOf(listOf(medA, medB, medC))

        val viewModel = createViewModel()

        // Move Med A down -> should become Med B, Med A, Med C
        viewModel.moveMedicineDown("med_a")
        assertEquals(MedicineSortMode.CUSTOM, viewModel.sortMode.value)
        assertEquals(listOf("med_b", "med_a", "med_c"), viewModel.medicines.value.map { it.medicineId })
        verify { orderPreferences.setSortMode("user_456", MedicineSortMode.CUSTOM) }
        verify { orderPreferences.setCustomOrder("user_456", listOf("med_b", "med_a", "med_c")) }

        // Move Med C up -> should become Med B, Med C, Med A
        viewModel.moveMedicineUp("med_c")
        assertEquals(listOf("med_b", "med_c", "med_a"), viewModel.medicines.value.map { it.medicineId })

        // Move Med B up (already at top) -> unchanged
        viewModel.moveMedicineUp("med_b")
        assertEquals(listOf("med_b", "med_c", "med_a"), viewModel.medicines.value.map { it.medicineId })

        // Move Med A down (already at bottom) -> unchanged
        viewModel.moveMedicineDown("med_a")
        assertEquals(listOf("med_b", "med_c", "med_a"), viewModel.medicines.value.map { it.medicineId })
    }

    @Test
    fun setCurrentUser_loadsPersistedSortPreferencesForNewProfile() = runTest {
        every { orderPreferences.getSortMode("user_789") } returns MedicineSortMode.NAME_DESC
        every { orderPreferences.getCustomOrder("user_789") } returns listOf("med_x")
        every { medicineRepository.getMedicinesByUser("user_789") } returns flowOf(emptyList())
        every { medicineRepository.getArchivedMedicinesByUser("user_789") } returns flowOf(emptyList())

        val viewModel = createViewModel()
        viewModel.setCurrentUser("user_789")

        assertEquals(MedicineSortMode.NAME_DESC, viewModel.sortMode.value)
        assertEquals(listOf("med_x"), viewModel.customOrder.value)
    }

    @Test
    fun deleteMedicine_prunesMedicineIdFromOrderPreferencesAndCustomOrder() = runTest {
        val viewModel = createViewModel()
        viewModel.setSortMode(MedicineSortMode.CUSTOM)

        viewModel.deleteMedicine(activeMedicine)

        verify { orderPreferences.removeMedicine("user_456", "med_active") }
        coVerify { medicineRepository.deleteMedicine(activeMedicine) }
    }

    @Test
    fun deleteMedicinePermanently_prunesMedicineIdFromOrderPreferencesAndCustomOrder() = runTest {
        val viewModel = createViewModel()
        viewModel.setSortMode(MedicineSortMode.CUSTOM)

        viewModel.deleteMedicinePermanently(activeMedicine)

        verify { orderPreferences.removeMedicine("user_456", "med_active") }
        coVerify { medicineRepository.deleteMedicinePermanently(activeMedicine) }
    }
}
