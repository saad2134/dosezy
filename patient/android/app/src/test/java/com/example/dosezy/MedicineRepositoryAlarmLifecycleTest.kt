/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy

import android.content.Context
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.dao.MedicineDao
import com.example.dosezy.data.dao.ScheduleDao
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class MedicineRepositoryAlarmLifecycleTest {

    @Test
    fun updateMedicine_whenScheduleChanges_cancelsAlarmsBEFOREDeletingUntakenEntries() = runBlocking {
        val database = mockk<DosezyDatabase>(relaxed = true)
        val medicineDao = mockk<MedicineDao>(relaxed = true)
        val scheduleDao = mockk<ScheduleDao>(relaxed = true)
        val scheduleRepository = mockk<ScheduleRepository>(relaxed = true)
        val context = mockk<Context>(relaxed = true)

        every { database.medicineDao() } returns medicineDao
        every { database.scheduleDao() } returns scheduleDao

        val medId = "med_test_1"
        val userId = "user_test_1"

        // Existing medicine scheduled at 08:00
        val oldMedicine = Medicine(
            medicineId = medId,
            userId = userId,
            medicationName = "Aspirin",
            dosage = 1.0,
            dosageUnit = com.example.dosezy.data.model.DosageUnit.TABLET,
            frequency = Frequency(pattern = FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0)),
            startDate = LocalDate.now(),
            timesPerDay = 1
        )

        // Updated medicine changed to 09:00
        val updatedMedicine = oldMedicine.copy(
            scheduledTimes = listOf(LocalTime.of(9, 0))
        )

        coEvery { medicineDao.getMedicineByIdDirect(medId) } returns oldMedicine

        val repository = MedicineRepository(database, scheduleRepository, context)
        repository.updateMedicine(updatedMedicine)

        // Verify CRITICAL order to prevent orphaned alarms (Issue #87):
        // 1. cancelAlarmsForMedicine MUST be called first while old entries still exist in DB
        // 2. deleteUntakenScheduleEntriesFrom deletes old entries
        // 3. insertScheduleEntries inserts new 09:00 entries
        // 4. rescheduleAllAlarms re-arms the active alarms
        coVerifyOrder {
            scheduleRepository.cancelAlarmsForMedicine(medId, any())
            scheduleDao.deleteUntakenScheduleEntriesFrom(medId, any())
            scheduleDao.insertScheduleEntries(any())
            scheduleRepository.rescheduleAllAlarms(userId, any())
        }
    }

    @Test
    fun updateMedicine_whenScheduleUnchanged_reschedulesWithoutDeletingEntries() = runBlocking {
        val database = mockk<DosezyDatabase>(relaxed = true)
        val medicineDao = mockk<MedicineDao>(relaxed = true)
        val scheduleDao = mockk<ScheduleDao>(relaxed = true)
        val scheduleRepository = mockk<ScheduleRepository>(relaxed = true)
        val context = mockk<Context>(relaxed = true)

        every { database.medicineDao() } returns medicineDao
        every { database.scheduleDao() } returns scheduleDao

        val medId = "med_test_2"
        val userId = "user_test_2"

        val oldMedicine = Medicine(
            medicineId = medId,
            userId = userId,
            medicationName = "Aspirin",
            dosage = 1.0,
            dosageUnit = com.example.dosezy.data.model.DosageUnit.TABLET,
            frequency = Frequency(pattern = FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0)),
            startDate = LocalDate.now(),
            timesPerDay = 1,
            notes = "Take with water"
        )

        // Only notes updated, scheduledTimes / dates remain identical
        val updatedMedicine = oldMedicine.copy(
            notes = "Take with food and water"
        )

        coEvery { medicineDao.getMedicineByIdDirect(medId) } returns oldMedicine

        val repository = MedicineRepository(database, scheduleRepository, context)
        repository.updateMedicine(updatedMedicine)

        // Entries must NOT be deleted when schedule didn't change
        coVerify(exactly = 0) { scheduleDao.deleteUntakenScheduleEntriesFrom(any(), any()) }
        coVerify(exactly = 0) { scheduleDao.insertScheduleEntries(any()) }

        // Alarms are refreshed with the new metadata
        coVerify(exactly = 1) { scheduleRepository.rescheduleAllAlarms(userId, any()) }
    }
}
