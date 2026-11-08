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
import com.example.dosezy.data.model.ScheduleEffectiveDate
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

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

    @Test
    fun updateMedicine_whenScheduleChanges_clampsCutoffToStartOfToday_preventingDeletionOfYesterdayDoses() = runBlocking {
        val database = mockk<DosezyDatabase>(relaxed = true)
        val medicineDao = mockk<MedicineDao>(relaxed = true)
        val scheduleDao = mockk<ScheduleDao>(relaxed = true)
        val scheduleRepository = mockk<ScheduleRepository>(relaxed = true)
        val context = mockk<Context>(relaxed = true)

        every { database.medicineDao() } returns medicineDao
        every { database.scheduleDao() } returns scheduleDao

        val medId = "med_test_cutoff"
        val userId = "user_test_cutoff"

        val oldMedicine = Medicine(
            medicineId = medId,
            userId = userId,
            medicationName = "Metformin",
            dosage = 500.0,
            dosageUnit = com.example.dosezy.data.model.DosageUnit.MG,
            frequency = Frequency(pattern = FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0)),
            startDate = LocalDate.now(),
            timesPerDay = 1
        )

        val updatedMedicine = oldMedicine.copy(
            scheduledTimes = listOf(LocalTime.of(12, 0))
        )

        coEvery { medicineDao.getMedicineByIdDirect(medId) } returns oldMedicine

        val cutoffSlot = slot<Long>()
        coEvery { scheduleDao.deleteUntakenScheduleEntriesFrom(medId, capture(cutoffSlot)) } returns Unit

        val repository = MedicineRepository(database, scheduleRepository, context)
        repository.updateMedicine(updatedMedicine)

        val startOfTodayMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertTrue("Cutoff timestamp must never precede the start of today to protect yesterday's untaken doses", cutoffSlot.captured >= startOfTodayMillis)
    }

    @Test
    fun updateMedicine_whenEffectiveDateTomorrow_deletesUntakenEntriesFromTomorrowStartAndGeneratesFromTomorrow() = runBlocking {
        val database = mockk<DosezyDatabase>(relaxed = true)
        val medicineDao = mockk<MedicineDao>(relaxed = true)
        val scheduleDao = mockk<ScheduleDao>(relaxed = true)
        val scheduleRepository = mockk<ScheduleRepository>(relaxed = true)
        val context = mockk<Context>(relaxed = true)

        every { database.medicineDao() } returns medicineDao
        every { database.scheduleDao() } returns scheduleDao

        val medId = "med_test_tomorrow"
        val userId = "user_test_tomorrow"

        val oldMedicine = Medicine(
            medicineId = medId,
            userId = userId,
            medicationName = "Lisinopril",
            dosage = 10.0,
            dosageUnit = com.example.dosezy.data.model.DosageUnit.MG,
            frequency = Frequency(pattern = FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0)),
            startDate = LocalDate.now(),
            timesPerDay = 1
        )

        val updatedMedicine = oldMedicine.copy(
            scheduledTimes = listOf(LocalTime.of(20, 0))
        )

        coEvery { medicineDao.getMedicineByIdDirect(medId) } returns oldMedicine

        val cutoffSlot = slot<Long>()
        val insertedEntriesSlot = slot<List<ScheduleEntry>>()
        coEvery { scheduleDao.deleteUntakenScheduleEntriesFrom(medId, capture(cutoffSlot)) } returns Unit
        coEvery { scheduleDao.insertScheduleEntries(capture(insertedEntriesSlot)) } returns Unit

        val repository = MedicineRepository(database, scheduleRepository, context)
        repository.updateMedicine(updatedMedicine, ScheduleEffectiveDate.TOMORROW)

        val tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay()
        val tomorrowStartMillis = tomorrowStart.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()

        assertTrue("Cutoff timestamp must be at or after tomorrow 00:00:00 to keep today's schedule intact", cutoffSlot.captured >= tomorrowStartMillis)
        assertTrue("All inserted entries must be for tomorrow or future dates", insertedEntriesSlot.captured.all { !it.scheduledDateTime.isBefore(tomorrowStart) })
    }

    @Test
    fun updateMedicine_whenEffectiveDateToday_deletesUntakenEntriesFromStartOfToday() = runBlocking {
        val database = mockk<DosezyDatabase>(relaxed = true)
        val medicineDao = mockk<MedicineDao>(relaxed = true)
        val scheduleDao = mockk<ScheduleDao>(relaxed = true)
        val scheduleRepository = mockk<ScheduleRepository>(relaxed = true)
        val context = mockk<Context>(relaxed = true)

        every { database.medicineDao() } returns medicineDao
        every { database.scheduleDao() } returns scheduleDao

        val medId = "med_test_today"
        val userId = "user_test_today"

        val oldMedicine = Medicine(
            medicineId = medId,
            userId = userId,
            medicationName = "Lisinopril",
            dosage = 10.0,
            dosageUnit = com.example.dosezy.data.model.DosageUnit.MG,
            frequency = Frequency(pattern = FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0)),
            startDate = LocalDate.now(),
            timesPerDay = 1
        )

        val updatedMedicine = oldMedicine.copy(
            scheduledTimes = listOf(LocalTime.of(20, 0))
        )

        coEvery { medicineDao.getMedicineByIdDirect(medId) } returns oldMedicine

        val cutoffSlot = slot<Long>()
        coEvery { scheduleDao.deleteUntakenScheduleEntriesFrom(medId, capture(cutoffSlot)) } returns Unit

        val repository = MedicineRepository(database, scheduleRepository, context)
        repository.updateMedicine(updatedMedicine, ScheduleEffectiveDate.TODAY)

        val startOfTodayMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertTrue("Cutoff timestamp for TODAY must delete untaken entries from start of today", cutoffSlot.captured == startOfTodayMillis)
    }
}

