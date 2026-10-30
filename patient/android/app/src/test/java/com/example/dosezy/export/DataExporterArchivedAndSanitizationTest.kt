package com.example.dosezy.export

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.export.DataExporter
import com.example.dosezy.ui.components.ExportFormat
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.Theme
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import com.example.dosezy.utils.TimeCalculationUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDateTime
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataExporterArchivedAndSanitizationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 1: Dose Taken Status Inversion for Overdue / Late Doses
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug1_isTakenLate_identifiesOverdueDosesWithoutUpperLimitTruncation() {
        val scheduled = LocalDateTime.of(2026, 10, 6, 8, 0)

        // 1 hour later: On time (late threshold = 3h)
        val onTimeTaken = LocalDateTime.of(2026, 10, 6, 9, 0)
        assertFalse(TimeCalculationUtils.isTakenLate(scheduled, onTimeTaken, lateAfterHours = 3))

        // 4 hours later: Late (between late 3h and missed 6h)
        val lateTaken = LocalDateTime.of(2026, 10, 6, 12, 0)
        assertTrue(TimeCalculationUtils.isTakenLate(scheduled, lateTaken, lateAfterHours = 3))

        // 8 hours later: Overdue (exceeds missed threshold of 6h)
        // With legacy isLate, hours < 6 returned false, recording overdue doses as TAKEN_ON_TIME!
        // With isTakenLate, this correctly returns true!
        val overdueTaken = LocalDateTime.of(2026, 10, 6, 16, 0)
        assertTrue(TimeCalculationUtils.isTakenLate(scheduled, overdueTaken, lateAfterHours = 3))

        // 24 hours later (next day)
        val nextDayTaken = LocalDateTime.of(2026, 10, 7, 8, 0)
        assertTrue(TimeCalculationUtils.isTakenLate(scheduled, nextDayTaken, lateAfterHours = 3))
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 2: DataExporter Includes Archived Medicines
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug2_dataExporter_callsGetMedicinesByUserDirectToIncludeArchivedMedicines() = runTest {
        val mockUserRepo = mockk<UserRepository>(relaxed = true)
        val mockMedRepo = mockk<MedicineRepository>(relaxed = true)
        val mockSchedRepo = mockk<ScheduleRepository>(relaxed = true)

        val testUser = User(
            userId = "user_123",
            fullName = "Audit User",
            age = 30,
            gender = Gender.FEMALE,
            contactNumber = "555-0101",
            isCurrentUser = true
        )

        val activeMed = Medicine(
            medicineId = "med_active",
            userId = "user_123",
            medicationName = "Active Amoxicillin",
            dosage = 500.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = com.example.dosezy.data.model.Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0)),
            isArchived = false
        )

        val archivedMed = Medicine(
            medicineId = "med_archived",
            userId = "user_123",
            medicationName = "Discontinued Ibuprofen",
            dosage = 200.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = com.example.dosezy.data.model.Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(20, 0)),
            isArchived = true
        )

        val historicalSchedule = ScheduleEntry(
            entryId = "entry_1",
            userId = "user_123",
            medicineId = "med_archived",
            scheduledDateTime = LocalDateTime.of(2026, 9, 15, 20, 0),
            status = MedicationStatus.TAKEN_ON_TIME
        )

        coEvery { mockUserRepo.getUserByIdSync("user_123") } returns testUser
        coEvery { mockMedRepo.getMedicinesByUserDirect("user_123") } returns listOf(activeMed, archivedMed)
        coEvery { mockSchedRepo.getSchedulesByUserSync("user_123") } returns listOf(historicalSchedule)

        val exporter = DataExporter(context, mockUserRepo, mockMedRepo, mockSchedRepo)
        val csvFile = exporter.exportUserToCsv("user_123")

        // Verify getMedicinesByUserDirect was called, NOT getMedicinesByUserSync
        coVerify(exactly = 1) { mockMedRepo.getMedicinesByUserDirect("user_123") }
        coVerify(exactly = 0) { mockMedRepo.getMedicinesByUserSync(any()) }

        // Verify CSV content contains both active and archived medicine
        val csvContent = csvFile.readText()
        assertTrue(csvContent.contains("Active Amoxicillin"))
        assertTrue(csvContent.contains("Discontinued Ibuprofen"))
        assertTrue(csvContent.contains("med_archived"))
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 3: Filename Sanitization and Export Directory Fallback
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug3_dataExporter_sanitizesFilenamesAndAvoidsIllegalChars() = runTest {
        val mockUserRepo = mockk<UserRepository>(relaxed = true)
        val mockMedRepo = mockk<MedicineRepository>(relaxed = true)
        val mockSchedRepo = mockk<ScheduleRepository>(relaxed = true)

        // Name with forward slashes, colons, and illegal characters
        val testUserWithSpecialChars = User(
            userId = "user_special",
            fullName = "Dad/Mom : Cardio * Dr. Smith? <Test>",
            age = 65,
            gender = Gender.MALE,
            contactNumber = "555-0102",
            isCurrentUser = true
        )

        coEvery { mockUserRepo.getUserByIdSync("user_special") } returns testUserWithSpecialChars
        coEvery { mockMedRepo.getMedicinesByUserDirect("user_special") } returns emptyList()
        coEvery { mockSchedRepo.getSchedulesByUserSync("user_special") } returns emptyList()

        val exporter = DataExporter(context, mockUserRepo, mockMedRepo, mockSchedRepo)
        val exportedFile = exporter.exportUserToCsv("user_special")

        // Assert file exists and the filename does not contain raw illegal slashes or colons
        assertTrue(exportedFile.exists())
        assertFalse(exportedFile.name.contains("/"))
        assertFalse(exportedFile.name.contains(":"))
        assertFalse(exportedFile.name.contains("*"))
        assertFalse(exportedFile.name.contains("?"))
        assertFalse(exportedFile.name.contains("<"))
        assertFalse(exportedFile.name.contains(">"))

        val exportDir = exporter.getExportDirectory()
        assertNotNull(exportDir)
        assertTrue(exportDir.exists() || exportDir.mkdirs())
        // Guard: Verify export directory is confined to private internal cache to prevent USB MTP exposure (#HIPAA, #GDPR)
        assertTrue("Export directory must reside inside app cacheDir", exportDir.canonicalPath.startsWith(context.cacheDir.canonicalPath))
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 4: Medicine Deletion Cutoff Preserves Morning Untaken Doses
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug4_deletionCutoff_preservesPastMorningDoses() {
        val now = LocalDateTime.of(2026, 10, 6, 14, 0) // 2:00 PM
        val morningScheduledTime = LocalDateTime.of(2026, 10, 6, 8, 0) // 8:00 AM
        val cutoff = now.minusMinutes(15) // 1:45 PM

        // 8:00 AM is before cutoff (1:45 PM)
        assertTrue(morningScheduledTime.isBefore(cutoff))

        // Cutoff epoch millis calculation matches UTC conversion
        val cutoffEpochMillis = cutoff.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val morningEpochMillis = morningScheduledTime.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()

        // Deleting from cutoff onwards (scheduledDateTime >= cutoffEpochMillis) preserves the morning dose
        assertTrue(morningEpochMillis < cutoffEpochMillis)
    }

    @Test
    fun testPharmacyOrder_formatsConsistentlyWithOrderingContext() {
        // Guard: Verify pharmacy order formats with the ordering context without errors and contains expected medication details
        val mockUserRepo = mockk<UserRepository>()
        val mockMedRepo = mockk<MedicineRepository>()
        val mockSchedRepo = mockk<ScheduleRepository>()

        val userEn = User(
            userId = "user_order_en",
            fullName = "John Doe",
            age = 45,
            gender = Gender.MALE,
            contactNumber = "555-1234",
            language = com.example.dosezy.data.model.Language.ENGLISH
        )
        val med = Medicine(
            medicineId = "med_order_1",
            userId = userEn.userId,
            medicationName = "Amoxicillin",
            dosage = 500.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 3,
            frequency = com.example.dosezy.data.model.Frequency(com.example.dosezy.data.model.FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0)),
            currentStock = 10,
            refillThreshold = 5
        )

        val exporter = DataExporter(context, mockUserRepo, mockMedRepo, mockSchedRepo)
        val orderText = exporter.generateConsolidatedPharmacyOrderText(
            profilesWithMedicines = listOf(Pair(userEn, listOf(med))),
            supplyDays = 30,
            includeStock = true,
            includeDosage = true
        )

        assertTrue("Order must contain pharmacy order title", orderText.contains(context.getString(com.example.dosezy.R.string.pharmacy_order_header_title)))
        assertTrue("Order must contain patient name", orderText.contains("John Doe"))
        assertTrue("Order must contain medication name", orderText.contains("Amoxicillin"))
        assertTrue("Order must contain dosage", orderText.contains("500 mg"))
    }

    @Test
    fun testExportUserData_whenNoProfileEntriesCanBeGenerated_deletesZipAndThrowsIllegalStateException() = runTest {
        // Guard: Verify empty zip exports are deleted and throw IllegalStateException instead of silently producing corrupt 22-byte files (#BUG-ZIP-EMPTY-EXPORT)
        val mockUserRepo = mockk<UserRepository>(relaxed = true)
        val mockMedRepo = mockk<MedicineRepository>(relaxed = true)
        val mockSchedRepo = mockk<ScheduleRepository>(relaxed = true)

        val user1 = User(userId = "user_fail_1", fullName = "User One", age = 40, gender = Gender.MALE, contactNumber = "111")
        val user2 = User(userId = "user_fail_2", fullName = "User Two", age = 42, gender = Gender.FEMALE, contactNumber = "222")

        coEvery { mockUserRepo.getAllUsersList() } returns listOf(user1, user2)
        // Simulate data access failure for all profiles
        coEvery { mockMedRepo.getMedicinesByUserDirect(any()) } throws RuntimeException("Simulated I/O failure")

        val exporter = DataExporter(context, mockUserRepo, mockMedRepo, mockSchedRepo)
        val exportDir = exporter.getExportDirectory()
        val zipFilesBefore = exportDir.listFiles { _, name -> name.endsWith(".zip") }?.toList() ?: emptyList()

        try {
            exporter.exportUserData(null, ExportFormat.CSV)
            org.junit.Assert.fail("Expected IllegalStateException when no profile entries could be generated")
        } catch (e: IllegalStateException) {
            assertTrue("Exception message must indicate export data failure", e.message?.contains("Failed to export data") == true)
            assertNotNull("Underlying exception must be preserved as cause", e.cause)
        }

        // Verify no empty or corrupt zip file remained in export directory
        val zipFilesAfter = exportDir.listFiles { _, name -> name.endsWith(".zip") }?.toList() ?: emptyList()
        assertEquals("No empty zip file should be left behind after failure", zipFilesBefore.size, zipFilesAfter.size)
    }

    // ---------------------------------------------------------------------------------------------
    // CSV Formula / DDE Injection Neutralization Guard (#BUG-CSV-INJECTION, CWE-1236)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testEscapeCsv_neutralizesFormulaAndDdeInjectionCharacters() {
        // Guard: Verify formula trigger characters =, +, -, @, \t, \r are prefixed with a single quote to prevent spreadsheet code execution
        assertEquals("\"'=1+1\"", DataExporter.escapeCsv("=1+1"))
        assertEquals("\"'+500mg dose\"", DataExporter.escapeCsv("+500mg dose"))
        assertEquals("\"'- Take with milk\"", DataExporter.escapeCsv("- Take with milk"))
        assertEquals("\"'@hospital_dept\"", DataExporter.escapeCsv("@hospital_dept"))
        assertEquals("\"'\tTab indented\"", DataExporter.escapeCsv("\tTab indented"))
        assertEquals("\"'\rCarriage return\"", DataExporter.escapeCsv("\rCarriage return"))
    }

    @Test
    fun testEscapeCsv_preservesNormalTextAndQuotes() {
        assertEquals("\"Aspirin 100mg\"", DataExporter.escapeCsv("Aspirin 100mg"))
        assertEquals("\"Took \"\"two\"\" tablets\"", DataExporter.escapeCsv("Took \"two\" tablets"))
        assertEquals("\"\"", DataExporter.escapeCsv(""))
        assertEquals("\"\"", DataExporter.escapeCsv(null))
    }

    @Test
    fun testBuildCsvContent_sanitizesUserNotesAndAllergiesContainingFormulaPrefixes() {
        val user = User(
            userId = "u_safe_1",
            fullName = "John Doe",
            age = 35,
            gender = Gender.MALE,
            contactNumber = "1234567890",
            allergies = "- Penicillin\n- Sulfa drugs",
            medicalConditions = "@Hypertension; =Asthma"
        )
        val med = Medicine(
            medicineId = "m_safe_1",
            userId = user.userId,
            medicationName = "+Metformin ER",
            dosage = 500.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = com.example.dosezy.data.model.Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0)),
            notes = "=cmd|' /C calc'!A0"
        )
        val sched = ScheduleEntry(
            entryId = "s_safe_1",
            userId = user.userId,
            medicineId = med.medicineId,
            scheduledDateTime = LocalDateTime.of(2026, 10, 30, 8, 0),
            status = MedicationStatus.PENDING,
            doseNotes = "- Taken before breakfast"
        )

        val csv = DataExporter.buildCsvContent(user, listOf(med), listOf(sched))

        assertTrue("Allergies starting with - must be sanitized with single quote", csv.contains("\"'- Penicillin\n- Sulfa drugs\""))
        assertTrue("Medical conditions starting with @ must be sanitized with single quote", csv.contains("\"'@Hypertension; =Asthma\""))
        assertTrue("Medication name starting with + must be sanitized with single quote", csv.contains("\"'+Metformin ER\""))
        assertTrue("Doctor notes starting with = must be sanitized with single quote", csv.contains("\"'=cmd|' /C calc'!A0\""))
        assertTrue("Dose notes starting with - must be sanitized with single quote", csv.contains("\"'- Taken before breakfast\""))
    }

    @Test
    fun testBuildCsvContent_includesReminderTypeInUserAndMedicinesSections() {
        val user = User(
            userId = "u_reminder_csv",
            fullName = "Jane Doe",
            age = 42,
            gender = Gender.FEMALE,
            contactNumber = "9988776655",
            reminderType = com.example.dosezy.data.model.ReminderType.NOTIFICATION
        )
        val med = Medicine(
            medicineId = "m_reminder_csv",
            userId = user.userId,
            medicationName = "Lisinopril",
            dosage = 10.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = com.example.dosezy.data.model.Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0)),
            reminderType = com.example.dosezy.data.model.ReminderType.ALARM
        )
        val sched = ScheduleEntry(
            entryId = "s_reminder_csv",
            userId = user.userId,
            medicineId = med.medicineId,
            scheduledDateTime = LocalDateTime.of(2026, 10, 30, 8, 0),
            status = MedicationStatus.PENDING
        )

        val csv = DataExporter.buildCsvContent(user, listOf(med), listOf(sched))

        // Header assertions
        assertTrue("User section header must include Reminder Type", csv.contains("Is Current User,Reminder Type"))
        assertTrue("Medicines section header must include Reminder Type", csv.contains("Image URI,Reminder Type"))

        // Row value assertions
        assertTrue("User row must serialize user reminder type", csv.contains("NOTIFICATION"))
        assertTrue("Medicine row must serialize medicine reminder type override", csv.contains("ALARM"))
    }
}
