package com.example.dosezy

import com.example.dosezy.data.converters.Converters
import com.example.dosezy.data.model.DosageUnit
import com.example.dosezy.data.model.Frequency
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.Gender
import com.example.dosezy.data.model.Language
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.PillShape
import com.example.dosezy.data.model.Theme
import com.example.dosezy.data.model.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Round-trip serialization tests for all Room [Converters].
 * Ensures data survives SQLite read/write without corruption.
 */
class ConvertersTest {

    private val converters = Converters()

    // ───────────────────────────────────────────────────────────────
    // 1. LocalDateTime ⟷ Long? (epoch millis UTC)
    // ───────────────────────────────────────────────────────────────

    @Test
    fun localDateTime_roundTrips() {
        val dt = LocalDateTime.of(2026, 9, 21, 14, 30, 0)
        val millis = converters.fromLocalDateTime(dt)
        assertNotNull(millis)
        val restored = converters.toLocalDateTime(millis)
        assertEquals(dt, restored)
    }

    @Test
    fun localDateTime_nullRoundTrips() {
        assertNull(converters.fromLocalDateTime(null))
        assertNull(converters.toLocalDateTime(null))
    }

    @Test
    fun localDateTime_epochZero() {
        // Epoch 0 should convert to 1970-01-01T00:00:00 UTC
        val restored = converters.toLocalDateTime(0L)
        assertNotNull(restored)
        assertEquals(LocalDateTime.of(1970, 1, 1, 0, 0, 0), restored)
    }

    @Test
    fun localDateTime_preservesMinuteAndSecondPrecision() {
        val dt = LocalDateTime.of(2026, 3, 15, 23, 59, 59)
        val restored = converters.toLocalDateTime(converters.fromLocalDateTime(dt))
        assertEquals(23, restored?.hour)
        assertEquals(59, restored?.minute)
        assertEquals(59, restored?.second)
    }

    // ───────────────────────────────────────────────────────────────
    // 2. LocalDate ⟷ Long? (epoch millis UTC at start of day)
    // ───────────────────────────────────────────────────────────────

    @Test
    fun localDate_roundTrips() {
        val date = LocalDate.of(2026, 2, 28)
        val millis = converters.fromLocalDate(date)
        assertNotNull(millis)
        val restored = converters.toLocalDate(millis)
        assertEquals(date, restored)
    }

    @Test
    fun localDate_nullRoundTrips() {
        assertNull(converters.fromLocalDate(null))
        assertNull(converters.toLocalDate(null))
    }

    @Test
    fun localDate_leapDay() {
        val leapDay = LocalDate.of(2028, 2, 29)
        val restored = converters.toLocalDate(converters.fromLocalDate(leapDay))
        assertEquals(leapDay, restored)
    }

    // ───────────────────────────────────────────────────────────────
    // 3. LocalTime ⟷ String? (ISO format)
    // ───────────────────────────────────────────────────────────────

    @Test
    fun localTime_roundTrips() {
        val time = LocalTime.of(14, 30, 0)
        val str = converters.fromLocalTime(time)
        assertNotNull(str)
        val restored = converters.toLocalTime(str)
        assertEquals(time, restored)
    }

    @Test
    fun localTime_midnight() {
        val midnight = LocalTime.of(0, 0, 0)
        val restored = converters.toLocalTime(converters.fromLocalTime(midnight))
        assertEquals(midnight, restored)
    }

    @Test
    fun localTime_endOfDay() {
        val eod = LocalTime.of(23, 59, 59)
        val restored = converters.toLocalTime(converters.fromLocalTime(eod))
        assertEquals(eod, restored)
    }

    @Test
    fun localTime_nullRoundTrips() {
        assertNull(converters.fromLocalTime(null))
        assertNull(converters.toLocalTime(null))
    }

    // ───────────────────────────────────────────────────────────────
    // 4. List<LocalTime> ⟷ String? (JSON array)
    // ───────────────────────────────────────────────────────────────

