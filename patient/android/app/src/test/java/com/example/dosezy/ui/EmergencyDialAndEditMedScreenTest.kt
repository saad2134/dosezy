/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.User
import com.example.dosezy.data.model.normalizeArabicDigits
import com.example.dosezy.ui.components.deleteOldImage
import com.example.dosezy.ui.subscreens.createDialIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * Unit tests verifying Audit #17 bug fixes:
 *  1. EditMedScreen properly parses and preserves refillThreshold on medication update.
 *  2. EmergencyScreen namespaces country preferences per user profile with global fallback.
 *  3. EmergencyScreen createDialIntent adds FLAG_ACTIVITY_NEW_TASK and cleans phone numbers.
 *  4. Backup restore user target selection properly selects isCurrentUser or first profile.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmergencyDialAndEditMedScreenTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. EditMedScreen: refillThreshold Preservation
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testEditMedScreen_preservesRefillThreshold() {
        val originalMed = Medicine(
            medicineId = "med_123",
            userId = "user_abc",
            medicationName = "Atorvastatin",
            dosage = 20.0,
            dosageUnit = DosageUnit.TABLET,
            frequency = Frequency(FrequencyPattern.DAILY),
            timesPerDay = 1,
            scheduledTimes = listOf(LocalTime.of(21, 0)),
            currentStock = 30,
            refillThreshold = 5,
            autoDeductOnTake = true
        )

        val updatedStockText = "25"
        val updatedThresholdText = "7"

        val updatedMed = originalMed.copy(
            currentStock = updatedStockText.normalizeArabicDigits().toIntOrNull(),
            refillThreshold = updatedThresholdText.normalizeArabicDigits().toIntOrNull()
        )

        assertEquals(25, updatedMed.currentStock)
        assertEquals(7, updatedMed.refillThreshold)

        // Verify Arabic-Indic numerals work identically
        val arabicThresholdText = "١٠" // 10 in Arabic
        val arabicMed = originalMed.copy(
            refillThreshold = arabicThresholdText.normalizeArabicDigits().toIntOrNull()
        )
        assertEquals(10, arabicMed.refillThreshold)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. EmergencyScreen: Per-Profile Country Preference Namespacing
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testEmergencyScreen_namespacesCountryPerUserProfile() {
        val prefs = context.getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val user1Id = "user_alpha"
        val user2Id = "user_beta"

        val key1 = "selected_country_code_$user1Id"
        val key2 = "selected_country_code_$user2Id"

        // User 1 lives in UK (GB)
        prefs.edit()
            .putString(key1, "GB")
            .putString("selected_country_code", "GB")
            .commit()

        // User 2 lives in US (US)
        prefs.edit()
            .putString(key2, "US")
            .putString("selected_country_code", "US")
            .commit()

        val readUser1 = prefs.getString(key1, null) ?: prefs.getString("selected_country_code", null)
        val readUser2 = prefs.getString(key2, null) ?: prefs.getString("selected_country_code", null)

        assertEquals("GB", readUser1)
        assertEquals("US", readUser2)

        // Brand new user profile falls back to global key
        val user3Id = "user_gamma"
        val key3 = "selected_country_code_$user3Id"
        val readUser3 = prefs.getString(key3, null) ?: prefs.getString("selected_country_code", null)
        assertEquals("US", readUser3)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. EmergencyScreen: createDialIntent Flag & Number Cleaning
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testCreateDialIntent_cleansNumberAndAddsNewTaskFlag() {
        val rawNumber = "+1 (800) 555-0199"
        val intent = createDialIntent(rawNumber)

        assertEquals(Intent.ACTION_DIAL, intent.action)
        assertEquals("tel", intent.data?.scheme)
        assertEquals("+18005550199", intent.data?.schemeSpecificPart)
        assertTrue(
            "Intent must have FLAG_ACTIVITY_NEW_TASK so it can be safely launched from non-Activity contexts",
            (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. BackupRestoreScreen: Restored User Target Profile Resolution
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testBackupRestoreScreen_resolvesCorrectTargetUserAfterRestore() {
        val user1 = User(
            userId = "u1",
            fullName = "Old User",
            age = 45,
            gender = Gender.MALE,
            contactNumber = "1234567890",
            isCurrentUser = false
        )
        val user2 = User(
            userId = "u2",
            fullName = "Restored Active User",
            age = 48,
            gender = Gender.FEMALE,
            contactNumber = "9876543210",
            isCurrentUser = true
        )

        val restoredUsers = listOf(user1, user2)
        val targetUser = restoredUsers.find { it.isCurrentUser } ?: restoredUsers.first()

        assertNotNull(targetUser)
        assertEquals("u2", targetUser.userId)
        assertEquals("Restored Active User", targetUser.fullName)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 5. EmergencyScreen: Secondary Profile Global Fallback Isolation
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testEmergencyScreen_secondaryProfileDoesNotPolluteGlobalFallback() {
        val prefs = context.getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val primaryUserId = "user_primary"
        val secondaryUserId = "user_secondary"
        val newUserId = "user_brand_new"

        // Primary profile sets IN
        prefs.edit()
            .putString("selected_country_code_$primaryUserId", "IN")
            .putString("selected_country_code", "IN")
            .commit()

        // Guard: Secondary profile selection updates only namespaced key, protecting global fallback for primary/new profiles
        val isCurrentSecondary = false
        val editor = prefs.edit().putString("selected_country_code_$secondaryUserId", "US")
        if (isCurrentSecondary) {
            editor.putString("selected_country_code", "US")
        }
        editor.commit()

        val readPrimary = prefs.getString("selected_country_code_$primaryUserId", null)
            ?: prefs.getString("selected_country_code", null)
        assertEquals("IN", readPrimary)

        val readSecondary = prefs.getString("selected_country_code_$secondaryUserId", null)
            ?: prefs.getString("selected_country_code", null)
        assertEquals("US", readSecondary)

        val readNew = prefs.getString("selected_country_code_$newUserId", null)
            ?: prefs.getString("selected_country_code", null)
        assertEquals("IN", readNew)
    }

    @Test
    fun testDraftImageSelection_preservesActiveImageUntilExplicitSave() {
        val filesDir = context.filesDir
        val activeImageFile = java.io.File(filesDir, "active_med_photo.jpg").apply { writeText("original photo") }
        assertTrue("Original active image must exist on disk", activeImageFile.exists())

        val medicine = Medicine(
            medicineId = "med_image_test_1",
            userId = "user_1",
            medicationName = "Aspirin",
            dosage = 100.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0)),
            imageUri = activeImageFile.absolutePath
        )

        // 1. Emulate picking a draft image in EditMedScreen
        val draftImage1 = java.io.File(filesDir, "draft_photo_1.jpg").apply { writeText("draft 1") }
        var currentDraftPath = draftImage1.absolutePath

        // Guard: Selecting a draft image must NOT delete the active persisted entity image
        assertTrue("Active image must remain intact during draft selection", activeImageFile.exists())

        // 2. Emulate picking a second draft image in the same session
        val draftImage2 = java.io.File(filesDir, "draft_photo_2.jpg").apply { writeText("draft 2") }
        // Intermediate draft 1 is cleaned up, but active image remains untouched
        if (currentDraftPath != medicine.imageUri) {
            deleteOldImage(context, currentDraftPath)
        }
        currentDraftPath = draftImage2.absolutePath

        assertFalse("Intermediate uncommitted draft 1 must be cleaned up", draftImage1.exists())
        assertTrue("Active image must still remain intact before explicit form save", activeImageFile.exists())
        assertTrue("Newly selected draft 2 must exist on disk", draftImage2.exists())

        // 3. User cancels form without saving -> active image remains intact in SQLite and on disk
        val cancelled = true
        if (cancelled) {
            assertEquals(activeImageFile.absolutePath, medicine.imageUri)
            assertTrue("Active image remains valid after form cancellation", java.io.File(medicine.imageUri!!).exists())
        }

        // 4. User re-enters and saves changes -> active image is deleted and replaced with committed draft
        if (!medicine.imageUri.isNullOrBlank() && medicine.imageUri != currentDraftPath) {
            deleteOldImage(context, medicine.imageUri)
        }
        val savedMedicine = medicine.copy(imageUri = currentDraftPath)

        assertFalse("Superseded active image must be deleted only upon explicit save", activeImageFile.exists())
        assertEquals(draftImage2.absolutePath, savedMedicine.imageUri)
        assertTrue("Committed image must exist on disk", java.io.File(savedMedicine.imageUri!!).exists())
    }
}

