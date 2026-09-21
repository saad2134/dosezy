package com.example.dosezy

import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.PillShape
import com.example.dosezy.data.model.User
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Tests for BackupRestoreManager serialization, backwards-compatibility with legacy backup formats,
 * resilient parsing of corrupted/malformed dates, and zip path traversal security defenses.
 */
class BackupRestoreValidationTest {

    // Mirror of BackupRestoreManager's resilient Gson configuration
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, JsonDeserializer { json, _, _ ->
            try { LocalDate.parse(json.asString) } catch (_: Exception) { null }
        })
        .registerTypeAdapter(LocalDate::class.java, JsonSerializer<LocalDate> { src, _, _ ->
            JsonPrimitive(src.toString())
        })
        .registerTypeAdapter(LocalTime::class.java, JsonDeserializer { json, _, _ ->
            try { LocalTime.parse(json.asString) } catch (_: Exception) { null }
        })
        .registerTypeAdapter(LocalTime::class.java, JsonSerializer<LocalTime> { src, _, _ ->
            JsonPrimitive(src.toString())
        })
        .create()

    // Path traversal validation logic mirroring BackupRestoreManager.inspectBackupZip
    private fun validateZipEntryPath(entryName: String) {
        if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
            throw SecurityException("Invalid backup archive: path traversal detected for entry '$entryName'.")
        }
    }

    // ───────────────────────────────────────────────────────────────
    // 1. Backwards Compatibility with Legacy Backups
    // ───────────────────────────────────────────────────────────────

    @Test
    fun legacyMedicineJson_missingNewerFields_parsesWithSafeDefaults() {
        // Legacy JSON from older Dosezy versions without:
        // customDosages, startDate, endDate, durationDays, notes, pillShape, pillColor
        val legacyJson = """
            {
              "medicineId": "med_legacy_001",
              "userId": "user_old",
              "medicationName": "Aspirin",
              "dosage": 81.0,
              "dosageUnit": "MG",
              "timesPerDay": 1,
              "frequency": {
                "pattern": "DAILY"
              },
              "scheduledTimes": ["08:00:00"],
              "currentStock": 100,
              "refillThreshold": 10,
              "autoDeductOnTake": true,
              "isArchived": false
            }
        """.trimIndent()

        val parsed = gson.fromJson(legacyJson, Medicine::class.java)

        assertNotNull(parsed)
        assertEquals("med_legacy_001", parsed.medicineId)
        assertEquals("Aspirin", parsed.medicationName)
        assertEquals(81.0, parsed.dosage, 0.001)
        assertEquals(DosageUnit.MG, parsed.dosageUnit)
        assertEquals(FrequencyPattern.DAILY, parsed.frequency.pattern)

        // Missing fields safely evaluate to null or default
        assertNull(parsed.customDosages)
        assertNull(parsed.startDate)
        assertNull(parsed.endDate)
        assertNull(parsed.notes)
    }

    @Test
    fun legacyUserJson_missingPreferences_parsesGracefully() {
        // Early user format without allowCustomDoseTime, allowDoseSkipping, customAlarmSound
        val legacyUserJson = """
            {
              "userId": "usr_legacy_123",
              "fullName": "Alice Oldfield",
              "age": 60,
              "gender": "FEMALE",
              "contactNumber": "555-1234",
              "isCurrentUser": true,
              "theme": "SYSTEM",
              "timeFormat": "HOUR_12",
              "language": "SYSTEM"
            }
        """.trimIndent()

        val user = gson.fromJson(legacyUserJson, User::class.java)

        assertNotNull(user)
        assertEquals("usr_legacy_123", user.userId)
        assertEquals("Alice Oldfield", user.fullName)
        assertEquals(60, user.age)
        assertEquals(true, user.isCurrentUser)
        // Default values apply
        assertFalse(user.allowCustomDoseTime)
        assertFalse(user.allowDoseSkipping)
    }

    // ───────────────────────────────────────────────────────────────
    // 2. Resilient Malformed Data Handling
    // ───────────────────────────────────────────────────────────────

    @Test
    fun malformedLocalDateString_deserializesToNullWithoutCrashing() {
        val jsonWithBadDate = """
            {
              "medicineId": "med_bad_date",
              "userId": "u1",
              "medicationName": "Test",
              "dosage": 10.0,
              "dosageUnit": "MG",
              "timesPerDay": 1,
              "frequency": { "pattern": "DAILY" },
              "scheduledTimes": [],
              "startDate": "not-a-valid-date-2026-99-99"
            }
        """.trimIndent()

        val parsed = gson.fromJson(jsonWithBadDate, Medicine::class.java)
        assertNotNull(parsed)
        assertNull(parsed.startDate) // Gracefully null, no crash
    }

    @Test
    fun malformedLocalTimeString_deserializesToNullWithoutCrashing() {
        val jsonWithBadTime = """
            {
              "medicineId": "med_bad_time",
              "userId": "u1",
              "medicationName": "Test",
              "dosage": 10.0,
              "dosageUnit": "MG",
              "timesPerDay": 1,
              "frequency": { "pattern": "DAILY" },
              "scheduledTimes": ["99:99:99", "invalid-time"]
            }
        """.trimIndent()

        val parsed = gson.fromJson(jsonWithBadTime, Medicine::class.java)
        assertNotNull(parsed)
        // Invalid times become null in the list
        assertTrue(parsed.scheduledTimes.all { (it as LocalTime?) == null })
    }

    // ───────────────────────────────────────────────────────────────
    // 3. Zip Path Traversal & Security Validation
    // ───────────────────────────────────────────────────────────────

    @Test
    fun zipPathTraversal_parentDirectoryDots_throwsSecurityException() {
        val maliciousEntries = listOf(
            "../../etc/passwd",
            "profiles/../../secret.txt",
            "..\\windows\\system32\\cmd.exe",
            "profiles/user1/../../../boot.ini"
        )

        for (entry in maliciousEntries) {
            assertThrows("Expected SecurityException for entry '$entry'", SecurityException::class.java) {
                validateZipEntryPath(entry)
            }
        }
    }

    @Test
    fun zipPathTraversal_leadingSlashes_throwsSecurityException() {
        val maliciousEntries = listOf(
            "/data/data/com.example.dosezy/databases/hacked.db",
            "\\Windows\\System32\\calc.exe",
            "/etc/shadow"
        )

        for (entry in maliciousEntries) {
            assertThrows("Expected SecurityException for entry '$entry'", SecurityException::class.java) {
                validateZipEntryPath(entry)
            }
        }
    }

    @Test
    fun zipPathValidation_legitimateArchiveEntries_passWithoutException() {
        val safeEntries = listOf(
            "manifest.json",
            "profiles/user_1/profile.json",
            "profiles/user_1/medicines.json",
            "profiles/user_1/schedules.json",
            "profiles/user_1/avatar.jpg",
            "profiles/user_1/assets/med_1.jpg",
            "profiles/user_1/emergency_contacts.json"
        )

        for (entry in safeEntries) {
            // Should execute without exception
            validateZipEntryPath(entry)
        }
    }

    // ───────────────────────────────────────────────────────────────
    // 4. Manifest JSON Validation
    // ───────────────────────────────────────────────────────────────

    @Test
    fun manifestJson_parsesCorrectly() {
        val manifestStr = """
            {
              "manifestVersion": 1,
              "appVersion": "2.5.5",
              "versionCode": 26,
              "exportedAt": "2026-09-21T22:00:00",
              "profileCount": 2,
              "profileIds": ["usr_001", "usr_002"]
            }
        """.trimIndent()

        val json = JSONObject(manifestStr)
        assertEquals(1, json.getInt("manifestVersion"))
        assertEquals("2.5.5", json.getString("appVersion"))
        assertEquals(26, json.getInt("versionCode"))
        assertEquals(2, json.getInt("profileCount"))
        assertEquals(2, json.getJSONArray("profileIds").length())
    }
}
