/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy

import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
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
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.Theme
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import com.example.dosezy.domain.usecases.TakeCohortUseCase
import com.example.dosezy.notifications.IntakeSafeguardManager
import com.example.dosezy.notifications.MedicineAlarmReceiver
import com.example.dosezy.utils.SoundUtils
import com.google.gson.GsonBuilder
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
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
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuditDeepDiveFixesTest {

    private lateinit var context: Context
    private lateinit var db: DosezyDatabase
    private lateinit var userRepository: UserRepository
    private lateinit var medicineRepository: MedicineRepository
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var backupManager: BackupRestoreManager
    private lateinit var notificationManager: NotificationManager

    private val testGson = GsonBuilder()
        .registerTypeAdapter(LocalTime::class.java, JsonSerializer<LocalTime> { src, _, _ -> JsonPrimitive(src.toString()) })
        .registerTypeAdapter(LocalDate::class.java, JsonSerializer<LocalDate> { src, _, _ -> JsonPrimitive(src.toString()) })
        .registerTypeAdapter(LocalDateTime::class.java, JsonSerializer<LocalDateTime> { src, _, _ -> JsonPrimitive(src.toString()) })
        .create()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, DosezyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userRepository = UserRepository(db)
        scheduleRepository = ScheduleRepository(db)
        medicineRepository = MedicineRepository(db, scheduleRepository, context)
        backupManager = BackupRestoreManager(context, db, scheduleRepository)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @After
    fun tearDown() {
        IntakeSafeguardManager.cancelCountdown(context)
        db.close()
    }

    // =========================================================================
    // Issue 1 Tests: Custom Alarm Audio Deletion on Profile Cleanup & Sandboxing
    // =========================================================================

    @Test
    fun testSoundUtils_deleteCustomAudio_deletesValidFileWithinCustomSounds() {
        val soundsDir = File(context.filesDir, "custom_sounds").apply { mkdirs() }
        val testAudio = File(soundsDir, "custom_alarm_test_123.mp3").apply { writeText("audio content") }
        assertTrue(testAudio.exists())

        val deleted = SoundUtils.deleteCustomAudio(context, testAudio.absolutePath)
        assertTrue("deleteCustomAudio must return true for valid custom sound", deleted)
        assertFalse("File must be deleted from internal storage", testAudio.exists())
    }

    @Test
    fun testSoundUtils_deleteCustomAudio_rejectsExternalPathTraversal() {
        val externalFile = File(context.cacheDir, "external_test_audio.mp3").apply { writeText("external audio") }
        assertTrue(externalFile.exists())

        // File is outside filesDir/custom_sounds, deleteCustomAudio must reject it
        val deleted = SoundUtils.deleteCustomAudio(context, externalFile.absolutePath)
        assertFalse("deleteCustomAudio must refuse to delete files outside custom_sounds directory", deleted)
        assertTrue("External file must remain untouched", externalFile.exists())

        externalFile.delete()
    }

    // =========================================================================
    // Issue 2 Tests: Backup & Restore Packaging and Unpacking of Custom Audio
    // =========================================================================

    @Test
    fun testBackupRestoreManager_packagesAndRestoresCustomAlarmAudio() = runBlocking {
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

    // =========================================================================
    // Issue 3 Tests: Canonical Path Sandboxing against Path Traversal
    // =========================================================================

    @Test
    fun testDeleteOldImage_sandboxing_allowsFilesDirAndCacheDirOnly() {
        val filesDir = context.filesDir
        val cacheDir = context.cacheDir

        val validFilesDirFile = File(filesDir, "test_profile_pic.jpg").apply { writeText("data") }
        val validCacheDirFile = File(cacheDir, "test_cache_pic.jpg").apply { writeText("data") }
        val outsideFile = File(context.filesDir.parentFile, "outside_sensitive.txt").apply { writeText("sensitive") }

        fun verifySandboxedPath(file: File): Boolean {
            val isWithinFilesDir = file.parentFile?.canonicalPath == filesDir.canonicalPath ||
                    file.canonicalPath.startsWith(filesDir.canonicalPath + File.separator)
            val isWithinCacheDir = file.parentFile?.canonicalPath == cacheDir.canonicalPath ||
                    file.canonicalPath.startsWith(cacheDir.canonicalPath + File.separator)
            return file.exists() && (isWithinFilesDir || isWithinCacheDir)
        }

        assertTrue("Files within filesDir must be allowed", verifySandboxedPath(validFilesDirFile))
        assertTrue("Files within cacheDir must be allowed", verifySandboxedPath(validCacheDirFile))
        assertFalse("Files outside filesDir and cacheDir must be blocked", verifySandboxedPath(outsideFile))

        // Clean up
        validFilesDirFile.delete()
        validCacheDirFile.delete()
        outsideFile.delete()
    }

    // =========================================================================
    // Issue 4 Tests: TakeCohortUseCase Cancels Active Intake Safeguard
    // =========================================================================

    @Test
    fun testTakeCohortUseCase_cancelsActiveIntakeSafeguardCountdown() = runBlocking {
        val user = User(
            userId = "user_safeguard_take",
            fullName = "Safeguard User",
            age = 28,
            gender = Gender.DO_NOT_SPECIFY,
            contactNumber = "+1122334455",
            theme = Theme.DARK,
            timeFormat = TimeFormat.HOUR_12,
            language = Language.ENGLISH,
            intakeSafeguardEnabled = true,
            intakeCountdownSeconds = 120,
            isCurrentUser = true
        )
        db.userDao().insertUser(user)

        val med = Medicine(
            medicineId = "med_safeguard_cohort",
            userId = user.userId,
            medicationName = "Methylphenidate",
            dosage = 10.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0)),
            currentStock = 20,
            autoDeductOnTake = true
        )
        db.medicineDao().insertMedicine(med)

        val entry = ScheduleEntry(
            entryId = "entry_safeguard_cohort_take",
            userId = user.userId,
            medicineId = med.medicineId,
            scheduledDateTime = LocalDateTime.now(),
            status = MedicationStatus.PENDING,
            dosage = 10.0
        )
        db.scheduleDao().insertScheduleEntry(entry)

        // 1. Simulate user initiating an intake countdown
        IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entry.entryId,
            entryIds = listOf(entry.entryId),
            medicineName = med.medicationName,
            medicineNames = listOf(med.medicationName),
            scheduledTime = "08:00 AM",
            durationSeconds = 120
        )
        assertNotNull("Intake countdown must be active", IntakeSafeguardManager.activeCountdown.value)

        // 2. User confirms taking the dose via TakeCohortUseCase
        val takeCohortUseCase = TakeCohortUseCase(db, scheduleRepository, context)
        takeCohortUseCase.invoke(listOf(entry.entryId), user = user)

        // 3. Assert dose is recorded as taken in database
        val updatedEntry = db.scheduleDao().getScheduleEntryById(entry.entryId)
        assertNotNull(updatedEntry)
        assertEquals(MedicationStatus.TAKEN_ON_TIME, updatedEntry!!.status)

        // 4. Guard assertion: Active intake countdown must be cancelled so no phantom re-alert fires
        assertNull("Active intake countdown must be cancelled upon dose completion", IntakeSafeguardManager.activeCountdown.value)
    }

    // =========================================================================
    // Issue 5 Tests: Refill Threshold Alert when Stock Equals Threshold
    // =========================================================================

    @Test
    fun testRecordDoseTaken_triggersRefillAlert_whenInitialStockEqualsThreshold() = runBlocking {
        MedicineAlarmReceiver.createNotificationChannels(context)

        val user = User(
            userId = "user_refill_boundary",
            fullName = "Refill User",
            age = 40,
            gender = Gender.MALE,
            contactNumber = "+1555666777",
            theme = Theme.SYSTEM,
            timeFormat = TimeFormat.HOUR_12,
            language = Language.ENGLISH,
            isCurrentUser = true
        )
        db.userDao().insertUser(user)

        // Stock is exactly equal to refillThreshold (5 pills)
        val med = Medicine(
            medicineId = "med_refill_boundary",
            userId = user.userId,
            medicationName = "Atorvastatin",
            dosage = 1.0,
            dosageUnit = DosageUnit.TABLET,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(9, 0)),
            currentStock = 5,
            refillThreshold = 5,
            autoDeductOnTake = true
        )
        db.medicineDao().insertMedicine(med)

        val entry = ScheduleEntry(
            entryId = "entry_refill_boundary_1",
            userId = user.userId,
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

        val user = User(
            userId = "user_prn_boundary",
            fullName = "PRN User",
            age = 35,
            gender = Gender.FEMALE,
            contactNumber = "+1444333222",
            theme = Theme.LIGHT,
            timeFormat = TimeFormat.HOUR_12,
            language = Language.ENGLISH,
            isCurrentUser = true
        )
        db.userDao().insertUser(user)

        // Stock is exactly equal to refillThreshold (3 pills)
        val prnMed = Medicine(
            medicineId = "med_prn_refill_boundary",
            userId = user.userId,
            medicationName = "Ibuprofen PRN",
            dosage = 1.0,
            dosageUnit = DosageUnit.TABLET,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.AS_NEEDED),
            scheduledTimes = listOf(LocalTime.of(12, 0)),
            currentStock = 3,
            refillThreshold = 3,
            autoDeductOnTake = true
        )
        db.medicineDao().insertMedicine(prnMed)

        // Log as-needed dose: stock drops from 3 to 2
        scheduleRepository.logAsNeededDose(prnMed, user.userId, context = context)

        val updatedMed = db.medicineDao().getMedicineByIdDirect(prnMed.medicineId)
        assertNotNull(updatedMed)
        assertEquals(2, updatedMed!!.currentStock)

        val refillNotifId = (prnMed.medicineId + "_refill").hashCode()
        val activeNotifs = notificationManager.activeNotifications
        assertTrue("Refill notification must be posted on PRN dose when stock transitions from threshold to below threshold",
            activeNotifs.any { it.id == refillNotifId })
    }
}
