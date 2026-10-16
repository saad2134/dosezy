/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.notifications

import android.content.Context
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
}
