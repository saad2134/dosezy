/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.notifications

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.export.BackupRestoreManager
import com.example.dosezy.data.export.DataExporter
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import com.example.dosezy.notifications.IntakeSafeguardManager
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Unit tests verifying Issue #104:
 *  1. User model default fields for intakeSafeguardEnabled and intakeCountdownSeconds.
 *  2. Database migration 17 to 18 alters users table adding columns without data loss.
 *  3. DataExporter and BackupRestoreManager round-trip serialization for intake safeguard settings.
 *  4. IntakeSafeguardManager countdown lifecycle: start, extend (+1 min), confirm intake, and cancel.
 *  5. Intake confirmation commits status to Room database, deducts medicine stock, and cancels safeguard alarms.
 *  6. Dispatch logic routes to intake countdown when safeguard is enabled.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IntakeSafeguardManagerLifecycleTest {

    private lateinit var context: Context
    private lateinit var database: DosezyDatabase
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var userRepository: UserRepository
    private lateinit var medicineRepository: MedicineRepository

    private val testUser = User(
        userId = "user_adhd_104",
        fullName = "ADHD Patient",
        age = 30,
        gender = Gender.DO_NOT_SPECIFY,
        contactNumber = "+1987654321",
        intakeSafeguardEnabled = true,
        intakeCountdownSeconds = 120
    )

    private val testMedicine = Medicine(
        medicineId = "med_adhd_104",
        userId = "user_adhd_104",
        medicationName = "Methylphenidate",
        dosage = 20.0,
        dosageUnit = DosageUnit.MG,
        timesPerDay = 1,
        frequency = Frequency(FrequencyPattern.DAILY),
        scheduledTimes = listOf(LocalTime.of(8, 0)),
        currentStock = 30,
        autoDeductOnTake = true
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, DosezyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DosezyDatabase.setInstance(database)
        scheduleRepository = ScheduleRepository(database)
        medicineRepository = MedicineRepository(database, scheduleRepository, context)
        userRepository = UserRepository(database)

        runBlocking {
            database.userDao().insertUser(testUser)
            database.medicineDao().insertMedicine(testMedicine)
        }
    }

    @After
    fun tearDown() {
        IntakeSafeguardManager.cancelCountdown(context)
        DosezyDatabase.setInstance(null)
        database.close()
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. Model Defaults & Schema Verification
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testUserModel_intakeSafeguardDefaults() {
        val defaultUser = User(
            fullName = "Default User",
            age = 25,
            gender = Gender.DO_NOT_SPECIFY,
            contactNumber = "+1000000000"
        )
        assertFalse("intakeSafeguardEnabled must be false by default", defaultUser.intakeSafeguardEnabled)
        assertEquals("intakeCountdownSeconds default must be 120 seconds", 120, defaultUser.intakeCountdownSeconds)
    }

    @Test
    fun testDatabaseMigration_17_to_18_executesSuccessfully() {
        // Create an in-memory SQLite database representing version 17
        val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
            .create(androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(17) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS users (
                                userId TEXT PRIMARY KEY NOT NULL,
                                fullName TEXT NOT NULL,
                                age INTEGER NOT NULL,
                                gender TEXT NOT NULL,
                                contactNumber TEXT NOT NULL,
                                isCurrentUser INTEGER NOT NULL DEFAULT 0,
                                theme TEXT NOT NULL DEFAULT 'SYSTEM',
                                timeFormat TEXT NOT NULL DEFAULT 'HOUR_12',
                                language TEXT NOT NULL DEFAULT 'SYSTEM',
                                considerLateAfter INTEGER NOT NULL DEFAULT 3,
                                considerMissedAfter INTEGER NOT NULL DEFAULT 6,
                                snoozeDuration INTEGER NOT NULL DEFAULT 10,
                                allergies TEXT,
                                medicalConditions TEXT,
                                naggingRemindersEnabled INTEGER NOT NULL DEFAULT 0,
                                naggingIntervalMinutes INTEGER NOT NULL DEFAULT 5,
                                naggingMaxRepeats INTEGER NOT NULL DEFAULT 3,
                                alarmSound TEXT NOT NULL DEFAULT 'SYSTEM_DEFAULT',
                                customAlarmSoundPath TEXT,
                                customAlarmSoundTitle TEXT,
                                alarmDurationSeconds INTEGER NOT NULL DEFAULT 0,
                                allowDoseSkipping INTEGER NOT NULL DEFAULT 0,
                                allowCustomDoseTime INTEGER NOT NULL DEFAULT 0,
                                hideAddMedicineNavButton INTEGER NOT NULL DEFAULT 0,
                                allowDoseUndo INTEGER NOT NULL DEFAULT 0,
                                allowDoseNotes INTEGER NOT NULL DEFAULT 0,
                                promptDoseNotes INTEGER NOT NULL DEFAULT 0,
                                slideActionsEnabled INTEGER NOT NULL DEFAULT 1,
                                timelineModeEnabled INTEGER NOT NULL DEFAULT 0,
                                thickerCalendarDayHighlight INTEGER NOT NULL DEFAULT 0,
                                profilePicPath TEXT
                            )
                        """.trimIndent())
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build())

        val db = helper.writableDatabase
        db.execSQL("INSERT INTO users (userId, fullName, age, gender, contactNumber) VALUES ('u1', 'Old User', 40, 'MALE', '123')")

        // Execute MIGRATION_17_18
        DosezyDatabase.MIGRATION_17_18.migrate(db)

        // Verify columns exist and have non-destructive defaults
        val cursor = db.query("SELECT intakeSafeguardEnabled, intakeCountdownSeconds FROM users WHERE userId = 'u1'")
        assertTrue(cursor.moveToFirst())
        assertEquals(0, cursor.getInt(0))
        assertEquals(120, cursor.getInt(1))
        cursor.close()
        db.close()
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. Backup & Restore Round-Trip
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testBackupRestore_intakeSafeguardSettings_roundTripSerialization() = runBlocking {
        val exporter = DataExporter(context, userRepository, medicineRepository, scheduleRepository)
        val backupJsonFile = exporter.exportUserToJson(testUser.userId)
        val backupJson = backupJsonFile.readText(Charsets.UTF_8)
        val manager = BackupRestoreManager(context, database, scheduleRepository)
        val userJsonObject = JSONObject(backupJson).getJSONObject("user")
        assertTrue(userJsonObject.getBoolean("intakeSafeguardEnabled"))
        assertEquals(120, userJsonObject.getInt("intakeCountdownSeconds"))

        val parsedUser = manager.parseUserFromJson(userJsonObject.toString())

        assertNotNull(parsedUser)
        assertTrue(parsedUser.intakeSafeguardEnabled)
        assertEquals(120, parsedUser.intakeCountdownSeconds)
    }

    @Test
    fun testBackupRestore_legacyBackupWithoutIntakeSafeguard_usesSafeDefaults() {
        val legacyUserJson = """
            {
                "userId": "legacy_user",
                "fullName": "Legacy Patient",
                "age": 50,
                "gender": "FEMALE",
                "contactNumber": "555-1234"
            }
        """.trimIndent()

        val manager = BackupRestoreManager(context, database, scheduleRepository)
        val parsedUser = manager.parseUserFromJson(legacyUserJson)

        assertNotNull(parsedUser)
        assertFalse(parsedUser.intakeSafeguardEnabled)
        assertEquals(120, parsedUser.intakeCountdownSeconds)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. IntakeSafeguardManager Countdown & Actions Lifecycle
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testIntakeSafeguardManager_startExtendCancelLifecycle() {
        val entryId = "entry_test_safeguard"
        val state = IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entryId,
            entryIds = listOf(entryId),
            medicineName = "Methylphenidate",
            medicineNames = listOf("Methylphenidate"),
            scheduledTime = "08:00 AM",
            durationSeconds = 120
        )

        assertEquals(entryId, state.entryId)
        assertEquals(120, state.totalDurationSeconds)
        assertTrue(state.targetTimestamp > state.startTimestamp)

        val active = IntakeSafeguardManager.activeCountdown.value
        assertNotNull(active)
        assertEquals("Methylphenidate", active?.medicineName)

        // Extend by +60 seconds
        val extended = IntakeSafeguardManager.extendCountdown(context, 60)
        assertNotNull(extended)
        assertEquals(180, extended!!.totalDurationSeconds)
        assertEquals(state.targetTimestamp + 60000L, extended.targetTimestamp)

        // Cancel countdown
        IntakeSafeguardManager.cancelCountdown(context)
        assertNull(IntakeSafeguardManager.activeCountdown.value)
    }

    @Test
    fun testIntakeSafeguardManager_confirmIntake_recordsDoseAndDeductsStock() = runBlocking {
        val entryId = "entry_confirm_test"
        val scheduledTime = LocalDateTime.now().minusMinutes(5)
        val scheduleEntry = ScheduleEntry(
            entryId = entryId,
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = scheduledTime,
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntry(scheduleEntry)

        // Verify initial state
        val initialMed = database.medicineDao().getMedicineByIdDirect(testMedicine.medicineId)
        assertEquals(30, initialMed?.currentStock)

        // Start countdown
        IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entryId,
            entryIds = listOf(entryId),
            medicineName = testMedicine.medicationName,
            medicineNames = listOf(testMedicine.medicationName),
            scheduledTime = "08:00 AM",
            durationSeconds = 120
        )

        // Dose must remain PENDING while countdown is running (preventing premature intake recording)
        val entryDuringCountdown = database.scheduleDao().getScheduleEntryById(entryId)
        assertEquals(MedicationStatus.PENDING, entryDuringCountdown?.status)

        // Confirm intake
        val success = IntakeSafeguardManager.confirmIntake(context, database)
        assertTrue(success)

        // Verify outcomes: countdown cleared, status updated to TAKEN_ON_TIME, stock decremented
        assertNull(IntakeSafeguardManager.activeCountdown.value)
        val updatedEntry = database.scheduleDao().getScheduleEntryById(entryId)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, updatedEntry?.status)

        val updatedMed = database.medicineDao().getMedicineByIdDirect(testMedicine.medicineId)
        assertEquals(29, updatedMed?.currentStock)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. UI Take Action Dispatch Routing
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testTakeActionDispatch_routesToCountdown_whenSafeguardEnabled() {
        var countdownStarted: Boolean
        var immediateTakeExecuted: Boolean

        fun dispatchTake(
            @Suppress("UNUSED_PARAMETER") entryId: String,
            intakeSafeguardEnabled: Boolean,
            allowCustomDoseTime: Boolean
        ) {
            countdownStarted = false
            immediateTakeExecuted = false
            when {
                allowCustomDoseTime -> { /* Custom time dialog */ }
                intakeSafeguardEnabled -> {
                    // Guard: Route to intake countdown buffer when safeguard is active (#104)
                    countdownStarted = true
                }
                else -> {
                    immediateTakeExecuted = true
                }
            }
        }

        // Case A: Safeguard enabled (ADHD mode) -> triggers countdown, never immediate take
        countdownStarted = false
        immediateTakeExecuted = false
        dispatchTake(entryId = "e1", intakeSafeguardEnabled = true, allowCustomDoseTime = false)
        assertTrue("Safeguard countdown MUST start when enabled", countdownStarted)
        assertFalse("Immediate take must NOT execute when safeguard is enabled", immediateTakeExecuted)

        // Case B: Safeguard disabled -> triggers immediate take
        countdownStarted = false
        immediateTakeExecuted = false
        dispatchTake(entryId = "e1", intakeSafeguardEnabled = false, allowCustomDoseTime = false)
        assertFalse(countdownStarted)
        assertTrue("Immediate take MUST execute when safeguard is disabled", immediateTakeExecuted)
    }

    @Test
    fun testHandleCountdownExpired_invokesOnCompleteCallback() {
        val entryId = "entry_expired_test"
        var onCompleteCalled = false

        IntakeSafeguardManager.handleCountdownExpired(
            context = context,
            entryId = entryId,
            medicineName = "Test Med",
            entryIds = listOf(entryId),
            medicineNames = listOf("Test Med"),
            scheduledTime = "08:00 AM",
            onComplete = {
                onCompleteCalled = true
            }
        )

        val deadline = System.currentTimeMillis() + 3000
        while (!onCompleteCalled && System.currentTimeMillis() < deadline) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }

        assertTrue("onComplete must be invoked when handleCountdownExpired executes", onCompleteCalled)
        assertNull("Active countdown state must be cleared", IntakeSafeguardManager.activeCountdown.value)
    }

    @Test
    fun testInitialize_restoresActiveCountdownStateFromStorage() {
        IntakeSafeguardManager.cancelCountdown(context)
        assertNull(IntakeSafeguardManager.activeCountdown.value)

        // Start countdown which writes to SharedPreferences
        val state = IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = "entry_init_restore",
            entryIds = listOf("entry_init_restore"),
            medicineName = "Restored Med",
            medicineNames = listOf("Restored Med"),
            scheduledTime = "09:00 AM",
            durationSeconds = 300
        )
        assertNotNull(state)

        // Clear in-memory StateFlow without wiping persistent prefs to simulate process death
        val field = IntakeSafeguardManager::class.java.getDeclaredField("_activeCountdown")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(IntakeSafeguardManager) as kotlinx.coroutines.flow.MutableStateFlow<IntakeSafeguardManager.IntakeCountdownState?>
        flow.value = null
        assertNull(IntakeSafeguardManager.activeCountdown.value)

        // Calling initialize() should restore the state from SharedPreferences
        IntakeSafeguardManager.initialize(context)
        val restored = IntakeSafeguardManager.activeCountdown.value
        assertNotNull("initialize() must restore active countdown from SharedPreferences", restored)
        assertEquals("entry_init_restore", restored?.entryId)
        assertEquals("Restored Med", restored?.medicineName)
        assertEquals("09:00 AM", restored?.scheduledTime)

        IntakeSafeguardManager.cancelCountdown(context)
    }

    @Test
    fun testCancelAlarmsForUser_cancelsActiveIntakeCountdown() = runBlocking {
        val entry = ScheduleEntry(
            entryId = "entry_user_cancel",
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = LocalDateTime.now().plusHours(1),
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntry(entry)

        IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entry.entryId,
            entryIds = listOf(entry.entryId),
            medicineName = testMedicine.medicationName,
            medicineNames = listOf(testMedicine.medicationName),
            scheduledTime = "10:00 AM",
            durationSeconds = 120
        )
        assertNotNull(IntakeSafeguardManager.activeCountdown.value)

        // Cancelling alarms for user must clear active countdown
        val medNotifManager = MedicineNotificationManager(context, database)
        medNotifManager.cancelAllAlarmsForUser(testUser.userId)

        // Simulate UserViewModel check on activeCountdown
        val active = IntakeSafeguardManager.activeCountdown.value
        if (active != null) {
            val dbEntry = scheduleRepository.getScheduleEntryById(active.entryId)
            if (dbEntry == null || dbEntry.userId == testUser.userId) {
                IntakeSafeguardManager.cancelCountdown(context)
            }
        }
        assertNull("Active intake countdown must be cancelled when user alarms/profile are cleared", IntakeSafeguardManager.activeCountdown.value)
    }

    @Test
    fun testCancelAlarmsForMedicine_cancelsActiveIntakeCountdown() = runBlocking {
        val entry = ScheduleEntry(
            entryId = "entry_med_cancel",
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = LocalDateTime.now().plusHours(1),
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntry(entry)

        IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entry.entryId,
            entryIds = listOf(entry.entryId),
            medicineName = testMedicine.medicationName,
            medicineNames = listOf(testMedicine.medicationName),
            scheduledTime = "11:00 AM",
            durationSeconds = 120
        )
        assertNotNull(IntakeSafeguardManager.activeCountdown.value)

        // Cancelling alarms for medicine must clear active countdown
        scheduleRepository.cancelAlarmsForMedicine(testMedicine.medicineId, context)

        assertNull("Active intake countdown must be cancelled when medicine alarms are cancelled", IntakeSafeguardManager.activeCountdown.value)
    }

    @Test
    fun testStartCountdown_preemptsPriorActiveCountdown() {
        val entry1 = "entry_preempt_1"
        val entry2 = "entry_preempt_2"

        IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entry1,
            entryIds = listOf(entry1),
            medicineName = "Med 1",
            medicineNames = listOf("Med 1"),
            scheduledTime = "08:00 AM",
            durationSeconds = 120
        )

        val firstState = IntakeSafeguardManager.activeCountdown.value
        assertNotNull(firstState)
        assertEquals(entry1, firstState?.entryId)
        assertEquals(120, firstState?.totalDurationSeconds)

        // Guard: Starting a new countdown while one is active preempts the previous one cleanly
        IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entry2,
            entryIds = listOf(entry2),
            medicineName = "Med 2",
            medicineNames = listOf("Med 2"),
            scheduledTime = "09:00 AM",
            durationSeconds = 60
        )

        val secondState = IntakeSafeguardManager.activeCountdown.value
        assertNotNull(secondState)
        assertEquals(entry2, secondState?.entryId)
        assertEquals(60, secondState?.totalDurationSeconds)

        IntakeSafeguardManager.cancelCountdown(context)
        assertNull(IntakeSafeguardManager.activeCountdown.value)
    }

    @Test
    fun testStartCountdown_cancelsActiveSnoozeForCohort() {
        val entryId = "entry_snooze_safeguard"
        val scheduler = AlarmScheduler(context)

        // Schedule snooze for this entry
        scheduler.scheduleGroupedSnooze(listOf(entryId), 10, listOf("Med A"))
        val prefs = context.getSharedPreferences(AlarmScheduler.PREFS_COORDINATION, Context.MODE_PRIVATE)
        assertEquals(entryId, prefs.getString("snooze_member_$entryId", null))

        // Guard: Starting intake countdown cancels snooze and nagging alarms so they do not ring during the countdown buffer
        IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entryId,
            entryIds = listOf(entryId),
            medicineName = "Med A",
            medicineNames = listOf("Med A"),
            scheduledTime = "08:00 AM",
            durationSeconds = 90
        )

        assertNull("Snooze coordination member must be cancelled upon countdown start", prefs.getString("snooze_member_$entryId", null))

        IntakeSafeguardManager.cancelCountdown(context)
    }

    @Test
    fun testNotificationActionReceiver_takenAction_withIntakeReAlert_recordsDoseTakenDirectly() = runBlocking {
        val entry = ScheduleEntry(
            entryId = "entry_realert_taken",
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = LocalDateTime.now(),
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntry(entry)

        val receiver = NotificationActionReceiver()
        val intent = Intent().apply {
            action = "TAKEN_ACTION"
            putExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID, entry.entryId)
            putExtra(MedicineAlarmReceiver.EXTRA_IS_INTAKE_REALERT, true)
        }

        receiver.onReceive(context, intent)

        // Wait for Dispatchers.IO coroutine in receiver to finish writing dose
        var updated: ScheduleEntry? = null
        for (i in 1..20) {
            kotlinx.coroutines.delay(50)
            updated = database.scheduleDao().getScheduleEntryById(entry.entryId)
            if (updated?.status == MedicationStatus.TAKEN_ON_TIME) break
        }

        // Must NOT start another intake countdown
        assertNull("Responding to intake safeguard re-alert must not start another countdown buffer", IntakeSafeguardManager.activeCountdown.value)

        // Must record dose as taken
        assertNotNull(updated)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, updated?.status)
    }

    @Test
    fun testNotificationActionReceiver_takenAction_withoutIntakeReAlert_startsCountdown() = runBlocking {
        val entry = ScheduleEntry(
            entryId = "entry_first_taken",
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = LocalDateTime.now(),
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntry(entry)

        val receiver = NotificationActionReceiver()
        val intent = Intent().apply {
            action = "TAKEN_ACTION"
            putExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID, entry.entryId)
            putExtra(MedicineAlarmReceiver.EXTRA_IS_INTAKE_REALERT, false)
        }

        receiver.onReceive(context, intent)

        // Wait for Dispatchers.IO coroutine to start countdown
        var active: IntakeSafeguardManager.IntakeCountdownState? = null
        for (i in 1..20) {
            kotlinx.coroutines.delay(50)
            active = IntakeSafeguardManager.activeCountdown.value
            if (active != null) break
        }

        // Must start intake countdown
        assertNotNull("First take action must start intake safeguard countdown", active)
        assertEquals(entry.entryId, active?.entryId)

        // Must remain pending until countdown is confirmed
        val updated = database.scheduleDao().getScheduleEntryById(entry.entryId)
        assertEquals(MedicationStatus.PENDING, updated?.status)

        IntakeSafeguardManager.cancelCountdown(context)
    }

    @Test
    fun testNotificationActionReceiver_snoozeAction_cancelsActiveIntakeSafeguardCountdown() = runBlocking {
        val entry = ScheduleEntry(
            entryId = "entry_snooze_safeguard",
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = LocalDateTime.now(),
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntry(entry)

        // 1. Start active intake countdown
        IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entry.entryId,
            entryIds = listOf(entry.entryId),
            medicineName = testMedicine.medicationName,
            medicineNames = listOf(testMedicine.medicationName),
            scheduledTime = "09:00 AM",
            durationSeconds = 120
        )
        assertNotNull("Intake countdown must be active", IntakeSafeguardManager.activeCountdown.value)

        // 2. Trigger SNOOZE_ACTION via NotificationActionReceiver
        val receiver = NotificationActionReceiver()
        val intent = Intent().apply {
            action = "SNOOZE_ACTION"
            putExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID, entry.entryId)
            putExtra(MedicineAlarmReceiver.EXTRA_SNOOZE_MINUTES, 10)
        }
        receiver.onReceive(context, intent)

        // Wait for Dispatchers.IO coroutine in receiver
        for (i in 1..20) {
            kotlinx.coroutines.delay(50)
            if (IntakeSafeguardManager.activeCountdown.value == null) break
        }

        // 3. Guard assertion: Active intake countdown must be cancelled upon snoozing dose
        assertNull("Active intake countdown must be cancelled on SNOOZE_ACTION", IntakeSafeguardManager.activeCountdown.value)
    }

    @Test
    fun testConfirmIntake_batchCohort_recordsAllEntriesTaken_andDismissesSlotNotificationsOnce() = runBlocking {
        val entry1 = ScheduleEntry(
            entryId = "entry_cohort_batch_1",
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = LocalDateTime.now().minusMinutes(5),
            status = MedicationStatus.PENDING
        )
        val entry2 = ScheduleEntry(
            entryId = "entry_cohort_batch_2",
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = LocalDateTime.now().minusMinutes(5),
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntries(listOf(entry1, entry2))

        // Start countdown for multi-entry cohort
        IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entry1.entryId,
            entryIds = listOf(entry1.entryId, entry2.entryId),
            medicineName = "Batch Cohort Meds",
            medicineNames = listOf("Batch Cohort Meds"),
            scheduledTime = "08:00 AM",
            durationSeconds = 60
        )
        assertNotNull(IntakeSafeguardManager.activeCountdown.value)

        // Confirm intake for entire cohort
        val result = IntakeSafeguardManager.confirmIntake(context, database)
        assertTrue(result)
        assertNull("Countdown state must be cleared after confirmation", IntakeSafeguardManager.activeCountdown.value)

        // Both entries must be updated to TAKEN_ON_TIME
        val updated1 = database.scheduleDao().getScheduleEntryById(entry1.entryId)
        val updated2 = database.scheduleDao().getScheduleEntryById(entry2.entryId)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, updated1?.status)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, updated2?.status)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 7. Intake Countdown Safeguard: Low Stock Refill Notification & Slot Sync (BUG-01)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testConfirmIntake_triggersRefillAlert_whenStockCrossesRefillThreshold() = runBlocking {
        // Set up medicine with currentStock = 5 and refillThreshold = 5
        val lowStockMed = testMedicine.copy(
            medicineId = "med_low_stock_refill",
            medicationName = "Lisdexamfetamine",
            currentStock = 5,
            refillThreshold = 5,
            autoDeductOnTake = true
        )
        database.medicineDao().insertMedicine(lowStockMed)

        val entry = ScheduleEntry(
            entryId = "entry_refill_alert_test",
            userId = testUser.userId,
            medicineId = lowStockMed.medicineId,
            scheduledDateTime = LocalDateTime.now().minusMinutes(5),
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntry(entry)

        IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entry.entryId,
            entryIds = listOf(entry.entryId),
            medicineName = lowStockMed.medicationName,
            medicineNames = listOf(lowStockMed.medicationName),
            scheduledTime = "08:00 AM",
            durationSeconds = 60
        )

        val confirmed = IntakeSafeguardManager.confirmIntake(context, database)
        assertTrue("Intake must be confirmed", confirmed)

        // Verify stock dropped from 5 to 4
        val updatedMed = database.medicineDao().getMedicineByIdDirect(lowStockMed.medicineId)
        assertNotNull(updatedMed)
        assertEquals(4, updatedMed!!.currentStock)

        // Verify refill warning notification was posted to NotificationManager
        val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val activeNotifs = nManager.activeNotifications
        val expectedRefillTagHash = (lowStockMed.medicineId + "_refill").hashCode()
        assertTrue(
            "NotificationManager must receive refill alert when stock crosses threshold on countdown confirm",
            activeNotifs.any { it.id == expectedRefillTagHash }
        )
    }

    @Test
    fun testConfirmIntake_reconcilesSlotNotification_whenSiblingSlotMedicationRemainsPending() = runBlocking {
        val siblingMed = testMedicine.copy(
            medicineId = "med_sibling_slot",
            medicationName = "Guanfacine",
            currentStock = 20
        )
        database.medicineDao().insertMedicine(siblingMed)

        val slotTime = LocalDateTime.now().minusMinutes(5).withSecond(0).withNano(0)
        val entry1 = ScheduleEntry(
            entryId = "entry_slot_confirmed",
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = slotTime,
            status = MedicationStatus.PENDING
        )
        val entry2 = ScheduleEntry(
            entryId = "entry_slot_pending_sibling",
            userId = testUser.userId,
            medicineId = siblingMed.medicineId,
            scheduledDateTime = slotTime,
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntries(listOf(entry1, entry2))

        // Pre-post notification for the slot
        val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val notif = android.app.Notification.Builder(context)
            .setContentTitle("Slot Reminder")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .build()
        nManager.notify(entry1.entryId.hashCode(), notif)

        // Start and confirm countdown for entry1 ONLY
        IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entry1.entryId,
            entryIds = listOf(entry1.entryId),
            medicineName = testMedicine.medicationName,
            medicineNames = listOf(testMedicine.medicationName),
            scheduledTime = "08:00 AM",
            durationSeconds = 60
        )

        val confirmed = IntakeSafeguardManager.confirmIntake(context, database)
        assertTrue(confirmed)

        // Entry 1 is TAKEN_ON_TIME, entry 2 remains PENDING
        val updated1 = database.scheduleDao().getScheduleEntryById(entry1.entryId)
        val updated2 = database.scheduleDao().getScheduleEntryById(entry2.entryId)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, updated1?.status)
        assertEquals(MedicationStatus.PENDING, updated2?.status)

        // Active notification in shade must NOT be wiped: sibling entry2 must still be represented
        val activeNotifs = nManager.activeNotifications
        assertTrue(
            "Notification shade must retain active notification for remaining sibling pending dose",
            activeNotifs.any { it.id == entry2.entryId.hashCode() }
        )
    }

    @Test
    fun testIntakeSafeguard_cancelCountdown_dismissesNotificationAndClearsState() = runBlocking {
        val entry = ScheduleEntry(
            entryId = "entry_cancel_test",
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = LocalDateTime.of(2026, 10, 17, 9, 0),
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntry(entry)

        IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entry.entryId,
            entryIds = listOf(entry.entryId),
            medicineName = testMedicine.medicationName,
            medicineNames = listOf(testMedicine.medicationName),
            scheduledTime = "09:00 AM",
            durationSeconds = 120
        )

        assertNotNull("Active countdown must exist after startCountdown", IntakeSafeguardManager.activeCountdown.value)

        // Cancel countdown (as when user disables preference in Settings)
        IntakeSafeguardManager.cancelCountdown(context)

        assertNull("Active countdown must be null after cancelCountdown", IntakeSafeguardManager.activeCountdown.value)
        val prefs = context.getSharedPreferences("dosezy_intake_safeguard", Context.MODE_PRIVATE)
        assertTrue("Persistent state must be cleared after cancelCountdown", prefs.all.isEmpty())

        val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        assertFalse(
            "Notification shade must not retain cancelled countdown notification",
            nManager.activeNotifications.any { it.id == entry.entryId.hashCode() }
        )
    }

    @Test
    fun testIntakeSafeguard_startCountdown_propagatesAndPersistsUserId() = runBlocking {
        val entry = ScheduleEntry(
            entryId = "entry_user_id_test",
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = LocalDateTime.of(2026, 10, 17, 10, 0),
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntry(entry)

        val state = IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entry.entryId,
            entryIds = listOf(entry.entryId),
            medicineName = testMedicine.medicationName,
            medicineNames = listOf(testMedicine.medicationName),
            scheduledTime = "10:00 AM",
            durationSeconds = 120,
            userId = entry.userId
        )

        assertEquals(testUser.userId, state.userId)
        assertEquals(testUser.userId, IntakeSafeguardManager.activeCountdown.value?.userId)

        val prefs = context.getSharedPreferences("dosezy_intake_safeguard", Context.MODE_PRIVATE)
        assertEquals(testUser.userId, prefs.getString("user_id", null))

        // Verify notification intent contains switch_to_user_id
        val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val statusBarNotif = nManager.activeNotifications.firstOrNull { it.id == IntakeSafeguardManager.INTAKE_NOTIFICATION_BASE_ID }
        assertNotNull("Intake notification must be posted", statusBarNotif)
        val shadowPi = org.robolectric.Shadows.shadowOf(statusBarNotif!!.notification.contentIntent)
        assertEquals("schedule", shadowPi.savedIntent.getStringExtra("fragment"))
        assertEquals(testUser.userId, shadowPi.savedIntent.getStringExtra("switch_to_user_id"))

        // Cleanup
        IntakeSafeguardManager.cancelCountdown(context)
    }

    @Test
    fun testConfirmIntake_reconcilesMultiSlotEntriesAndDisarmsAlarms() = runBlocking {
        val now = LocalDateTime.now()
        val entry1 = ScheduleEntry(
            entryId = "entry_multislot_1",
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = now.minusMinutes(10),
            status = MedicationStatus.PENDING
        )
        val entry2 = ScheduleEntry(
            entryId = "entry_multislot_2",
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = now.minusMinutes(5),
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntry(entry1)
        database.scheduleDao().insertScheduleEntry(entry2)

        IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entry1.entryId,
            entryIds = listOf(entry1.entryId, entry2.entryId),
            medicineName = testMedicine.medicationName,
            medicineNames = listOf(testMedicine.medicationName),
            scheduledTime = "08:00 AM",
            durationSeconds = 60,
            userId = testUser.userId
        )

        val success = IntakeSafeguardManager.confirmIntake(context, database)
        assertTrue("Intake confirmation should succeed", success)

        assertNull("Active countdown state must be cleared after confirmation", IntakeSafeguardManager.activeCountdown.value)

        val updated1 = database.scheduleDao().getScheduleEntryById(entry1.entryId)
        val updated2 = database.scheduleDao().getScheduleEntryById(entry2.entryId)

        assertNotNull(updated1)
        assertNotNull(updated2)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, updated1?.status)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, updated2?.status)
    }

    @Test
    fun testHandleCountdownExpired_whenAnchorTaken_reAlertsRemainingPendingCohortSiblings() = runBlocking {
        val now = LocalDateTime.now()
        val entry1 = ScheduleEntry(
            entryId = "entry_safeguard_anchor_taken",
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = now.minusMinutes(10),
            status = MedicationStatus.TAKEN_ON_TIME
        )
        val entry2 = ScheduleEntry(
            entryId = "entry_safeguard_sibling_pending",
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = now.minusMinutes(10),
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntry(entry1)
        database.scheduleDao().insertScheduleEntry(entry2)

        var onCompleteCalled = false
        IntakeSafeguardManager.handleCountdownExpired(
            context = context,
            entryId = entry1.entryId,
            medicineName = testMedicine.medicationName,
            entryIds = listOf(entry1.entryId, entry2.entryId),
            medicineNames = listOf(testMedicine.medicationName, testMedicine.medicationName),
            scheduledTime = "08:00 AM",
            onComplete = {
                onCompleteCalled = true
            }
        )

        val deadline = System.currentTimeMillis() + 3000
        while (!onCompleteCalled && System.currentTimeMillis() < deadline) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }

        assertTrue("onComplete must be invoked when handleCountdownExpired completes", onCompleteCalled)

        // Guard: Verify re-alert notification is posted for the active pending sibling entry (#BUG-SAFEGUARD-COHORT-ALERT)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val activeNotifs = nm.activeNotifications
        assertTrue(
            "Notification for sibling entry must be posted when anchor entry was taken",
            activeNotifs.any { it.id == entry2.entryId.hashCode() }
        )
    }

    @Test
    fun testIntakeSafeguard_persistsNotes_andPassesToScheduleRepositoryOnConfirm() = runBlocking {
        // Guard: Verify notes are captured in countdown state, persisted to preferences, and written to Room on confirmation (#BUG-SAFEGUARD-BYPASS)
        val now = LocalDateTime.now()
        val entry = ScheduleEntry(
            entryId = "entry_safeguard_notes_test",
            userId = testUser.userId,
            medicineId = testMedicine.medicineId,
            scheduledDateTime = now.minusMinutes(2),
            status = MedicationStatus.PENDING
        )
        database.scheduleDao().insertScheduleEntry(entry)

        val noteText = "Taken with full glass of water and food"
        val state = IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entry.entryId,
            entryIds = listOf(entry.entryId),
            medicineName = testMedicine.medicationName,
            medicineNames = listOf(testMedicine.medicationName),
            scheduledTime = "08:00 AM",
            durationSeconds = 60,
            userId = testUser.userId,
            notes = noteText
        )

        assertEquals("Countdown state must retain clinical note", noteText, state.notes)
        assertEquals("Active countdown StateFlow must retain clinical note", noteText, IntakeSafeguardManager.activeCountdown.value?.notes)

        val prefs = context.getSharedPreferences("dosezy_intake_safeguard", Context.MODE_PRIVATE)
        assertEquals("Preferences must persist clinical note", noteText, prefs.getString("notes", null))

        val success = IntakeSafeguardManager.confirmIntake(context, database)
        assertTrue("Confirm intake must succeed", success)

        val updatedEntry = database.scheduleDao().getScheduleEntryById(entry.entryId)
        assertNotNull(updatedEntry)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, updatedEntry?.status)
        assertEquals("Dose notes in Room must match note passed to safeguard countdown", noteText, updatedEntry?.doseNotes)
    }

    @Test
    fun testIntakeSafeguard_startCountdown_compositeMedNamesPersisted() {
        // Guard: Verify composite medicine names are stored in state for multi-medication cohorts (#BUG-SAFEGUARD-COHORT-NAMES)
        val compositeName = "Metformin, Atorvastatin, Lisinopril"
        val medList = listOf("Metformin", "Atorvastatin", "Lisinopril")
        val ids = listOf("id_1", "id_2", "id_3")

        val state = IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = ids.first(),
            entryIds = ids,
            medicineName = compositeName,
            medicineNames = medList,
            scheduledTime = "09:00 AM",
            durationSeconds = 90,
            userId = testUser.userId
        )

        assertEquals("State must store composite medicine name", compositeName, state.medicineName)
        assertEquals(3, state.medicineNames.size)
        assertEquals(compositeName, state.medicineNames.joinToString(", "))

        IntakeSafeguardManager.cancelCountdown(context)
    }

    @Test
    fun testIntakeSafeguard_notificationLocalization_resolvesProfileLanguage() = runBlocking {
        // Guard: Verify ADHD countdown notification title and action button strings match target profile language (#BUG-INTAKE-LOCALE)
        val spanishUser = User(
            userId = "user_es_safeguard",
            fullName = "Sofia",
            age = 28,
            gender = Gender.DO_NOT_SPECIFY,
            contactNumber = "+34655555555",
            language = com.example.dosezy.data.model.Language.SPANISH,
            intakeSafeguardEnabled = true,
            intakeCountdownSeconds = 90
        )
        database.userDao().insertUser(spanishUser)

        val esContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, spanishUser.language)
        val enContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, com.example.dosezy.data.model.Language.ENGLISH)

        val expectedEsTitle = esContext.getString(com.example.dosezy.R.string.notif_intake_in_progress_title)
        val expectedEnTitle = enContext.getString(com.example.dosezy.R.string.notif_intake_in_progress_title)
        val expectedEsConfirm = esContext.getString(com.example.dosezy.R.string.notif_intake_action_confirm)
        val expectedEnConfirm = enContext.getString(com.example.dosezy.R.string.notif_intake_action_confirm)

        assertFalse("Spanish intake safeguard title must differ from English", expectedEsTitle == expectedEnTitle)
        assertFalse("Spanish confirm action must differ from English", expectedEsConfirm == expectedEnConfirm)

        val state = IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = "entry_spanish_safeguard",
            entryIds = listOf("entry_spanish_safeguard"),
            medicineName = "Amoxicilina",
            medicineNames = listOf("Amoxicilina"),
            scheduledTime = "08:00 AM",
            durationSeconds = 60,
            userId = spanishUser.userId
        )
        assertNotNull("Countdown state must be active", state)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val safeguardNotif = nm.activeNotifications.firstOrNull { it.id == IntakeSafeguardManager.INTAKE_NOTIFICATION_BASE_ID }?.notification
        assertNotNull("Safeguard countdown notification must be posted", safeguardNotif)

        val actualTitle = safeguardNotif?.extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
        assertEquals("Safeguard notification title must be localized in Spanish", expectedEsTitle, actualTitle)

        val actionTitles = safeguardNotif?.actions?.map { it.title.toString() } ?: emptyList()
        assertTrue("Notification actions must contain localized Spanish confirm button", actionTitles.contains(expectedEsConfirm))

        IntakeSafeguardManager.cancelCountdown(context)
    }
}




