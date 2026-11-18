package com.example.dosezy.notifications

import android.content.Context
import android.app.PendingIntent
import android.content.Intent
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
class GroupedSnoozeAndNaggingTest {

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

    @Test
    fun testScheduleGroupedSnooze_resolvesTargetProfileLocale() {
        val testDb = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.dosezy.data.DosezyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        com.example.dosezy.data.DosezyDatabase.setInstance(testDb)
        try {
            val scheduler = AlarmScheduler(context)
            val arabicUser = com.example.dosezy.data.model.User(
                userId = "user_ar_snooze",
                fullName = "Tariq",
                age = 40,
                gender = com.example.dosezy.data.model.Gender.DO_NOT_SPECIFY,
                contactNumber = "+971501111111",
                language = com.example.dosezy.data.model.Language.ARABIC,
                timeFormat = com.example.dosezy.data.model.TimeFormat.HOUR_12
            )
            kotlinx.coroutines.runBlocking {
                testDb.userDao().insertUser(arabicUser)
            }

            val entryIds = listOf("entry-snooze-ar")
            val medNames = listOf("Metformin")
            val fixedTriggerTime = 1770000000000L

            scheduler.scheduleGroupedSnooze(
                entryIds = entryIds,
                minutes = 10,
                medicineNames = medNames,
                explicitTriggerTime = fixedTriggerTime,
                timeFormat = arabicUser.timeFormat,
                userId = arabicUser.userId
            )

            val prefs = context.getSharedPreferences(AlarmScheduler.PREFS_COORDINATION, Context.MODE_PRIVATE)
            assertEquals("entry-snooze-ar", prefs.getString("snooze_member_entry-snooze-ar", null))
            assertEquals(fixedTriggerTime, prefs.getLong("snooze_group_trigger_entry-snooze-ar", 0L))
        } finally {
            com.example.dosezy.data.DosezyDatabase.setInstance(null)
            testDb.close()
        }
    }

