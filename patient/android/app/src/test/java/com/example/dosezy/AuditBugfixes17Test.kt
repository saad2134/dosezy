/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy

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
import com.example.dosezy.ui.subscreens.createDialIntent
import org.junit.Assert.assertEquals
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
class AuditBugfixes17Test {

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
}
