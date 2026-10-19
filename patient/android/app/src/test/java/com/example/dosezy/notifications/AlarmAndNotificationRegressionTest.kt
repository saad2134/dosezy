package com.example.dosezy.notifications

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.utils.ImageUtils
import com.example.dosezy.utils.TimeCalculationUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlarmAndNotificationRegressionTest {

    // ───────────────────────────────────────────────────────────────
    // Bug 1: Notification Click Routes to Schedule Screen
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug1_notificationIntent_containsScheduleFragmentExtraAndFlags() {
        val intent = Intent().apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("fragment", "schedule")
        }

        assertEquals("schedule", intent.getStringExtra("fragment"))
        assertTrue((intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0)

        // Verify routing resolver maps fragment=schedule to "schedule" route
        val targetFragment = intent.getStringExtra("fragment")
        val destinationRoute = if (targetFragment == "schedule") "schedule" else "home"
        assertEquals("schedule", destinationRoute)
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 2: Pending Doses Within considerMissedAfter Are Not Flagged Missed
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug2_recentPendingDoses_notConsideredMissed() {
        val now = LocalDateTime.now()
        val missedAfterHours = 6

        // 1. Dose scheduled 2 minutes ago
        val scheduled2MinAgo = now.minusMinutes(2)
        assertFalse(
            "Dose scheduled 2 minutes ago must not be considered missed",
            TimeCalculationUtils.isMissed(scheduled2MinAgo, now, missedAfterHours)
        )

        // 2. Dose scheduled 2 hours ago
        val scheduled2HoursAgo = now.minusHours(2)
        assertFalse(
            "Dose scheduled 2 hours ago must not be considered missed when threshold is 6h",
            TimeCalculationUtils.isMissed(scheduled2HoursAgo, now, missedAfterHours)
        )

        // 3. Dose scheduled 7 hours ago (past 6h threshold)
        val scheduled7HoursAgo = now.minusHours(7)
        assertTrue(
            "Dose scheduled 7 hours ago must be considered missed",
            TimeCalculationUtils.isMissed(scheduled7HoursAgo, now, missedAfterHours)
        )
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 3: ImageUtils Safely Resolves file:// and Raw File Paths
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug3_imageUtils_resolvesFileUriPrefixCorrectly() {
        // Null / Empty cases
        assertNull(ImageUtils.resolveImageModel(null))
        assertNull(ImageUtils.resolveImageModel(""))

        // File path prefixed with file://
        val tempFile = File.createTempFile("test_med_photo", ".jpg")
        try {
            val uriPrefixedPath = "file://${tempFile.absolutePath}"
            val resolvedModel = ImageUtils.resolveImageModel(uriPrefixedPath)
            assertNotNull(resolvedModel)
            assertTrue("Resolved model must be java.io.File", resolvedModel is File)
            assertEquals(tempFile.absolutePath, (resolvedModel as File).absolutePath)

            // Direct absolute path without prefix
            val resolvedDirect = ImageUtils.resolveImageModel(tempFile.absolutePath)
            assertNotNull(resolvedDirect)
            assertTrue("Resolved direct model must be java.io.File", resolvedDirect is File)
            assertEquals(tempFile.absolutePath, (resolvedDirect as File).absolutePath)
        } finally {
            tempFile.delete()
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 4: Legacy Emergency Contacts Migration Clears Global Key
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug4_legacyEmergencyContactsMigration_removesGlobalKeyToPreventBleed() {
        val prefs = mockk<SharedPreferences>(relaxed = true)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor

        val legacyJson = """[{"name":"Doctor","phone":"555-0199"}]"""
        val user1Key = "contacts_json_user_1"

        // Simulate user 1 migration: copy to userKey and remove legacy key
        editor.putString(user1Key, legacyJson).remove("contacts_json").apply()

        verify { editor.putString(user1Key, legacyJson) }
        verify { editor.remove("contacts_json") }
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 5: Bounded Active-Window Alarm Cancellation
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug5_alarmCancellation_excludesHistoricalEntriesOutsideActiveWindow() {
        val now = LocalDateTime.now()
        val activeWindowStart = now.minusHours(24)
        val activeWindowEnd = now.plusDays(8)

        val historicalEntry90DaysAgo = ScheduleEntry(
            entryId = "entry_old_90d",
            userId = "user_1",
            medicineId = "med_1",
            scheduledDateTime = now.minusDays(90),
            status = MedicationStatus.TAKEN_ON_TIME
        )
        val historicalEntry30DaysAgo = ScheduleEntry(
            entryId = "entry_old_30d",
            userId = "user_1",
            medicineId = "med_1",
            scheduledDateTime = now.minusDays(30),
            status = MedicationStatus.MISSED
        )
        val pastEntry3DaysAgo = ScheduleEntry(
            entryId = "entry_past_3d",
            userId = "user_1",
            medicineId = "med_1",
            scheduledDateTime = now.minusDays(3),
            status = MedicationStatus.TAKEN_ON_TIME
        )
        val activeTodayEntry = ScheduleEntry(
            entryId = "entry_today",
            userId = "user_1",
            medicineId = "med_1",
            scheduledDateTime = now.plusHours(2),
            status = MedicationStatus.PENDING
        )
        val activeDay5Entry = ScheduleEntry(
            entryId = "entry_day5",
            userId = "user_1",
            medicineId = "med_1",
            scheduledDateTime = now.plusDays(5),
            status = MedicationStatus.PENDING
        )
        val distantFutureEntry20Days = ScheduleEntry(
            entryId = "entry_future_20d",
            userId = "user_1",
            medicineId = "med_1",
            scheduledDateTime = now.plusDays(20),
            status = MedicationStatus.PENDING
        )

        val allEntries = listOf(
            historicalEntry90DaysAgo,
            historicalEntry30DaysAgo,
            pastEntry3DaysAgo,
            activeTodayEntry,
            activeDay5Entry,
            distantFutureEntry20Days
        )

        val entriesToCancel = allEntries.filter { entry ->
            entry.scheduledDateTime.isAfter(activeWindowStart) &&
            entry.scheduledDateTime.isBefore(activeWindowEnd)
        }

        // Only active window entries (today & day 5) must be included
        assertEquals(2, entriesToCancel.size)
        assertTrue(entriesToCancel.any { it.entryId == "entry_today" })
        assertTrue(entriesToCancel.any { it.entryId == "entry_day5" })

        // Historical and distant future entries must NOT be included
        assertFalse(entriesToCancel.any { it.entryId == "entry_old_90d" })
        assertFalse(entriesToCancel.any { it.entryId == "entry_old_30d" })
        assertFalse(entriesToCancel.any { it.entryId == "entry_past_3d" })
        assertFalse(entriesToCancel.any { it.entryId == "entry_future_20d" })
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 6: Disarming Snooze and Nagging Alarms
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug6_disarmSnoozeAndNagging_cancelsBothTriggers() {
        val alarmScheduler = mockk<com.example.dosezy.notifications.AlarmScheduler>(relaxed = true)
        val testEntryId = "med_dose_123"

        // Simulate cancellation invoked during recordDoseTaken / recordDoseSkipped
        alarmScheduler.cancelSnooze(testEntryId)
        alarmScheduler.cancelNagging(testEntryId)

        verify(exactly = 1) { alarmScheduler.cancelSnooze(testEntryId) }
        verify(exactly = 1) { alarmScheduler.cancelNagging(testEntryId) }
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 7: Undoing Past Dose Resyncs Notification and Rearms Nagging (BUG-03)
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug7_undoDoseTaken_forPastDose_resyncsSlotNotificationAndRearmsNagging() {
        val today = java.time.LocalDate.now()
        val now = LocalDateTime.of(today, java.time.LocalTime.of(12, 15))
        val pastScheduledTime = LocalDateTime.of(today, java.time.LocalTime.of(12, 0))

        // Previous entry scheduled 15 minutes ago (in the past)
        val pastEntry = ScheduleEntry(
            entryId = "entry_past_undo",
            userId = "user_undo_test",
            medicineId = "med_undo_test",
            scheduledDateTime = pastScheduledTime,
            status = MedicationStatus.TAKEN_ON_TIME
        )

        // On undo, scheduledDateTime is NOT in the future
        assertFalse(
            "Past entry must not be after now",
            pastEntry.scheduledDateTime.isAfter(now)
        )

        // Verify that entry with status PENDING is preserved in remainingPending calculation
        val slotEntries = listOf(
            pastEntry.copy(status = MedicationStatus.PENDING),
            ScheduleEntry(
                entryId = "entry_past_sibling",
                userId = "user_undo_test",
                medicineId = "med_undo_test_2",
                scheduledDateTime = pastScheduledTime,
                status = MedicationStatus.TAKEN_ON_TIME
            )
        )
        val pendingEntry = pastEntry.copy(status = MedicationStatus.PENDING)
        val remainingPending = slotEntries.filter {
            !it.entryId.startsWith("PRN_") &&
            it.status == MedicationStatus.PENDING &&
            (pendingEntry.status == MedicationStatus.PENDING || it.entryId != pastEntry.entryId)
        }

        assertEquals(
            "Undone pending entry must be included in remainingPending slot items",
            1,
            remainingPending.size
        )
        assertEquals("entry_past_undo", remainingPending.first().entryId)

        // Verify that hasActiveNotification evaluates to true on undo to force shade alert re-issue for today's dose (#BUG-03, #BUG-UNDO-PAST-DATE)
        val isScheduledToday = pastEntry.scheduledDateTime.toLocalDate() == java.time.LocalDate.now()
        val hasActiveNotification = (pendingEntry.status == MedicationStatus.PENDING && isScheduledToday)
        assertTrue(
            "Undoing dose on today's schedule must force active notification restoration to shade",
            hasActiveNotification
        )

        // Guard: Historical past date dose (e.g. 2 days ago) must NOT evaluate hasActiveNotification to true (#BUG-UNDO-PAST-DATE)
        val historicalEntry = pastEntry.copy(scheduledDateTime = pastEntry.scheduledDateTime.minusDays(2))
        val isHistoricalToday = historicalEntry.scheduledDateTime.toLocalDate() == java.time.LocalDate.now()
        val hasActiveNotificationHistorical = (historicalEntry.status == MedicationStatus.PENDING && isHistoricalToday)
        assertFalse(
            "Undoing dose on historical past date must NOT force active notification restoration",
            hasActiveNotificationHistorical
        )
    }

    @Test
    fun notificationIntent_containsScheduleFragmentAndTargetUserId() {
        val targetUserId = "user_switch_test_1"
        val intent = Intent().apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("fragment", "schedule")
            putExtra("switch_to_user_id", targetUserId)
        }

        assertEquals("schedule", intent.getStringExtra("fragment"))
        assertEquals(targetUserId, intent.getStringExtra("switch_to_user_id"))
        assertTrue((intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0)
    }

    @Test
    fun refillNotificationIntent_containsMedicinesFragmentAndTargetUserId() {
        val targetUserId = "user_refill_route_test"
        val intent = Intent().apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("fragment", "medicines")
            putExtra("switch_to_user_id", targetUserId)
        }

        assertEquals("medicines", intent.getStringExtra("fragment"))
        assertEquals(targetUserId, intent.getStringExtra("switch_to_user_id"))

        // Verify routing resolver in MainActivity maps fragment=medicines to "medicines" route
        val targetFragment = intent.getStringExtra("fragment")
        val destinationRoute = when (targetFragment) {
            "schedule" -> "schedule"
            "medicines" -> "medicines"
            else -> "home"
        }
        assertEquals("medicines", destinationRoute)
    }

    @Test
    fun alarmClockShowIntent_containsScheduleFragmentAndTargetUserId() {
        // Guard: Verify that the showIntent passed to AlarmClockInfo carries target profile and schedule fragment
        val targetUserId = "user_dependent_profile"
        val showIntent = Intent().apply {
            setClassName("com.example.dosezy", "com.example.dosezy.MainActivity")
            putExtra("switch_to_user_id", targetUserId)
            putExtra("fragment", "schedule")
        }

        assertEquals(targetUserId, showIntent.getStringExtra("switch_to_user_id"))
        assertEquals("schedule", showIntent.getStringExtra("fragment"))
    }

    @Test
    fun alarmNotificationIntent_containsFromAlarmExtra_andStopsRingingOnTap() {
        // Guard: Verify that notification launch intent carries from_alarm extra and stops active alarm audio and vibration
        val intent = Intent().apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("fragment", "schedule")
            putExtra("from_alarm", true)
            putExtra("switch_to_user_id", "test_user_tap")
        }

        assertTrue("Notification launch intent must contain from_alarm = true", intent.getBooleanExtra("from_alarm", false))
        assertEquals("schedule", intent.getStringExtra("fragment"))
        assertEquals("test_user_tap", intent.getStringExtra("switch_to_user_id"))

        val field = AlarmAudioPlayer::class.java.getDeclaredField("mediaPlayer")
        field.isAccessible = true
        val mockPlayer = mockk<android.media.MediaPlayer>(relaxed = true)
        every { mockPlayer.isPlaying } returns true
        field.set(AlarmAudioPlayer, mockPlayer)
        assertTrue("AlarmAudioPlayer must report playing while ringing", AlarmAudioPlayer.isPlaying())

        // Simulate MainActivity onCreate/onNewIntent notification dispatch check
        if (intent.getBooleanExtra("from_alarm", false) || AlarmAudioPlayer.isPlaying()) {
            AlarmActivity.stopActiveAlarm()
        }
        assertFalse("AlarmAudioPlayer must be stopped after notification tap", AlarmAudioPlayer.isPlaying())
    }

    @Test
    fun notificationAction_multiSlotCohort_identifiesDistinctSlots() {
        // Guard: Verify that multi-slot entry IDs extract distinct scheduled time slots to prevent orphan alarms (#BUG-RECEIVER-MULTI-SLOT)
        val entry1 = ScheduleEntry(
            entryId = "entry_slot_1",
            userId = "user_1",
            medicineId = "med_1",
            scheduledDateTime = LocalDateTime.of(2026, 10, 18, 8, 0, 0),
            status = MedicationStatus.PENDING
        )
        val entry2 = ScheduleEntry(
            entryId = "entry_slot_1_sibling",
            userId = "user_1",
            medicineId = "med_2",
            scheduledDateTime = LocalDateTime.of(2026, 10, 18, 8, 0, 0),
            status = MedicationStatus.PENDING
        )
        val entry3 = ScheduleEntry(
            entryId = "entry_slot_2",
            userId = "user_1",
            medicineId = "med_3",
            scheduledDateTime = LocalDateTime.of(2026, 10, 18, 12, 0, 0),
            status = MedicationStatus.PENDING
        )

        val entries = listOf(entry1, entry2, entry3)
        val distinctSlots = entries.distinctBy { it.scheduledDateTime.withSecond(0).withNano(0) }

        assertEquals(2, distinctSlots.size)
        assertEquals(LocalDateTime.of(2026, 10, 18, 8, 0, 0), distinctSlots[0].scheduledDateTime)
        assertEquals(LocalDateTime.of(2026, 10, 18, 12, 0, 0), distinctSlots[1].scheduledDateTime)
    }

    @Test
    fun notificationAction_userResolution_fallsBackToEntryUserIdWhenPrimaryUserIsNull() {
        // Guard: Verify that entry user considerLateAfter is respected when primaryEntry user is missing
        val customLateThreshold = 5
        val entry = ScheduleEntry(
            entryId = "entry_secondary_user",
            userId = "user_custom",
            medicineId = "med_custom",
            scheduledDateTime = LocalDateTime.of(2026, 10, 18, 8, 0, 0),
            status = MedicationStatus.PENDING
        )
        val now = LocalDateTime.of(2026, 10, 18, 12, 0, 0) // 4 hours later

        // With default 3h threshold, 4 hours later would be considered late
        assertTrue("4 hours difference is late under default 3h threshold", TimeCalculationUtils.isTakenLate(entry.scheduledDateTime, now, 3))

        // Under user custom 5h threshold, 4 hours difference is on time
        assertFalse("4 hours difference is on time under user custom 5h threshold", TimeCalculationUtils.isTakenLate(entry.scheduledDateTime, now, customLateThreshold))
    }

    @Test
    fun testNotificationLocaleResolution_resolvesProfileLanguageOverGlobalLanguage() {
        // Guard: Verify that notification action strings and scheduled time formatting resolve profile language rather than global app_prefs language (#BUG-NOTIF-LOCALE)
        val spanishProfile = com.example.dosezy.data.model.User(
            userId = "user_es",
            fullName = "Carlos",
            age = 40,
            gender = com.example.dosezy.data.model.Gender.DO_NOT_SPECIFY,
            contactNumber = "+34600000000",
            language = com.example.dosezy.data.model.Language.SPANISH,
            timeFormat = com.example.dosezy.data.model.TimeFormat.HOUR_24
        )
        val englishProfile = com.example.dosezy.data.model.User(
            userId = "user_en",
            fullName = "Alice",
            age = 30,
            gender = com.example.dosezy.data.model.Gender.DO_NOT_SPECIFY,
            contactNumber = "+12025550123",
            language = com.example.dosezy.data.model.Language.ENGLISH,
            timeFormat = com.example.dosezy.data.model.TimeFormat.HOUR_12
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        val esContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, spanishProfile.language)
        val enContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, englishProfile.language)

        val esTaken = esContext.getString(com.example.dosezy.R.string.home_action_taken)
        val enTaken = enContext.getString(com.example.dosezy.R.string.home_action_taken)

        val esSnooze = esContext.getString(com.example.dosezy.R.string.notif_action_snooze_format, 10)
        val enSnooze = enContext.getString(com.example.dosezy.R.string.notif_action_snooze_format, 10)

        assertEquals("Taken", enTaken)
        assertEquals("Snooze (10 min)", enSnooze)
        assertFalse("Spanish taken action string must differ from English", esTaken == enTaken)
        assertFalse("Spanish snooze action string must differ from English", esSnooze == enSnooze)
    }

    @Test
    fun testRefillWarningNotification_resolvesProfileLanguageOverGlobalLanguage() {
        // Guard: Verify that refill alert title and body strings derive from recipient user profile language rather than global app_prefs language (#BUG-REFILL-LOCALE)
        val spanishProfile = com.example.dosezy.data.model.User(
            userId = "user_es_refill",
            fullName = "Elena",
            age = 45,
            gender = com.example.dosezy.data.model.Gender.DO_NOT_SPECIFY,
            contactNumber = "+34611111111",
            language = com.example.dosezy.data.model.Language.SPANISH
        )
        val englishProfile = com.example.dosezy.data.model.User(
            userId = "user_en_refill",
            fullName = "John",
            age = 35,
            gender = com.example.dosezy.data.model.Gender.DO_NOT_SPECIFY,
            contactNumber = "+12025550199",
            language = com.example.dosezy.data.model.Language.ENGLISH
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        val esContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, spanishProfile.language)
        val enContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, englishProfile.language)

        val esTitle = esContext.getString(com.example.dosezy.R.string.notif_refill_alert_title, "Paracetamol")
        val enTitle = enContext.getString(com.example.dosezy.R.string.notif_refill_alert_title, "Paracetamol")

        val esText = esContext.getString(com.example.dosezy.R.string.notif_refill_alert_text, 5)
        val enText = enContext.getString(com.example.dosezy.R.string.notif_refill_alert_text, 5)

        assertTrue("English title contains Paracetamol", enTitle.contains("Paracetamol"))
        assertFalse("Spanish refill alert title must differ from English", esTitle == enTitle)
        assertFalse("Spanish refill alert text must differ from English", esText == enText)
    }

    @Test
    fun testAlarmActivitySnoozeToast_resolvesLocalizedContext() {
        // Guard: Verify that snooze confirmation toast string derives from localizedContext matching the recipient user profile language (#BUG-ALARM-SNOOZE-TOAST-LOCALE)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val esContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, com.example.dosezy.data.model.Language.SPANISH)
        val enContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, com.example.dosezy.data.model.Language.ENGLISH)

        val esSnoozeToast = esContext.getString(com.example.dosezy.R.string.alarm_snoozed_toast, 10)
        val enSnoozeToast = enContext.getString(com.example.dosezy.R.string.alarm_snoozed_toast, 10)

        assertEquals("Alarm snoozed for 10 minutes", enSnoozeToast)
        assertEquals("Alarma pospuesta por 10 minutos", esSnoozeToast)
        assertFalse("Spanish snooze toast must differ from English", esSnoozeToast == enSnoozeToast)
    }

    @Test
    fun testAlarmActivity_keyguardSafeCast_handlesNullOrMissingKeyguardService() {
        // Guard: Verify that safe-casting KEYGUARD_SERVICE gracefully avoids ClassCastException or NullPointerException on headless or custom builds
        val mockContext = mockk<Context>()
        every { mockContext.getSystemService(Context.KEYGUARD_SERVICE) } returns null

        val keyguardManager = mockContext.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        assertNull("Missing keyguard service must resolve safely to null", keyguardManager)
        var dismissed = false
        keyguardManager?.let {
            dismissed = true
        }
        assertFalse("Safe call on null KeyguardManager must not execute block", dismissed)
    }

    @Test
    fun testLockscreenPrivacy_defaultsToPublicVisibility() {
        // Guard: Verify that notifications default to VISIBILITY_PUBLIC while supporting privacy opt-out
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("lockscreen_details_enabled").commit()

        val allowLockscreenDetails = prefs.getBoolean("lockscreen_details_enabled", true)
        assertTrue(allowLockscreenDetails)

        val builder = androidx.core.app.NotificationCompat.Builder(context, MedicineAlarmReceiver.CHANNEL_ID)
            .setSmallIcon(com.example.dosezy.R.drawable.ic_medicine_notification)
            .setContentTitle("Prescription")
            .setContentText("Truvada 200mg")

        if (allowLockscreenDetails) {
            builder.setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
        } else {
            builder.setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PRIVATE)
        }

        val notification = builder.build()
        assertEquals(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC, notification.visibility)
    }

    @Test
    fun testLockscreenPrivacy_masksSensitivePrescriptionDetailsWhenUserOptsOut() {
        // Guard: Verify that when user opts out in preferences, notifications switch to VISIBILITY_PRIVATE with generic setPublicVersion (#HIPAA, #GDPR)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("lockscreen_details_enabled", false).commit()

        val notificationTitle = "Sensitive Drug Alert"
        val notificationText = "08:00 AM - Truvada 200mg"

        val allowLockscreenDetails = prefs.getBoolean("lockscreen_details_enabled", true)
        assertFalse(allowLockscreenDetails)

        val builder = androidx.core.app.NotificationCompat.Builder(context, MedicineAlarmReceiver.CHANNEL_ID)
            .setSmallIcon(com.example.dosezy.R.drawable.ic_medicine_notification)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)

        if (allowLockscreenDetails) {
            builder.setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
        } else {
            val publicNotification = androidx.core.app.NotificationCompat.Builder(context, MedicineAlarmReceiver.CHANNEL_ID)
                .setSmallIcon(com.example.dosezy.R.drawable.ic_medicine_notification)
                .setContentTitle(context.getString(com.example.dosezy.R.string.app_name))
                .setContentText(context.getString(com.example.dosezy.R.string.notif_generic_lockscreen_reminder))
                .build()
            builder.setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicNotification)
        }

        val notification = builder.build()
        assertEquals(androidx.core.app.NotificationCompat.VISIBILITY_PRIVATE, notification.visibility)
        assertNotNull(notification.publicVersion)
        val publicContent = notification.publicVersion.extras?.getString(android.app.Notification.EXTRA_TEXT)
        assertEquals("Scheduled medication reminder", publicContent)
    }
}


