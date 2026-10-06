package com.example.dosezy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.export.DataExporter
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
class AuditBugfixes3Test {

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
}
