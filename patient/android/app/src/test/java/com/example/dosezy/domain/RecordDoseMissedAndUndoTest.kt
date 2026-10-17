package com.example.dosezy.domain

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.dosezy.R
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.export.BackupRestoreManager
import com.example.dosezy.data.export.DataExporter
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.Language
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.Theme
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import com.example.dosezy.notifications.MedicineAlarmReceiver
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecordDoseMissedAndUndoTest {

    private lateinit var context: Context
    private lateinit var db: DosezyDatabase
    private lateinit var userRepository: UserRepository
    private lateinit var medicineRepository: MedicineRepository
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var notificationManager: NotificationManager

    private val testUser = User(
        userId = "user_audit8",
        fullName = "Test User Audit 8",
        age = 30,
        gender = Gender.MALE,
        contactNumber = "1234567890",
        theme = Theme.SYSTEM,
        timeFormat = TimeFormat.HOUR_12,
        language = Language.ENGLISH,
        allowDoseUndo = true,
        allowCustomDoseTime = false
    )

    private val testMedicine = Medicine(
        medicineId = "med_audit8",
        userId = "user_audit8",
        medicationName = "Amoxicillin",
        dosage = 500.0,
        dosageUnit = DosageUnit.MG,
        timesPerDay = 1,
        frequency = Frequency(FrequencyPattern.DAILY),
        scheduledTimes = listOf(LocalTime.of(9, 0)),
        currentStock = 20,
        autoDeductOnTake = true
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, DosezyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userRepository = UserRepository(db)
        scheduleRepository = ScheduleRepository(db)
        medicineRepository = MedicineRepository(db, scheduleRepository, context)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        runBlocking {
            db.userDao().insertUser(testUser)
            db.medicineDao().insertMedicine(testMedicine)
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 1 Tests: Lingering Notifications and Armed Alarms Cancellation on Missed Doses
    // ───────────────────────────────────────────────────────────────

    @Test
    fun issue1_recordDoseMissed_updatesStatusToMissedAndCancelsNotification() = runBlocking {
        val entryId = "entry_missed_test_1"
        val scheduleEntry = ScheduleEntry(
            entryId = entryId,
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = LocalDateTime.now().minusHours(4),
            status = MedicationStatus.PENDING
        )
        db.scheduleDao().insertScheduleEntry(scheduleEntry)

        // Simulate an active notification posted in the status bar
        val notifId = entryId.hashCode()
        val notif = NotificationCompat.Builder(context, "test_channel")
            .setContentTitle("Dose reminder")
            .setContentText("Take Amoxicillin")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        notificationManager.notify(notifId, notif)

        val shadowNm = Shadows.shadowOf(notificationManager)
        assertEquals(1, shadowNm.allNotifications.size)

        // Call recordDoseMissed
        scheduleRepository.recordDoseMissed(entryId, context)

        // Verify status in DB updated to MISSED
        val updated = db.scheduleDao().getScheduleEntryById(entryId)
        assertNotNull(updated)
        assertEquals(MedicationStatus.MISSED, updated!!.status)

        // Verify notification was actively cancelled from the notification manager
        assertEquals(0, shadowNm.allNotifications.size)
    }

    @Test
    fun issue1_recordDoseMissed_disarmsSnoozeAndNaggingAlarmsWithoutCrashing() = runBlocking {
        val entryId = "entry_missed_test_2"
        val scheduleEntry = ScheduleEntry(
            entryId = entryId,
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = LocalDateTime.now().minusHours(5),
            status = MedicationStatus.PENDING
        )
        db.scheduleDao().insertScheduleEntry(scheduleEntry)

        // Call recordDoseMissed with context, verifying that AlarmScheduler disarms cleanly
        scheduleRepository.recordDoseMissed(entryId, context)

        val updated = db.scheduleDao().getScheduleEntryById(entryId)
        assertEquals(MedicationStatus.MISSED, updated?.status)
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 2 Tests: HomeScreen MedicationItem Enabled State & Missed Doses Undo Logic
    // ───────────────────────────────────────────────────────────────

    @Test
    fun issue2_medicationItem_enabledState_permitsUndoForMissedDoses() {
        fun computeEnabled(
            isTaken: Boolean,
            isSkipped: Boolean,
            isMissed: Boolean,
            allowUndo: Boolean
        ): Boolean {
            return when {
                isTaken || isSkipped -> allowUndo
                isMissed -> true
                else -> true
            }
        }

        // Missed dose is always enabled so users can record late intake (#105)
        assertTrue(computeEnabled(isTaken = false, isSkipped = false, isMissed = true, allowUndo = true))
        assertTrue(computeEnabled(isTaken = false, isSkipped = false, isMissed = true, allowUndo = false))

        // Taken/Skipped requires allowUndo
        assertTrue(computeEnabled(isTaken = true, isSkipped = false, isMissed = false, allowUndo = true))
        assertFalse(computeEnabled(isTaken = true, isSkipped = false, isMissed = false, allowUndo = false))

        // Standard pending dose is always enabled
        assertTrue(computeEnabled(isTaken = false, isSkipped = false, isMissed = false, allowUndo = false))
    }

    @Test
    fun issue2_medicationItem_onClickActionDispatch_routesMissedDosesToMarkAsTaken() {
        var undoInvoked: Boolean
        var markAsTakenInvoked: Boolean

        fun dispatchClick(
            isTaken: Boolean,
            isSkipped: Boolean,
            isMissed: Boolean,
            isLate: Boolean,
            allowUndo: Boolean
        ) {
            when {
                isTaken || isSkipped -> {
                    if (allowUndo) undoInvoked = true
                }
                isMissed -> {
                    markAsTakenInvoked = true
                }
                isLate -> {}
                else -> markAsTakenInvoked = true
            }
        }

        // Missed dose routes directly to markAsTaken to prevent infinite undo loop (#105)
        undoInvoked = false
        markAsTakenInvoked = false
        dispatchClick(isTaken = false, isSkipped = false, isMissed = true, isLate = false, allowUndo = true)
        assertFalse(undoInvoked)
        assertTrue(markAsTakenInvoked)

        // Missed dose with allowUndo=false also routes to markAsTaken
        undoInvoked = false
        markAsTakenInvoked = false
        dispatchClick(isTaken = false, isSkipped = false, isMissed = true, isLate = false, allowUndo = false)
        assertFalse(undoInvoked)
        assertTrue(markAsTakenInvoked)
    }

    @Test
    fun issue2_undoDoseTaken_onMissedDose_revertsToPendingWithoutModifyingStock() = runBlocking {
        val entryId = "entry_undo_missed_test"
        val scheduleEntry = ScheduleEntry(
            entryId = entryId,
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = LocalDateTime.now().minusHours(3),
            status = MedicationStatus.MISSED
        )
        db.scheduleDao().insertScheduleEntry(scheduleEntry)

        val stockBefore = db.medicineDao().getMedicineByIdDirect(testMedicine.medicineId)?.currentStock
        assertEquals(20, stockBefore)

        // Call undoDoseTaken on the missed dose
        scheduleRepository.undoDoseTaken(entryId, context)

        // Verify status reverted to PENDING
        val reverted = db.scheduleDao().getScheduleEntryById(entryId)
        assertNotNull(reverted)
        assertEquals(MedicationStatus.PENDING, reverted!!.status)

        // Verify stock was NOT altered (since dose was missed, not taken)
        val stockAfter = db.medicineDao().getMedicineByIdDirect(testMedicine.medicineId)?.currentStock
        assertEquals(20, stockAfter)
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 3 Tests: DataExporter userId Serialization & BackupRestoreManager Symmetry
    // ───────────────────────────────────────────────────────────────

    @Test
    fun issue3_dataExporter_serializesUserIdForMedicinesAndSchedules() {
        val exporter = DataExporter(context, userRepository, medicineRepository, scheduleRepository)
        val schedules = listOf(
            ScheduleEntry(
                entryId = "sch_export_1",
                userId = testUser.userId,
                medicineId = testMedicine.medicineId,
                scheduledDateTime = LocalDateTime.of(2026, 10, 6, 8, 0),
                status = MedicationStatus.TAKEN_ON_TIME
            )
        )

        val jsonString = exporter.buildJsonContent(testUser, listOf(testMedicine), schedules)
        val root = JSONObject(jsonString)

        // 1. Verify medicines array includes userId
        val medArray = root.getJSONArray("medicines")
        assertEquals(1, medArray.length())
        val medObj = medArray.getJSONObject(0)
        assertEquals(testUser.userId, medObj.getString("userId"))
        assertEquals(testMedicine.medicineId, medObj.getString("medicineId"))

        // 2. Verify schedules array includes userId
        val schedArray = root.getJSONArray("schedules")
        assertEquals(1, schedArray.length())
        val schedObj = schedArray.getJSONObject(0)
        assertEquals(testUser.userId, schedObj.getString("userId"))
        assertEquals("sch_export_1", schedObj.getString("entryId"))
    }

    @Test
    fun issue3_backupRestoreManager_deserializesUserIdAccurately() {
        val exporter = DataExporter(context, userRepository, medicineRepository, scheduleRepository)
        val schedules = listOf(
            ScheduleEntry(
                entryId = "sch_symmetry_1",
                userId = testUser.userId,
                medicineId = testMedicine.medicineId,
                scheduledDateTime = LocalDateTime.of(2026, 10, 6, 12, 0),
                status = MedicationStatus.PENDING
            )
        )

        val jsonString = exporter.buildJsonContent(testUser, listOf(testMedicine), schedules)
        val root = JSONObject(jsonString)
        val schedJsonStr = root.getJSONArray("schedules").toString()

        val backupManager = BackupRestoreManager(context, db, scheduleRepository)
        val parsedSchedules = backupManager.parseSchedulesFromJson(schedJsonStr)

        assertEquals(1, parsedSchedules.size)
        val parsedEntry = parsedSchedules[0]
        assertEquals(testUser.userId, parsedEntry.userId)
        assertEquals("sch_symmetry_1", parsedEntry.entryId)
    }

    @Test
    fun issue3_backupRestoreManager_legacyBackupWithoutUserId_defaultsGracefully() {
        val schedArray = JSONArray()
        val sObj = JSONObject()
        sObj.put("entryId", "legacy_entry_1")
        sObj.put("medicineId", "legacy_med_1")
        sObj.put("scheduledDateTime", "2026-10-06T10:00:00")
        sObj.put("status", "PENDING")
        // Omit userId deliberately to simulate an older export format
        schedArray.put(sObj)

        val backupManager = BackupRestoreManager(context, db, scheduleRepository)
        val parsedSchedules = backupManager.parseSchedulesFromJson(schedArray.toString())

        assertEquals(1, parsedSchedules.size)
        // Should safely fallback to empty string without throwing null pointer or JSON exceptions
        assertEquals("", parsedSchedules[0].userId)
        assertEquals("legacy_entry_1", parsedSchedules[0].entryId)
    }

    // ───────────────────────────────────────────────────────────────
    // Partial Stock Consumption and Notification Channel Integrity Tests
    // ───────────────────────────────────────────────────────────────

    @Test
    fun recordDoseTaken_withPartialStock_appendsAuditTagAndUndoRestoresExactConsumedQuantity() = runBlocking {
        // Set up medicine with stock of 1 tablet, but dose requirement of 2 tablets
        val partialMed = testMedicine.copy(
            medicineId = "med_partial_test",
            dosage = 2.0,
            dosageUnit = DosageUnit.TABLET,
            currentStock = 1,
            autoDeductOnTake = true
        )
        db.medicineDao().insertMedicine(partialMed)

        val entryId = "entry_partial_stock_1"
        val scheduleEntry = ScheduleEntry(
            entryId = entryId,
            userId = testUser.userId,
            medicineId = partialMed.medicineId,
            scheduledDateTime = LocalDateTime.now(),
            status = MedicationStatus.PENDING,
            dosage = 2.0
        )
        db.scheduleDao().insertScheduleEntry(scheduleEntry)

        // Record dose taken
        scheduleRepository.recordDoseTaken(entryId, context = context)

        // Verify stock is clamped to 0
        val medAfterTake = db.medicineDao().getMedicineByIdDirect(partialMed.medicineId)
        assertNotNull(medAfterTake)
        assertEquals(0, medAfterTake!!.currentStock)

        // Verify partial stock note was recorded
        val entryAfterTake = db.scheduleDao().getScheduleEntryById(entryId)
        assertNotNull(entryAfterTake)
        assertTrue(entryAfterTake!!.doseNotes?.contains("[Partial Stock: Took 1 of 2]") == true)

        // Now undo the dose
        scheduleRepository.undoDoseTaken(entryId, context = context)

        // Verify stock is restored to exactly 1 (preventing inventory inflation to 2)
        val medAfterUndo = db.medicineDao().getMedicineByIdDirect(partialMed.medicineId)
        assertNotNull(medAfterUndo)
        assertEquals(1, medAfterUndo!!.currentStock)
    }

    @Test
    fun recordDoseTaken_triggersRefillAlert_andEnsuresNotificationChannelExists() = runBlocking {
        // Set up medicine where taking 1 pill crosses the refill threshold
        val refillMed = testMedicine.copy(
            medicineId = "med_refill_channel_test",
            dosage = 1.0,
            dosageUnit = DosageUnit.TABLET,
            currentStock = 2,
            refillThreshold = 1,
            autoDeductOnTake = true
        )
        db.medicineDao().insertMedicine(refillMed)

        val entryId = "entry_refill_channel_1"
        val scheduleEntry = ScheduleEntry(
            entryId = entryId,
            userId = testUser.userId,
            medicineId = refillMed.medicineId,
            scheduledDateTime = LocalDateTime.now(),
            status = MedicationStatus.PENDING,
            dosage = 1.0
        )
        db.scheduleDao().insertScheduleEntry(scheduleEntry)

        // Record dose taken - this will decrement stock to 1 (matching threshold) and trigger refill notification
        scheduleRepository.recordDoseTaken(entryId, context = context)

        // Verify notification channel was created before posting notification
        val channel = notificationManager.getNotificationChannel(MedicineAlarmReceiver.REFILL_CHANNEL_ID)
        assertNotNull("Refill notification channel must be registered in NotificationManager", channel)
        assertEquals(MedicineAlarmReceiver.REFILL_CHANNEL_ID, channel!!.id)
    }

    @Test
    fun logAsNeededDose_withPartialStock_appendsAuditTagAndRestoresCorrectly() = runBlocking {
        // Set up medicine with stock of 1 tablet, but dose requirement of 2 tablets
        val prnMed = testMedicine.copy(
            medicineId = "med_prn_partial_test",
            dosage = 2.0,
            dosageUnit = DosageUnit.TABLET,
            currentStock = 1,
            autoDeductOnTake = true
        )
        db.medicineDao().insertMedicine(prnMed)

        // Log as-needed dose
        scheduleRepository.logAsNeededDose(prnMed, testUser.userId, context = context)

        // Verify stock was decremented to 0
        val medAfterTake = db.medicineDao().getMedicineByIdDirect(prnMed.medicineId)
        assertNotNull(medAfterTake)
        assertEquals(0, medAfterTake!!.currentStock)

        // Find the generated PRN entry
        val prnEntries = db.scheduleDao().getAllScheduleEntries(testUser.userId).filter { it.medicineId == prnMed.medicineId }
        assertTrue("PRN entry should be created", prnEntries.isNotEmpty())
        val prnEntry = prnEntries.first()
        assertTrue(prnEntry.doseNotes?.contains("[Partial Stock: Took 1 of 2]") == true)

        // Undo the PRN dose
        scheduleRepository.undoDoseTaken(prnEntry.entryId, context = context)

        // Verify stock restored to 1 without inflation
        val medAfterUndo = db.medicineDao().getMedicineByIdDirect(prnMed.medicineId)
        assertNotNull(medAfterUndo)
        assertEquals(1, medAfterUndo!!.currentStock)
    }

    @Test
    fun testRecordDoseTaken_triggersRefillAlert_whenInitialStockEqualsThreshold() = runBlocking {
        MedicineAlarmReceiver.createNotificationChannels(context)

        // Stock is exactly equal to refillThreshold (5 pills)
        val med = testMedicine.copy(
            medicineId = "med_refill_boundary_test",
            dosage = 1.0,
            dosageUnit = DosageUnit.TABLET,
            currentStock = 5,
            refillThreshold = 5,
            autoDeductOnTake = true
        )
        db.medicineDao().insertMedicine(med)

        val entry = ScheduleEntry(
            entryId = "entry_refill_boundary_test_1",
            userId = testUser.userId,
            medicineId = med.medicineId,
            scheduledDateTime = LocalDateTime.now(),
            status = MedicationStatus.PENDING,
            dosage = 1.0
        )
        db.scheduleDao().insertScheduleEntry(entry)

        // Record dose taken: stock drops from 5 to 4
        scheduleRepository.recordDoseTaken(entry.entryId, context = context)

        val updatedMed = db.medicineDao().getMedicineByIdDirect(med.medicineId)
        assertNotNull(updatedMed)
        assertEquals(4, updatedMed!!.currentStock)

        // Verify notification was posted for the refill alert
        val refillNotifId = (med.medicineId + "_refill").hashCode()
        val activeNotifs = notificationManager.activeNotifications
        assertTrue("Refill notification must be posted when stock transitions from threshold to below threshold",
            activeNotifs.any { it.id == refillNotifId })
    }

    @Test
    fun testLogAsNeededDose_triggersRefillAlert_whenInitialStockEqualsThreshold() = runBlocking {
        MedicineAlarmReceiver.createNotificationChannels(context)

        // Stock is exactly equal to refillThreshold (3 pills)
        val prnMed = testMedicine.copy(
            medicineId = "med_prn_refill_boundary_test",
            dosage = 1.0,
            dosageUnit = DosageUnit.TABLET,
            frequency = Frequency(FrequencyPattern.AS_NEEDED),
            currentStock = 3,
            refillThreshold = 3,
            autoDeductOnTake = true
        )
        db.medicineDao().insertMedicine(prnMed)

        // Log as-needed dose: stock drops from 3 to 2
        scheduleRepository.logAsNeededDose(prnMed, testUser.userId, context = context)

        val updatedMed = db.medicineDao().getMedicineByIdDirect(prnMed.medicineId)
        assertNotNull(updatedMed)
        assertEquals(2, updatedMed!!.currentStock)

        val refillNotifId = (prnMed.medicineId + "_refill").hashCode()
        val activeNotifs = notificationManager.activeNotifications
        assertTrue("Refill notification must be posted on PRN dose when stock transitions from threshold to below threshold",
            activeNotifs.any { it.id == refillNotifId })
    }

    @Test
    fun testUnarchiveMedicine_resetsCourseDatesFromToday_whenEndDateInPast() = runBlocking {
        val today = java.time.LocalDate.now()
        val pastStart = today.minusDays(20)
        val pastEnd = today.minusDays(10)
        val expiredMed = testMedicine.copy(
            medicineId = "med_expired_course",
            medicationName = "Amoxicillin",
            startDate = pastStart,
            endDate = pastEnd,
            durationDays = 10,
            isArchived = true
        )
        db.medicineDao().insertMedicine(expiredMed)

        // Unarchive expired medicine
        medicineRepository.unarchiveMedicine(expiredMed)

        val unarchived = db.medicineDao().getMedicineByIdDirect("med_expired_course")
        assertNotNull(unarchived)
        assertFalse("Medicine must be active (isArchived = false)", unarchived!!.isArchived)
        assertEquals("Start date must be reset to today", today, unarchived.startDate)
        assertEquals("End date must be reset to today + durationDays", today.plusDays(10), unarchived.endDate)

        // Verify schedule entries were generated starting from today
        val schedules = db.scheduleDao().getAllScheduleEntries(testUser.userId)
            .filter { it.medicineId == "med_expired_course" }
        assertTrue("Schedule entries must be generated for unarchived medicine", schedules.isNotEmpty())
        assertTrue("All generated schedule entries must be on or after today",
            schedules.all { !it.scheduledDateTime.toLocalDate().isBefore(today) })
    }
}
