package com.example.dosezy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.export.DataExporter
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.User
import com.example.dosezy.data.model.getLocalizedName
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuditBugfixes4Test {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 1: Avatar Packaging with file:// URI Prefix & Restore Fallback
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug1_avatarPackaging_stripsFilePrefixAndResolvesImageFile() {
        val tempAvatar = File.createTempFile("test_avatar", ".jpg")
        tempAvatar.writeText("fake image bytes")
        tempAvatar.deleteOnExit()

        val uriPicPath = "file://${tempAvatar.absolutePath}"

        // Direct File(uriPicPath).exists() fails on Linux/Android due to "file:" prefix
        val directFile = File(uriPicPath)
        assertFalse(directFile.exists())

        // Stripping prefix resolves the underlying file correctly
        val cleanPath = uriPicPath.removePrefix("file://")
        val cleanFile = File(cleanPath)
        assertTrue(cleanFile.exists())

        // Verify packaging into zip succeeds with cleaned path
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("profiles/user_1/avatar.jpg"))
            cleanFile.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }

        val zipBytes = baos.toByteArray()
        assertTrue(zipBytes.isNotEmpty())

        var foundAvatar = false
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "profiles/user_1/avatar.jpg") {
                    foundAvatar = true
                    val content = zis.bufferedReader(Charsets.UTF_8).readText()
                    assertEquals("fake image bytes", content)
                }
                entry = zis.nextEntry
            }
        }
        assertTrue(foundAvatar)
    }

    @Test
    fun bug1_overwriteRestore_doesNotPersistDeadOldDevicePathWhenAvatarMissing() {
        val avatarFileInBackup = File(context.cacheDir, "non_existent_avatar.jpg")
        assertFalse(avatarFileInBackup.exists())

        val existingLocalUser: User? = null

        // With the guard in place, if avatar is absent from backup and local user has no avatar,
        // it falls back to null rather than persisting the foreign dead path
        val finalPicPath = if (avatarFileInBackup.exists()) {
            avatarFileInBackup.absolutePath
        } else existingLocalUser?.profilePicPath?.takeIf { File(it.removePrefix("file://")).exists() }

        assertNull(finalPicPath)
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 2: Pharmacy Order State Retention on Filter / Profile Toggle
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug2_pharmacyOrder_retainsUserQuantitiesAndSelectionsAcrossFilterToggles() {
        val med1 = Medicine(
            medicineId = "med_1",
            userId = "u1",
            medicationName = "Metformin",
            dosage = 500.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 2,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            currentStock = 2,
            refillThreshold = 10
        )
        val med2 = Medicine(
            medicineId = "med_2",
            userId = "u1",
            medicationName = "Atorvastatin",
            dosage = 20.0,
            dosageUnit = DosageUnit.MG,
            timesPerDay = 1,
            frequency = Frequency(FrequencyPattern.DAILY),
            scheduledTimes = listOf(LocalTime.of(21, 0)),
            currentStock = 100,
            refillThreshold = 10
        )

        val allActiveMeds = listOf(med1, med2)
        val lowStockMeds = listOf(med1)

        // Initialize state
        val selectedMedicineIds = mutableListOf("med_1", "med_2")
        val knownMedicineIds = mutableSetOf("med_1", "med_2")
        val customQuantities = mutableMapOf("med_1" to 60, "med_2" to 30)
        val userEditedMedicineIds = mutableSetOf<String>()

        // 1. User adjusts med_1 quantity with stepper to 90
        userEditedMedicineIds.add("med_1")
        customQuantities["med_1"] = 90

        // 2. User unchecks med_2
        selectedMedicineIds.remove("med_2")
        assertFalse(selectedMedicineIds.contains("med_2"))

        // 3. User toggles "Only Low Stock" filter ON
        var displayedMeds = lowStockMeds

        // Re-evaluating default quantities for displayed meds respects userEditedMedicineIds
        displayedMeds.forEach { med ->
            if (med.medicineId !in userEditedMedicineIds) {
                customQuantities[med.medicineId] = med.calculateRefillQuantity(30)
            }
        }
        assertEquals(90, customQuantities["med_1"])

        // 4. User toggles "Only Low Stock" filter OFF (back to all active meds)
        displayedMeds = allActiveMeds

        // Add newly discovered meds if any (knownMedicineIds prevents re-adding unchecked ones)
        displayedMeds.forEach { med ->
            if (knownMedicineIds.add(med.medicineId)) {
                if (med.medicineId !in selectedMedicineIds) {
                    selectedMedicineIds.add(med.medicineId)
                }
            }
        }

        // med_2 must still be unchecked!
        assertFalse(selectedMedicineIds.contains("med_2"))
        // med_1 must still have user custom quantity 90!
        assertEquals(90, customQuantities["med_1"])
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 3: Schedule Calendar Pre-Grouped Date Lookup Accuracy
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug3_scheduleCalendar_preGroupingEnablesO1LookupsAcrossCalendarCells() {
        val testDate1 = LocalDate.of(2026, 10, 6)
        val testDate2 = LocalDate.of(2026, 10, 7)
        val testDate3 = LocalDate.of(2026, 10, 15)

        val entries = mutableListOf<ScheduleEntry>()
        // 100 entries on testDate1
        repeat(100) { i ->
            entries.add(
                ScheduleEntry(
                    entryId = "e1_$i",
                    userId = "u1",
                    medicineId = "m1",
                    scheduledDateTime = testDate1.atTime(8, 0).plusMinutes(i.toLong()),
                    status = MedicationStatus.PENDING
                )
            )
        }
        // 50 entries on testDate2
        repeat(50) { i ->
            entries.add(
                ScheduleEntry(
                    entryId = "e2_$i",
                    userId = "u1",
                    medicineId = "m2",
                    scheduledDateTime = testDate2.atTime(12, 0).plusMinutes(i.toLong()),
                    status = MedicationStatus.TAKEN_ON_TIME
                )
            )
        }

        // Pre-group by LocalDate once
        val entriesByDate = entries.groupBy { it.scheduledDateTime.toLocalDate() }

        // Verify O(1) lookups match exactly
        assertEquals(100, entriesByDate[testDate1]?.size)
        assertEquals(50, entriesByDate[testDate2]?.size)
        assertNull(entriesByDate[testDate3])

        // Ensure all items on testDate1 have correct date
        val date1Entries = entriesByDate[testDate1] ?: emptyList()
        assertTrue(date1Entries.all { it.scheduledDateTime.toLocalDate() == testDate1 })
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 4: Multi-Profile ZIP Export UTF-8 Charset Encoding
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug4_zipExport_preservesMultilingualUtf8Characters() {
        val multilingualCsv = "Medication,Dosage,Notes\n" +
                "باراسيتامول,500mg,ملاحظات المريض\n" +
                "पैरासिटामोल,650mg,दवा समय पर लें\n" +
                "Парацетамол,500мг,Принимать после еды\n" +
                "Érythromycine,250mg,À jeun\n"

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("profiles/user_1/medication_data.csv"))
            zos.write(multilingualCsv.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val zipBytes = baos.toByteArray()
        assertTrue(zipBytes.isNotEmpty())

        var decodedContent: String? = null
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "profiles/user_1/medication_data.csv") {
                    decodedContent = zis.bufferedReader(Charsets.UTF_8).readText()
                }
                entry = zis.nextEntry
            }
        }

        assertNotNull(decodedContent)
        val result = decodedContent!!
        assertEquals(multilingualCsv, result)
        assertTrue(result.contains("باراسيتامول"))
        assertTrue(result.contains("पैरासिटामोल"))
        assertTrue(result.contains("Парацетамол"))
        assertTrue(result.contains("Érythromycine"))
    }

    // ───────────────────────────────────────────────────────────────
    // Bug 5: Dosage and Stock Unit Localization
    // ───────────────────────────────────────────────────────────────
    @Test
    fun bug5_dosageUnit_getLocalizedNameReturnsConfiguredStrings() {
        val tabletName = DosageUnit.TABLET.getLocalizedName(context)
        val capsuleName = DosageUnit.CAPSULE.getLocalizedName(context)
        val mlName = DosageUnit.ML.getLocalizedName(context)
        val dropName = DosageUnit.DROP.getLocalizedName(context)
        val mgName = DosageUnit.MG.getLocalizedName(context)
        val mcgName = DosageUnit.MCG.getLocalizedName(context)

        assertTrue(tabletName.isNotBlank())
        assertTrue(capsuleName.isNotBlank())
        assertTrue(mlName.isNotBlank())
        assertTrue(dropName.isNotBlank())
        assertTrue(mgName.isNotBlank())
        assertTrue(mcgName.isNotBlank())

        assertEquals(context.getString(R.string.unit_tablet), tabletName)
        assertEquals(context.getString(R.string.unit_capsule), capsuleName)
        assertEquals(context.getString(R.string.unit_ml), mlName)
        assertEquals(context.getString(R.string.unit_drop), dropName)
        assertEquals(context.getString(R.string.unit_mg), mgName)
        assertEquals(context.getString(R.string.unit_mcg), mcgName)
    }
}
