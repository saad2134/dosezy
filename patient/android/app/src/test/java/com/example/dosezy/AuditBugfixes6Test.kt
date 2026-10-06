package com.example.dosezy

import android.app.AlarmManager
import android.content.Context
import android.media.MediaPlayer
import androidx.test.core.app.ApplicationProvider
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
class AuditBugfixes6Test {

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
}
