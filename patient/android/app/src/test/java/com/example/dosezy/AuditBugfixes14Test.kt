package com.example.dosezy

import android.content.Context
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.dao.MedicineDao
import com.example.dosezy.data.dao.ScheduleDao
import com.example.dosezy.data.export.BackupRestoreManager
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Unit tests verifying Audit #14 bug fixes:
 *  1. FrequencyPattern.EVERY_X_HOURS preserves nocturnal doses when startDate is null and across date boundaries.
 *  2. Schedule auto-extension anchors to latestDate and filters by isAfter(latestEntry) without dropping doses.
 *  3. BackupRestoreManager decodes epoch millis using ZoneOffset.UTC matching Room Converters.
 *  4. MedicineRepository.deleteMedicine and deleteMedicineWithAlarms clean up orphaned schedule entries and handle null records gracefully.
 */
class AuditBugfixes14Test {

    private lateinit var mockContext: Context
    private lateinit var mockDatabase: DosezyDatabase
    private lateinit var mockScheduleDao: ScheduleDao
    private lateinit var mockMedicineDao: MedicineDao
    private lateinit var mockScheduleRepo: ScheduleRepository
    private lateinit var backupRestoreManager: BackupRestoreManager

    @Before
    fun setup() {
        mockContext = mockk<Context>(relaxed = true)
        mockDatabase = mockk<DosezyDatabase>(relaxed = true)
        mockScheduleDao = mockk<ScheduleDao>(relaxed = true)
        mockMedicineDao = mockk<MedicineDao>(relaxed = true)
        mockScheduleRepo = mockk<ScheduleRepository>(relaxed = true)

        coEvery { mockDatabase.scheduleDao() } returns mockScheduleDao
        coEvery { mockDatabase.medicineDao() } returns mockMedicineDao

        backupRestoreManager = BackupRestoreManager(
            context = mockContext,
            database = mockDatabase,
            scheduleRepository = mockScheduleRepo
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. FrequencyPattern.EVERY_X_HOURS Preserves Nocturnal Doses with Null startDate
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testEveryXHours_nullStartDate_preservesEarlyMorningSlotsOnFutureRange() {
        val medicine = Medicine(
            medicineId = "med_hourly_null_start",
            userId = "user_1",
            medicationName = "Tramadol",
            dosage = 50.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 4,
            frequency = Frequency(pattern = FrequencyPattern.EVERY_X_HOURS, intervalHours = 6),
            scheduledTimes = listOf(LocalTime.of(8, 0)),
            startDate = null // Null start date (ongoing regimen)
        )

        // Generate for a target date in the future
        val targetDate = LocalDate.of(2030, 6, 15)
        val entries = medicine.generateScheduleEntries(startDateRange = targetDate, days = 0)

        // Doses must include early morning slots (e.g. 02:00) before 08:00
        val times = entries.map { it.scheduledDateTime.toLocalTime() }
        assertTrue("Expected early morning slot (02:00) on target date", times.contains(LocalTime.of(2, 0)))
        assertTrue("Expected 08:00 slot on target date", times.contains(LocalTime.of(8, 0)))
        assertTrue("Expected 14:00 slot on target date", times.contains(LocalTime.of(14, 0)))
        assertTrue("Expected 20:00 slot on target date", times.contains(LocalTime.of(20, 0)))
        assertEquals(4, entries.size)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. Schedule Auto-Extension Anchoring & Filtering
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testAutoExtendFiltering_preservesSlotsOccurringStrictlyAfterLatestEntry() {
        val medicine = Medicine(
            medicineId = "med_daily_multi",
            userId = "user_1",
            medicationName = "Metformin",
            dosage = 500.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 2,
            frequency = Frequency(pattern = FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            startDate = LocalDate.of(2030, 1, 1)
        )

        val latestDate = LocalDate.of(2030, 1, 15)
        val latestEntry = ScheduleEntry(
            entryId = "latest_entry",
            userId = "user_1",
            medicineId = medicine.medicineId,
            scheduledDateTime = latestDate.atTime(8, 0),
            status = MedicationStatus.TAKEN_ON_TIME
        )

        // When autoExtend generates from latestDate for 2 days
        val generated = medicine.generateScheduleEntries(startDateRange = latestDate, days = 1)
        val filtered = generated.filter { it.scheduledDateTime.isAfter(latestEntry.scheduledDateTime) }

        // Must include the 20:00 dose on latestDate and both doses on latestDate + 1
        assertEquals(3, filtered.size)
        assertEquals(latestDate.atTime(20, 0), filtered[0].scheduledDateTime)
        assertEquals(latestDate.plusDays(1).atTime(8, 0), filtered[1].scheduledDateTime)
        assertEquals(latestDate.plusDays(1).atTime(20, 0), filtered[2].scheduledDateTime)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. BackupRestoreManager Decodes Epoch Millis with ZoneOffset.UTC
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testBackupRestoreManager_parseSchedulesFromJson_decodesNumericEpochMillisWithUtc() {
        val testUtcMillis = 1893456000000L // 2030-01-01T00:00:00Z
        val expectedUtcDateTime = Instant.ofEpochMilli(testUtcMillis).atZone(ZoneOffset.UTC).toLocalDateTime()

        val json = """
            [
                {
                    "entryId": "entry_epoch_test",
                    "userId": "user_1",
                    "medicineId": "med_1",
                    "scheduledDateTime": "$testUtcMillis",
                    "status": "PENDING",
                    "takenAt": "$testUtcMillis"
                }
            ]
        """.trimIndent()

        val entries = backupRestoreManager.parseSchedulesFromJson(json)
        assertEquals(1, entries.size)
        val entry = entries.first()

        assertEquals("Scheduled time must match UTC epoch conversion", expectedUtcDateTime, entry.scheduledDateTime)
        assertNotNull(entry.takenAt)
        assertEquals("TakenAt time must match UTC epoch conversion", expectedUtcDateTime, entry.takenAt)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. MedicineRepository Deletion & Alarm Rescheduling
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testMedicineRepository_deleteMedicineWithAlarms_handlesNonExistentMedicineSafely() = kotlinx.coroutines.test.runTest {
        val medRepo = MedicineRepository(
            database = mockDatabase,
            scheduleRepository = mockScheduleRepo,
            context = mockContext
        )

        coEvery { mockMedicineDao.getMedicineByIdDirect("missing_id") } returns null

        medRepo.deleteMedicineWithAlarms("missing_id", mockContext)

        // Must cancel alarms and purge orphaned schedule entries without crashing
        coVerify { mockScheduleRepo.cancelAlarmsForMedicine("missing_id", mockContext) }
        coVerify { mockScheduleDao.deleteScheduleEntriesByMedicine("missing_id") }
    }
}
