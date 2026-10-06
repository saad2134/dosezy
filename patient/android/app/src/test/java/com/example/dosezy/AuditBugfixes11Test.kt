package com.example.dosezy

import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.User
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Tests for Audit #11 fixes:
 *  1. BackupRestoreManager.parseSchedulesFromJson epoch-millis timestamp handling
 *  2. DataExporter PDF adherence formula with stale PENDING entries
 *  3. DataExporter JSON export null field serialization
 */
class AuditBugfixes11Test {

    // ───────────────────────────────────────────────────────────────
    // Test Utilities
    // ───────────────────────────────────────────────────────────────

    /**
     * Mirrors BackupRestoreManager.parseSchedulesFromJson's scheduledDateTime parsing chain
     * (post-fix) so we can unit-test the parsing logic without needing the full Android context.
     */
    private fun parseScheduledDateTime(raw: String): LocalDateTime {
        // Guard: Handle epoch-millis formatted timestamps before ISO parsing
        return raw.toLongOrNull()?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
        } ?: try {
            LocalDateTime.parse(raw)
        } catch (_: Exception) {
            try {
                java.time.OffsetDateTime.parse(raw).toLocalDateTime()
            } catch (_: Exception) {
                try {
                    Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDateTime()
                } catch (_: Exception) {
                    LocalDateTime.now()
                }
            }
        }
    }

    /**
     * Mirrors the fixed takenAt parsing logic from BackupRestoreManager.parseSchedulesFromJson.
     */
    private fun parseTakenAt(raw: String?): LocalDateTime? {
        if (raw.isNullOrBlank()) return null
        // Guard: Handle epoch-millis formatted takenAt
        return raw.toLongOrNull()?.let { millis ->
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        } ?: runCatching { LocalDateTime.parse(raw) }.getOrNull()
    }

    /**
     * Mirrors AnalyticsViewModel.isEntryMissed and the fixed DataExporter.buildPdfDocument adherence logic.
     */
    private fun isEntryMissed(entry: ScheduleEntry, now: LocalDateTime, missedAfterHours: Int): Boolean =
        entry.status == MedicationStatus.MISSED ||
        (entry.status == MedicationStatus.PENDING && now.isAfter(entry.scheduledDateTime) &&
            Duration.between(entry.scheduledDateTime, now).toHours() >= missedAfterHours.toLong())

    private fun sampleUser(
        missedAfterHours: Int = 6
    ) = User(
        userId = "usr_test",
        fullName = "Test Patient",
        age = 30,
        gender = Gender.MALE,
        contactNumber = "+1234567890",
        considerMissedAfter = missedAfterHours
    )

    private fun sampleSchedule(
        entryId: String = "entry_1",
        scheduledDateTime: LocalDateTime = LocalDateTime.now().minusHours(1),
        status: MedicationStatus = MedicationStatus.PENDING,
        takenAt: LocalDateTime? = null
    ) = ScheduleEntry(
        entryId = entryId,
        userId = "usr_test",
        medicineId = "med_1",
        scheduledDateTime = scheduledDateTime,
        status = status,
        takenAt = takenAt
    )

    // ───────────────────────────────────────────────────────────────
    // 1. Epoch-Millis Timestamp Parsing (Issue 1)
    // ───────────────────────────────────────────────────────────────

    @Test
    fun parseScheduledDateTime_epochMillis_parsesCorrectly() {
        // 2026-10-01T10:00:00 UTC as epoch millis
        val epoch = LocalDateTime.of(2026, 10, 1, 10, 0, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val parsed = parseScheduledDateTime(epoch.toString())

        assertEquals(2026, parsed.year)
        assertEquals(10, parsed.monthValue)
        assertEquals(1, parsed.dayOfMonth)
        assertEquals(10, parsed.hour)
        assertEquals(0, parsed.minute)
    }

    @Test
    fun parseScheduledDateTime_isoString_stillParsesCorrectly() {
        val isoStr = "2026-10-01T10:00:00"
        val parsed = parseScheduledDateTime(isoStr)

        assertEquals(2026, parsed.year)
        assertEquals(10, parsed.monthValue)
        assertEquals(1, parsed.dayOfMonth)
        assertEquals(10, parsed.hour)
    }

    @Test
    fun parseScheduledDateTime_offsetDateTime_parsesCorrectly() {
        val offsetStr = "2026-10-01T10:00:00+05:30"
        val parsed = parseScheduledDateTime(offsetStr)

        assertEquals(2026, parsed.year)
        assertEquals(10, parsed.monthValue)
        assertEquals(1, parsed.dayOfMonth)
        assertEquals(10, parsed.hour)
    }

    @Test
    fun parseScheduledDateTime_instantFormat_parsesCorrectly() {
        val instantStr = "2026-10-01T10:00:00Z"
        val parsed = parseScheduledDateTime(instantStr)

        // The Instant is converted to local zone, so exact hour may vary
        assertEquals(2026, parsed.year)
        assertEquals(10, parsed.monthValue)
    }

    @Test
    fun parseScheduledDateTime_epochMillis_notConfusedWithNow() {
        // Epoch for a date far in the past: 2020-06-15T08:30:00 system zone
        val pastDate = LocalDateTime.of(2020, 6, 15, 8, 30, 0)
        val pastEpoch = pastDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val parsed = parseScheduledDateTime(pastEpoch.toString())

        // Critical: Must be 2020, NOT now() (which the old code would produce)
        assertEquals(2020, parsed.year)
        assertEquals(6, parsed.monthValue)
        assertEquals(15, parsed.dayOfMonth)
        assertEquals(8, parsed.hour)
        assertEquals(30, parsed.minute)
    }

    @Test
    fun parseTakenAt_epochMillis_parsesCorrectly() {
        val takenDate = LocalDateTime.of(2026, 10, 1, 10, 5, 0)
        val epoch = takenDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val parsed = parseTakenAt(epoch.toString())

        assertNotNull(parsed)
        assertEquals(2026, parsed!!.year)
        assertEquals(10, parsed.monthValue)
        assertEquals(1, parsed.dayOfMonth)
        assertEquals(10, parsed.hour)
        assertEquals(5, parsed.minute)
    }

    @Test
    fun parseTakenAt_isoString_stillParsesCorrectly() {
        val parsed = parseTakenAt("2026-10-01T10:05:00")

        assertNotNull(parsed)
        assertEquals(10, parsed!!.hour)
        assertEquals(5, parsed.minute)
    }

    @Test
    fun parseTakenAt_null_returnsNull() {
        assertNull(parseTakenAt(null))
        assertNull(parseTakenAt(""))
        assertNull(parseTakenAt("  "))
    }

    @Test
    fun parseTakenAt_garbage_returnsNull() {
        assertNull(parseTakenAt("not-a-date"))
    }

    // ───────────────────────────────────────────────────────────────
    // 2. PDF Adherence Formula with Stale PENDING (Issue 2)
    // ───────────────────────────────────────────────────────────────

    @Test
    fun adherence_stalePendingCountedAsMissed() {
        val now = LocalDateTime.now()
        val user = sampleUser(missedAfterHours = 6)

        // 10 doses taken on time, 5 stale PENDING (scheduled 8 hours ago = past 6h threshold)
        val takenEntries = (1..10).map { i ->
            sampleSchedule(
                entryId = "taken_$i",
                scheduledDateTime = now.minusHours(2),
                status = MedicationStatus.TAKEN_ON_TIME,
                takenAt = now.minusHours(2)
            )
        }
        val stalePendingEntries = (1..5).map { i ->
            sampleSchedule(
                entryId = "stale_$i",
                scheduledDateTime = now.minusHours(8), // 8h ago > 6h threshold = should be MISSED
                status = MedicationStatus.PENDING
            )
        }
        val allEntries = takenEntries + stalePendingEntries

        // Compute adherence using the fixed formula
        val taken = allEntries.count { it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE }
        val missed = allEntries.count { isEntryMissed(it, now, user.considerMissedAfter) }
        val decided = taken + missed
        val rate = if (decided > 0) (taken.toDouble() / decided * 100.0) else 0.0

        // Fixed: 10 taken / (10 taken + 5 missed) = 66.7%
        assertEquals(10, taken)
        assertEquals(5, missed)
        assertEquals(15, decided)
        assertTrue("Adherence should be ~66.7%, was $rate", rate > 66.0 && rate < 67.0)
    }

    @Test
    fun adherence_oldFormula_wouldInflateRate() {
        val now = LocalDateTime.now()
        val user = sampleUser(missedAfterHours = 6)

        val takenEntries = (1..10).map { i ->
            sampleSchedule(
                entryId = "taken_$i",
                scheduledDateTime = now.minusHours(2),
                status = MedicationStatus.TAKEN_ON_TIME,
                takenAt = now.minusHours(2)
            )
        }
        val stalePendingEntries = (1..5).map { i ->
            sampleSchedule(
                entryId = "stale_$i",
                scheduledDateTime = now.minusHours(8),
                status = MedicationStatus.PENDING
            )
        }
        val allEntries = takenEntries + stalePendingEntries

        // Old (broken) formula: only counts explicit MISSED status
        val takenOld = allEntries.count { it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE }
        val missedOld = allEntries.count { it.status == MedicationStatus.MISSED } // 0 - none explicitly marked
        val decidedOld = takenOld + missedOld
        val rateOld = if (decidedOld > 0) (takenOld.toDouble() / decidedOld * 100.0) else 0.0

        // Old formula: 10/10 = 100% (inflated!)
        assertEquals(0, missedOld)
        assertEquals(100.0, rateOld, 0.01)

        // New (fixed) formula
        val missedNew = allEntries.count { isEntryMissed(it, now, user.considerMissedAfter) }
        val decidedNew = takenOld + missedNew
        val rateNew = if (decidedNew > 0) (takenOld.toDouble() / decidedNew * 100.0) else 0.0

        // New formula: 10/15 = 66.7% (accurate)
        assertEquals(5, missedNew)
        assertTrue("New rate ($rateNew) should be significantly lower than old rate ($rateOld)", rateNew < rateOld)
    }

    @Test
    fun adherence_recentPendingNotCountedAsMissed() {
        val now = LocalDateTime.now()
        val user = sampleUser(missedAfterHours = 6)

        // PENDING entry only 2 hours ago (< 6h threshold) should NOT count as missed
        val recentPending = sampleSchedule(
            entryId = "recent_pending",
            scheduledDateTime = now.minusHours(2),
            status = MedicationStatus.PENDING
        )

        assertFalse(isEntryMissed(recentPending, now, user.considerMissedAfter))
    }

    @Test
    fun adherence_futurePendingNotCountedAsMissed() {
        val now = LocalDateTime.now()
        val user = sampleUser(missedAfterHours = 6)

        // Future PENDING entry should never be missed
        val futurePending = sampleSchedule(
            entryId = "future_pending",
            scheduledDateTime = now.plusHours(3),
            status = MedicationStatus.PENDING
        )

        assertFalse(isEntryMissed(futurePending, now, user.considerMissedAfter))
    }

    @Test
    fun adherence_explicitlyMissedAlwaysCountsAsMissed() {
        val now = LocalDateTime.now()
        val user = sampleUser(missedAfterHours = 6)

        val explicitMissed = sampleSchedule(
            entryId = "explicit_missed",
            scheduledDateTime = now.minusHours(1),
            status = MedicationStatus.MISSED
        )

        assertTrue(isEntryMissed(explicitMissed, now, user.considerMissedAfter))
    }

    @Test
    fun adherence_prnEntriesExcludedFromCalculation() {
        val now = LocalDateTime.now()

        val regularEntry = sampleSchedule(
            entryId = "regular_1",
            scheduledDateTime = now.minusHours(2),
            status = MedicationStatus.TAKEN_ON_TIME,
            takenAt = now.minusHours(2)
        )
        val prnEntry = sampleSchedule(
            entryId = "PRN_med_1_123456",
            scheduledDateTime = now.minusHours(1),
            status = MedicationStatus.TAKEN_ON_TIME,
            takenAt = now.minusHours(1)
        )

        // Guard: PRN entries should be filtered out before adherence calculation
        val adherenceEntries = listOf(regularEntry, prnEntry).filter { !it.entryId.startsWith("PRN_") }
        assertEquals(1, adherenceEntries.size)
        assertEquals("regular_1", adherenceEntries[0].entryId)
    }

    @Test
    fun adherence_boundaryExactlyAtThreshold() {
        val now = LocalDateTime.now()
        val user = sampleUser(missedAfterHours = 6)

        // PENDING entry exactly 6 hours ago (== threshold) should count as missed
        val exactThreshold = sampleSchedule(
            entryId = "exact_threshold",
            scheduledDateTime = now.minusHours(6),
            status = MedicationStatus.PENDING
        )

        assertTrue(
            "Entry exactly at missedAfterHours threshold should be counted as missed",
            isEntryMissed(exactThreshold, now, user.considerMissedAfter)
        )
    }

    // ───────────────────────────────────────────────────────────────
    // 3. JSON Export Null Field Serialization (Issue 3)
    // ───────────────────────────────────────────────────────────────

    @Test
    fun jsonExport_nullTakenAt_omittedFromJson() {
        val entry = sampleSchedule(
            status = MedicationStatus.PENDING,
            takenAt = null
        )

        val sObj = JSONObject()
        sObj.put("entryId", entry.entryId)
        sObj.put("status", entry.status.name)
        // Fixed serialization: omit null fields
        entry.takenAt?.let { sObj.put("takenAt", it.toString()) }

        assertFalse("Null takenAt should be omitted from JSON, not written as empty string", sObj.has("takenAt"))
    }

    @Test
    fun jsonExport_nonNullTakenAt_includedInJson() {
        val takenTime = LocalDateTime.of(2026, 10, 1, 10, 5, 0)
        val entry = sampleSchedule(
            status = MedicationStatus.TAKEN_ON_TIME,
            takenAt = takenTime
        )

        val sObj = JSONObject()
        sObj.put("entryId", entry.entryId)
        sObj.put("status", entry.status.name)
        entry.takenAt?.let { sObj.put("takenAt", it.toString()) }

        assertTrue("Non-null takenAt should be present in JSON", sObj.has("takenAt"))
        assertEquals("2026-10-01T10:05", sObj.getString("takenAt"))
    }

    @Test
    fun jsonExport_nullSkipReason_omittedFromJson() {
        val entry = sampleSchedule(status = MedicationStatus.TAKEN_ON_TIME)

        val sObj = JSONObject()
        entry.skipReason?.let { sObj.put("skipReason", it) }

        assertFalse("Null skipReason should be omitted", sObj.has("skipReason"))
    }

    @Test
    fun jsonExport_nonNullSkipReason_includedInJson() {
        val entry = sampleSchedule(status = MedicationStatus.SKIPPED).copy(skipReason = "Felt nauseous")

        val sObj = JSONObject()
        entry.skipReason?.let { sObj.put("skipReason", it) }

        assertTrue(sObj.has("skipReason"))
        assertEquals("Felt nauseous", sObj.getString("skipReason"))
    }

    @Test
    fun jsonExport_nullDoseNotes_omittedFromJson() {
        val entry = sampleSchedule(status = MedicationStatus.TAKEN_ON_TIME)

        val sObj = JSONObject()
        entry.doseNotes?.let { sObj.put("doseNotes", it) }

        assertFalse("Null doseNotes should be omitted", sObj.has("doseNotes"))
    }

    @Test
    fun jsonExport_nonNullDoseNotes_includedInJson() {
        val entry = sampleSchedule(status = MedicationStatus.TAKEN_ON_TIME).copy(doseNotes = "Took with food")

        val sObj = JSONObject()
        entry.doseNotes?.let { sObj.put("doseNotes", it) }

        assertTrue(sObj.has("doseNotes"))
        assertEquals("Took with food", sObj.getString("doseNotes"))
    }

    @Test
    fun jsonExport_roundTrip_nullFieldsParseBackAsNull() {
        // Simulate export with null takenAt, skipReason, doseNotes
        val entry = sampleSchedule(status = MedicationStatus.PENDING)

        val sObj = JSONObject()
        sObj.put("entryId", entry.entryId)
        sObj.put("userId", entry.userId)
        sObj.put("medicineId", entry.medicineId)
        sObj.put("scheduledDateTime", entry.scheduledDateTime.toString())
        sObj.put("status", entry.status.name)
        entry.skipReason?.let { sObj.put("skipReason", it) }
        entry.takenAt?.let { sObj.put("takenAt", it.toString()) }
        entry.doseNotes?.let { sObj.put("doseNotes", it) }

        val jsonStr = sObj.toString()
        val parsed = JSONObject(jsonStr)

        // These keys should be absent for null values
        assertFalse("skipReason should be absent for null", parsed.has("skipReason"))
        assertFalse("takenAt should be absent for null", parsed.has("takenAt"))
        assertFalse("doseNotes should be absent for null", parsed.has("doseNotes"))

        // Simulating the parser's safe extraction (mirrors parseSchedulesFromJson's pattern)
        val parsedSkipReason = if (parsed.has("skipReason")) parsed.getString("skipReason") else null
        val parsedTakenAt = if (parsed.has("takenAt")) parsed.getString("takenAt") else null
        val parsedDoseNotes = if (parsed.has("doseNotes")) parsed.getString("doseNotes") else null

        assertNull("skipReason should round-trip as null", parsedSkipReason)
        assertNull("takenAt should round-trip as null", parsedTakenAt)
        assertNull("doseNotes should round-trip as null", parsedDoseNotes)
    }

    @Test
    fun jsonExport_oldFormat_emptyStringsDontBreakParsing() {
        // Backward compatibility: old exports that wrote "" for null fields
        val jsonStr = """
            {
                "entryId": "entry_old",
                "userId": "usr_test",
                "medicineId": "med_1",
                "scheduledDateTime": "2026-10-01T10:00",
                "status": "PENDING",
                "skipReason": "",
                "takenAt": "",
                "doseNotes": ""
            }
        """.trimIndent()

        val parsed = JSONObject(jsonStr)
        val skipReason = parsed.optString("skipReason", null)?.ifEmpty { null }
        val takenAtStr = parsed.optString("takenAt", null)?.ifEmpty { null }
        val doseNotes = parsed.optString("doseNotes", null)?.ifEmpty { null }

        // Empty strings from old exports should gracefully resolve to null
        assertNull("Empty skipReason should be treated as null", skipReason)
        assertNull("Empty takenAt should be treated as null", takenAtStr)
        assertNull("Empty doseNotes should be treated as null", doseNotes)
    }
}
