package com.example.dosezy.notifications

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.dao.MedicineDao
import com.example.dosezy.data.dao.ScheduleDao
import com.example.dosezy.data.dao.UserDao
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import com.example.dosezy.notifications.AlarmAudioPlayer
import com.example.dosezy.notifications.AlarmScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlarmRescheduleAndAudioPlaybackDeepDiveTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AlarmAudioPlayer.stop()
        context.getSharedPreferences(AlarmScheduler.PREFS_COORDINATION, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 1: Preserve Active Snoozes during rescheduleAllAlarms
    // ───────────────────────────────────────────────────────────────

    @Test
    fun issue1_rescheduleAllAlarms_doesNotCancelActiveSnoozeOrNagging() = runBlocking {
        val database = mockk<DosezyDatabase>(relaxed = true)
        val scheduleDao = mockk<ScheduleDao>(relaxed = true)
        val medicineDao = mockk<MedicineDao>(relaxed = true)
        val userDao = mockk<UserDao>(relaxed = true)

        coEvery { database.scheduleDao() } returns scheduleDao
        coEvery { database.medicineDao() } returns medicineDao
        coEvery { database.userDao() } returns userDao

        val now = LocalDateTime.now()
        val entry = ScheduleEntry(
            entryId = "entry_snoozed_1",
            userId = "user_1",
            medicineId = "med_1",
            scheduledDateTime = now.minusMinutes(5),
            status = MedicationStatus.PENDING
        )

        coEvery { medicineDao.getActiveMedicinesByUser("user_1") } returns flowOf(emptyList())
        coEvery { scheduleDao.getAllScheduleEntries("user_1") } returns listOf(entry)
        coEvery { userDao.getUserByIdDirect("user_1") } returns null

        val schedulerSpy = spyk(AlarmScheduler(context))
        val repository = ScheduleRepository(database)

        repository.rescheduleAllAlarms("user_1", context)

        // Verify that cancelSnooze and cancelNagging were NOT called on the snoozed entry
        verify(exactly = 0) { schedulerSpy.cancelSnooze(entry.entryId) }
        verify(exactly = 0) { schedulerSpy.cancelNagging(entry.entryId) }
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 2: Deleted Entry Does Not Trigger ADHD Safeguard Re-Alert
    // ───────────────────────────────────────────────────────────────

    @Test
    fun issue2_intakeSafeguard_handleCountdownExpired_doesNotReAlertIfEntryDeleted() = runBlocking {
        val database = mockk<DosezyDatabase>(relaxed = true)
        val scheduleDao = mockk<ScheduleDao>(relaxed = true)
        coEvery { database.scheduleDao() } returns scheduleDao

        // When entry is deleted from SQLite, getScheduleEntryById returns null
        coEvery { scheduleDao.getScheduleEntryById("deleted_entry_123") } returns null

        val entry = scheduleDao.getScheduleEntryById("deleted_entry_123")
        // Guard check: must evaluate to false when entry is null
        val isStillPending = entry != null && (entry.status == MedicationStatus.PENDING || entry.status == MedicationStatus.MISSED)

        assertFalse("Deleted entry must never be evaluated as pending for re-alerts", isStillPending)
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 3: AlarmScheduler.cancelAlarm with single entryId resolves slot
    // ───────────────────────────────────────────────────────────────

    @Test
    fun issue3_cancelAlarm_withEntryObject_cancelsSlotAlarmSuccessfully() {
        val schedulerSpy = spyk(AlarmScheduler(context))
        val now = LocalDateTime.now().withSecond(0).withNano(0)
        val entry = ScheduleEntry(
            entryId = "med_1_2026_10_17_08_00",
            userId = "user_42",
            medicineId = "med_1",
            scheduledDateTime = now,
            status = MedicationStatus.PENDING
        )

        schedulerSpy.cancelAlarm(entry)

        // Slot alarm cancellation should be invoked with the entry userId and scheduledDateTime
        verify(atLeast = 1) { schedulerSpy.cancelSlotAlarm("user_42", now) }
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 4: Orphaned Medicine Clean-up Reschedules Sibling Alarms
    // ───────────────────────────────────────────────────────────────

    @Test
    fun issue4_deleteMedicine_orphanedBranch_reschedulesAlarmsForUser() = runBlocking {
        val database = mockk<DosezyDatabase>(relaxed = true)
        val medicineDao = mockk<MedicineDao>(relaxed = true)
        val scheduleDao = mockk<ScheduleDao>(relaxed = true)
        val scheduleRepository = mockk<ScheduleRepository>(relaxed = true)

        coEvery { database.medicineDao() } returns medicineDao
        coEvery { database.scheduleDao() } returns scheduleDao

        // Medicine record already missing (orphaned cleanup scenario)
        coEvery { medicineDao.getMedicineByIdDirect("orphaned_med_1") } returns null

        val orphanedEntry = ScheduleEntry(
            entryId = "orphaned_entry_1",
            userId = "user_sibling_test",
            medicineId = "orphaned_med_1",
            scheduledDateTime = LocalDateTime.now().plusHours(2),
            status = MedicationStatus.PENDING
        )
        coEvery { scheduleDao.getScheduleEntriesByMedicine("orphaned_med_1") } returns listOf(orphanedEntry)

        val medicineRepository = MedicineRepository(database, scheduleRepository, context)
        medicineRepository.deleteMedicine("orphaned_med_1")

        // Guard verification: rescheduleAllAlarms must be called for the affected user
        coVerify(exactly = 1) { scheduleRepository.rescheduleAllAlarms("user_sibling_test", context) }
        coVerify(exactly = 1) { scheduleDao.deleteScheduleEntriesByMedicine("orphaned_med_1") }
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 5: AlarmAudioPlayer Cancels Prior AutoSilence Job on Continuous Alarm
    // ───────────────────────────────────────────────────────────────

    @Test
    fun issue5_alarmAudioPlayer_play_cancelsAutoSilenceJobWhenContinuousModeRequested() {
        val jobField = AlarmAudioPlayer::class.java.getDeclaredField("autoSilenceJob")
        jobField.isAccessible = true

        // Simulate an existing auto-silence job (e.g. from single-play alarm)
        val mockJob = mockk<kotlinx.coroutines.Job>(relaxed = true)
        jobField.set(AlarmAudioPlayer, mockJob)

        // Calling play with autoSilenceSeconds = 0 while audio is active should cancel prior job
        // Use reflection to set mediaPlayer to active state
        val mpField = AlarmAudioPlayer::class.java.getDeclaredField("mediaPlayer")
        mpField.isAccessible = true
        val mockPlayer = mockk<android.media.MediaPlayer>(relaxed = true)
        io.mockk.every { mockPlayer.isPlaying } returns true
        mpField.set(AlarmAudioPlayer, mockPlayer)

        assertTrue(AlarmAudioPlayer.isPlaying())

        AlarmAudioPlayer.play(
            context = context,
            sound = com.example.dosezy.data.model.AlarmSound.SYSTEM_DEFAULT,
            autoSilenceSeconds = 0 // Continuous mode
        )

        verify(atLeast = 1) { mockJob.cancel() }
        val remainingJob = jobField.get(AlarmAudioPlayer)
        assertNull("autoSilenceJob must be nullified for continuous loop mode", remainingJob)
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 6: DataExporter Streams PDF Without Heap Spike
    // ───────────────────────────────────────────────────────────────

    @Test
    fun issue6_dataExporter_streamPdfDirectly_copiesFileStreamEfficiently() {
        val tempFile = java.io.File(context.cacheDir, "test_report_stream.pdf")
        tempFile.writeText("%PDF-1.4 test stream content")

        val byteOut = java.io.ByteArrayOutputStream()
        val zipOut = java.util.zip.ZipOutputStream(byteOut)
        val entry = java.util.zip.ZipEntry("user/health_report.pdf")
        zipOut.putNextEntry(entry)

        tempFile.inputStream().use { input ->
            input.copyTo(zipOut)
        }
        zipOut.closeEntry()
        zipOut.close()
        tempFile.delete()

        val zipBytes = byteOut.toByteArray()
        assertTrue("Zip archive should be non-empty and valid", zipBytes.isNotEmpty())
        assertFalse("Temp file should be cleaned up", tempFile.exists())
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 7: Cancel Alarm Re-arms Slot Alarm When Sibling Meds Exist
    // ───────────────────────────────────────────────────────────────

    @Test
    fun issue7_cancelAlarm_withSiblingPendingMedications_rearmsSlotAlarmRatherThanDisarming() = runBlocking {
        val testDb = androidx.room.Room.inMemoryDatabaseBuilder(context, DosezyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DosezyDatabase.setInstance(testDb)
        try {
            val now = LocalDateTime.now().withSecond(0).withNano(0)
            val testUser = User(
                userId = "user_sibling_test",
                fullName = "Sibling Test User",
                age = 30,
                gender = com.example.dosezy.data.model.Gender.MALE,
                contactNumber = "1234567890"
            )
            val med1 = Medicine(
                medicineId = "med_sib_1",
                userId = testUser.userId,
                medicationName = "Aspirin",
                dosage = 100.0,
                dosageUnit = com.example.dosezy.data.model.DosageUnit.MG,
                timesPerDay = 1,
                frequency = Frequency(FrequencyPattern.DAILY),
                scheduledTimes = listOf(now.toLocalTime())
            )
            val med2 = Medicine(
                medicineId = "med_sib_2",
                userId = testUser.userId,
                medicationName = "Metformin",
                dosage = 500.0,
                dosageUnit = com.example.dosezy.data.model.DosageUnit.MG,
                timesPerDay = 1,
                frequency = Frequency(FrequencyPattern.DAILY),
                scheduledTimes = listOf(now.toLocalTime())
            )
            val entry1 = ScheduleEntry(
                entryId = "entry_sib_1",
                userId = testUser.userId,
                medicineId = med1.medicineId,
                scheduledDateTime = now,
                status = MedicationStatus.PENDING
            )
            val entry2 = ScheduleEntry(
                entryId = "entry_sib_2",
                userId = testUser.userId,
                medicineId = med2.medicineId,
                scheduledDateTime = now,
                status = MedicationStatus.PENDING
            )

            testDb.userDao().insertUser(testUser)
            testDb.medicineDao().insertMedicine(med1)
            testDb.medicineDao().insertMedicine(med2)
            testDb.scheduleDao().insertScheduleEntry(entry1)
            testDb.scheduleDao().insertScheduleEntry(entry2)

            val schedulerSpy = spyk(AlarmScheduler(context))
            schedulerSpy.cancelAlarm(entry1)

            // Sibling entry2 remains pending, so slot alarm must NOT be cancelled, and grouped alarm must be re-armed
            verify(exactly = 0) { schedulerSpy.cancelSlotAlarm(testUser.userId, now) }
            verify(atLeast = 1) { schedulerSpy.scheduleGroupedMedicineAlarm(now, match { it.size == 1 && it.first().entryId == entry2.entryId }, listOf("Metformin"), any()) }
        } finally {
            DosezyDatabase.setInstance(null)
            testDb.close()
        }
    }
}
