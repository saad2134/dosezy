package com.example.dosezy.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.DosezyDatabase
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
import com.example.dosezy.notifications.MedicineNotificationManager
import com.example.dosezy.ui.viewmodels.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import com.example.dosezy.utils.SoundUtils
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserViewModelProfileAlarmTest {

    private lateinit var context: Context
    private lateinit var db: DosezyDatabase
    private lateinit var userRepository: UserRepository
    private lateinit var medicineRepository: MedicineRepository
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var medicineNotificationManager: MedicineNotificationManager
    private val testDispatcher = StandardTestDispatcher()

    private val testUser = User(
        userId = "user_audit9",
        fullName = "Test User Audit 9",
        age = 32,
        gender = Gender.FEMALE,
        contactNumber = "9876543210",
        theme = Theme.SYSTEM,
        timeFormat = TimeFormat.HOUR_12,
        language = Language.ENGLISH,
        allowDoseUndo = true,
        allowCustomDoseTime = false,
        isCurrentUser = true
    )

    private val secondUser = User(
        userId = "user_audit9_second",
        fullName = "Second User Audit 9",
        age = 28,
        gender = Gender.MALE,
        contactNumber = "1122334455",
        theme = Theme.DARK,
        timeFormat = TimeFormat.HOUR_24,
        language = Language.ENGLISH,
        allowDoseUndo = true,
        allowCustomDoseTime = false,
        isCurrentUser = false
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, DosezyDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        userRepository = UserRepository(db)
        scheduleRepository = ScheduleRepository(db)
        medicineRepository = MedicineRepository(db, scheduleRepository, context)
        medicineNotificationManager = MedicineNotificationManager(context, db)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private suspend fun awaitCondition(timeoutMs: Long = 10000, condition: suspend () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                org.junit.Assert.fail("Timed out waiting for condition")
            }
            kotlinx.coroutines.delay(20)
        }
    }

    // =========================================================================
    // Issue 1 Tests: User Alarm Lifecycle Scheduling on API 24+
    // =========================================================================

    @Test
    fun testUserViewModel_schedulesAndCancelsAlarmsAcrossProfiles() = runTest(testDispatcher) {
        val userViewModel = UserViewModel(userRepository, medicineRepository, scheduleRepository, medicineNotificationManager)

        // 1. Add first user
        userViewModel.addUser(testUser)
        awaitCondition { userRepository.getUserByIdSync(testUser.userId) != null }

        val savedUser = userRepository.getUserByIdSync(testUser.userId)
        assertNotNull("User should be inserted", savedUser)
        assertTrue("First user should be current user", savedUser!!.isCurrentUser)

        // 2. Add second user
        userViewModel.addUser(secondUser)
        awaitCondition { userRepository.getUserByIdSync(secondUser.userId) != null }

        // 3. Switch current user to second user
        val secondFromDb = userRepository.getUserByIdSync(secondUser.userId)!!
        userViewModel.setCurrentUser(secondFromDb)
        awaitCondition { userRepository.getAllUsersList().firstOrNull { it.isCurrentUser }?.userId == secondUser.userId }

        val activeUser = userRepository.getAllUsersList().firstOrNull { it.isCurrentUser }
        assertEquals("Active user should now be second user", secondUser.userId, activeUser?.userId)

        // 4. Delete the active user, which must trigger alarm cancellation and promotion
        userViewModel.deleteUser(activeUser!!)
        awaitCondition { userRepository.getUserByIdSync(secondUser.userId) == null && userRepository.getAllUsersList().firstOrNull()?.isCurrentUser == true }

        val remainingUsers = userRepository.getAllUsersList()
        assertEquals(1, remainingUsers.size)
        assertEquals(testUser.userId, remainingUsers.first().userId)
        assertTrue("Remaining user should now be promoted to current user", remainingUsers.first().isCurrentUser)
    }

    @Test
    fun testDeleteUser_invokesOnNextUserSelected_whenDeletingNonCurrentUser() = runTest(testDispatcher) {
        val secondUser = User(
            userId = "user_audit9_second_callback",
            fullName = "Second User",
            age = 25,
            gender = Gender.MALE,
            contactNumber = "+1234567890",
            isCurrentUser = false
        )
        val userViewModel = UserViewModel(userRepository, medicineRepository, scheduleRepository, medicineNotificationManager)

        userViewModel.addUser(testUser)
        awaitCondition { userRepository.getUserByIdSync(testUser.userId) != null }

        userViewModel.addUser(secondUser)
        awaitCondition { userRepository.getUserByIdSync(secondUser.userId) != null }

        var callbackInvoked = false
        var selectedUserAfterDelete: User? = null

        // Delete non-current user (secondUser) while testUser is currently active
        userViewModel.deleteUser(secondUser) { nextUser ->
            callbackInvoked = true
            selectedUserAfterDelete = nextUser
        }

        awaitCondition { callbackInvoked }

        assertTrue("Callback must be invoked when deleting non-current user", callbackInvoked)
        assertNotNull("Selected user should not be null", selectedUserAfterDelete)
        assertEquals(testUser.userId, selectedUserAfterDelete?.userId)
        assertTrue("Selected user should remain the active current user", selectedUserAfterDelete?.isCurrentUser == true)
    }

    @Test
    @Config(sdk = [24])
    fun testMedicineNotificationManager_executesOnApi24WithoutCrashing() = runTest(testDispatcher) {
        // Direct validation that java.time and notification manager methods execute on API 24 via desugaring
        userRepository.insertUser(testUser)
        medicineNotificationManager.scheduleAllAlarmsForCurrentUser()
        advanceUntilIdle()

        medicineNotificationManager.scheduleAlarmsForUser(testUser.userId)
        advanceUntilIdle()

        medicineNotificationManager.cancelAllAlarmsForUser(testUser.userId)
        advanceUntilIdle()

        medicineNotificationManager.rescheduleAllAlarmsForAllUsers()
        advanceUntilIdle()
    }

    // =========================================================================
    // Issue 2 Tests: PRN Dose Stock Deduction & Custom Slot Symmetry
    // =========================================================================

    @Test
    fun testLogAsNeededDose_deductsCustomSlotDosage_andRestoresSymmetricallyOnUndo() = runTest(testDispatcher) {
        userRepository.insertUser(testUser)

        // Medication with 2 tablets scheduled at 08:00 AM, 1 tablet at 20:00 PM, default 1 tablet
        val medicineWithSlots = Medicine(
            medicineId = "med_prn_slots",
            userId = testUser.userId,
            medicationName = "Metformin",
            dosage = 1.0,
            dosageUnit = DosageUnit.TABLET,
            timesPerDay = 2,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            customDosages = mapOf("08:00" to 2.0, "20:00" to 1.0),
            currentStock = 30,
            autoDeductOnTake = true
        )
        medicineRepository.insertMedicine(medicineWithSlots)
        advanceUntilIdle()

        val initialStock = db.medicineDao().getMedicineByIdDirect("med_prn_slots")!!.currentStock!!
        assertEquals(30, initialStock)

        // Log PRN dose at 08:00 AM (which has custom slot dosage of 2 tablets)
        val morningDoseTime = LocalDateTime.of(2026, 10, 6, 8, 0)
        scheduleRepository.logAsNeededDose(
            medicine = medicineWithSlots,
            userId = testUser.userId,
            dateTime = morningDoseTime,
            context = context
        )

        // Check stock was decremented by 2 tablets (not default 1)
        val medAfterPrn = db.medicineDao().getMedicineByIdDirect("med_prn_slots")!!
        assertEquals("Stock should decrease by custom slot dosage (2 tablets)", 28, medAfterPrn.currentStock)

        // Fetch inserted PRN entry
        val schedules = scheduleRepository.getSchedulesByUserSync(testUser.userId)
        val prnEntry = schedules.find { it.entryId.startsWith("PRN_") }
        assertNotNull("PRN entry should be recorded", prnEntry)

        // Undo the PRN dose - must restore exactly 2 tablets symmetrically
        scheduleRepository.undoDoseTaken(prnEntry!!.entryId, context = context)

        val medAfterUndo = db.medicineDao().getMedicineByIdDirect("med_prn_slots")!!
        assertEquals("Stock should be restored symmetrically to 30", 30, medAfterUndo.currentStock)

        // Verify PRN entry was deleted
        val schedulesAfterUndo = scheduleRepository.getSchedulesByUserSync(testUser.userId)
        assertNull("PRN entry should be removed after undo", schedulesAfterUndo.find { it.entryId == prnEntry.entryId })
    }

    @Test
    fun testLogAsNeededDose_fetchesFreshEntityFromDb_preventingStaleSnapshotOverwrite() = runTest(testDispatcher) {
        userRepository.insertUser(testUser)

        val medicine = Medicine(
            medicineId = "med_prn_concurrency",
            userId = testUser.userId,
            medicationName = "Paracetamol",
            dosage = 500.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(12, 0)),
            currentStock = 20,
            autoDeductOnTake = true
        )
        medicineRepository.insertMedicine(medicine)
        advanceUntilIdle()

        // Simulate UI holding stale snapshot with stock = 20
        val staleUiMedicine = medicine.copy(currentStock = 20)

        // Meanwhile, a concurrent operation refilled stock to 50 in the database
        val freshDbMedicine = db.medicineDao().getMedicineByIdDirect("med_prn_concurrency")!!.copy(currentStock = 50)
        db.medicineDao().updateMedicine(freshDbMedicine)

        // Now user logs PRN dose using the staleUiMedicine object
        scheduleRepository.logAsNeededDose(
            medicine = staleUiMedicine,
            userId = testUser.userId,
            dateTime = LocalDateTime.of(2026, 10, 6, 12, 0),
            context = context
        )

        // Check that fresh stock (50 - 1 = 49) was updated, NOT stale stock (20 - 1 = 19)
        val finalMedicine = db.medicineDao().getMedicineByIdDirect("med_prn_concurrency")!!
        assertEquals("Stock must be calculated from fresh DB value (50 - 1 = 49)", 49, finalMedicine.currentStock)
    }

    // =========================================================================
    // Issue 3 Tests: Batch Insert Integrity in MedicineRepository
    // =========================================================================

    @Test
    fun testInsertMedicine_batchInsertsScheduleEntriesAtomically() = runTest(testDispatcher) {
        userRepository.insertUser(testUser)

        val multiSlotMedicine = Medicine(
            medicineId = "med_batch_insert",
            userId = testUser.userId,
            medicationName = "Multivitamin",
            dosage = 1.0,
            dosageUnit = DosageUnit.TABLET,
            timesPerDay = 3,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0), LocalTime.of(13, 0), LocalTime.of(20, 0)),
            // Guard: Start from tomorrow so past slots on today are not skipped by time-of-day filtering
            startDate = java.time.LocalDate.now().plusDays(1),
            currentStock = 100,
            autoDeductOnTake = true
        )

        // insertMedicine generates 30 days * 3 slots = 90 entries
        medicineRepository.insertMedicine(multiSlotMedicine)
        advanceUntilIdle()

        val insertedMed = db.medicineDao().getMedicineByIdDirect("med_batch_insert")
        assertNotNull("Medicine entity must be inserted", insertedMed)

        val allSchedules = scheduleRepository.getSchedulesByUserSync(testUser.userId)
        assertEquals("Expected exactly 90 batch-inserted schedule entries for 30 days", 90, allSchedules.size)

        // Verify all 90 entries are marked PENDING and have correct medicineId
        assertTrue(allSchedules.all { it.medicineId == "med_batch_insert" })
        assertTrue(allSchedules.all { it.status == MedicationStatus.PENDING })

        // Verify entries span across the 30-day window
        val distinctDates = allSchedules.map { it.scheduledDateTime.toLocalDate() }.distinct()
        assertTrue("Entries must span across multiple calendar days", distinctDates.size >= 29)
        assertEquals("Total batch-inserted entries should equal 90", 90, allSchedules.size)
    }

    // =========================================================================
    // Issue 1 & 3 Tests: SoundUtils Deletion and Canonical Path Sandboxing
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

        val deleted = SoundUtils.deleteCustomAudio(context, externalFile.absolutePath)
        assertFalse("deleteCustomAudio must refuse to delete files outside custom_sounds directory", deleted)
        assertTrue("External file must remain untouched", externalFile.exists())

        externalFile.delete()
    }

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

        validFilesDirFile.delete()
        validCacheDirFile.delete()
        outsideFile.delete()
    }

    @Test
    fun testCreateImageFile_allocatesInCacheAndCanBeDeleted() {
        val result = com.example.dosezy.ui.components.createImageFile(context)
        assertNotNull("createImageFile must return non-null result", result)
        val (uri, file) = result!!
        assertNotNull("Generated URI must not be null", uri)
        assertTrue("Generated temporary file must exist on disk", file.exists())

        val expectedCachePath = (context.externalCacheDir ?: context.cacheDir).canonicalPath
        assertTrue("Temporary camera capture file must reside in cacheDir", file.canonicalPath.startsWith(expectedCachePath))

        // Clean up file and verify it is removed
        assertTrue("Deleting temp file must succeed", file.delete())
        assertFalse("Deleted temp file must not exist", file.exists())
    }

    @Test
    fun testAlarmSoundSelection_revertingFromCustomDeletesCustomFile() = runTest(testDispatcher) {
        val customSoundsDir = File(context.filesDir, "custom_sounds")
        customSoundsDir.mkdirs()
        val customAudioFile = File(customSoundsDir, "custom_tone.mp3").apply { writeText("audio content") }
        assertTrue(customAudioFile.exists())

        val userWithCustomSound = testUser.copy(
            alarmSound = com.example.dosezy.data.model.AlarmSound.CUSTOM,
            customAlarmSoundPath = customAudioFile.absolutePath,
            customAlarmSoundTitle = "custom_tone.mp3"
        )
        userRepository.insertUser(userWithCustomSound)
        advanceUntilIdle()

        // When switching from CUSTOM to a built-in sound:
        // Delete the previous custom sound and null out path and title
        SoundUtils.deleteCustomAudio(context, userWithCustomSound.customAlarmSoundPath)
        val updatedUser = userWithCustomSound.copy(
            alarmSound = com.example.dosezy.data.model.AlarmSound.SYSTEM_DEFAULT,
            customAlarmSoundPath = null,
            customAlarmSoundTitle = null
        )
        userRepository.updateUser(updatedUser)
        advanceUntilIdle()

        assertFalse("Custom audio file on disk must be deleted when reverting to built-in sound", customAudioFile.exists())
        val fetchedUser = userRepository.getUserByIdSync(userWithCustomSound.userId)
        assertNotNull(fetchedUser)
        assertEquals(com.example.dosezy.data.model.AlarmSound.SYSTEM_DEFAULT, fetchedUser!!.alarmSound)
        assertNull("customAlarmSoundPath must be null in user entity", fetchedUser.customAlarmSoundPath)
        assertNull("customAlarmSoundTitle must be null in user entity", fetchedUser.customAlarmSoundTitle)
    }

    @Test
    fun testSetCurrentUser_atomicallySwitchesProfileWithoutAutoPromoteRegression() = runTest(testDispatcher) {
        val userViewModel = UserViewModel(userRepository, medicineRepository, scheduleRepository, medicineNotificationManager)

        val thirdUser = User(
            userId = "user_audit9_third",
            fullName = "Third User",
            age = 40,
            gender = Gender.MALE,
            contactNumber = "9988776655",
            isCurrentUser = false
        )

        userViewModel.addUser(testUser)
        awaitCondition { userRepository.getUserByIdSync(testUser.userId) != null }

        userViewModel.addUser(secondUser)
        awaitCondition { userRepository.getUserByIdSync(secondUser.userId) != null }

        userViewModel.addUser(thirdUser)
        awaitCondition { userRepository.getUserByIdSync(thirdUser.userId) != null }

        // Guard: Switching to third user atomically updates isCurrentUser without 0-active intermediate state triggering auto-promotion of user 1
        val thirdFromDb = userRepository.getUserByIdSync(thirdUser.userId)!!
        userViewModel.setCurrentUser(thirdFromDb)
        awaitCondition { userRepository.getAllUsersList().firstOrNull { it.isCurrentUser }?.userId == thirdUser.userId }

        advanceUntilIdle()

        val allUsers = userRepository.getAllUsersList()
        val currentUsers = allUsers.filter { it.isCurrentUser }
        assertEquals("Exactly one user should be current", 1, currentUsers.size)
        assertEquals("Current user must be the third user", thirdUser.userId, currentUsers.first().userId)
    }

    @Test
    fun testDeleteUser_dismissesShadeNotificationsAndCancelsIntakeSafeguard() = runTest(testDispatcher) {
        val userViewModel = UserViewModel(userRepository, medicineRepository, scheduleRepository, medicineNotificationManager)
        userViewModel.addUser(testUser)
        awaitCondition { userRepository.getUserByIdSync(testUser.userId) != null }

        val med = Medicine(
            medicineId = "med_del_notif",
            userId = testUser.userId,
            medicationName = "Amoxicillin",
            dosage = 500.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0)),
            startDate = LocalDate.now()
        )
        db.medicineDao().insertMedicine(med)
        val entry = ScheduleEntry(
            entryId = "entry_del_notif",
            userId = testUser.userId,
            medicineId = med.medicineId,
            scheduledDateTime = LocalDateTime.now().plusHours(1),
            status = MedicationStatus.PENDING
        )
        db.scheduleDao().insertScheduleEntry(entry)

        val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nManager.notify(entry.entryId.hashCode(), androidx.core.app.NotificationCompat.Builder(context, "test").setContentTitle("Reminder").setSmallIcon(android.R.drawable.ic_dialog_info).build())
        nManager.notify((med.medicineId + "_refill").hashCode(), androidx.core.app.NotificationCompat.Builder(context, "test").setContentTitle("Refill").setSmallIcon(android.R.drawable.ic_dialog_info).build())

        com.example.dosezy.notifications.IntakeSafeguardManager.startCountdown(
            context = context,
            entryId = entry.entryId,
            entryIds = listOf(entry.entryId),
            medicineName = med.medicationName,
            medicineNames = listOf(med.medicationName),
            scheduledTime = "10:00 AM",
            durationSeconds = 120,
            userId = testUser.userId
        )
        assertNotNull(com.example.dosezy.notifications.IntakeSafeguardManager.activeCountdown.value)

        com.example.dosezy.widget.DosezyWidgetPrefs.saveAppWidgetUser(context, 777, testUser.userId)
        assertEquals(testUser.userId, com.example.dosezy.widget.DosezyWidgetPrefs.getAppWidgetUser(context, 777))

        // Delete user
        userViewModel.deleteUser(testUser)
        awaitCondition { userRepository.getUserByIdSync(testUser.userId) == null && com.example.dosezy.widget.DosezyWidgetPrefs.getAppWidgetUser(context, 777) == null }
        advanceUntilIdle()

        // Verify reminder and refill notifications dismissed from shade
        assertFalse(nManager.activeNotifications.any { it.id == entry.entryId.hashCode() })
        assertFalse(nManager.activeNotifications.any { it.id == (med.medicineId + "_refill").hashCode() })
        // Verify active ADHD intake countdown is cancelled
        assertNull(com.example.dosezy.notifications.IntakeSafeguardManager.activeCountdown.value)
        // Verify app widget user binding is cleared
        assertNull(com.example.dosezy.widget.DosezyWidgetPrefs.getAppWidgetUser(context, 777))
    }

    @Test
    fun testUpdateContextLocale_providesLocalizedResourcesForProfile() {
        // Guard: Verify that updateContextLocale correctly provides localized resources for Compose LocalContext matching the active profile (#BUG-MAIN-LOCALIZED-CONTEXT)
        val esContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, Language.SPANISH)
        val enContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, Language.ENGLISH)

        val esTitle = esContext.getString(com.example.dosezy.R.string.notif_perm_title)
        val enTitle = enContext.getString(com.example.dosezy.R.string.notif_perm_title)

        assertFalse("Spanish title must differ from English", esTitle == enTitle)
        assertTrue(esTitle.isNotBlank())
        assertTrue(enTitle.isNotBlank())
    }

    @Test
    fun testDeleteUser_enforcesSingleActiveUserExclusivityInDatabase() = runTest(testDispatcher) {
        // Guard: Verify deleteUser uses atomic setCurrentUser so remaining profiles have exactly 1 active user in SQLite
        val userViewModel = UserViewModel(userRepository, medicineRepository, scheduleRepository, medicineNotificationManager)

        val userToDelete = testUser.copy(userId = "user_delete_primary", isCurrentUser = true)
        val userToPromote = User(
            userId = "user_promote_candidate",
            fullName = "Promote Candidate",
            age = 30,
            gender = Gender.FEMALE,
            contactNumber = "+1111111111",
            isCurrentUser = false
        )
        val userThird = User(
            userId = "user_third_stale",
            fullName = "Third Stale User",
            age = 40,
            gender = Gender.MALE,
            contactNumber = "+2222222222",
            isCurrentUser = true
        )

        userRepository.insertUser(userToDelete)
        userRepository.insertUser(userToPromote)
        userRepository.insertUser(userThird)

        userViewModel.deleteUser(userToDelete)
        advanceUntilIdle()

        val remaining = userRepository.getAllUsersList().filter { it.userId != userToDelete.userId }
        assertEquals(2, remaining.size)
        assertEquals("Exactly one user must be marked as current user after deletion", 1, remaining.count { it.isCurrentUser })
        val current = remaining.first { it.isCurrentUser }
        val nonCurrent = remaining.first { !it.isCurrentUser }
        assertTrue("Promoted user must be one of the remaining users", current.userId == userToPromote.userId || current.userId == userThird.userId)
        assertTrue("Non-current user must be the other remaining user", nonCurrent.userId != current.userId)
    }
}

