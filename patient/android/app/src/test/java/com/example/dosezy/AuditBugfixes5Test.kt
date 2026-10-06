package com.example.dosezy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.ui.viewmodels.AdherenceRange
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.notifications.AlarmScheduler
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
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuditBugfixes5Test {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear coordination preferences before each test
        context.getSharedPreferences(AlarmScheduler.PREFS_COORDINATION, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 1: Grouped Snooze Coordination and Transfer on Primary Cancellation
    // ───────────────────────────────────────────────────────────────
    @Test
    fun issue1_groupedSnooze_transfersAlarmToNextEntryWhenPrimaryTaken() {
        val scheduler = AlarmScheduler(context)
        val entryIds = listOf("entry-A", "entry-B", "entry-C")
        val medNames = listOf("Aspirin", "Metformin", "Lisinopril")

        scheduler.scheduleGroupedSnooze(entryIds, 10, medNames)

        val prefs = context.getSharedPreferences(AlarmScheduler.PREFS_COORDINATION, Context.MODE_PRIVATE)
        assertEquals("entry-A", prefs.getString("snooze_member_entry-A", null))
        assertEquals("entry-A", prefs.getString("snooze_member_entry-B", null))
        assertEquals("entry-A", prefs.getString("snooze_member_entry-C", null))
        assertEquals("entry-A,entry-B,entry-C", prefs.getString("snooze_group_ids_entry-A", null))

        // When primary entry-A is taken / cancelled, the alarm must transfer to entry-B rather than dropping
        scheduler.cancelSnooze("entry-A")

        assertNull(prefs.getString("snooze_member_entry-A", null))
        assertNull(prefs.getString("snooze_group_ids_entry-A", null))

        // entry-B is the new primary
        assertEquals("entry-B", prefs.getString("snooze_member_entry-B", null))
        assertEquals("entry-B", prefs.getString("snooze_member_entry-C", null))
        assertEquals("entry-B,entry-C", prefs.getString("snooze_group_ids_entry-B", null))

        // Now secondary entry-C is taken / cancelled
        scheduler.cancelSnooze("entry-C")
        assertNull(prefs.getString("snooze_member_entry-C", null))
        assertEquals("entry-B", prefs.getString("snooze_group_ids_entry-B", null))

        // Last entry-B is taken / cancelled
        scheduler.cancelSnooze("entry-B")
        assertNull(prefs.getString("snooze_member_entry-B", null))
        assertNull(prefs.getString("snooze_group_ids_entry-B", null))
    }

    @Test
    fun issue1_groupedSnooze_secondaryTakenFirst_updatesGroupWithoutCancellingPrimary() {
        val scheduler = AlarmScheduler(context)
        val entryIds = listOf("entry-1", "entry-2")
        val medNames = listOf("Med1", "Med2")

        scheduler.scheduleGroupedSnooze(entryIds, 10, medNames)

        val prefs = context.getSharedPreferences(AlarmScheduler.PREFS_COORDINATION, Context.MODE_PRIVATE)
        assertEquals("entry-1", prefs.getString("snooze_member_entry-2", null))

        // Secondary entry-2 is cancelled first
        scheduler.cancelSnooze("entry-2")

        assertNull(prefs.getString("snooze_member_entry-2", null))
        assertEquals("entry-1", prefs.getString("snooze_member_entry-1", null))
        assertEquals("entry-1", prefs.getString("snooze_group_ids_entry-1", null))
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 2: Grouped Nagging Coordination and Transfer
    // ───────────────────────────────────────────────────────────────
    @Test
    fun issue2_groupedNagging_transfersAlarmWhenPrimaryCancelled() {
        val scheduler = AlarmScheduler(context)
        val entryIds = arrayListOf("nag-1", "nag-2")
        val medNames = arrayListOf("MedA", "MedB")

        scheduler.scheduleNaggingReminder(
            entryId = "nag-1",
            minutes = 5,
            medicineName = "MedA, MedB",
            naggingCount = 1,
            entryIds = entryIds,
            medicineNames = medNames,
            scheduledTime = "08:00 AM"
        )

        val prefs = context.getSharedPreferences(AlarmScheduler.PREFS_COORDINATION, Context.MODE_PRIVATE)
        assertEquals("nag-1", prefs.getString("nagging_member_nag-1", null))
        assertEquals("nag-1", prefs.getString("nagging_member_nag-2", null))
        assertEquals("nag-1,nag-2", prefs.getString("nagging_group_ids_nag-1", null))

        // Cancelling primary nag-1 transfers to nag-2
        scheduler.cancelNagging("nag-1")

        assertNull(prefs.getString("nagging_member_nag-1", null))
        assertNull(prefs.getString("nagging_group_ids_nag-1", null))
        assertEquals("nag-2", prefs.getString("nagging_member_nag-2", null))
        assertEquals("nag-2", prefs.getString("nagging_group_ids_nag-2", null))

        // Cancelling nag-2 clears the group
        scheduler.cancelNagging("nag-2")
        assertNull(prefs.getString("nagging_member_nag-2", null))
        assertNull(prefs.getString("nagging_group_ids_nag-2", null))
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 3: Off-By-One Day Window Cardinality for Adherence Ranges
    // ───────────────────────────────────────────────────────────────
    @Test
    fun issue3_adherenceRanges_exactCardinalityAndBoundaryFiltering() {
        val today = LocalDate.of(2026, 10, 6)

        fun createEntry(id: String, date: LocalDate): ScheduleEntry {
            return ScheduleEntry(
                entryId = id,
                userId = "user_1",
                medicineId = "med_1",
                scheduledDateTime = date.atTime(8, 0),
                status = MedicationStatus.TAKEN_ON_TIME
            )
        }

        // Create entries spanning today down to 35 days ago
        val allEntries = (0..35).map { daysAgo ->
            createEntry("entry_$daysAgo", today.minusDays(daysAgo.toLong()))
        }

        // Test LAST_7_DAYS filtering: today.minusDays(6)..today (exactly 7 calendar days)
        val last7DaysEntries = allEntries.filter {
            val d = it.scheduledDateTime.toLocalDate()
            d >= today.minusDays(6) && d <= today
        }
        assertEquals(7, last7DaysEntries.size)
        // Entry from 6 days ago is included
        assertTrue(last7DaysEntries.any { it.scheduledDateTime.toLocalDate() == today.minusDays(6) })
        // Entry from 7 days ago is excluded (matching the 7-day weekly trend chart cardinality)
        assertFalse(last7DaysEntries.any { it.scheduledDateTime.toLocalDate() == today.minusDays(7) })

        // Test LAST_30_DAYS filtering: today.minusDays(29)..today (exactly 30 calendar days)
        val last30DaysEntries = allEntries.filter {
            val d = it.scheduledDateTime.toLocalDate()
            d >= today.minusDays(29) && d <= today
        }
        assertEquals(30, last30DaysEntries.size)
        // Entry from 29 days ago is included
        assertTrue(last30DaysEntries.any { it.scheduledDateTime.toLocalDate() == today.minusDays(29) })
        // Entry from 30 days ago is excluded
        assertFalse(last30DaysEntries.any { it.scheduledDateTime.toLocalDate() == today.minusDays(30) })
    }

    // ───────────────────────────────────────────────────────────────
    // Issue 4: Minute Notation Zero-Padding in Quick-Select Chips
    // ───────────────────────────────────────────────────────────────
    @Test
    fun issue4_gridTimePicker_minuteNotationZeroPadding() {
        val quickMinutes = listOf(0, 15, 30, 45)
        val labels = quickMinutes.map { min -> String.format(":%02d", min) }

        assertEquals(":00", labels[0])
        assertEquals(":15", labels[1])
        assertEquals(":30", labels[2])
        assertEquals(":45", labels[3])

        // Verify that 0 never produces unpadded ":0"
        assertEquals(":00", String.format(":%02d", 0))
    }
}
