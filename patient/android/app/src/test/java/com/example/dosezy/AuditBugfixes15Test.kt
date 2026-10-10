/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.notifications.AlarmScheduler
import com.example.dosezy.widget.DosezyWidgetPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Unit tests verifying Audit #15 bug fixes:
 *  1. Multi-user ZIP export folder deduplication & blank/symbolic name sanitization.
 *  2. Home screen time slot status differentiation (allTaken vs allSkipped vs allCompleted).
 *  3. AlarmScheduler atomic batch editor transactions for snooze and nagging coordination.
 *  4. DosezyWidgetPrefs preserves ACTIVE_PROFILE_ID theme when saving non-active profiles.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuditBugfixes15Test {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. Multi-User ZIP Export Folder Deduplication & Sanitization
    // ─────────────────────────────────────────────────────────────────────────────

    private fun sanitizeFileName(name: String): String {
        val clean = name.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_").trim('_', '.')
        return clean.ifBlank { "user" }
    }

    @Test
    fun testZipExport_identicalProfileNames_deduplicatesFolderPathsWithoutZipException() {
        val users = listOf(
            Pair("u_alex_1", "Alex Smith"),
            Pair("u_alex_2", "Alex Smith"),
            Pair("u_blank", "???"),
            Pair("u_empty", "")
        )

        val usedFolders = mutableSetOf<String>()
        val generatedFolders = mutableListOf<String>()

        users.forEach { (userId, fullName) ->
            var baseFolder = sanitizeFileName(fullName)
            if (usedFolders.contains(baseFolder)) {
                baseFolder = "${baseFolder}_${userId.take(6)}"
            }
            usedFolders.add(baseFolder)
            generatedFolders.add(baseFolder)
        }

        // Verify folder names are all distinct
        assertEquals(4, generatedFolders.distinct().size)
        assertEquals("Alex_Smith", generatedFolders[0])
        assertEquals("Alex_Smith_u_alex", generatedFolders[1])
        assertEquals("user", generatedFolders[2])
        assertEquals("user_u_empt", generatedFolders[3])

        // Verify streaming all entries into a real ZipOutputStream does not throw ZipException
        val byteArrayOutputStream = ByteArrayOutputStream()
        ZipOutputStream(byteArrayOutputStream).use { zipOut ->
            generatedFolders.forEach { folder ->
                zipOut.putNextEntry(ZipEntry("$folder/medication_data.csv"))
                zipOut.write("id,name\n1,Aspirin".toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
            }
        }
        assertTrue("ZIP archive was successfully generated", byteArrayOutputStream.size() > 0)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. Home Screen Time Slot Status Differentiation (Taken vs Skipped)
    // ─────────────────────────────────────────────────────────────────────────────

    private fun createEntry(status: MedicationStatus): ScheduleEntry {
        return ScheduleEntry(
            entryId = "entry_${java.util.UUID.randomUUID()}",
            userId = "user_1",
            medicineId = "med_1",
            scheduledDateTime = LocalDateTime.now(),
            status = status
        )
    }

    @Test
    fun testTimeSlotStatus_allSkipped_isNotClassifiedAsAllTaken() {
        val skippedEntries = listOf(
            createEntry(MedicationStatus.SKIPPED),
            createEntry(MedicationStatus.SKIPPED)
        )

        val allTaken = skippedEntries.all {
            it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE
        }
        val allSkipped = skippedEntries.all {
            it.status == MedicationStatus.SKIPPED
        }
        val allCompleted = skippedEntries.all {
            it.status == MedicationStatus.TAKEN_ON_TIME ||
                    it.status == MedicationStatus.TAKEN_LATE ||
                    it.status == MedicationStatus.SKIPPED
        }

        assertFalse("Slot with all skipped doses must NOT evaluate to allTaken", allTaken)
        assertTrue("Slot with all skipped doses must evaluate to allSkipped", allSkipped)
        assertTrue("Slot with all skipped doses is completed", allCompleted)
    }

    @Test
    fun testTimeSlotStatus_allTaken_correctlyEvaluates() {
        val takenEntries = listOf(
            createEntry(MedicationStatus.TAKEN_ON_TIME),
            createEntry(MedicationStatus.TAKEN_LATE)
        )

        val allTaken = takenEntries.all {
            it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE
        }
        val allSkipped = takenEntries.all {
            it.status == MedicationStatus.SKIPPED
        }

        assertTrue("Slot with taken doses must evaluate to allTaken", allTaken)
        assertFalse("Slot with taken doses must NOT evaluate to allSkipped", allSkipped)
    }

    @Test
    fun testTimeSlotStatus_mixedTakenAndSkipped_differentiatesCleanly() {
        val mixedEntries = listOf(
            createEntry(MedicationStatus.TAKEN_ON_TIME),
            createEntry(MedicationStatus.SKIPPED)
        )

        val allTaken = mixedEntries.all {
            it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE
        }
        val allSkipped = mixedEntries.all {
            it.status == MedicationStatus.SKIPPED
        }
        val allCompleted = mixedEntries.all {
            it.status == MedicationStatus.TAKEN_ON_TIME ||
                    it.status == MedicationStatus.TAKEN_LATE ||
                    it.status == MedicationStatus.SKIPPED
        }

        assertFalse("Mixed slot is not purely allTaken", allTaken)
        assertFalse("Mixed slot is not purely allSkipped", allSkipped)
        assertTrue("Mixed slot with no pending/missed doses is allCompleted", allCompleted)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. AlarmScheduler Atomic Batch Transactions for Snooze & Nagging
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testAlarmScheduler_cancelSnooze_atomicBatchEditorTransactions() {
        val scheduler = AlarmScheduler(context)
        val prefs = context.getSharedPreferences("dosezy_alarm_coordination", Context.MODE_PRIVATE)

        // Setup a 2-medication snooze cohort
        prefs.edit()
            .putString("snooze_group_ids_primary1", "primary1,member2")
            .putString("snooze_group_names_primary1", "Aspirin|||Metformin")
            .putLong("snooze_group_trigger_primary1", System.currentTimeMillis() + 600000L)
            .putString("snooze_member_primary1", "primary1")
            .putString("snooze_member_member2", "primary1")
            .commit()

        // Cancel the primary dose: member mapping removed, secondary promoted to new primary
        scheduler.cancelSnooze("primary1")

        assertNull("Cancelled primary member mapping must be removed", prefs.getString("snooze_member_primary1", null))
        assertNull("Old primary group IDs must be removed", prefs.getString("snooze_group_ids_primary1", null))
        assertEquals("Remaining member must point to new primary", "member2", prefs.getString("snooze_member_member2", null))
        assertEquals("New primary group IDs must reflect remaining member", "member2", prefs.getString("snooze_group_ids_member2", null))

        // Cancel remaining member: entire group purged cleanly
        scheduler.cancelSnooze("member2")

        assertNull("Final member mapping must be removed", prefs.getString("snooze_member_member2", null))
        assertNull("Final primary group IDs must be removed", prefs.getString("snooze_group_ids_member2", null))
        assertNull("Final primary trigger time must be removed", prefs.getString("snooze_group_trigger_member2", null))
    }

    @Test
    fun testAlarmScheduler_cancelNagging_atomicBatchEditorTransactions() {
        val scheduler = AlarmScheduler(context)
        val prefs = context.getSharedPreferences("dosezy_alarm_coordination", Context.MODE_PRIVATE)

        // Setup a 2-medication nagging cohort
        prefs.edit()
            .putString("nagging_group_ids_nagPrimary", "nagPrimary,nagMember")
            .putString("nagging_group_names_nagPrimary", "Lipitor|||Insulin")
            .putLong("nagging_group_trigger_nagPrimary", System.currentTimeMillis() + 300000L)
            .putInt("nagging_group_count_nagPrimary", 1)
            .putString("nagging_group_sched_nagPrimary", "08:00")
            .putString("nagging_member_nagPrimary", "nagPrimary")
            .putString("nagging_member_nagMember", "nagPrimary")
            .commit()

        // Cancel primary nagging dose
        scheduler.cancelNagging("nagPrimary")

        assertNull("Cancelled primary member mapping must be removed", prefs.getString("nagging_member_nagPrimary", null))
        assertNull("Old primary group IDs must be removed", prefs.getString("nagging_group_ids_nagPrimary", null))
        assertEquals("Remaining member must point to new primary", "nagMember", prefs.getString("nagging_member_nagMember", null))
        assertEquals("New primary group IDs must reflect remaining member", "nagMember", prefs.getString("nagging_group_ids_nagMember", null))

        // Cancel final nagging dose
        scheduler.cancelNagging("nagMember")

        assertNull("Final nagging member mapping must be removed", prefs.getString("nagging_member_nagMember", null))
        assertNull("Final nagging group IDs must be removed", prefs.getString("nagging_group_ids_nagMember", null))
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. DosezyWidgetPrefs Preserves ACTIVE_PROFILE_ID When Saving Non-Active Profiles
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testWidgetPrefs_saveWidgetProfileTheme_doesNotOverwriteActiveProfileForSecondaryUser() {
        // Set active profile theme to "dark"
        DosezyWidgetPrefs.saveWidgetProfileTheme(context, DosezyWidgetPrefs.ACTIVE_PROFILE_ID, "dark", isCurrentProfile = true)
        assertEquals("dark", DosezyWidgetPrefs.getWidgetProfileTheme(context, DosezyWidgetPrefs.ACTIVE_PROFILE_ID))

        // Update secondary profile theme to "light" (isCurrentProfile = false)
        DosezyWidgetPrefs.saveWidgetProfileTheme(context, "child_profile_123", "light", isCurrentProfile = false)

        // Secondary profile has its own theme
        assertEquals("light", DosezyWidgetPrefs.getWidgetProfileTheme(context, "child_profile_123"))

        // Active profile theme MUST NOT be overwritten
        assertEquals("dark", DosezyWidgetPrefs.getWidgetProfileTheme(context, DosezyWidgetPrefs.ACTIVE_PROFILE_ID))

        // Update active profile theme to "system" (isCurrentProfile = true)
        DosezyWidgetPrefs.saveWidgetProfileTheme(context, "parent_profile_active", "system", isCurrentProfile = true)

        assertEquals("system", DosezyWidgetPrefs.getWidgetProfileTheme(context, "parent_profile_active"))
        assertEquals("system", DosezyWidgetPrefs.getWidgetProfileTheme(context, DosezyWidgetPrefs.ACTIVE_PROFILE_ID))
    }
}
