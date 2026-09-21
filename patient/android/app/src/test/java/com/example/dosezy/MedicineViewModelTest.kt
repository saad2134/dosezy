package com.example.dosezy

import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.UserRepository
import com.example.dosezy.ui.viewmodels.MedicineViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class MedicineViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val medicineRepository: MedicineRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)

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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MedicineViewModel {
        return MedicineViewModel(
            medicineRepository = medicineRepository,
            userRepository = userRepository
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
}
