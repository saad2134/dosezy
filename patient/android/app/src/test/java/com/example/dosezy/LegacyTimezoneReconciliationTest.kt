/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy

import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.converters.Converters
import com.example.dosezy.data.dao.MedicineDao
import com.example.dosezy.data.dao.ScheduleDao
import com.example.dosezy.data.dao.UserDao
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.repository.ScheduleRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class LegacyTimezoneReconciliationTest {

    private val converters = Converters()

    // ───────────────────────────────────────────────────────────────
    // 1. entryId Date & Time Parsing Tests
    // ───────────────────────────────────────────────────────────────

    @Test
    fun parseDateTimeFromEntryId_standardFormatWithSeconds() {
        val entryId = "med123_2026_09_22_08_15_00"
        val parsed = ScheduleRepository.parseDateTimeFromEntryId(entryId)
        assertEquals(LocalDateTime.of(2026, 9, 22, 8, 15, 0), parsed)
    }

    @Test
    fun parseDateTimeFromEntryId_eveningTime() {
        val entryId = "med123_2026_09_22_18_00_00"
        val parsed = ScheduleRepository.parseDateTimeFromEntryId(entryId)
        assertEquals(LocalDateTime.of(2026, 9, 22, 18, 0, 0), parsed)
    }

    @Test
    fun parseDateTimeFromEntryId_withoutSeconds() {
        val entryId = "med123_2026_09_22_08_15"
        val parsed = ScheduleRepository.parseDateTimeFromEntryId(entryId)
        assertEquals(LocalDateTime.of(2026, 9, 22, 8, 15, 0), parsed)
    }

    @Test
    fun parseDateTimeFromEntryId_uuidWithUnderscoresPrefix() {
        val entryId = "8e32a67e_5932_47bf_870f_f3e46ffec7b4_2026_09_23_08_15_00"
        val parsed = ScheduleRepository.parseDateTimeFromEntryId(entryId)
        assertEquals(LocalDateTime.of(2026, 9, 23, 8, 15, 0), parsed)
    }

    @Test
    fun parseDateTimeFromEntryId_prnIgnored() {
        val entryId = "PRN_med123_1727111222333"
        val parsed = ScheduleRepository.parseDateTimeFromEntryId(entryId)
        assertNull(parsed)
    }

    @Test
    fun parseDateTimeFromEntryId_arbitraryStringIgnored() {
        val entryId = "simple_uuid_string_without_timestamp"
        val parsed = ScheduleRepository.parseDateTimeFromEntryId(entryId)
        assertNull(parsed)
    }

    // ───────────────────────────────────────────────────────────────
    // 2. Closest Scheduled Time Matching Tests (Fallback)
    // ───────────────────────────────────────────────────────────────

    @Test
    fun findClosestScheduledTime_matchesShiftedMorningDose() {
        // Entry is 06:15, but scheduledTimes are 08:15 and 18:00
        val shiftedDateTime = LocalDateTime.of(2026, 9, 23, 6, 15, 0)
        val scheduledTimes = listOf(LocalTime.of(8, 15), LocalTime.of(18, 0))

        val closest = ScheduleRepository.findClosestScheduledTime(shiftedDateTime, scheduledTimes)
        assertEquals(LocalDateTime.of(2026, 9, 23, 8, 15, 0), closest)
    }

    @Test
    fun findClosestScheduledTime_matchesShiftedEveningDose() {
        // Entry is 16:00, but scheduledTimes are 08:15 and 18:00
        val shiftedDateTime = LocalDateTime.of(2026, 9, 23, 16, 0, 0)
        val scheduledTimes = listOf(LocalTime.of(8, 15), LocalTime.of(18, 0))

        val closest = ScheduleRepository.findClosestScheduledTime(shiftedDateTime, scheduledTimes)
        assertEquals(LocalDateTime.of(2026, 9, 23, 18, 0, 0), closest)
    }

    @Test
    fun findClosestScheduledTime_alreadyCorrectReturnsSame() {
        val correctDateTime = LocalDateTime.of(2026, 9, 23, 8, 15, 0)
        val scheduledTimes = listOf(LocalTime.of(8, 15), LocalTime.of(18, 0))

        val closest = ScheduleRepository.findClosestScheduledTime(correctDateTime, scheduledTimes)
        assertEquals(correctDateTime, closest)
    }

    @Test
    fun findClosestScheduledTime_acrossMidnightWrap() {
        // Entry at 23:00 on the 22nd due to a -2h shift from 01:00 AM on the 23rd
        val shiftedDateTime = LocalDateTime.of(2026, 9, 22, 23, 0, 0)
        val scheduledTimes = listOf(LocalTime.of(1, 0))

        val closest = ScheduleRepository.findClosestScheduledTime(shiftedDateTime, scheduledTimes)
        assertEquals(LocalDateTime.of(2026, 9, 23, 1, 0, 0), closest)
    }

    // ───────────────────────────────────────────────────────────────
    // 3. Database Reconciliation Flow Tests
    // ───────────────────────────────────────────────────────────────

    @Test
    fun reconcileLegacyScheduleEntries_recalibratesShiftedEntryAndTakenAt() = runBlocking {
        val database = mockk<DosezyDatabase>()
        val scheduleDao = mockk<ScheduleDao>(relaxed = true)
        val medicineDao = mockk<MedicineDao>(relaxed = true)
        val userDao = mockk<UserDao>(relaxed = true)

        every { database.scheduleDao() } returns scheduleDao
        every { database.medicineDao() } returns medicineDao
        every { database.userDao() } returns userDao

        val userId = "user_1"
        val medId = "med_1"

        // Entry shifted: says 06:15, but entryId says 08:15. Taken at 06:20.
        val shiftedEntry = ScheduleEntry(
            entryId = "${medId}_2026_09_22_08_15_00",
            userId = userId,
            medicineId = medId,
            scheduledDateTime = LocalDateTime.of(2026, 9, 22, 6, 15, 0),
            status = MedicationStatus.TAKEN_ON_TIME,
            takenAt = LocalDateTime.of(2026, 9, 22, 6, 20, 0)
        )

        coEvery { scheduleDao.getAllScheduleEntries(userId) } returns listOf(shiftedEntry)
        coEvery { medicineDao.getMedicinesByUserDirect(userId) } returns emptyList()

        val repository = ScheduleRepository(database)
        val count = repository.reconcileLegacyScheduleEntries(userId)

        assertEquals(1, count)

        // Verify update was called with recalibrated 08:15 and takenAt 08:20 (+2h)
        val expectedCorrectedEntry = shiftedEntry.copy(
            scheduledDateTime = LocalDateTime.of(2026, 9, 22, 8, 15, 0),
            takenAt = LocalDateTime.of(2026, 9, 22, 8, 20, 0)
        )
        coVerify { scheduleDao.updateScheduleEntry(expectedCorrectedEntry) }
    }

    @Test
    fun reconcileLegacyScheduleEntries_leavesCorrectEntriesUntouched() = runBlocking {
        val database = mockk<DosezyDatabase>()
        val scheduleDao = mockk<ScheduleDao>(relaxed = true)
        val medicineDao = mockk<MedicineDao>(relaxed = true)

        every { database.scheduleDao() } returns scheduleDao
        every { database.medicineDao() } returns medicineDao

        val userId = "user_1"
        val medId = "med_1"

        val correctEntry = ScheduleEntry(
            entryId = "${medId}_2026_09_22_08_15_00",
            userId = userId,
            medicineId = medId,
            scheduledDateTime = LocalDateTime.of(2026, 9, 22, 8, 15, 0),
            status = MedicationStatus.PENDING
        )

        coEvery { scheduleDao.getAllScheduleEntries(userId) } returns listOf(correctEntry)
        coEvery { medicineDao.getMedicinesByUserDirect(userId) } returns emptyList()

        val repository = ScheduleRepository(database)
        val count = repository.reconcileLegacyScheduleEntries(userId)

        assertEquals(0, count)
        coVerify(exactly = 0) { scheduleDao.updateScheduleEntry(any()) }
    }

    // ───────────────────────────────────────────────────────────────
    // 4. Converters LocalDate Tests (UTC Midnight vs Legacy systemDefault)
    // ───────────────────────────────────────────────────────────────

    @Test
    fun toLocalDate_convertsUtcMidnightDirectly() {
        val date = LocalDate.of(2026, 9, 22)
        val utcMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(0L, utcMillis % 86400000L)

        val restored = converters.toLocalDate(utcMillis)
        assertEquals(date, restored)
    }

    @Test
    fun toLocalDate_recoversLegacySystemDefaultMidnight() {
        val date = LocalDate.of(2026, 9, 22)
        val systemDefaultMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val restored = converters.toLocalDate(systemDefaultMillis)
        assertEquals(date, restored)
    }
}