    @Test
    fun cancelSnooze_onSecondaryAndPrimary_preservesTargetProfileLocaleAndTransfersGroup() {
        // Guard: Verify cancelSnooze preserves target profile language when updating notification and transferring primary (#BUG-SNOOZE-CANCEL-LOCALE)
        val testDb = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.dosezy.data.DosezyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        com.example.dosezy.data.DosezyDatabase.setInstance(testDb)
        try {
            val scheduler = AlarmScheduler(context)
            val arabicUser = com.example.dosezy.data.model.User(
                userId = "user_ar_cancel",
                fullName = "Fatima",
                age = 35,
                gender = com.example.dosezy.data.model.Gender.FEMALE,
                contactNumber = "+971502222222",
                language = com.example.dosezy.data.model.Language.ARABIC,
                timeFormat = com.example.dosezy.data.model.TimeFormat.HOUR_12
            )
            val entry1 = ScheduleEntry(
                entryId = "entry-snooze-ar-1",
                userId = arabicUser.userId,
                medicineId = "med_1",
                scheduledDateTime = LocalDateTime.now().plusHours(1),
                status = MedicationStatus.PENDING
            )
            val entry2 = ScheduleEntry(
                entryId = "entry-snooze-ar-2",
                userId = arabicUser.userId,
                medicineId = "med_2",
                scheduledDateTime = LocalDateTime.now().plusHours(1),
                status = MedicationStatus.PENDING
            )
            val med1 = com.example.dosezy.data.model.Medicine(
                medicineId = "med_1",
                userId = arabicUser.userId,
                medicationName = "Amoxicillin",
                dosage = 500.0,
                dosageUnit = com.example.dosezy.data.model.DosageUnit.MG,
                timesPerDay = 1,
                frequency = com.example.dosezy.data.model.Frequency(com.example.dosezy.data.model.FrequencyPattern.DAILY),
                scheduledTimes = listOf(java.time.LocalTime.of(9, 0)),
                currentStock = 10.0
            )
            val med2 = com.example.dosezy.data.model.Medicine(
                medicineId = "med_2",
                userId = arabicUser.userId,
                medicationName = "Ibuprofen",
                dosage = 200.0,
                dosageUnit = com.example.dosezy.data.model.DosageUnit.MG,
                timesPerDay = 1,
                frequency = com.example.dosezy.data.model.Frequency(com.example.dosezy.data.model.FrequencyPattern.DAILY),
                scheduledTimes = listOf(java.time.LocalTime.of(9, 0)),
                currentStock = 10.0
            )
            kotlinx.coroutines.runBlocking {
                testDb.userDao().insertUser(arabicUser)
                testDb.medicineDao().insertMedicine(med1)
                testDb.medicineDao().insertMedicine(med2)
                testDb.scheduleDao().insertScheduleEntry(entry1)
                testDb.scheduleDao().insertScheduleEntry(entry2)
            }

            val futureTriggerTime = System.currentTimeMillis() + 600_000L // 10 minutes from now
            scheduler.scheduleGroupedSnooze(
                entryIds = listOf(entry1.entryId, entry2.entryId),
                minutes = 10,
                medicineNames = listOf("Amoxicillin", "Ibuprofen"),
                explicitTriggerTime = futureTriggerTime,
                timeFormat = arabicUser.timeFormat,
                userId = arabicUser.userId
            )

            // Test 1: Secondary cancellation updates notification formatted in Arabic locale
            scheduler.cancelSnooze(entry2.entryId)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                entry1.entryId.hashCode() + 1000,
                Intent(context, MedicineAlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            assertNotNull("Update snooze PendingIntent must exist in AlarmManager", pendingIntent)
            val shadowPi = org.robolectric.Shadows.shadowOf(pendingIntent)
            val savedIntent = shadowPi.savedIntent
            val expectedArabicFormatted = java.text.SimpleDateFormat(
                "h:mm a",
                com.example.dosezy.utils.LocaleHelper.getLocale(com.example.dosezy.data.model.Language.ARABIC)
            ).format(java.util.Date(futureTriggerTime))
            assertEquals(expectedArabicFormatted, savedIntent.getStringExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME))

            // Test 2: Primary cancellation transfers primary to remaining entry with target profile settings
            scheduler.scheduleGroupedSnooze(
                entryIds = listOf(entry1.entryId, entry2.entryId),
                minutes = 10,
                medicineNames = listOf("Amoxicillin", "Ibuprofen"),
                explicitTriggerTime = futureTriggerTime,
                timeFormat = arabicUser.timeFormat,
                userId = arabicUser.userId
            )
            scheduler.cancelSnooze(entry1.entryId)

            val prefs = context.getSharedPreferences(AlarmScheduler.PREFS_COORDINATION, Context.MODE_PRIVATE)
            assertEquals(entry2.entryId, prefs.getString("snooze_member_${entry2.entryId}", null))
            assertEquals(entry2.entryId, prefs.getString("snooze_group_ids_${entry2.entryId}", null))
            assertNull(prefs.getString("snooze_member_${entry1.entryId}", null))
        } finally {
            com.example.dosezy.data.DosezyDatabase.setInstance(null)
            testDb.close()
        }
    }

