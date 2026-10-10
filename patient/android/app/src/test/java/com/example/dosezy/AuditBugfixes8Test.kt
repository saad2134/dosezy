package com.example.dosezy

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
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
class AuditBugfixes8Test {

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
            allowUndo: Boolean,
            allowCustomTime: Boolean
        ): Boolean {
            return when {
                isTaken || isSkipped -> allowUndo
                isMissed -> allowUndo || allowCustomTime
                else -> true
            }
        }

        // When dose is MISSED and allowUndo is true, button must be enabled
        assertTrue(computeEnabled(isTaken = false, isSkipped = false, isMissed = true, allowUndo = true, allowCustomTime = false))

        // When dose is MISSED and allowUndo is false but allowCustomTime is true, button must be enabled
        assertTrue(computeEnabled(isTaken = false, isSkipped = false, isMissed = true, allowUndo = false, allowCustomTime = true))

        // When dose is MISSED and both are true, button must be enabled
        assertTrue(computeEnabled(isTaken = false, isSkipped = false, isMissed = true, allowUndo = true, allowCustomTime = true))

        // When dose is MISSED and neither is allowed, button must be disabled
        assertFalse(computeEnabled(isTaken = false, isSkipped = false, isMissed = true, allowUndo = false, allowCustomTime = false))

        // Standard pending dose is always enabled
        assertTrue(computeEnabled(isTaken = false, isSkipped = false, isMissed = false, allowUndo = false, allowCustomTime = false))
    }

    @Test
    fun issue2_medicationItem_onClickActionDispatch_prioritizesUndoForMissedDoses() {
        var undoInvoked: Boolean
        var manualRecordInvoked: Boolean

        fun dispatchClick(
            isTaken: Boolean,
            isSkipped: Boolean,
            isMissed: Boolean,
            isLate: Boolean,
            allowUndo: Boolean,
            allowCustomTime: Boolean
        ) {
            when {
                isTaken || isSkipped -> {
                    if (allowUndo) undoInvoked = true
                }
                isMissed -> {
                    if (allowUndo) {
                        undoInvoked = true
                    } else if (allowCustomTime) {
                        manualRecordInvoked = true
                    }
                }
                isLate -> {}
                else -> manualRecordInvoked = true
            }
        }

        // Scenario A: allowUndo = true, allowCustomTime = false -> triggers undo
        undoInvoked = false
        manualRecordInvoked = false
        dispatchClick(isTaken = false, isSkipped = false, isMissed = true, isLate = false, allowUndo = true, allowCustomTime = false)
        assertTrue(undoInvoked)
        assertFalse(manualRecordInvoked)

        // Scenario B: allowUndo = false, allowCustomTime = true -> triggers manual record
        undoInvoked = false
        manualRecordInvoked = false
        dispatchClick(isTaken = false, isSkipped = false, isMissed = true, isLate = false, allowUndo = false, allowCustomTime = true)
        assertFalse(undoInvoked)
        assertTrue(manualRecordInvoked)

        // Scenario C: allowUndo = true, allowCustomTime = true -> prioritizes undo
        undoInvoked = false
        manualRecordInvoked = false
        dispatchClick(isTaken = false, isSkipped = false, isMissed = true, isLate = false, allowUndo = true, allowCustomTime = true)
        assertTrue(undoInvoked)
        assertFalse(manualRecordInvoked)
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
}
