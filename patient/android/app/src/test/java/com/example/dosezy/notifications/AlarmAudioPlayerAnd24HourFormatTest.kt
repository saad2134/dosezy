package com.example.dosezy.notifications

import android.app.AlarmManager
import android.content.Context
import android.media.MediaPlayer
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.R
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.Language
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.notifications.AlarmAudioPlayer
import com.example.dosezy.notifications.AlarmScheduler
import com.example.dosezy.notifications.MedicineAlarmReceiver
import com.example.dosezy.utils.LocaleHelper
import com.example.dosezy.utils.TimeFormatUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlarmAudioPlayerAnd24HourFormatTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Reset AlarmAudioPlayer state before each test
        AlarmAudioPlayer.stop()
        context.getSharedPreferences(AlarmScheduler.PREFS_COORDINATION, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 1: MediaPlayer Exception & Sink Leak Prevention
    // ───────────────────────────────────────────────────────────────

    @Test
    fun issue1_alarmAudioPlayer_stop_safelyReleasesPlayerWhenIsPlayingThrowsIllegalStateException() {
        val field = AlarmAudioPlayer::class.java.getDeclaredField("mediaPlayer")
        field.isAccessible = true

        val mockPlayer = mockk<MediaPlayer>(relaxed = true)
        every { mockPlayer.isPlaying } throws IllegalStateException("Simulated native decoder error state")

        field.set(AlarmAudioPlayer, mockPlayer)

        // Must not throw IllegalStateException and must call release() despite error in isPlaying
        AlarmAudioPlayer.stop()

        verify(exactly = 1) { mockPlayer.release() }
        assertNull("mediaPlayer field must be cleared to null after stop()", field.get(AlarmAudioPlayer))
        assertFalse(AlarmAudioPlayer.isPlaying())
    }

    @Test
    fun issue1_alarmAudioPlayer_stop_safelyReleasesPlayerWhenStopThrowsIllegalStateException() {
        val field = AlarmAudioPlayer::class.java.getDeclaredField("mediaPlayer")
        field.isAccessible = true

        val mockPlayer = mockk<MediaPlayer>(relaxed = true)
        every { mockPlayer.isPlaying } returns true
        every { mockPlayer.stop() } throws IllegalStateException("Simulated native stop error state")

        field.set(AlarmAudioPlayer, mockPlayer)

        // Must catch stop() failure and still invoke release()
        AlarmAudioPlayer.stop()

        verify(exactly = 1) { mockPlayer.release() }
        assertNull("mediaPlayer field must be cleared to null after stop()", field.get(AlarmAudioPlayer))
        assertFalse(AlarmAudioPlayer.isPlaying())
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 2: 24-Hour vs 12-Hour Time Format Resolution & Alarm Extras
    // ───────────────────────────────────────────────────────────────

    @Test
    fun issue2_timeFormatUtils_respectsHour24AndHour12Formats() {
        val eveningDateTime = LocalDateTime.of(2026, 10, 6, 20, 45)
        val morningLocalTime = LocalTime.of(8, 5)

        // 24-hour format
        val formatted24 = TimeFormatUtils.formatTime(eveningDateTime, TimeFormat.HOUR_24, Locale.US)
        assertEquals("20:45", formatted24)

        val local24 = TimeFormatUtils.formatLocalTime(morningLocalTime, TimeFormat.HOUR_24, Locale.US)
        assertEquals("08:05", local24)

        // 12-hour format
        val formatted12 = TimeFormatUtils.formatTime(eveningDateTime, TimeFormat.HOUR_12, Locale.US)
        assertEquals("8:45 PM", formatted12)

        val local12 = TimeFormatUtils.formatLocalTime(morningLocalTime, TimeFormat.HOUR_12, Locale.US)
        assertEquals("8:05 AM", local12)
    }

    @Test
    fun issue2_alarmScheduler_scheduleGroupedSnooze_formats24HourCorrectly() {
        val scheduler = AlarmScheduler(context)
        val entryIds = listOf("entry_snooze_1")
        val medNames = listOf("Amoxicillin")

        // 8:30 PM = 20:30
        val explicitTrigger = LocalDateTime.of(2026, 10, 6, 20, 30)
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        scheduler.scheduleGroupedSnooze(
            entryIds = entryIds,
            minutes = 10,
            medicineNames = medNames,
            explicitTriggerTime = explicitTrigger,
            timeFormat = TimeFormat.HOUR_24
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadow = shadowOf(alarmManager)
        val scheduledAlarms = shadow.scheduledAlarms
        assertFalse("Alarm should be scheduled in AlarmManager", scheduledAlarms.isEmpty())

        val lastAlarm = scheduledAlarms.last()
        val shadowIntent = shadowOf(lastAlarm.operation)
        val intent = shadowIntent.savedIntent

        val scheduledTimeExtra = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME)
        assertNotNull(scheduledTimeExtra)
        assertEquals("20:30", scheduledTimeExtra)
    }

    @Test
    fun issue2_alarmScheduler_scheduleGroupedMedicineAlarm_formats24HourCorrectly() {
        val scheduler = AlarmScheduler(context)
        val futureEvening = LocalDateTime.now().plusDays(1).withHour(20).withMinute(0).withSecond(0).withNano(0)
        val entry = ScheduleEntry(
            entryId = "entry_24h_1",
            userId = "user_24h",
            medicineId = "med_1",
            scheduledDateTime = futureEvening,
            status = MedicationStatus.PENDING
        )

        scheduler.scheduleGroupedMedicineAlarm(
            scheduledDateTime = futureEvening,
            entries = listOf(entry),
            medicineNames = listOf("Metformin"),
            timeFormat = TimeFormat.HOUR_24
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadow = shadowOf(alarmManager)
        val scheduledAlarms = shadow.scheduledAlarms
        assertFalse("Grouped alarm should be scheduled", scheduledAlarms.isEmpty())

        val lastAlarm = scheduledAlarms.last()
        val shadowIntent = shadowOf(lastAlarm.operation)
        val intent = shadowIntent.savedIntent

        val scheduledTimeExtra = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME)
        assertNotNull(scheduledTimeExtra)
        assertEquals("20:00", scheduledTimeExtra)
    }

    @Test
    fun issue2_alarmScheduler_cancelSnooze_transferPreserves24HourFormat() {
        // Guard: Set system setting to 24-hour format so fallback logic respects 24h mode
        android.provider.Settings.System.putString(context.contentResolver, android.provider.Settings.System.TIME_12_24, "24")

        val scheduler = AlarmScheduler(context)
        val entryIds = listOf("entry_transfer_1", "entry_transfer_2", "entry_transfer_3")
        val medNames = listOf("Aspirin", "Metformin", "Lisinopril")

        // 8:30 PM = 20:30 (set 1 day in the future so triggerTime is guaranteed > System.currentTimeMillis())
        val futureEvening = LocalDateTime.now().plusDays(1).withHour(20).withMinute(30).withSecond(0).withNano(0)
        val explicitTrigger = futureEvening
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        scheduler.scheduleGroupedSnooze(
            entryIds = entryIds,
            minutes = 10,
            medicineNames = medNames,
            explicitTriggerTime = explicitTrigger,
            timeFormat = TimeFormat.HOUR_24
        )

        // When primary entry_transfer_1 is cancelled/taken, transfer to entry_transfer_2 must preserve 24-hour format
        scheduler.cancelSnooze("entry_transfer_1")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadow = shadowOf(alarmManager)
        val scheduledAlarms = shadow.scheduledAlarms
        assertFalse("Transferred alarm should be scheduled", scheduledAlarms.isEmpty())

        val lastAlarm = scheduledAlarms.last()
        val shadowIntent = shadowOf(lastAlarm.operation)
        val intent = shadowIntent.savedIntent

        val scheduledTimeExtra = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME)
        assertNotNull(scheduledTimeExtra)
        assertEquals("20:30", scheduledTimeExtra)
        assertFalse("Must not format in 12-hour format containing AM/PM", scheduledTimeExtra!!.contains("PM") || scheduledTimeExtra.contains("AM"))

        // When secondary entry_transfer_3 is cancelled, update to primary entry_transfer_2 must also preserve 24-hour format
        scheduler.cancelSnooze("entry_transfer_3")
        val updatedAlarm = shadow.scheduledAlarms.last()
        val updatedIntent = shadowOf(updatedAlarm.operation).savedIntent
        val updatedTimeExtra = updatedIntent.getStringExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME)
        assertNotNull(updatedTimeExtra)
        assertEquals("20:30", updatedTimeExtra)
        assertFalse("Must not format in 12-hour format containing AM/PM", updatedTimeExtra!!.contains("PM") || updatedTimeExtra.contains("AM"))
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 3: Profile Age, Gender, and Fallback String Localization
    // ───────────────────────────────────────────────────────────────

    @Test
    fun issue3_profileDetails_usesLocalizedResourcesWithoutRawEnglishStrings() {
        val localizedEnglish = LocaleHelper.updateContextLocale(context, Language.ENGLISH)

        val maleString = localizedEnglish.getString(R.string.gender_male)
        val formattedAgeGender = localizedEnglish.getString(
            R.string.profile_age_gender_format,
            45,
            maleString
        )
        // Must match "%1$d years • %2$s" pattern
        assertTrue(formattedAgeGender.contains("45 years • "))
        assertEquals("45 years • Male", formattedAgeGender)
        assertFalse("Must not contain hardcoded 'yrs' notation", formattedAgeGender.contains(" yrs"))

        val formattedYearsOnly = localizedEnglish.getString(R.string.years_format, 45)
        assertEquals("45 years", formattedYearsOnly)

        val fallbackProfile = localizedEnglish.getString(R.string.profile)
        assertEquals("Profile", fallbackProfile)
        assertFalse("Must not fallback to unlocalized 'Medication Profile'", fallbackProfile.contains("Medication Profile"))
    }

    @Test
    fun issue3_profileDetails_foreignLocales_translateAccurately() {
        // Spanish
        val spanishContext = LocaleHelper.updateContextLocale(context, Language.SPANISH)
        val spanishAgeGender = spanishContext.getString(
            R.string.profile_age_gender_format,
            30,
            spanishContext.getString(R.string.gender_male)
        )
        assertEquals("30 años • Masculino", spanishAgeGender)
        assertEquals("Perfil", spanishContext.getString(R.string.profile))

        // German
        val germanContext = LocaleHelper.updateContextLocale(context, Language.GERMAN)
        val germanAgeGender = germanContext.getString(
            R.string.profile_age_gender_format,
            30,
            germanContext.getString(R.string.gender_male)
        )
        assertEquals("30 Jahre • Männlich", germanAgeGender)
        assertEquals("Profil", germanContext.getString(R.string.profile))
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 4: AlarmAudioPlayer Continuous Looping Upgrade & Watchdog
    // ───────────────────────────────────────────────────────────────

    @Test
    fun issue4_alarmAudioPlayer_continuousAlarm_upgradesNonLoopingPlayer() {
        val mockPlayer = mockk<MediaPlayer>(relaxed = true)
        every { mockPlayer.isPlaying } returns true
        var looping = false
        every { mockPlayer.isLooping } answers { looping }
        every { mockPlayer.isLooping = any() } answers { looping = firstArg() }

        val field = AlarmAudioPlayer::class.java.getDeclaredField("mediaPlayer")
        field.isAccessible = true
        field.set(null, mockPlayer)

        try {
            // Incoming continuous loop alarm arrives while non-looping audio is active
            AlarmAudioPlayer.play(context, com.example.dosezy.data.model.AlarmSound.SYSTEM_DEFAULT, autoSilenceSeconds = 0)

            // Verify player was upgraded to looping and completion listener detached
            assertTrue("MediaPlayer must be upgraded to isLooping = true", looping)
            verify { mockPlayer.setOnCompletionListener(null) }
        } finally {
            AlarmAudioPlayer.stop()
        }
    }

    @Test
    fun issue4_alarmAudioPlayer_singlePlayAlarm_schedulesWatchdogTimeout() {
        val mockPlayer = mockk<MediaPlayer>(relaxed = true)
        every { mockPlayer.isPlaying } returns true
        val field = AlarmAudioPlayer::class.java.getDeclaredField("mediaPlayer")
        field.isAccessible = true
        field.set(null, mockPlayer)

        val jobField = AlarmAudioPlayer::class.java.getDeclaredField("autoSilenceJob")
        jobField.isAccessible = true

        try {
            AlarmAudioPlayer.play(context, com.example.dosezy.data.model.AlarmSound.SYSTEM_DEFAULT, autoSilenceSeconds = -1)
            val job = jobField.get(null) as? kotlinx.coroutines.Job
            assertNotNull("autoSilenceJob watchdog must be scheduled for autoSilenceSeconds = -1", job)
            assertTrue("watchdog job must be active", job?.isActive == true)
        } finally {
            AlarmAudioPlayer.stop()
        }
    }

    @Test
    fun issue4_alarmAudioPlayer_continuousAlarm_upgradesFallbackRingtoneToLooping() {
        val mockRingtone = mockk<android.media.Ringtone>(relaxed = true)
        every { mockRingtone.isPlaying } returns true
        var looping = false
        every { mockRingtone.isLooping } answers { looping }
        every { mockRingtone.isLooping = any() } answers { looping = firstArg() }

        val mpField = AlarmAudioPlayer::class.java.getDeclaredField("mediaPlayer")
        mpField.isAccessible = true
        mpField.set(null, null)

        val rtField = AlarmAudioPlayer::class.java.getDeclaredField("ringtone")
        rtField.isAccessible = true
        rtField.set(null, mockRingtone)

        try {
            // Incoming continuous loop alarm arrives while fallback ringtone is playing
            AlarmAudioPlayer.play(context, com.example.dosezy.data.model.AlarmSound.SYSTEM_DEFAULT, autoSilenceSeconds = 0)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                assertTrue("Fallback Ringtone must be upgraded to isLooping = true", looping)
            }
        } finally {
            AlarmAudioPlayer.stop()
        }
    }
}