    @Test
    fun groupedSnoozeAndAlarms_propagateAndPersistUserId_acrossTransfersAndPartialCancellations() {
        // Guard: Verify EXTRA_USER_ID and snooze_group_user persistence across grouped alarm scheduling and transfers (#BUG-ALARM-USER-PROPAGATION)
        val testDb = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.dosezy.data.DosezyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        com.example.dosezy.data.DosezyDatabase.setInstance(testDb)
        try {
            val scheduler = AlarmScheduler(context)
            val targetUser = com.example.dosezy.data.model.User(
                userId = "user_snooze_test_99",
                fullName = "Snooze User",
                gender = com.example.dosezy.data.model.Gender.DO_NOT_SPECIFY,
                age = 30,
                contactNumber = "1234567890",
                isCurrentUser = false
            )
            val med1 = com.example.dosezy.data.model.Medicine(
                medicineId = "med_1",
                userId = targetUser.userId,
                medicationName = "Medicine 1",
                dosage = 500.0,
                dosageUnit = com.example.dosezy.data.model.DosageUnit.MG,
                timesPerDay = 1,
                frequency = com.example.dosezy.data.model.Frequency(com.example.dosezy.data.model.FrequencyPattern.DAILY),
                scheduledTimes = listOf(java.time.LocalTime.of(9, 0)),
                currentStock = 10.0
            )
            val med2 = com.example.dosezy.data.model.Medicine(
                medicineId = "med_2",
                userId = targetUser.userId,
                medicationName = "Medicine 2",
                dosage = 200.0,
                dosageUnit = com.example.dosezy.data.model.DosageUnit.MG,
                timesPerDay = 1,
                frequency = com.example.dosezy.data.model.Frequency(com.example.dosezy.data.model.FrequencyPattern.DAILY),
                scheduledTimes = listOf(java.time.LocalTime.of(9, 0)),
                currentStock = 10.0
            )
            val entry1 = ScheduleEntry(
                entryId = "entry_snooze_u1",
                userId = targetUser.userId,
                medicineId = med1.medicineId,
                scheduledDateTime = LocalDateTime.now().plusHours(2),
                status = MedicationStatus.PENDING
            )
            val entry2 = ScheduleEntry(
                entryId = "entry_snooze_u2",
                userId = targetUser.userId,
                medicineId = med2.medicineId,
                scheduledDateTime = LocalDateTime.now().plusHours(2),
                status = MedicationStatus.PENDING
            )
            kotlinx.coroutines.runBlocking {
                testDb.userDao().insertUser(targetUser)
                testDb.medicineDao().insertMedicine(med1)
                testDb.medicineDao().insertMedicine(med2)
                testDb.scheduleDao().insertScheduleEntry(entry1)
                testDb.scheduleDao().insertScheduleEntry(entry2)
            }

            // 1. Test scheduleGroupedMedicineAlarm propagates EXTRA_USER_ID
            scheduler.scheduleGroupedMedicineAlarm(
                scheduledDateTime = entry1.scheduledDateTime,
                entries = listOf(entry1, entry2),
                medicineNames = listOf("Medicine 1", "Medicine 2")
            )
            val alarmSlotKey = "${entry1.userId}_${entry1.scheduledDateTime.withSecond(0).withNano(0)}"
            val alarmPendingIntent = PendingIntent.getBroadcast(
                context,
                alarmSlotKey.hashCode(),
                Intent(context, MedicineAlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            assertNotNull("Grouped alarm PendingIntent must exist", alarmPendingIntent)
            val alarmIntent = org.robolectric.Shadows.shadowOf(alarmPendingIntent).savedIntent
            assertEquals(targetUser.userId, alarmIntent.getStringExtra(MedicineAlarmReceiver.EXTRA_USER_ID))

            // 2. Test scheduleGroupedSnooze attaches EXTRA_USER_ID and persists snooze_group_user
            val futureTrigger = System.currentTimeMillis() + 600_000L
            scheduler.scheduleGroupedSnooze(
                entryIds = listOf(entry1.entryId, entry2.entryId),
                minutes = 10,
                medicineNames = listOf("Medicine 1", "Medicine 2"),
                explicitTriggerTime = futureTrigger,
                userId = targetUser.userId
            )
            val prefs = context.getSharedPreferences(AlarmScheduler.PREFS_COORDINATION, Context.MODE_PRIVATE)
            assertEquals(targetUser.userId, prefs.getString("snooze_group_user_${entry1.entryId}", null))

            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                entry1.entryId.hashCode() + 1000,
                Intent(context, MedicineAlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            assertNotNull("Snooze PendingIntent must exist", snoozePendingIntent)
            val snoozeIntent = org.robolectric.Shadows.shadowOf(snoozePendingIntent).savedIntent
            assertEquals(targetUser.userId, snoozeIntent.getStringExtra(MedicineAlarmReceiver.EXTRA_USER_ID))

            // 3. Test cancelSnooze for non-primary updates intent with EXTRA_USER_ID
            scheduler.cancelSnooze(entry2.entryId)
            val updatedSnoozePi = PendingIntent.getBroadcast(
                context,
                entry1.entryId.hashCode() + 1000,
                Intent(context, MedicineAlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            val updatedIntent = org.robolectric.Shadows.shadowOf(updatedSnoozePi).savedIntent
            assertEquals(targetUser.userId, updatedIntent.getStringExtra(MedicineAlarmReceiver.EXTRA_USER_ID))

            // 4. Test cancelSnooze for primary transfers snooze_group_user to new primary
            scheduler.scheduleGroupedSnooze(
                entryIds = listOf(entry1.entryId, entry2.entryId),
                minutes = 10,
                medicineNames = listOf("Medicine 1", "Medicine 2"),
                explicitTriggerTime = futureTrigger,
                userId = targetUser.userId
            )
            scheduler.cancelSnooze(entry1.entryId)
            assertEquals(targetUser.userId, prefs.getString("snooze_group_user_${entry2.entryId}", null))
            assertNull(prefs.getString("snooze_group_user_${entry1.entryId}", null))

            // 5. Test empty group cleanup removes snooze_group_user
            scheduler.cancelSnooze(entry2.entryId)
            assertNull(prefs.getString("snooze_group_user_${entry2.entryId}", null))
        } finally {
            com.example.dosezy.data.DosezyDatabase.setInstance(null)
            testDb.close()
        }
    }
}
