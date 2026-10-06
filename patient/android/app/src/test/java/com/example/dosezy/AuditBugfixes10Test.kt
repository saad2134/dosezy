package com.example.dosezy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.export.DataExporter
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.data.model.User
import com.example.dosezy.notifications.AlarmScheduler
import com.example.dosezy.utils.TimeFormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuditBugfixes10Test {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ─────────────────────────────────────────────────────────────────
    // Issue 1 Tests: TimeFormatUtils 12h/24h & Locale Format Verification
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun timeFormatUtils_formats12HourAnd24HourCorrectlyAcrossLocales() {
        val testDateTime = LocalDateTime.of(2026, 10, 6, 14, 30)

        // 12-hour format in US locale
        val formatted12h = TimeFormatUtils.formatTime(testDateTime, TimeFormat.HOUR_12, Locale.US)
        assertEquals("2:30 PM", formatted12h)

        // 24-hour format
        val formatted24h = TimeFormatUtils.formatTime(testDateTime, TimeFormat.HOUR_24, Locale.US)
        assertEquals("14:30", formatted24h)

        // formatLocalTime check
        val localTime = LocalTime.of(8, 15)
        val formattedLocal12h = TimeFormatUtils.formatLocalTime(localTime, TimeFormat.HOUR_12, Locale.US)
        assertEquals("8:15 AM", formattedLocal12h)
        val formattedLocal24h = TimeFormatUtils.formatLocalTime(localTime, TimeFormat.HOUR_24, Locale.US)
        assertEquals("08:15", formattedLocal24h)
    }

    // ─────────────────────────────────────────────────────────────────
    // Issue 2 Tests: Grouped Snooze Disarming Lifecycle
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun alarmScheduler_disarmingAllCohortIdsClearsCoordinationPreferences() {
        val scheduler = AlarmScheduler(context)
        val entryIds = listOf("entry_audit10_1", "entry_audit10_2")
        val medNames = listOf("Aspirin", "Ibuprofen")

        // Schedule grouped snooze
        scheduler.scheduleGroupedSnooze(
            entryIds = entryIds,
            minutes = 10,
            medicineNames = medNames,
            timeFormat = TimeFormat.HOUR_12
        )

        val prefs = context.getSharedPreferences(AlarmScheduler.PREFS_COORDINATION, Context.MODE_PRIVATE)
        assertEquals("entry_audit10_1", prefs.getString("snooze_member_entry_audit10_1", null))
        assertEquals("entry_audit10_1", prefs.getString("snooze_member_entry_audit10_2", null))

        // Disarm cohort entry 1 (primary) then entry 2 upfront
        scheduler.cancelSnooze("entry_audit10_1")
        scheduler.cancelSnooze("entry_audit10_2")

        // Assert all members are completely removed from coordination preferences
        assertNull(prefs.getString("snooze_member_entry_audit10_1", null))
        assertNull(prefs.getString("snooze_member_entry_audit10_2", null))
        assertNull(prefs.getString("snooze_group_ids_entry_audit10_1", null))
        assertNull(prefs.getString("snooze_group_ids_entry_audit10_2", null))
    }

    // ─────────────────────────────────────────────────────────────────
    // Issue 3 Tests: CSV Full Schema Parity & Inventory Serialization
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun dataExporter_buildCsvContent_serializesAllInventoryAndCustomDosageFields() {
        val user = User(
            userId = "usr_audit10",
            fullName = "Jane Doe",
            age = 40,
            gender = Gender.FEMALE,
            contactNumber = "+19876543210",
            allergies = "Penicillin",
            medicalConditions = "Asthma"
        )

        val medicine = Medicine(
            medicineId = "med_audit10",
            userId = "usr_audit10",
            medicationName = "Inhaler Pro",
            dosage = 1.0,
            dosageUnit = DosageUnit.TABLET,
            timesPerDay = 2,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            currentStock = 120,
            refillThreshold = 20,
            autoDeductOnTake = true,
            notes = "Take with water",
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 12, 31),
            durationDays = 365,
            isArchived = false,
            customDosages = mapOf("08:00" to 2.0, "20:00" to 1.0)
        )

        val schedule = ScheduleEntry(
            entryId = "sch_audit10",
            userId = "usr_audit10",
            medicineId = "med_audit10",
            scheduledDateTime = LocalDateTime.of(2026, 10, 6, 8, 0),
            status = MedicationStatus.TAKEN_ON_TIME,
            dosage = 2.0,
            doseNotes = "Taken after breakfast"
        )

        val csv = DataExporter.buildCsvContent(user, listOf(medicine), listOf(schedule))

        // Check Header columns
        assertTrue(csv.contains("Refill Threshold,Auto Deduct Stock,Is Archived,Custom Dosages"))

        // Check Medicine inventory and custom slot dosages row
        assertTrue(csv.contains(",120,20,true,false,\"08:00:2.0;20:00:1.0\""))
        assertTrue(csv.contains("\"Inhaler Pro\""))
        assertTrue(csv.contains("\"med_audit10\""))

        // Check Schedule values row
        assertTrue(csv.contains("\"sch_audit10\""))
        assertTrue(csv.contains("TAKEN_ON_TIME"))
        assertTrue(csv.contains("\"Taken after breakfast\""))
    }
}
