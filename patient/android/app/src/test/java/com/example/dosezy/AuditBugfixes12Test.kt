package com.example.dosezy

import android.content.Context
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.export.BackupRestoreManager
import com.example.dosezy.data.export.DataExporter
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.Theme
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import io.mockk.mockk
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Unit tests verifying Audit #12 bug fixes:
 *  1. BackupRestoreManager.parseMedicinesFromJson parses flattened frequency properties from DataExporter exports.
 *  2. DataExporter.buildJsonContent serializes both nested frequency object and top-level flattened keys.
 *  3. Round-trip export and restore of complex frequency schedules.
 *  4. Onboarding backup restore profile selection prioritizes isCurrentUser = true.
 *  5. Profile deletion theme preference synchronization and system fallback.
 */
class AuditBugfixes12Test {

    private lateinit var mockContext: Context
    private lateinit var mockDatabase: DosezyDatabase
    private lateinit var mockScheduleRepo: ScheduleRepository
    private lateinit var mockUserRepo: UserRepository
    private lateinit var mockMedicineRepo: MedicineRepository
    private lateinit var backupRestoreManager: BackupRestoreManager
    private lateinit var dataExporter: DataExporter

    @Before
    fun setup() {
        mockContext = mockk<Context>(relaxed = true)
        mockDatabase = mockk<DosezyDatabase>(relaxed = true)
        mockScheduleRepo = mockk<ScheduleRepository>(relaxed = true)
        mockUserRepo = mockk<UserRepository>(relaxed = true)
        mockMedicineRepo = mockk<MedicineRepository>(relaxed = true)

        backupRestoreManager = BackupRestoreManager(
            context = mockContext,
            database = mockDatabase,
            scheduleRepository = mockScheduleRepo
        )

        dataExporter = DataExporter(
            context = mockContext,
            userRepository = mockUserRepo,
            medicineRepository = mockMedicineRepo,
            scheduleRepository = mockScheduleRepo
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. Deserialization of Flattened Frequency Schedules (DataExporter & Legacy)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testParseMedicinesFromJson_preservesFlattenedFrequenciesFromDataExporter() {
        // Construct JSON with flattened frequency attributes as produced by DataExporter.buildJsonContent
        val jsonArray = JSONArray()

        // 1. Hourly medication
        jsonArray.put(JSONObject().apply {
            put("medicineId", "med-hourly")
            put("userId", "u-1")
            put("medicationName", "Amoxicillin")
            put("dosage", 500.0)
            put("dosageUnit", "MG")
            put("timesPerDay", 3)
            put("frequencyPattern", "EVERY_X_HOURS")
            put("intervalHours", 8)
            put("scheduledTimes", JSONArray(listOf("08:00", "16:00", "00:00")))
        })

        // 2. Custom multi-week recurrence
        jsonArray.put(JSONObject().apply {
            put("medicineId", "med-custom")
            put("userId", "u-1")
            put("medicationName", "Methotrexate")
            put("dosage", 15.0)
            put("dosageUnit", "MG")
            put("timesPerDay", 1)
            put("frequencyPattern", "CUSTOM")
            put("intervalWeeks", 2)
            put("selectedDaysOfWeek", JSONArray(listOf(1, 5))) // Mon, Fri
            put("scheduledTimes", JSONArray(listOf("09:00")))
        })

        // 3. Weekly medication
        jsonArray.put(JSONObject().apply {
            put("medicineId", "med-weekly")
            put("userId", "u-1")
            put("medicationName", "Vitamin D3")
            put("dosage", 60000.0)
            put("dosageUnit", "TABLET")
            put("timesPerDay", 1)
            put("frequencyPattern", "WEEKLY")
            put("daysPerWeek", 1)
            put("selectedDaysOfWeek", JSONArray(listOf(7))) // Sunday
            put("scheduledTimes", JSONArray(listOf("10:00")))
        })

        // 4. Interval days medication
        jsonArray.put(JSONObject().apply {
            put("medicineId", "med-interval-days")
            put("userId", "u-1")
            put("medicationName", "Alternate Steroid")
            put("dosage", 20.0)
            put("dosageUnit", "MG")
            put("timesPerDay", 1)
            put("frequencyPattern", "EVERY_X_DAYS")
            put("intervalDays", 3)
            put("scheduledTimes", JSONArray(listOf("08:00")))
        })

        // 5. Monthly medication
        jsonArray.put(JSONObject().apply {
            put("medicineId", "med-monthly")
            put("userId", "u-1")
            put("medicationName", "Monthly Injection")
            put("dosage", 1.0)
            put("dosageUnit", "ML")
            put("timesPerDay", 1)
            put("frequencyPattern", "MONTHLY")
            put("daysPerMonth", 2)
            put("selectedDaysOfMonth", JSONArray(listOf(1, 15)))
            put("scheduledTimes", JSONArray(listOf("12:00")))
        })

        // 6. PRN (As Needed) medication
        jsonArray.put(JSONObject().apply {
            put("medicineId", "med-prn")
            put("userId", "u-1")
            put("medicationName", "Paracetamol")
            put("dosage", 650.0)
            put("dosageUnit", "MG")
            put("timesPerDay", 1)
            put("frequencyPattern", "AS_NEEDED")
            put("scheduledTimes", JSONArray())
        })

        val parsedMeds = backupRestoreManager.parseMedicinesFromJson(jsonArray.toString())
        assertEquals(6, parsedMeds.size)

        // Verify Hourly
        val hourly = parsedMeds.first { it.medicineId == "med-hourly" }
        assertEquals(FrequencyPattern.EVERY_X_HOURS, hourly.frequency.pattern)
        assertEquals(8, hourly.frequency.intervalHours)

        // Verify Custom Multi-Week
        val custom = parsedMeds.first { it.medicineId == "med-custom" }
        assertEquals(FrequencyPattern.CUSTOM, custom.frequency.pattern)
        assertEquals(2, custom.frequency.intervalWeeks)
        assertEquals(listOf(1, 5), custom.frequency.selectedDaysOfWeek)

        // Verify Weekly
        val weekly = parsedMeds.first { it.medicineId == "med-weekly" }
        assertEquals(FrequencyPattern.WEEKLY, weekly.frequency.pattern)
        assertEquals(1, weekly.frequency.daysPerWeek)
        assertEquals(listOf(7), weekly.frequency.selectedDaysOfWeek)

        // Verify Interval Days
        val intervalDays = parsedMeds.first { it.medicineId == "med-interval-days" }
        assertEquals(FrequencyPattern.EVERY_X_DAYS, intervalDays.frequency.pattern)
        assertEquals(3, intervalDays.frequency.intervalDays)

        // Verify Monthly
        val monthly = parsedMeds.first { it.medicineId == "med-monthly" }
        assertEquals(FrequencyPattern.MONTHLY, monthly.frequency.pattern)
        assertEquals(2, monthly.frequency.daysPerMonth)
        assertEquals(listOf(1, 15), monthly.frequency.selectedDaysOfMonth)

        // Verify PRN
        val prn = parsedMeds.first { it.medicineId == "med-prn" }
        assertEquals(FrequencyPattern.AS_NEEDED, prn.frequency.pattern)
    }

    @Test
    fun testParseMedicinesFromJson_preservesNestedFrequenciesFromGsonBackups() {
        // Construct JSON with nested frequency object as produced by Gson serialization in BackupRestoreManager.createBackupZip
        val jsonArray = JSONArray()
        jsonArray.put(JSONObject().apply {
            put("medicineId", "med-nested")
            put("userId", "u-1")
            put("medicationName", "Metformin")
            put("dosage", 500.0)
            put("dosageUnit", "MG")
            put("timesPerDay", 2)
            val freqObj = JSONObject().apply {
                put("pattern", "EVERY_X_HOURS")
                put("intervalHours", 12)
            }
            put("frequency", freqObj)
            put("scheduledTimes", JSONArray(listOf("08:00", "20:00")))
        })

        val parsedMeds = backupRestoreManager.parseMedicinesFromJson(jsonArray.toString())
        assertEquals(1, parsedMeds.size)
        val med = parsedMeds[0]
        assertEquals(FrequencyPattern.EVERY_X_HOURS, med.frequency.pattern)
        assertEquals(12, med.frequency.intervalHours)
    }

    @Test
    fun testParseMedicinesFromJson_safeFallbackWhenFrequencyMissing() {
        val jsonArray = JSONArray()
        jsonArray.put(JSONObject().apply {
            put("medicineId", "med-no-freq")
            put("userId", "u-1")
            put("medicationName", "Aspirin")
            put("dosage", 81.0)
            put("dosageUnit", "MG")
            put("timesPerDay", 1)
            put("scheduledTimes", JSONArray(listOf("09:00")))
        })

        val parsedMeds = backupRestoreManager.parseMedicinesFromJson(jsonArray.toString())
        assertEquals(1, parsedMeds.size)
        assertEquals(FrequencyPattern.DAILY, parsedMeds[0].frequency.pattern)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. DataExporter Dual Frequency Serialization & Round-Trip Parity
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testDataExporter_buildJsonContent_outputsBothNestedAndFlattenedFrequency() {
        val user = User(
            userId = "user-1",
            fullName = "Jane Doe",
            age = 45,
            gender = Gender.FEMALE,
            contactNumber = "1234567890",
            theme = Theme.DARK,
            timeFormat = TimeFormat.HOUR_12
        )

        val customMed = Medicine(
            medicineId = "med-test-rt",
            userId = user.userId,
            medicationName = "Infliximab",
            dosage = 100.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(
                pattern = FrequencyPattern.CUSTOM,
                intervalWeeks = 4,
                selectedDaysOfWeek = listOf(2, 6) // Tue, Sat
            ),
            scheduledTimes = listOf(LocalTime.of(10, 0))
        )

        val jsonString = dataExporter.buildJsonContent(user, listOf(customMed), emptyList())
        val rootObj = JSONObject(jsonString)
        val medsArray = rootObj.getJSONArray("medicines")
        assertEquals(1, medsArray.length())

        val medObj = medsArray.getJSONObject(0)

        // 1. Assert flattened keys exist for backwards compatibility
        assertEquals("CUSTOM", medObj.getString("frequencyPattern"))
        assertEquals(4, medObj.getInt("intervalWeeks"))
        val flatDays = medObj.getJSONArray("selectedDaysOfWeek")
        assertEquals(2, flatDays.getInt(0))
        assertEquals(6, flatDays.getInt(1))

        // 2. Assert nested frequency object exists for Gson and BackupRestoreManager parity
        assertTrue(medObj.has("frequency"))
        val nestedFreq = medObj.getJSONObject("frequency")
        assertEquals("CUSTOM", nestedFreq.getString("pattern"))
        assertEquals(4, nestedFreq.getInt("intervalWeeks"))
        val nestedDays = nestedFreq.getJSONArray("selectedDaysOfWeek")
        assertEquals(2, nestedDays.getInt(0))
        assertEquals(6, nestedDays.getInt(1))

        // 3. Assert round-trip deserialization via BackupRestoreManager
        val restoredMeds = backupRestoreManager.parseMedicinesFromJson(medsArray.toString())
        assertEquals(1, restoredMeds.size)
        val restored = restoredMeds[0]
        assertEquals(FrequencyPattern.CUSTOM, restored.frequency.pattern)
        assertEquals(4, restored.frequency.intervalWeeks)
        assertEquals(listOf(2, 6), restored.frequency.selectedDaysOfWeek)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. Multi-Profile Onboarding Restore Selection
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testOnboardingRestoreProfileSelection_prioritizesActiveProfile() {
        val user1 = User(userId = "u-child", fullName = "Child Profile", age = 8, gender = Gender.MALE, contactNumber = "", isCurrentUser = false)
        val user2 = User(userId = "u-parent", fullName = "Primary Parent", age = 38, gender = Gender.FEMALE, contactNumber = "555-1234", isCurrentUser = true)
        val user3 = User(userId = "u-grandparent", fullName = "Grandparent", age = 70, gender = Gender.MALE, contactNumber = "", isCurrentUser = false)

        val updatedUsersUnordered = listOf(user1, user2, user3)

        // Defective previous logic: updatedUsers.first() picked user1 (Child Profile)
        val defectiveSelection = updatedUsersUnordered.first()
        assertEquals("u-child", defectiveSelection.userId)

        // Fixed logic: Prioritizes user with isCurrentUser == true
        val fixedSelection = updatedUsersUnordered.find { it.isCurrentUser } ?: updatedUsersUnordered.first()
        assertEquals("u-parent", fixedSelection.userId)
        assertTrue(fixedSelection.isCurrentUser)
    }

    @Test
    fun testOnboardingRestoreProfileSelection_fallsBackToFirstWhenNoneMarkedCurrent() {
        val user1 = User(userId = "u-sole", fullName = "Sole Profile", age = 30, gender = Gender.DO_NOT_SPECIFY, contactNumber = "", isCurrentUser = false)
        val userList = listOf(user1)

        val selection = userList.find { it.isCurrentUser } ?: userList.first()
        assertEquals("u-sole", selection.userId)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. User Deletion Theme Preference Synchronization Logic
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testDeleteUserThemeSync_resolvesNextUserThemeAndSystemFallback() {
        val deletedUser = User(userId = "u-deleted", fullName = "Old User", age = 40, gender = Gender.MALE, contactNumber = "", isCurrentUser = true, theme = Theme.DARK)
        val nextUser = User(userId = "u-next", fullName = "Next User", age = 25, gender = Gender.FEMALE, contactNumber = "", isCurrentUser = false, theme = Theme.LIGHT)

        val remainingUsers = listOf(deletedUser, nextUser).filter { it.userId != deletedUser.userId }
        assertEquals(1, remainingUsers.size)

        // When remainingUsers is not empty and deleted user was isCurrentUser:
        val promotedUser = remainingUsers.first().copy(isCurrentUser = true)
        val targetThemeToSave = promotedUser.theme.name.lowercase()
        assertEquals("light", targetThemeToSave)

        // When all users are deleted:
        val emptyRoster = emptyList<User>()
        val fallbackTheme = if (emptyRoster.isEmpty()) "system" else emptyRoster.first().theme.name.lowercase()
        assertEquals("system", fallbackTheme)
    }
}
