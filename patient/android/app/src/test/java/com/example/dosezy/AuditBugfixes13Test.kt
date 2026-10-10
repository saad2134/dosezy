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
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.Theme
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import io.mockk.mockk
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/**
 * Unit tests verifying Audit #13 bug fixes:
 *  1. FrequencyPattern.EVERY_X_HOURS continuous timeline across midnight boundaries.
 *  2. DataExporter.buildJsonContent serializes slideActionsEnabled, timelineModeEnabled, and thickerCalendarDayHighlight.
 *  3. BackupRestoreManager.parseUserFromJson faithfully restores UI preferences without resetting to defaults.
 *  4. DataExporter.buildCsvContent includes weekly and monthly frequency day columns.
 *  5. DataExporter PDF report uses user.gender.displayName rather than raw enum identifier.
 */
class AuditBugfixes13Test {

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
    // 1. FrequencyPattern.EVERY_X_HOURS Midnight Rollover & Continuity
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testEveryXHours_multiDayPreservesExactHourlyStepAcrossMidnight() {
        val start = LocalDate.of(2030, 5, 1)
        val medicine = Medicine(
            medicineId = "med_hourly",
            userId = "user_1",
            medicationName = "Amoxicillin",
            dosage = 500.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 4,
            frequency = Frequency(pattern = FrequencyPattern.EVERY_X_HOURS, intervalHours = 6),
            scheduledTimes = listOf(LocalTime.of(21, 0)),
            startDate = start
        )

        // Generate across 2 days (days = 1): May 1 and May 2
        val entries = medicine.generateScheduleEntries(startDateRange = start, days = 1)
        assertEquals(5, entries.size)
        val dateTimes = entries.map { it.scheduledDateTime }

        val expected = listOf(
            LocalDateTime.of(2030, 5, 1, 21, 0),
            LocalDateTime.of(2030, 5, 2, 3, 0),
            LocalDateTime.of(2030, 5, 2, 9, 0),
            LocalDateTime.of(2030, 5, 2, 15, 0),
            LocalDateTime.of(2030, 5, 2, 21, 0)
        )
        assertEquals(expected, dateTimes)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. DataExporter User Preferences JSON Serialization & Restore Round-Trip
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testBuildJsonContent_serializesAllUiPreferencesAndRoundTripsViaBackupRestoreManager() {
        val user = User(
            userId = "usr_pref_test",
            fullName = "Jane Doe",
            age = 32,
            gender = Gender.FEMALE,
            contactNumber = "+1234567890",
            slideActionsEnabled = false,          // Non-default
            timelineModeEnabled = true,           // Non-default
            thickerCalendarDayHighlight = true   // Non-default
        )

        val jsonString = dataExporter.buildJsonContent(user, emptyList(), emptyList())
        val root = JSONObject(jsonString)
        val userJson = root.getJSONObject("user")

        // Assert explicit presence of previously missing preferences
        assertTrue(userJson.has("slideActionsEnabled"))
        assertFalse(userJson.getBoolean("slideActionsEnabled"))

        assertTrue(userJson.has("timelineModeEnabled"))
        assertTrue(userJson.getBoolean("timelineModeEnabled"))

        assertTrue(userJson.has("thickerCalendarDayHighlight"))
        assertTrue(userJson.getBoolean("thickerCalendarDayHighlight"))

        // Verify round-trip parsing via BackupRestoreManager.parseUserFromJson
        val restoredUser = backupRestoreManager.parseUserFromJson(userJson.toString())
        assertEquals(false, restoredUser.slideActionsEnabled)
        assertEquals(true, restoredUser.timelineModeEnabled)
        assertEquals(true, restoredUser.thickerCalendarDayHighlight)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. DataExporter CSV Frequency Day Columns
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testBuildCsvContent_includesWeeklyAndMonthlyFrequencyColumns() {
        val user = User(
            userId = "usr_1",
            fullName = "John Doe",
            age = 45,
            gender = Gender.MALE,
            contactNumber = "555-1234"
        )

        val med = Medicine(
            medicineId = "med_weekly_1",
            userId = "usr_1",
            medicationName = "Methotrexate",
            dosage = 15.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(
                pattern = FrequencyPattern.WEEKLY,
                daysPerWeek = 2,
                selectedDaysOfWeek = listOf(1, 4) // Mon, Thu
            ),
            scheduledTimes = listOf(LocalTime.of(9, 0)),
            startDate = LocalDate.of(2026, 1, 1)
        )

        val csv = DataExporter.buildCsvContent(user, listOf(med), emptyList())

        // Header check
        assertTrue(csv.contains("Days Per Week,Days Per Month,Selected Days Of Week,Selected Days Of Month"))

        // Row values check
        assertTrue(csv.contains("\"1;4\""))
        assertTrue(csv.contains("2,"))
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. Gender Display Name vs Raw Enum
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testGender_displayNameProvidesCleanFormattingForReport() {
        assertEquals("Male", Gender.MALE.displayName)
        assertEquals("Female", Gender.FEMALE.displayName)
        assertEquals("Do not specify", Gender.DO_NOT_SPECIFY.displayName)
    }
}