    @Test
    fun localTimeList_roundTrips() {
        val times = listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(21, 30))
        val json = converters.fromLocalTimeList(times)
        assertNotNull(json)
        val restored = converters.toLocalTimeList(json)
        assertEquals(times, restored)
    }

    @Test
    fun localTimeList_emptyList() {
        val empty = emptyList<LocalTime>()
        val json = converters.fromLocalTimeList(empty)
        val restored = converters.toLocalTimeList(json)
        assertNotNull(restored)
        assertEquals(0, restored!!.size)
    }

    @Test
    fun localTimeList_singleElement() {
        val single = listOf(LocalTime.of(6, 45))
        val restored = converters.toLocalTimeList(converters.fromLocalTimeList(single))
        assertEquals(single, restored)
    }

    @Test
    fun localTimeList_nullRoundTrips() {
        assertNull(converters.fromLocalTimeList(null))
        assertNull(converters.toLocalTimeList(null))
    }

    // ───────────────────────────────────────────────────────────────
    // 5. Frequency ⟷ String? (JSON object)
    // ───────────────────────────────────────────────────────────────

    @Test
    fun frequency_dailyRoundTrips() {
        val freq = Frequency(FrequencyPattern.DAILY)
        val json = converters.fromFrequency(freq)
        val restored = converters.toFrequency(json)
        assertNotNull(restored)
        assertEquals(FrequencyPattern.DAILY, restored!!.pattern)
    }

    @Test
    fun frequency_weeklyWithSelectedDaysRoundTrips() {
        val freq = Frequency(
            FrequencyPattern.WEEKLY,
            selectedDaysOfWeek = listOf(1, 3, 5),
            daysPerWeek = 3
        )
        val json = converters.fromFrequency(freq)
        val restored = converters.toFrequency(json)
        assertNotNull(restored)
        assertEquals(FrequencyPattern.WEEKLY, restored!!.pattern)
        assertEquals(listOf(1, 3, 5), restored.selectedDaysOfWeek)
        assertEquals(3, restored.daysPerWeek)
    }

    @Test
    fun frequency_monthlyWithSelectedDaysRoundTrips() {
        val freq = Frequency(
            FrequencyPattern.MONTHLY,
            selectedDaysOfMonth = listOf(1, 15, 28),
            daysPerMonth = 3
        )
        val json = converters.fromFrequency(freq)
        val restored = converters.toFrequency(json)
        assertNotNull(restored)
        assertEquals(FrequencyPattern.MONTHLY, restored!!.pattern)
        assertEquals(listOf(1, 15, 28), restored.selectedDaysOfMonth)
    }

    @Test
    fun frequency_everyXDaysRoundTrips() {
        val freq = Frequency(FrequencyPattern.EVERY_X_DAYS, intervalDays = 5)
        val json = converters.fromFrequency(freq)
        val restored = converters.toFrequency(json)
        assertNotNull(restored)
        assertEquals(FrequencyPattern.EVERY_X_DAYS, restored!!.pattern)
        assertEquals(5, restored.intervalDays)
    }

    @Test
    fun frequency_everyXHoursRoundTrips() {
        val freq = Frequency(FrequencyPattern.EVERY_X_HOURS, intervalHours = 8)
        val json = converters.fromFrequency(freq)
        val restored = converters.toFrequency(json)
        assertNotNull(restored)
        assertEquals(8, restored!!.intervalHours)
    }

    @Test
    fun frequency_nullRoundTrips() {
        assertNull(converters.fromFrequency(null))
        assertNull(converters.toFrequency(null))
    }

    @Test
    fun frequency_invalidJsonDefaultsToDaily() {
        val restored = converters.toFrequency("not a json")
        assertNotNull(restored)
        assertEquals(FrequencyPattern.DAILY, restored!!.pattern)
    }

    // ───────────────────────────────────────────────────────────────
    // 6. Map<String, Double>? ⟷ String? (custom dosages)
    // ───────────────────────────────────────────────────────────────

    @Test
    fun customDosages_roundTrips() {
        val map = mapOf("08:00" to 25.5, "14:00" to 12.0, "21:30" to 50.0)
        val json = converters.fromCustomDosagesMap(map)
        assertNotNull(json)
        val restored = converters.toCustomDosagesMap(json)
        assertNotNull(restored)
        assertEquals(3, restored!!.size)
        assertEquals(25.5, restored["08:00"]!!, 0.001)
        assertEquals(12.0, restored["14:00"]!!, 0.001)
        assertEquals(50.0, restored["21:30"]!!, 0.001)
    }

    @Test
    fun customDosages_emptyMap() {
        val map = emptyMap<String, Double>()
        val json = converters.fromCustomDosagesMap(map)
        val restored = converters.toCustomDosagesMap(json)
        // Empty JSON object "{}" should parse to empty map
        assertNotNull(restored)
        assertEquals(0, restored!!.size)
    }

    @Test
    fun customDosages_nullAndEmptyStringHandling() {
        assertNull(converters.fromCustomDosagesMap(null))
        assertNull(converters.toCustomDosagesMap(null))
        assertNull(converters.toCustomDosagesMap(""))
    }

    @Test
    fun customDosages_invalidJsonReturnsNull() {
        assertNull(converters.toCustomDosagesMap("invalid json"))
    }

    // ───────────────────────────────────────────────────────────────
    // 7. Enum converters (all primitive enums)
    // ───────────────────────────────────────────────────────────────

    @Test
    fun gender_roundTrips() {
        Gender.values().forEach { gender ->
            assertEquals(gender, converters.toGender(converters.fromGender(gender)))
        }
    }

    @Test
    fun dosageUnit_roundTrips() {
        DosageUnit.values().forEach { unit ->
            assertEquals(unit, converters.toDosageUnit(converters.fromDosageUnit(unit)))
        }
    }

    @Test
    fun medicationStatus_roundTrips() {
        MedicationStatus.values().forEach { status ->
            assertEquals(status, converters.toMedicationStatus(converters.fromMedicationStatus(status)))
        }
    }

    @Test
    fun theme_roundTrips() {
        Theme.values().forEach { theme ->
            assertEquals(theme, converters.toTheme(converters.fromTheme(theme)))
        }
    }

    @Test
    fun timeFormat_roundTrips() {
        TimeFormat.values().forEach { tf ->
            assertEquals(tf, converters.toTimeFormat(converters.fromTimeFormat(tf)))
        }
    }

    @Test
    fun language_roundTrips() {
        Language.values().forEach { lang ->
            assertEquals(lang, converters.toLanguage(converters.fromLanguage(lang)))
        }
    }

    @Test
    fun language_invalidStringDefaultsToSystem() {
        assertEquals(Language.SYSTEM, converters.toLanguage("UNKNOWN_LANG"))
    }

    @Test
    fun pillShape_roundTrips() {
        PillShape.values().forEach { shape ->
            assertEquals(shape, converters.toPillShape(converters.fromPillShape(shape)))
        }
    }

    @Test
    fun pillShape_nullDefaultsToRound() {
        assertEquals(PillShape.ROUND, converters.toPillShape(null))
    }

    @Test
    fun pillShape_invalidStringDefaultsToRound() {
        assertEquals(PillShape.ROUND, converters.toPillShape("NONEXISTENT"))
    }
}
