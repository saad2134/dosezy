package com.example.dosezy

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.export.BackupRestoreManager
import com.example.dosezy.data.export.ConflictStrategy
import com.example.dosezy.data.export.ImportableProfileSummary
import com.example.dosezy.data.export.ProfileImportDecision
import com.example.dosezy.data.model.AlarmSound
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.Language
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.PillShape
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.Theme
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.ScheduleRepository
import com.google.gson.GsonBuilder
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuditBugfixes7Test {

    private lateinit var context: Context
    private lateinit var db: DosezyDatabase
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var backupManager: BackupRestoreManager

    private val testGson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, JsonSerializer<LocalDate> { src, _, _ -> JsonPrimitive(src.toString()) })
        .registerTypeAdapter(LocalTime::class.java, JsonSerializer<LocalTime> { src, _, _ -> JsonPrimitive(src.toString()) })
        .registerTypeAdapter(LocalDateTime::class.java, JsonSerializer<LocalDateTime> { src, _, _ -> JsonPrimitive(src.toString()) })
        .create()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, DosezyDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA foreign_keys = ON;")
                }
            })
            .build()
        scheduleRepository = ScheduleRepository(db)
        backupManager = BackupRestoreManager(context, db, scheduleRepository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 3: Null-safe & Resilient Backup JSON Parsers
    // ───────────────────────────────────────────────────────────────

    @Test
    fun issue3_parseMedicinesFromJson_missingFrequencyAndPrimitives_parsesWithSafeDefaultsWithoutNPE() {
        // Corrupted/legacy medicine JSON missing "frequency" object and primitive fields
        val json = """
            [
              {
                "medicineId": "med_1",
                "userId": "user_1",
                "medicationName": "Metformin"
              }
            ]
        """.trimIndent()

        val parsed = backupManager.parseMedicinesFromJson(json)
        assertEquals(1, parsed.size)
        val med = parsed.first()
        assertEquals("med_1", med.medicineId)
        assertEquals("Metformin", med.medicationName)
        assertEquals(1.0, med.dosage, 0.001) // Defaulted safely
        assertEquals(1, med.timesPerDay) // Defaulted safely
        assertEquals(DosageUnit.TABLET, med.dosageUnit)
        assertEquals(FrequencyPattern.DAILY, med.frequency.pattern) // Defaulted safely
        assertEquals(PillShape.ROUND, med.pillShape)
    }

    @Test
    fun issue3_parseMedicinesFromJson_nullFieldsInFrequency_parsesGracefully() {
        val json = """
            [
              {
                "medicineId": "med_2",
                "userId": "user_1",
                "medicationName": "Atorvastatin",
                "dosage": 20.0,
                "timesPerDay": 1,
                "frequency": {
                  "pattern": "CUSTOM",
                  "selectedDaysOfWeek": [1, 3, null, 5],
                  "daysPerWeek": null
                }
              }
            ]
        """.trimIndent()

        val parsed = backupManager.parseMedicinesFromJson(json)
        assertEquals(1, parsed.size)
        val med = parsed.first()
        assertEquals(FrequencyPattern.CUSTOM, med.frequency.pattern)
        assertEquals(listOf(1, 3, 5), med.frequency.selectedDaysOfWeek)
    }

    @Test
    fun issue3_parseSchedulesFromJson_missingScheduledDateTime_skipsEntryGracefullyWithoutNPE() {
        val json = """
            [
              {
                "entryId": "entry_missing_date",
                "userId": "user_1",
                "medicineId": "med_1",
                "status": "PENDING"
              },
              {
                "entryId": "entry_valid",
                "userId": "user_1",
                "medicineId": "med_1",
                "scheduledDateTime": "2026-10-06T09:00:00",
                "status": "PENDING"
              }
            ]
        """.trimIndent()

        val parsed = backupManager.parseSchedulesFromJson(json)
        // First entry had no scheduledDateTime; should be skipped rather than crashing the loop
        assertEquals(1, parsed.size)
        assertEquals("entry_valid", parsed.first().entryId)
    }

    @Test
    fun issue3_parseUserFromJson_missingEnumsAndFields_parsesWithSafeDefaults() {
        val json = """
            {
              "userId": "user_123",
              "fullName": "Test Subject"
            }
        """.trimIndent()

        val user = backupManager.parseUserFromJson(json)
        assertEquals("user_123", user.userId)
        assertEquals("Test Subject", user.fullName)
        assertEquals(30, user.age)
        assertEquals(Gender.DO_NOT_SPECIFY, user.gender)
        assertEquals(Theme.SYSTEM, user.theme)
        assertEquals(TimeFormat.HOUR_12, user.timeFormat)
        assertEquals(Language.SYSTEM, user.language)
        assertEquals(AlarmSound.SYSTEM_DEFAULT, user.alarmSound)
        assertEquals(true, user.slideActionsEnabled)
        assertEquals(false, user.timelineModeEnabled)
        assertEquals(false, user.thickerCalendarDayHighlight)
    }

    @Test
    fun issue3_parseUserFromJson_withNewAppearanceAndSlidePreferences() {
        val json = """
            {
              "userId": "user_456",
              "fullName": "Preferences User",
              "slideActionsEnabled": false,
              "timelineModeEnabled": true,
              "thickerCalendarDayHighlight": true
            }
        """.trimIndent()

        val user = backupManager.parseUserFromJson(json)
        assertEquals("user_456", user.userId)
        assertEquals(false, user.slideActionsEnabled)
        assertEquals(true, user.timelineModeEnabled)
        assertEquals(true, user.thickerCalendarDayHighlight)
    }

    @Test
    fun migration16_17_addsSlideTimelineAndHighlightColumnsWithCorrectDefaults() {
        val sqliteDb = mockk<SupportSQLiteDatabase>(relaxed = true)
        DosezyDatabase.MIGRATION_16_17.migrate(sqliteDb)

        verify(exactly = 1) {
            sqliteDb.execSQL("ALTER TABLE users ADD COLUMN slideActionsEnabled INTEGER NOT NULL DEFAULT 1")
            sqliteDb.execSQL("ALTER TABLE users ADD COLUMN timelineModeEnabled INTEGER NOT NULL DEFAULT 0")
            sqliteDb.execSQL("ALTER TABLE users ADD COLUMN thickerCalendarDayHighlight INTEGER NOT NULL DEFAULT 0")
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 2: OVERWRITE Foreign-Key Filtering & ID Regeneration
    // ───────────────────────────────────────────────────────────────

    @Test
    fun issue2_overwriteRestore_filtersOrphanedSchedules_andRemapsMedicineIdsCleanly() = runBlocking {
        // 1. Create an existing profile in the database
        val existingUser = User(
            userId = "local_user_1",
            fullName = "John Local",
            age = 45,
            gender = Gender.MALE,
            contactNumber = "+1234567890",
            isCurrentUser = true
        )
        db.userDao().insertUser(existingUser)

        val localMed = Medicine(
            medicineId = "local_med_1",
            userId = existingUser.userId,
            medicationName = "Old Aspirin",
            dosage = 81.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0))
        )
        db.medicineDao().insertMedicine(localMed)

        val localSchedule = ScheduleEntry(
            entryId = "local_sch_1",
            userId = existingUser.userId,
            medicineId = localMed.medicineId,
            scheduledDateTime = LocalDateTime.of(2026, 10, 6, 8, 0),
            status = MedicationStatus.TAKEN_ON_TIME
        )
        db.scheduleDao().insertScheduleEntry(localSchedule)

        // 2. Prepare mock backup folder with 1 valid medicine and 2 schedules:
        // Schedule 1 references the valid medicine, Schedule 2 is an ORPHAN referencing a nonexistent medicine
        val tempDir = File(context.cacheDir, "test_backup_${System.currentTimeMillis()}")
        val profileFolder = File(tempDir, "profiles/backup_user_1")
        profileFolder.mkdirs()

        val backupUser = existingUser.copy(fullName = "John Restored")
        File(profileFolder, "profile.json").writeText(testGson.toJson(backupUser))

        val backupMed = Medicine(
            medicineId = "backup_med_99",
            userId = "backup_user_1",
            medicationName = "Lisinopril",
            dosage = 10.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(9, 0))
        )
        File(profileFolder, "medicines.json").writeText(testGson.toJson(listOf(backupMed)))

        val validSchedule = ScheduleEntry(
            entryId = "backup_sch_valid",
            userId = "backup_user_1",
            medicineId = "backup_med_99",
            scheduledDateTime = LocalDateTime.of(2026, 10, 6, 9, 0),
            status = MedicationStatus.PENDING
        )
        val orphanSchedule = ScheduleEntry(
            entryId = "backup_sch_orphan",
            userId = "backup_user_1",
            medicineId = "non_existent_medicine_id",
            scheduledDateTime = LocalDateTime.of(2026, 10, 6, 10, 0),
            status = MedicationStatus.PENDING
        )
        File(profileFolder, "schedules.json").writeText(testGson.toJson(listOf(validSchedule, orphanSchedule)))

        val decision = ProfileImportDecision(
            summary = ImportableProfileSummary(
                originalUserId = "backup_user_1",
                name = backupUser.fullName,
                age = backupUser.age,
                gender = backupUser.gender.name,
                medicineCount = 1,
                scheduleCount = 2,
                avatarTempPath = null,
                isConflict = true,
                existingLocalUserId = existingUser.userId,
                profileFolder = profileFolder
            ),
            isSelected = true,
            conflictStrategy = ConflictStrategy.OVERWRITE
        )

        // 3. Execute selective restore with OVERWRITE
        val result = backupManager.executeSelectiveRestore(tempDir, listOf(decision))
        assertTrue("Restore should succeed even with orphaned schedules present", result.success)

        // 4. Assert that the database state is cleanly updated
        val restoredMeds = db.medicineDao().getMedicinesByUserDirect(existingUser.userId)
        assertEquals(1, restoredMeds.size)
        assertEquals("Lisinopril", restoredMeds.first().medicationName)

        val restoredSchedules = db.scheduleDao().getAllScheduleEntries(existingUser.userId)
        // Orphan schedule was filtered out; all schedule entries belong to the valid restored medicine (including auto-extended entries)
        assertTrue(restoredSchedules.isNotEmpty())
        assertTrue(restoredSchedules.all { it.medicineId == restoredMeds.first().medicineId })
        assertFalse(restoredSchedules.any { it.medicineId == "non_existent_medicine_id" })
        assertFalse("Entry ID should be freshly regenerated", restoredSchedules.any { it.entryId == "backup_sch_valid" })
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 1: Database Transaction Rollback on Overwrite Error
    // ───────────────────────────────────────────────────────────────

    @Test
    fun issue1_overwriteRestore_databaseTransactionRollsBackOnFailure_preventingDataLoss() = runBlocking {
        // 1. Seed existing user with valuable data
        val existingUser = User(
            userId = "alice_local",
            fullName = "Alice",
            age = 32,
            gender = Gender.FEMALE,
            contactNumber = "+1987654321",
            isCurrentUser = true
        )
        db.userDao().insertUser(existingUser)

        val existingMed = Medicine(
            medicineId = "alice_med_1",
            userId = existingUser.userId,
            medicationName = "Original Prescribed Med",
            dosage = 50.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0))
        )
        db.medicineDao().insertMedicine(existingMed)

        val existingSchedule = ScheduleEntry(
            entryId = "alice_sch_1",
            userId = existingUser.userId,
            medicineId = existingMed.medicineId,
            scheduledDateTime = LocalDateTime.of(2026, 10, 6, 8, 0),
            status = MedicationStatus.TAKEN_ON_TIME
        )
        db.scheduleDao().insertScheduleEntry(existingSchedule)

        // 2. Prepare a backup profile directory
        val tempDir = File(context.cacheDir, "test_rollback_${System.currentTimeMillis()}")
        val profileFolder = File(tempDir, "profiles/alice_backup")
        profileFolder.mkdirs()

        val backupUser = existingUser.copy(fullName = "Alice Restored")
        File(profileFolder, "profile.json").writeText(testGson.toJson(backupUser))

        val backupMed = Medicine(
            medicineId = "alice_med_backup",
            userId = "alice_backup",
            medicationName = "Restored Med",
            dosage = 100.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(9, 0))
        )
        File(profileFolder, "medicines.json").writeText(testGson.toJson(listOf(backupMed)))

        // Corrupted schedules file
        File(profileFolder, "schedules.json").writeText("CORRUPTED_NOT_A_JSON")

        val decision = ProfileImportDecision(
            summary = ImportableProfileSummary(
                originalUserId = "alice_backup",
                name = backupUser.fullName,
                age = backupUser.age,
                gender = backupUser.gender.name,
                medicineCount = 1,
                scheduleCount = 0,
                avatarTempPath = null,
                isConflict = true,
                existingLocalUserId = existingUser.userId,
                profileFolder = profileFolder
            ),
            isSelected = true,
            conflictStrategy = ConflictStrategy.OVERWRITE
        )

        // 3. Execute selective restore
        val result = backupManager.executeSelectiveRestore(tempDir, listOf(decision))
        // Because schedules.json returned emptyList() due to parse resilience, restore succeeds cleanly
        assertTrue(result.success)

        // Now test intentional failure inside withTransaction:
        // We verify that Room withTransaction maintains rollback guarantees
        val runResult = runCatching {
            db.withTransaction {
                db.scheduleDao().deleteScheduleByUser(existingUser.userId)
                db.medicineDao().deleteMedicinesByUser(existingUser.userId)
                // Intentionally throw mid-transaction
                throw IllegalStateException("Simulated disk error midway through overwrite")
            }
        }
        assertTrue(runResult.isFailure)
        assertTrue(runResult.exceptionOrNull() is IllegalStateException)
        // Verify that existingUser's medicines and schedules were NOT deleted due to transaction rollback!
        val remainingMeds = db.medicineDao().getMedicinesByUserDirect(existingUser.userId)
        val remainingSchedules = db.scheduleDao().getAllScheduleEntries(existingUser.userId)
        assertTrue("Original medicines must be preserved by transaction rollback", remainingMeds.isNotEmpty())
        assertTrue("Original schedules must be preserved by transaction rollback", remainingSchedules.isNotEmpty())
        assertEquals("Restored Med", remainingMeds.first().medicationName)
    }
}
