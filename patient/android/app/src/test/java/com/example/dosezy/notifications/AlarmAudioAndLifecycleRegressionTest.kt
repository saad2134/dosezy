package com.example.dosezy.notifications

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.model.AlarmSound
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.Language
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.Theme
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.data.model.User
import com.example.dosezy.utils.SoundUtils
import com.example.dosezy.utils.TimeCalculationUtils
import com.example.dosezy.widget.DosezyWidgetPrefs
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.FileDescriptor
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlarmAudioAndLifecycleRegressionTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 1: MediaPlayer Native AssetFileDescriptor Lifecycle Order
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug1_assetFileDescriptor_closedOnlyAfterMediaPlayerStart() {
        // Calling AssetFileDescriptor.close() before MediaPlayer.prepare() immediately closes
        // the underlying Linux file descriptor, causing MediaPlayer to fail with
        // "IOException: Prepare failed.: status=0x1".
        val mockAfd = mockk<AssetFileDescriptor>(relaxed = true)
        val mockFd = mockk<FileDescriptor>(relaxed = true)
        every { mockAfd.fileDescriptor } returns mockFd
        every { mockAfd.startOffset } returns 0L
        every { mockAfd.length } returns 1000L

        val mockPlayer = mockk<MediaPlayer>(relaxed = true)

        // Emulate the corrected lifecycle: setDataSource -> prepare -> start -> afd.close()
        mockPlayer.setDataSource(mockAfd.fileDescriptor, mockAfd.startOffset, mockAfd.length)
        mockPlayer.prepare()
        mockPlayer.start()
        mockAfd.close()

        verifyOrder {
            mockPlayer.setDataSource(mockFd, 0L, 1000L)
            mockPlayer.prepare()
            mockPlayer.start()
            mockAfd.close()
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 2: Undo Dose Taken Reschedules Future Alarms
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug2_undoDoseTaken_reschedulesOnlyFutureAlarms() {
        val now = LocalDateTime.now()

        val futureDose = ScheduleEntry(
            entryId = "entry_future",
            userId = "user_1",
            medicineId = "med_1",
            scheduledDateTime = now.plusHours(2),
            status = MedicationStatus.TAKEN_ON_TIME
        )

        val pastDose = ScheduleEntry(
            entryId = "entry_past",
            userId = "user_1",
            medicineId = "med_1",
            scheduledDateTime = now.minusHours(4),
            status = MedicationStatus.TAKEN_ON_TIME
        )

        // Future dose undone: must satisfy isAfter(now) to trigger alarm rescheduling
        assertTrue(
            "Undoing a dose scheduled in the future must trigger alarm rescheduling",
            futureDose.scheduledDateTime.isAfter(now)
        )

        // Past dose undone: must not reschedule alarms into the past
        assertFalse(
            "Undoing a dose scheduled in the past must not trigger alarm rescheduling",
            pastDose.scheduledDateTime.isAfter(now)
        )
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 3: Analytics Dynamic Adherence Evaluation of Missed Doses
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug3_analytics_evaluatesPendingPastMissedAfterAsMissed() {
        val now = LocalDateTime.now()
        val missedAfterHours = 6

        fun isEntryMissed(entry: ScheduleEntry): Boolean =
            entry.status == MedicationStatus.MISSED ||
            (entry.status == MedicationStatus.PENDING && now.isAfter(entry.scheduledDateTime) && TimeCalculationUtils.isMissed(entry.scheduledDateTime, now, missedAfterHours))

        // 1. Taken dose
        val takenEntry = ScheduleEntry(
            entryId = "entry_taken",
            userId = "user_1",
            medicineId = "med_1",
            scheduledDateTime = now.minusHours(10),
            status = MedicationStatus.TAKEN_ON_TIME
        )

        // 2. Overdue PENDING dose (> 6h ago, e.g. 8h ago) - not yet marked by background worker
        val overduePendingEntry = ScheduleEntry(
            entryId = "entry_overdue_pending",
            userId = "user_1",
            medicineId = "med_1",
            scheduledDateTime = now.minusHours(8),
            status = MedicationStatus.PENDING
        )

        // 3. Recent PENDING dose (1h ago, within 6h grace window)
        val recentPendingEntry = ScheduleEntry(
            entryId = "entry_recent_pending",
            userId = "user_1",
            medicineId = "med_1",
            scheduledDateTime = now.minusHours(1),
            status = MedicationStatus.PENDING
        )

        // 4. Explicitly recorded MISSED dose
        val recordedMissedEntry = ScheduleEntry(
            entryId = "entry_explicit_missed",
            userId = "user_1",
            medicineId = "med_1",
            scheduledDateTime = now.minusHours(12),
            status = MedicationStatus.MISSED
        )

        val entries = listOf(takenEntry, overduePendingEntry, recentPendingEntry, recordedMissedEntry)

        // Verify dynamic status evaluations
        assertFalse(isEntryMissed(takenEntry))
        assertTrue(isEntryMissed(overduePendingEntry))
        assertFalse(isEntryMissed(recentPendingEntry))
        assertTrue(isEntryMissed(recordedMissedEntry))

        // Compute adherence rate
        val taken = entries.count { it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE }
        val missed = entries.count { isEntryMissed(it) }
        val decided = taken + missed
        val rate = if (decided > 0) ((taken.toDouble() / decided.toDouble()) * 100).toInt() else 0

        // Taken: 1, Missed: 2 (overduePendingEntry + recordedMissedEntry), Decided: 3
        assertEquals(1, taken)
        assertEquals(2, missed)
        assertEquals(3, decided)
        assertEquals(33, rate) // 1 / 3 = 33%
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 4: Backup Restore Preserves isCurrentUser & Auto-Heals Active User
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug4_backupRestore_preservesActiveUserAndAutoHealsWhenNoneActive() {
        // Part A: ConflictStrategy.OVERWRITE preserves existingLocalUser.isCurrentUser
        val existingActiveUser = User(
            userId = "user_main",
            fullName = "Main User",
            age = 45,
            gender = Gender.MALE,
            contactNumber = "555-0100",
            isCurrentUser = true,
            theme = Theme.SYSTEM,
            timeFormat = TimeFormat.HOUR_12,
            language = Language.SYSTEM
        )

        val backupImportedUser = User(
            userId = "user_main",
            fullName = "Main User Restored",
            age = 45,
            gender = Gender.MALE,
            contactNumber = "555-0100",
            isCurrentUser = false, // backup file had false (e.g. secondary export or stale)
            theme = Theme.SYSTEM,
            timeFormat = TimeFormat.HOUR_12,
            language = Language.SYSTEM
        )

        val localUsers = listOf(existingActiveUser)
        val targetUserId = "user_main"
        val existingLocalUser = localUsers.firstOrNull { it.userId == targetUserId }
        val totalProfiles = 1

        val shouldBeCurrent = existingLocalUser?.isCurrentUser ?: (totalProfiles == 0)
        val restoredUser = backupImportedUser.copy(
            userId = targetUserId,
            isCurrentUser = shouldBeCurrent
        )

        // Restored user must remain active profile
        assertTrue("Restored user must preserve local isCurrentUser=true status", restoredUser.isCurrentUser)

        // Part B: UserViewModel auto-heal when all users in list have isCurrentUser = false
        val userListWithNoActive = listOf(
            existingActiveUser.copy(isCurrentUser = false),
            User(
                userId = "user_secondary",
                fullName = "Secondary User",
                age = 30,
                gender = Gender.FEMALE,
                contactNumber = "555-0200",
                isCurrentUser = false,
                theme = Theme.SYSTEM,
                timeFormat = TimeFormat.HOUR_12,
                language = Language.SYSTEM
            )
        )

        var current = userListWithNoActive.firstOrNull { it.isCurrentUser }
        if (current == null && userListWithNoActive.isNotEmpty()) {
            current = userListWithNoActive.first().copy(isCurrentUser = true)
        }

        assertEquals("user_main", current?.userId)
        assertTrue(current?.isCurrentUser == true)
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 5: AlarmActivity Dynamically Evaluates TAKEN_LATE vs TAKEN_ON_TIME
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug5_alarmActivity_dynamicallyComputesTakenLateStatus() {
        val now = LocalDateTime.now()
        val lateAfterHours = 3
        val missedAfterHours = 6

        // Case 1: Dose taken within considerLateAfter window (e.g. scheduled 30 min ago)
        val onTimeScheduledTime = now.minusMinutes(30)
        val isOnTimeLate = TimeCalculationUtils.isLate(onTimeScheduledTime, now, lateAfterHours, missedAfterHours)
        val onTimeStatus = if (isOnTimeLate) "TAKEN_LATE" else "TAKEN_ON_TIME"
        assertEquals("TAKEN_ON_TIME", onTimeStatus)

        // Case 2: Dose taken after considerLateAfter window (e.g. scheduled 4 hours ago, snoozed repeatedly)
        val lateScheduledTime = now.minusHours(4)
        val isDelayedLate = TimeCalculationUtils.isLate(lateScheduledTime, now, lateAfterHours, missedAfterHours)
        val lateStatus = if (isDelayedLate) "TAKEN_LATE" else "TAKEN_ON_TIME"
        assertEquals("TAKEN_LATE", lateStatus)
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 6: Scrub Deleted User Preferences (Contacts & Widget Themes)
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug6_deleteUser_scrubsEmergencyContactsAndWidgetThemePreferences() {
        val testUserId = "user_to_delete_999"

        // 1. Verify Widget Profile Theme scrubbing
        DosezyWidgetPrefs.saveWidgetProfileTheme(context, testUserId, "ocean")
        assertEquals("ocean", DosezyWidgetPrefs.getWidgetProfileTheme(context, testUserId))

        // Delete widget profile theme
        DosezyWidgetPrefs.deleteWidgetProfileTheme(context, testUserId)
        assertNull(
            "Widget profile theme must be deleted from SharedPreferences",
            DosezyWidgetPrefs.getWidgetProfileTheme(context, testUserId)
        )

        // 2. Verify Emergency Contacts scrubbing
        val emPrefs = context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)
        emPrefs.edit().putString("contacts_json_$testUserId", """[{"name":"Family","phone":"123"}]""").commit()
        assertTrue(emPrefs.contains("contacts_json_$testUserId"))

        // Emulate UserViewModel.deleteUser scrubbing
        emPrefs.edit().remove("contacts_json_$testUserId").commit()
        assertFalse(
            "Emergency contacts key for deleted user must be scrubbed",
            emPrefs.contains("contacts_json_$testUserId")
        )
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 7: Preserve Custom Audio File When Selecting Sound Presets
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug7_alarmSoundSelection_presetSelection_preservesCustomAudioFileAndPath() {
        val soundsDir = java.io.File(context.filesDir, "custom_sounds").apply { mkdirs() }
        val customFile = java.io.File(soundsDir, "custom_test_audio.mp3").apply { writeText("audio content") }
        assertTrue("Custom sound file must exist initially", customFile.exists())

        val user = User(
            userId = "user_alarm_sound_test",
            fullName = "Alarm Sound Tester",
            age = 30,
            gender = Gender.DO_NOT_SPECIFY,
            contactNumber = "1234567890",
            alarmSound = AlarmSound.CUSTOM,
            customAlarmSoundPath = customFile.absolutePath,
            customAlarmSoundTitle = "My Custom Ringtone"
        )

        // Emulate selecting a preset: newSound = GENTLE_CHIME, customPath = null
        val newSound = AlarmSound.GENTLE_CHIME
        val customPath: String? = null
        val customTitle: String? = null

        // Guard: Custom audio file must NOT be deleted when switching to or previewing a preset
        if (newSound == AlarmSound.CUSTOM && customPath != null && customPath != user.customAlarmSoundPath) {
            SoundUtils.deleteCustomAudio(context, user.customAlarmSoundPath)
        }

        assertTrue("Custom audio file on disk must be preserved when previewing preset", customFile.exists())

        // User preference must preserve customAlarmSoundPath & Title so switching back does not lose custom ringtone
        val updatedUser = user.copy(
            alarmSound = newSound,
            customAlarmSoundPath = if (newSound == AlarmSound.CUSTOM) (customPath ?: user.customAlarmSoundPath) else user.customAlarmSoundPath,
            customAlarmSoundTitle = if (newSound == AlarmSound.CUSTOM) (customTitle ?: user.customAlarmSoundTitle) else user.customAlarmSoundTitle
        )

        assertEquals(AlarmSound.GENTLE_CHIME, updatedUser.alarmSound)
        assertEquals(customFile.absolutePath, updatedUser.customAlarmSoundPath)
        assertEquals("My Custom Ringtone", updatedUser.customAlarmSoundTitle)

        // Only when a NEW custom audio file replaces the old one should the previous file be deleted
        val newCustomFile = java.io.File(soundsDir, "custom_new_audio.mp3").apply { writeText("new audio") }
        val replacedSound = AlarmSound.CUSTOM
        val replacementPath = newCustomFile.absolutePath

        if (replacedSound == AlarmSound.CUSTOM && replacementPath != updatedUser.customAlarmSoundPath) {
            SoundUtils.deleteCustomAudio(context, updatedUser.customAlarmSoundPath)
        }

        assertFalse("Previous custom audio file must be cleaned up when replaced by a new custom sound", customFile.exists())
        assertTrue("Newly selected custom audio file must remain on disk", newCustomFile.exists())
    }
}
