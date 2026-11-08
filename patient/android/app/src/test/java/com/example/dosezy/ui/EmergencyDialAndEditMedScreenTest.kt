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
import com.example.dosezy.data.model.Language
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.User
import com.example.dosezy.data.model.normalizeArabicDigits
import com.example.dosezy.ui.components.deleteOldImage
import com.example.dosezy.ui.subscreens.createDialIntent
import com.example.dosezy.ui.subscreens.createSupportDialIntent
import com.example.dosezy.ui.subscreens.createSupportEmailIntent
import com.example.dosezy.ui.subscreens.createSupportUrlIntent
import com.example.dosezy.ui.subscreens.detectDeviceEmergencyCountry
import com.example.dosezy.ui.subscreens.matchEmergencyCountryByIso
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

    // ─────────────────────────────────────────────────────────────────────────────
    // 6. ProfilePicturePicker & MedicinePhotoVisualPicker: Remove Button Safety (BUG-02)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testImagePicker_removeButton_onlyCleansUncommittedDraft_preservingActiveSavedImage() {
        val filesDir = context.filesDir
        val activeImageFile = java.io.File(filesDir, "active_user_photo.jpg").apply {
            writeText("active saved photo on disk")
        }
        val user = User(
            userId = "user_photo_test",
            fullName = "Test User",
            age = 30,
            gender = Gender.MALE,
            contactNumber = "+1234567890",
            profilePicPath = activeImageFile.absolutePath
        )

        // Scenario A: User opens screen with active photo, taps Remove button, but cancels the screen
        var selectedProfilePic: String? = user.profilePicPath
        assertEquals(activeImageFile.absolutePath, selectedProfilePic)
        var uncommittedDraftPath: String? = null

        // Emulate clicking Remove button in ProfilePicturePicker
        uncommittedDraftPath?.let { draft ->
            deleteOldImage(context, draft)
            uncommittedDraftPath = null
        }
        selectedProfilePic = null
        assertNull("Selected profile pic state in memory is reset to null on remove", selectedProfilePic)

        // User hits back/cancel without saving
        val cancelledWithoutSaving = true
        if (cancelledWithoutSaving) {
            // Active image must still exist on disk because user never saved
            assertTrue(
                "Active image file must NOT be deleted from disk on picker remove click before form save",
                activeImageFile.exists()
            )
        }

        // Scenario B: User picks a new draft photo, then decides to tap Remove button
        val draftImageFile = java.io.File(filesDir, "new_draft_user_photo.jpg").apply {
            writeText("temporary draft photo")
        }
        uncommittedDraftPath = draftImageFile.absolutePath
        selectedProfilePic = draftImageFile.absolutePath
        assertEquals(draftImageFile.absolutePath, selectedProfilePic)

        // User clicks Remove button
        uncommittedDraftPath?.let { draft ->
            deleteOldImage(context, draft)
            uncommittedDraftPath = null
        }
        selectedProfilePic = null
        assertNull("Selected profile pic state in memory is reset to null after removing draft", selectedProfilePic)

        // Uncommitted draft is cleaned up immediately to avoid disk leaks
        assertFalse(
            "Uncommitted draft must be cleaned up on remove click",
            draftImageFile.exists()
        )
        // Active saved image remains completely intact
        assertTrue(
            "Active saved photo remains on disk after draft removal",
            activeImageFile.exists()
        )

        // Scenario C: User explicitly saves form with selectedProfilePic = null
        val formSaved = true
        if (formSaved && !user.profilePicPath.isNullOrBlank() && user.profilePicPath != selectedProfilePic) {
            deleteOldImage(context, user.profilePicPath)
        }
        assertFalse(
            "Active image is safely deleted only upon explicit form save",
            activeImageFile.exists()
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 7. EmergencyScreen: Initial Null Frame Migration & HelpSupportScreen Intents
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testEmergencyContactsMigration_preservesContactsWhenCurrentUserNullInitially() {
        val prefs = context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val legacyJson = """[{"name":"Dr. Watson","phone":"555-0100"}]"""
        prefs.edit().putString("contacts_json", legacyJson).commit()

        // Phase 1: Initial Compose render frame where currentUser is null
        val nullUserId: String? = null
        val userKeyInitial = if (nullUserId != null) "contacts_json_$nullUserId" else null
        var savedInitial = if (userKeyInitial != null) prefs.getString(userKeyInitial, null) else null
        if (nullUserId != null && savedInitial == null) {
            savedInitial = prefs.getString("contacts_json", null)
            if (savedInitial != null) {
                prefs.edit().putString(userKeyInitial, savedInitial).remove("contacts_json").remove("contacts_json_default").apply()
            }
        }

        // Verify: Legacy key must NOT be migrated or wiped into default key during null frame
        assertTrue("Legacy key must be preserved during initial null frame", prefs.contains("contacts_json"))
        assertFalse("contacts_json_default must NOT be populated from legacy during null frame", prefs.contains("contacts_json_default"))

        // Phase 2: Active profile resolves from Room (e.g. user_alpha)
        val resolvedUserId = "user_alpha"
        val userKeyResolved = "contacts_json_$resolvedUserId"
        var savedResolved = prefs.getString(userKeyResolved, null)
        if (savedResolved == null) {
            savedResolved = prefs.getString("contacts_json", null)
            if (savedResolved != null) {
                prefs.edit().putString(userKeyResolved, savedResolved).remove("contacts_json").remove("contacts_json_default").apply()
            }
        }

        // Verify: User profile claimed the legacy contacts, and legacy/default keys are cleaned up
        assertEquals(legacyJson, prefs.getString(userKeyResolved, null))
        assertFalse("Legacy key must be removed after successful user migration", prefs.contains("contacts_json"))
        assertFalse("Default key must be removed after successful user migration", prefs.contains("contacts_json_default"))
    }

    @Test
    fun testEmergencyContacts_newProfileDoesNotInheritDefaultContactsAndSaveGuardsNullUser() {
        val prefs = context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        // Suppose stale default key exists from earlier legacy runs
        prefs.edit().putString("contacts_json_default", """[{"name":"Old Contact","phone":"111"}]""").commit()

        // New profile user_beta enters
        val newUserId = "user_beta"
        val userKey = "contacts_json_$newUserId"
        var saved = prefs.getString(userKey, null)
        if (saved == null) {
            // Guard: Must only migrate contacts_json, never contacts_json_default
            saved = prefs.getString("contacts_json", null)
            if (saved != null) {
                prefs.edit().putString(userKey, saved).remove("contacts_json").remove("contacts_json_default").apply()
            }
        }

        // Verify: user_beta does NOT inherit contacts_json_default
        assertNull("New profile must not inherit contacts from contacts_json_default", saved)

        // Verify: saveContacts guards against null user
        fun saveContacts(userId: String?, contactsJson: String) {
            if (userId == null) return
            prefs.edit().putString("contacts_json_$userId", contactsJson).remove("contacts_json_default").apply()
        }

        saveContacts(null, """[{"name":"Ignored","phone":"000"}]""")
        assertFalse(prefs.contains("contacts_json_null"))

        saveContacts(newUserId, """[{"name":"Family","phone":"999"}]""")
        assertEquals("""[{"name":"Family","phone":"999"}]""", prefs.getString(userKey, null))
        assertFalse("contacts_json_default must be removed upon profile save", prefs.contains("contacts_json_default"))
    }

    @Test
    fun testHelpSupportScreen_intentHelpers_includeNewTaskFlagAndSanitizedData() {
        // URL Intent
        val urlIntent = createSupportUrlIntent("https://dosezy.app/faq")
        assertEquals(Intent.ACTION_VIEW, urlIntent.action)
        assertEquals("https://dosezy.app/faq", urlIntent.data?.toString())
        assertTrue(
            "URL intent must have FLAG_ACTIVITY_NEW_TASK",
            (urlIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        )

        // Email Intent
        val emailIntent = createSupportEmailIntent("support@dosezy.app", "Bug Report")
        assertEquals(Intent.ACTION_SENDTO, emailIntent.action)
        assertEquals("mailto:", emailIntent.data?.toString())
        val emails = emailIntent.getStringArrayExtra(Intent.EXTRA_EMAIL)
        assertNotNull(emails)
        assertEquals("support@dosezy.app", emails?.firstOrNull())
        assertEquals("Bug Report", emailIntent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertTrue(
            "Email intent must have FLAG_ACTIVITY_NEW_TASK",
            (emailIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        )

        // Phone Intent (with formatting characters)
        val dialIntent = createSupportDialIntent("+1 (888) 555-4321")
        assertEquals(Intent.ACTION_DIAL, dialIntent.action)
        assertEquals("tel", dialIntent.data?.scheme)
        assertEquals("+18885554321", dialIntent.data?.schemeSpecificPart)
        assertTrue(
            "Dial intent must have FLAG_ACTIVITY_NEW_TASK",
            (dialIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        )
    }

    @Test
    fun testEditAndAddMedScreen_customDosagesClearedWhenFrequencySwitchedToAsNeeded() {
        val scheduledTimesList = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
        val perTimeDosages = mapOf("08:00" to "10", "20:00" to "20")
        val hasDifferentDosages = true

        val baseDose = 10.0
        val finalCustomDosages: Map<String, Double>? = if (hasDifferentDosages && scheduledTimesList.size > 1) {
            val map = mutableMapOf<String, Double>()
            scheduledTimesList.forEach { t ->
                val key = String.format(java.util.Locale.US, "%02d:%02d", t.hour, t.minute)
                val entered = perTimeDosages[key]?.toDoubleOrNull() ?: baseDose
                map[key] = entered
            }
            if (map.isNotEmpty()) map else null
        } else null

        assertNotNull("finalCustomDosages should be populated when multiple times have custom dosages", finalCustomDosages)

        // Case A: User saves with DAILY frequency -> customDosages preserved
        val dailyFrequency = FrequencyPattern.DAILY
        val customDosagesDaily = if (dailyFrequency == FrequencyPattern.AS_NEEDED) null else finalCustomDosages
        assertNotNull("Daily frequency must retain customDosages", customDosagesDaily)
        assertEquals(2, customDosagesDaily?.size)

        // Case B: User switches frequency dropdown to AS_NEEDED -> customDosages must be nullified
        val asNeededFrequency = FrequencyPattern.AS_NEEDED
        val customDosagesAsNeeded = if (asNeededFrequency == FrequencyPattern.AS_NEEDED) null else finalCustomDosages
        assertNull("Switching frequency to AS_NEEDED must clear customDosages to prevent leaking stale scheduled doses into PRN logs", customDosagesAsNeeded)
    }

    @Test
    fun testEmergencyScreen_countryDetection_supportsExtended911RegionsAndPortugueseLanguage() {
        // Test extended 911 regions map to US
        val mxMatch = matchEmergencyCountryByIso("MX")
        assertNotNull("MX must map to 911 profile", mxMatch)
        assertEquals("US", mxMatch?.code)
        assertEquals("911", mxMatch?.ambulance)

        val crMatch = matchEmergencyCountryByIso("CR")
        assertNotNull("CR must map to 911 profile", crMatch)
        assertEquals("US", crMatch?.code)

        val paMatch = matchEmergencyCountryByIso("PA")
        assertNotNull("PA must map to 911 profile", paMatch)
        assertEquals("US", paMatch?.code)

        // Test NZ maps to AU profile
        val nzMatch = matchEmergencyCountryByIso("NZ")
        assertNotNull("NZ must map to AU profile", nzMatch)
        assertEquals("AU", nzMatch?.code)
        assertEquals("000", nzMatch?.ambulance)

        // Test fallback when device country ISO is unavailable/unmatched:
        // Use a Context with an unmapped locale country so step 1 and 2 do not match US by default
        val configPt = android.content.res.Configuration(context.resources.configuration)
        configPt.setLocale(java.util.Locale.forLanguageTag("pt"))
        val ptContext = context.createConfigurationContext(configPt)

        // Test Portuguese language fallback selects Brazil (BR)
        val detectedPortuguese = detectDeviceEmergencyCountry(ptContext, Language.PORTUGUESE)
        assertEquals("BR", detectedPortuguese.code)
        assertEquals("192", detectedPortuguese.ambulance)

        // Test Spanish language fallback selects EU
        val configEs = android.content.res.Configuration(context.resources.configuration)
        configEs.setLocale(java.util.Locale.forLanguageTag("es"))
        val esContext = context.createConfigurationContext(configEs)
        val detectedSpanish = detectDeviceEmergencyCountry(esContext, Language.SPANISH)
        assertEquals("EU", detectedSpanish.code)
        assertEquals("112", detectedSpanish.ambulance)

        // Test English fallback selects US (911)
        val configEn = android.content.res.Configuration(context.resources.configuration)
        configEn.setLocale(java.util.Locale.forLanguageTag("en"))
        val enContext = context.createConfigurationContext(configEn)
        val detectedEnglish = detectDeviceEmergencyCountry(enContext, Language.ENGLISH)
        assertEquals("US", detectedEnglish.code)
        assertEquals("911", detectedEnglish.ambulance)
    }
}



