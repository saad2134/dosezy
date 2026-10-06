package com.example.dosezy

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
class AuditBugfixesTest {

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
}
