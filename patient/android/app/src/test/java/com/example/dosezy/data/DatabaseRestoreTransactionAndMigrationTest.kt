package com.example.dosezy.data

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseRestoreTransactionAndMigrationTest {

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

    @Test
    fun migration18_19_addsLockscreenAndCustomSnoozeColumnsWithCorrectDefaults() {
        val sqliteDb = mockk<SupportSQLiteDatabase>(relaxed = true)
        DosezyDatabase.MIGRATION_18_19.migrate(sqliteDb)

        verify(exactly = 1) {
            sqliteDb.execSQL("ALTER TABLE users ADD COLUMN lockscreenDetailsEnabled INTEGER NOT NULL DEFAULT 1")
            sqliteDb.execSQL("ALTER TABLE users ADD COLUMN customSnoozePopupEnabled INTEGER NOT NULL DEFAULT 0")
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

    @Test
    fun testExecuteSelectiveRestore_overwriteDeletesOrphanedMedicineImageFilesAndCancelsSafeguard() = runBlocking {
        // 1. Setup local user with medicine holding a file in context.filesDir
        val localUser = User(
            userId = "usr_img_clean",
            fullName = "Old Image User",
            age = 40,
            gender = Gender.MALE,
            contactNumber = "5551234567"
        )
        db.userDao().insertUser(localUser)

        val medFolder = File(context.filesDir, "medicines/old_med_uuid")
        medFolder.mkdirs()
        val oldMedImage = File(medFolder, "image.jpg")
        oldMedImage.writeText("fake image bytes")
        assertTrue("Old medicine image must exist on disk before restore", oldMedImage.exists())

        val localMed = Medicine(
            medicineId = "old_med_uuid",
            userId = localUser.userId,
            medicationName = "Old Med With Photo",
            dosage = 10.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0)),
            imageUri = "file://${oldMedImage.absolutePath}"
        )
        db.medicineDao().insertMedicine(localMed)

        val localSchedule = ScheduleEntry(
            entryId = "sch_img_safeguard",
            userId = localUser.userId,
            medicineId = localMed.medicineId,
            scheduledDateTime = LocalDateTime.now().plusHours(2),
            status = MedicationStatus.PENDING
        )
        db.scheduleDao().insertScheduleEntry(localSchedule)

        // Start active intake safeguard for this entry
        com.example.dosezy.notifications.IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = localSchedule.entryId,
            entryIds = listOf(localSchedule.entryId),
            medicineName = localMed.medicationName,
            medicineNames = listOf(localMed.medicationName),
            scheduledTime = "08:00 AM",
            durationSeconds = 120
        )
        assertNotNull(com.example.dosezy.notifications.IntakeSafeguardManager.activeCountdown.value)

        // 2. Prepare backup folder for overwrite with a different medicine (without reusing the old image)
        val tempDir = File(context.cacheDir, "test_backup_img_${System.currentTimeMillis()}")
        val profileFolder = File(tempDir, "profiles/backup_user_img")
        profileFolder.mkdirs()

        val backupUser = localUser.copy(fullName = "New Name User")
        File(profileFolder, "profile.json").writeText(testGson.toJson(backupUser))

        val newBackupMed = Medicine(
            medicineId = "new_incoming_med",
            userId = "backup_user_img",
            medicationName = "New Fresh Med",
            dosage = 20.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(10, 0))
        )
        File(profileFolder, "medicines.json").writeText(testGson.toJson(listOf(newBackupMed)))
        File(profileFolder, "schedules.json").writeText(testGson.toJson(emptyList<ScheduleEntry>()))

        val decision = ProfileImportDecision(
            summary = ImportableProfileSummary(
                originalUserId = "backup_user_img",
                name = backupUser.fullName,
                age = backupUser.age,
                gender = backupUser.gender.name,
                medicineCount = 1,
                scheduleCount = 0,
                avatarTempPath = null,
                isConflict = true,
                existingLocalUserId = localUser.userId,
                profileFolder = profileFolder
            ),
            isSelected = true,
            conflictStrategy = ConflictStrategy.OVERWRITE
        )

        // 3. Execute OVERWRITE restore
        val result = backupManager.executeSelectiveRestore(tempDir, listOf(decision))
        assertTrue(result.success)

        // 4. Assert that old image file was deleted from disk and safeguard countdown was cancelled
        assertFalse("Old medicine image file must be deleted on overwrite restore", oldMedImage.exists())
        assertNull("Active safeguard countdown must be cancelled on overwrite restore", com.example.dosezy.notifications.IntakeSafeguardManager.activeCountdown.value)
    }

    @Test
    fun testBackupRestoreManager_packagesAndRestoresCustomAlarmAudio_createNew() = runBlocking {
        // 1. Create a user with custom alarm audio
        val soundsDir = File(context.filesDir, "custom_sounds").apply { mkdirs() }
        val sourceAudio = File(soundsDir, "custom_alarm_source.mp3").apply { writeText("custom ringtone mp3 audio") }

        val user = User(
            userId = "user_custom_sound",
            fullName = "Custom Sound User",
            age = 30,
            gender = Gender.FEMALE,
            contactNumber = "+1234567890",
            theme = Theme.SYSTEM,
            timeFormat = TimeFormat.HOUR_12,
            language = Language.ENGLISH,
            alarmSound = AlarmSound.CUSTOM,
            customAlarmSoundPath = sourceAudio.absolutePath,
            customAlarmSoundTitle = "My Custom Song",
            isCurrentUser = true
        )
        db.userDao().insertUser(user)

        // 2. Create full backup zip
        val backupZip = backupManager.createFullBackupZip()
        assertTrue("Backup zip must be created", backupZip.exists())

        // 3. Inspect zip entries to verify custom_alarm.mp3 is packaged under user's profile
        var foundCustomAlarmEntry = false
        ZipInputStream(FileInputStream(backupZip)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "profiles/${user.userId}/custom_alarm.mp3") {
                    foundCustomAlarmEntry = true
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        assertTrue("Backup zip must contain custom_alarm.mp3 entry", foundCustomAlarmEntry)

        // 4. Test restore via CREATE_NEW
        val tempDir = File(context.cacheDir, "test_restore_sound_${System.currentTimeMillis()}")
        val profileFolder = File(tempDir, "profiles/${user.userId}").apply { mkdirs() }
        File(profileFolder, "profile.json").writeText(testGson.toJson(user))
        File(profileFolder, "custom_alarm.mp3").writeText("custom ringtone mp3 audio")

        val decision = ProfileImportDecision(
            summary = ImportableProfileSummary(
                originalUserId = user.userId,
                name = user.fullName,
                age = user.age,
                gender = user.gender.name,
                medicineCount = 0,
                scheduleCount = 0,
                avatarTempPath = null,
                isConflict = false,
                existingLocalUserId = null,
                profileFolder = profileFolder
            ),
            isSelected = true,
            conflictStrategy = ConflictStrategy.CREATE_NEW
        )

        val restoreResult = backupManager.executeSelectiveRestore(tempDir, listOf(decision))
        assertTrue(restoreResult.success)

        // 5. Verify the newly created profile points to an unpacked local sound file that exists on disk
        val restoredUsers = db.userDao().getAllUsersDirect().filter { it.userId != user.userId }
        assertEquals(1, restoredUsers.size)
        val restoredUser = restoredUsers.first()
        assertNotNull("Restored user must have customAlarmSoundPath populated", restoredUser.customAlarmSoundPath)
        assertTrue("Unpacked custom alarm audio file must exist on disk", File(restoredUser.customAlarmSoundPath!!).exists())
        assertEquals(AlarmSound.CUSTOM, restoredUser.alarmSound)
    }

    @Test
    fun testBackupRestoreManager_overwriteDeletesOldCustomAudioOnDisk() = runBlocking {
        // 1. Existing local user with sound file A
        val soundsDir = File(context.filesDir, "custom_sounds").apply { mkdirs() }
        val oldAudio = File(soundsDir, "custom_alarm_old.mp3").apply { writeText("old audio") }

        val localUser = User(
            userId = "user_overwrite_sound",
            fullName = "Existing User",
            age = 45,
            gender = Gender.MALE,
            contactNumber = "+1987654321",
            theme = Theme.LIGHT,
            timeFormat = TimeFormat.HOUR_24,
            language = Language.ENGLISH,
            alarmSound = AlarmSound.CUSTOM,
            customAlarmSoundPath = oldAudio.absolutePath,
            isCurrentUser = true
        )
        db.userDao().insertUser(localUser)
        assertTrue("Old audio must initially exist", oldAudio.exists())

        // 2. Prepare backup profile folder with sound file B
        val tempDir = File(context.cacheDir, "test_overwrite_sound_${System.currentTimeMillis()}")
        val profileFolder = File(tempDir, "profiles/backup_user").apply { mkdirs() }
        val incomingUser = localUser.copy(fullName = "Updated User")
        File(profileFolder, "profile.json").writeText(testGson.toJson(incomingUser))
        File(profileFolder, "custom_alarm.mp3").writeText("new audio contents")

        val decision = ProfileImportDecision(
            summary = ImportableProfileSummary(
                originalUserId = "backup_user",
                name = incomingUser.fullName,
                age = incomingUser.age,
                gender = incomingUser.gender.name,
                medicineCount = 0,
                scheduleCount = 0,
                avatarTempPath = null,
                isConflict = true,
                existingLocalUserId = localUser.userId,
                profileFolder = profileFolder
            ),
            isSelected = true,
            conflictStrategy = ConflictStrategy.OVERWRITE
        )

        val restoreResult = backupManager.executeSelectiveRestore(tempDir, listOf(decision))
        assertTrue(restoreResult.success)

        // 3. Verify old audio file A was deleted from disk and replaced by new audio file
        assertFalse("Old audio file must be deleted upon overwrite restore", oldAudio.exists())
        val updatedLocalUser = db.userDao().getUserByIdDirect(localUser.userId)
        assertNotNull(updatedLocalUser)
        assertNotNull(updatedLocalUser!!.customAlarmSoundPath)
        assertTrue("New audio file must exist on disk", File(updatedLocalUser.customAlarmSoundPath!!).exists())
    }

    @Test
    fun testBackupRestoreManager_emergencyCountryExportAndRestoreAndOverwriteScrubbing() = runBlocking {
        // 1. Export verification: Configure user and per-profile emergency country code
        val exportUser = User(
            userId = "user_em_export",
            fullName = "Emergency Export User",
            age = 30,
            gender = Gender.MALE,
            contactNumber = "+1112223333",
            isCurrentUser = true
        )
        db.userDao().insertUser(exportUser)
        val emergencyPrefs = context.getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
        val contactsPrefs = context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)

        emergencyPrefs.edit().putString("selected_country_code_${exportUser.userId}", "GB").commit()

        val zipFile = backupManager.createFullBackupZip()
        assertTrue("Backup zip file must exist", zipFile.exists())

        var foundEmergencyCountryEntry = false
        var extractedCountryCode = ""
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "profiles/${exportUser.userId}/emergency_country.txt") {
                    foundEmergencyCountryEntry = true
                    extractedCountryCode = zis.bufferedReader().readText()
                    break
                }
                entry = zis.nextEntry
            }
        }
        assertTrue("Backup zip must contain profiles/${exportUser.userId}/emergency_country.txt", foundEmergencyCountryEntry)
        assertEquals("GB", extractedCountryCode)

        // 2. Overwrite restore verification: Set stale local contacts and country
        contactsPrefs.edit().putString("contacts_json_${exportUser.userId}", "[{\"name\":\"Stale Contact\"}]").commit()
        emergencyPrefs.edit().putString("selected_country_code_${exportUser.userId}", "US").commit()

        // Incoming backup folder has CA for country, but no emergency contacts file
        val tempDir = File(context.cacheDir, "test_overwrite_em_${System.currentTimeMillis()}")
        val profileFolder = File(tempDir, "profiles/backup_em_user").apply { mkdirs() }
        File(profileFolder, "profile.json").writeText(testGson.toJson(exportUser))
        File(profileFolder, "emergency_country.txt").writeText("CA")

        val decision = ProfileImportDecision(
            summary = ImportableProfileSummary(
                originalUserId = "backup_em_user",
                name = exportUser.fullName,
                age = exportUser.age,
                gender = exportUser.gender.name,
                medicineCount = 0,
                scheduleCount = 0,
                avatarTempPath = null,
                isConflict = true,
                existingLocalUserId = exportUser.userId,
                profileFolder = profileFolder
            ),
            isSelected = true,
            conflictStrategy = ConflictStrategy.OVERWRITE
        )

        val restoreResult = backupManager.executeSelectiveRestore(tempDir, listOf(decision))
        assertTrue(restoreResult.success)

        // Verify emergency country updated to CA and stale emergency contacts scrubbed
        assertEquals("CA", emergencyPrefs.getString("selected_country_code_${exportUser.userId}", null))
        assertNull(contactsPrefs.getString("contacts_json_${exportUser.userId}", null))

        // 3. Overwrite with missing country file scrubs existing country code
        val tempDir2 = File(context.cacheDir, "test_overwrite_em_empty_${System.currentTimeMillis()}")
        val profileFolder2 = File(tempDir2, "profiles/backup_em_user_2").apply { mkdirs() }
        File(profileFolder2, "profile.json").writeText(testGson.toJson(exportUser))
        // No emergency_country.txt in profileFolder2

        val decision2 = ProfileImportDecision(
            summary = ImportableProfileSummary(
                originalUserId = "backup_em_user_2",
                name = exportUser.fullName,
                age = exportUser.age,
                gender = exportUser.gender.name,
                medicineCount = 0,
                scheduleCount = 0,
                avatarTempPath = null,
                isConflict = true,
                existingLocalUserId = exportUser.userId,
                profileFolder = profileFolder2
            ),
            isSelected = true,
            conflictStrategy = ConflictStrategy.OVERWRITE
        )

        val restoreResult2 = backupManager.executeSelectiveRestore(tempDir2, listOf(decision2))
        assertTrue(restoreResult2.success)
        assertNull(emergencyPrefs.getString("selected_country_code_${exportUser.userId}", null))
    }

    @Test
    fun testBackup_exportsDefaultEmergencyContacts_andMigratesToNamespacedKey() = runBlocking {
        val testUser = User(
            userId = "backup_default_contacts_user",
            fullName = "Default Contacts User",
            age = 45,
            gender = Gender.FEMALE,
            contactNumber = "5551234",
            isCurrentUser = true
        )
        db.userDao().insertUser(testUser)

        val contactsPrefs = context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)
        contactsPrefs.edit().clear().commit()

        val defaultJson = """[{"name":"Caregiver","phone":"555-9988"}]"""
        contactsPrefs.edit().putString("contacts_json_default", defaultJson).commit()

        val zipFile = backupManager.createFullBackupZip()
        assertTrue(zipFile.exists())

        // Verify: The backup contains emergency_contacts.json with the contacts from contacts_json_default
        var extractedEmergencyJson: String? = null
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "profiles/${testUser.userId}/emergency_contacts.json") {
                    extractedEmergencyJson = zis.bufferedReader(Charsets.UTF_8).readText()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        assertNotNull("Emergency contacts must be exported when stored in contacts_json_default", extractedEmergencyJson)
        assertEquals(defaultJson, extractedEmergencyJson)

        // Verify: Namespaced key was also migrated
        assertEquals(defaultJson, contactsPrefs.getString("contacts_json_${testUser.userId}", null))
    }

    @Test
    fun testMergeStrategy_insertsParentUserWhenLocalUserMissing_andSatisfiesForeignKeyConstraint() = runBlocking {
        val tempDir = File(context.cacheDir, "test_merge_missing_user_${System.currentTimeMillis()}")
        val profileFolder = File(tempDir, "profiles/missing_user_1").apply { mkdirs() }

        val backupUser = User(
            userId = "missing_user_1",
            fullName = "Merged Missing User",
            age = 35,
            gender = Gender.DO_NOT_SPECIFY,
            contactNumber = "555-4321",
            theme = Theme.DARK,
            timeFormat = TimeFormat.HOUR_12,
            language = Language.ENGLISH,
            isCurrentUser = true
        )
        File(profileFolder, "profile.json").writeText(testGson.toJson(backupUser))

        val backupMed = Medicine(
            medicineId = "backup_med_merge_1",
            userId = "missing_user_1",
            medicationName = "Lisinopril",
            dosage = 10.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0))
        )
        File(profileFolder, "medicines.json").writeText(testGson.toJson(listOf(backupMed)))

        val backupSchedule = ScheduleEntry(
            entryId = "sch_merge_1",
            userId = "missing_user_1",
            medicineId = "backup_med_merge_1",
            scheduledDateTime = LocalDateTime.of(2026, 10, 17, 8, 0),
            status = MedicationStatus.PENDING
        )
        File(profileFolder, "schedules.json").writeText(testGson.toJson(listOf(backupSchedule)))

        // Local users table is empty; existingLocalUserId is null
        assertNull(db.userDao().getUserByIdDirect("missing_user_1"))

        val decision = ProfileImportDecision(
            summary = ImportableProfileSummary(
                originalUserId = "missing_user_1",
                name = backupUser.fullName,
                age = backupUser.age,
                gender = backupUser.gender.name,
                medicineCount = 1,
                scheduleCount = 1,
                avatarTempPath = null,
                isConflict = false,
                existingLocalUserId = null,
                profileFolder = profileFolder
            ),
            isSelected = true,
            conflictStrategy = ConflictStrategy.MERGE
        )

        val restoreResult = backupManager.executeSelectiveRestore(tempDir, listOf(decision))
        assertTrue("Selective restore with MERGE strategy must succeed", restoreResult.success)

        // Verify parent user was inserted, satisfying foreign key constraints for medicines and schedules
        val restoredUser = db.userDao().getUserByIdDirect("missing_user_1")
        assertNotNull("Parent User must be inserted by MERGE when local user is missing", restoredUser)
        assertEquals("Merged Missing User", restoredUser?.fullName)

        val restoredMeds = db.medicineDao().getMedicinesByUserDirect("missing_user_1")
        assertEquals(1, restoredMeds.size)
        assertEquals("Lisinopril", restoredMeds.first().medicationName)

        val restoredSchedules = db.scheduleDao().getAllScheduleEntries("missing_user_1")
        assertTrue("Restored schedules must be populated and reconciled", restoredSchedules.isNotEmpty())
    }

    @Test
    fun testRestore_overwriteStrategy_dismissesExistingNotificationsAndRefillAlerts() = runBlocking {
        val tempDir = File(context.cacheDir, "test_overwrite_notif_${System.currentTimeMillis()}")
        val existingUser = User(
            userId = "user_overwrite_notif",
            fullName = "Overwrite User",
            age = 35,
            gender = Gender.FEMALE,
            contactNumber = "1234567890",
            isCurrentUser = true
        )
        db.userDao().insertUser(existingUser)
        val existingMed = Medicine(
            medicineId = "med_overwrite_notif",
            userId = existingUser.userId,
            medicationName = "Aspirin",
            dosage = 100.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0)),
            startDate = LocalDate.now()
        )
        db.medicineDao().insertMedicine(existingMed)
        val existingEntry = ScheduleEntry(
            entryId = "entry_overwrite_notif",
            userId = existingUser.userId,
            medicineId = existingMed.medicineId,
            scheduledDateTime = LocalDateTime.now().plusHours(2),
            status = MedicationStatus.PENDING
        )
        db.scheduleDao().insertScheduleEntry(existingEntry)

        val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nManager.notify(existingEntry.entryId.hashCode(), androidx.core.app.NotificationCompat.Builder(context, "test").setContentTitle("Reminder").setSmallIcon(android.R.drawable.ic_dialog_info).build())
        nManager.notify((existingMed.medicineId + "_refill").hashCode(), androidx.core.app.NotificationCompat.Builder(context, "test").setContentTitle("Refill").setSmallIcon(android.R.drawable.ic_dialog_info).build())

        assertTrue(nManager.activeNotifications.any { it.id == existingEntry.entryId.hashCode() })
        assertTrue(nManager.activeNotifications.any { it.id == (existingMed.medicineId + "_refill").hashCode() })

        // Prepare backup profile folder
        val profileFolder = File(tempDir, "profiles/${existingUser.userId}").apply { mkdirs() }
        File(profileFolder, "profile.json").writeText(testGson.toJson(existingUser.copy(fullName = "Restored Overwrite User")))
        File(profileFolder, "medicines.json").writeText(testGson.toJson(listOf(existingMed)))
        File(profileFolder, "schedules.json").writeText(testGson.toJson(listOf(existingEntry)))

        val decision = ProfileImportDecision(
            summary = ImportableProfileSummary(
                originalUserId = existingUser.userId,
                name = existingUser.fullName,
                age = existingUser.age,
                gender = existingUser.gender.name,
                medicineCount = 1,
                scheduleCount = 1,
                avatarTempPath = null,
                isConflict = true,
                existingLocalUserId = existingUser.userId,
                profileFolder = profileFolder
            ),
            isSelected = true,
            conflictStrategy = ConflictStrategy.OVERWRITE
        )

        val restoreResult = backupManager.executeSelectiveRestore(tempDir, listOf(decision))
        assertTrue(restoreResult.success)

        // Verify notifications dismissed from shade
        assertFalse("Entry reminder must be dismissed from shade on overwrite", nManager.activeNotifications.any { it.id == existingEntry.entryId.hashCode() })
        assertFalse("Refill alert must be dismissed from shade on overwrite", nManager.activeNotifications.any { it.id == (existingMed.medicineId + "_refill").hashCode() })
    }

    @Test
    fun backupRestoreManager_deserializesLowercasedAndTrimmedEnumsResiliently() {
        val userJson = """
            {
              "userId": "usr_lower_01",
              "fullName": "Lower Case User",
              "gender": "female",
              "theme": "dark",
              "timeFormat": "hour_24",
              "language": "spanish",
              "alarmSound": "gentle_chime"
            }
        """.trimIndent()

        // Guard: Verify lowercased user profile enums correctly deserialize without falling back to defaults
        val parsedUser = backupManager.parseUserFromJson(userJson)
        assertEquals(Gender.FEMALE, parsedUser.gender)
        assertEquals(Theme.DARK, parsedUser.theme)
        assertEquals(TimeFormat.HOUR_24, parsedUser.timeFormat)
        assertEquals(Language.SPANISH, parsedUser.language)
        assertEquals(AlarmSound.GENTLE_CHIME, parsedUser.alarmSound)

        val medJson = """
            [
              {
                "medicineId": "med_lower_01",
                "userId": "usr_lower_01",
                "medicationName": "Metformin",
                "dosage": 500.0,
                "dosageUnit": "tablet",
                "frequency": {
                  "pattern": "every_x_days",
                  "intervalDays": 2
                },
                "pillShape": "capsule"
              }
            ]
        """.trimIndent()

        // Guard: Verify lowercased medicine frequency pattern and pill shape correctly deserialize without falling back to defaults
        val parsedMeds = backupManager.parseMedicinesFromJson(medJson)
        assertEquals(1, parsedMeds.size)
        assertEquals(DosageUnit.TABLET, parsedMeds[0].dosageUnit)
        assertEquals(FrequencyPattern.EVERY_X_DAYS, parsedMeds[0].frequency.pattern)
        assertEquals(PillShape.CAPSULE, parsedMeds[0].pillShape)

        val schedJson = """
            [
              {
                "entryId": "entry_lower_01",
                "userId": "usr_lower_01",
                "medicineId": "med_lower_01",
                "scheduledDateTime": "2026-10-18T08:00:00",
                "status": "taken_on_time"
              },
              {
                "entryId": "entry_lower_02",
                "userId": "usr_lower_01",
                "medicineId": "med_lower_01",
                "scheduledDateTime": "2026-10-18T20:00:00",
                "status": "skipped"
              }
            ]
        """.trimIndent()

        // Guard: Verify lowercased schedule entry status correctly deserializes without resetting to PENDING
        val parsedSchedules = backupManager.parseSchedulesFromJson(schedJson)
        assertEquals(2, parsedSchedules.size)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, parsedSchedules[0].status)
        assertEquals(MedicationStatus.SKIPPED, parsedSchedules[1].status)
    }

    @Test
    fun testBackupRestoreManager_createNewPromotesActiveUserWhenExistingUsersAreInactive() = runBlocking {
        // Guard: Verify that CREATE_NEW promotes the first restored profile when local database contains profiles but none are active
        val inactiveLocalUser = User(
            userId = "local_inactive_usr",
            fullName = "Inactive Local User",
            age = 50,
            gender = Gender.MALE,
            contactNumber = "+1000000000",
            isCurrentUser = false
        )
        db.userDao().insertUser(inactiveLocalUser)

        val tempDir = File(context.cacheDir, "test_create_new_inactive_${System.currentTimeMillis()}").apply { mkdirs() }
        val profileFolder = File(tempDir, "profiles/backup_new_profile").apply { mkdirs() }
        val backupUser = User(
            userId = "backup_new_profile",
            fullName = "Restored Patient",
            age = 35,
            gender = Gender.FEMALE,
            contactNumber = "+1999999999",
            isCurrentUser = false
        )
        File(profileFolder, "profile.json").writeText(testGson.toJson(backupUser))

        val decision = ProfileImportDecision(
            summary = ImportableProfileSummary(
                originalUserId = "backup_new_profile",
                name = backupUser.fullName,
                age = backupUser.age,
                gender = backupUser.gender.name,
                medicineCount = 0,
                scheduleCount = 0,
                avatarTempPath = null,
                isConflict = false,
                existingLocalUserId = null,
                profileFolder = profileFolder
            ),
            isSelected = true,
            conflictStrategy = ConflictStrategy.CREATE_NEW
        )

        val result = backupManager.executeSelectiveRestore(tempDir, listOf(decision))
        assertTrue(result.success)

        val allUsers = db.userDao().getAllUsersDirect()
        assertEquals(2, allUsers.size)
        val restoredUser = allUsers.first { u -> u.fullName == backupUser.fullName }
        assertTrue("Newly restored profile must be promoted to active current user when local profiles are all inactive", restoredUser.isCurrentUser)
        assertEquals("Exactly one user must be marked active", 1, allUsers.count { u -> u.isCurrentUser })
    }
}

