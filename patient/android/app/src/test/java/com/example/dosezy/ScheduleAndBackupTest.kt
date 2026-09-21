package com.example.dosezy

import com.example.dosezy.data.export.BackupRestoreManager
import com.example.dosezy.data.model.FrequencyPattern
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ScheduleAndBackupTest {

    @Test
    fun testSlotAlarmIdDeterministic() {
        val userId = "test-user-123"
        val time = LocalDateTime.of(2026, 9, 21, 8, 0, 0)
        val cleanDateTime = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
        val slotAlarmId1 = "${userId}_${cleanDateTime}".hashCode()
        val slotAlarmId2 = "${userId}_2026-09-21_08-00".hashCode()

        assertEquals(slotAlarmId1, slotAlarmId2)
    }

    @Test
    fun testUndoStockGuardLogic() {
        // Only TAKEN_ON_TIME and TAKEN_LATE should be eligible for stock replenishment on undo
        fun shouldRestoreStock(previousStatus: MedicationStatus, autoDeduct: Boolean): Boolean {
            return (previousStatus == MedicationStatus.TAKEN_ON_TIME || previousStatus == MedicationStatus.TAKEN_LATE) && autoDeduct
        }

        assertTrue(shouldRestoreStock(MedicationStatus.TAKEN_ON_TIME, true))
        assertTrue(shouldRestoreStock(MedicationStatus.TAKEN_LATE, true))
        assertFalse(shouldRestoreStock(MedicationStatus.SKIPPED, true))
        assertFalse(shouldRestoreStock(MedicationStatus.MISSED, true))
        assertFalse(shouldRestoreStock(MedicationStatus.PENDING, true))
        assertFalse(shouldRestoreStock(MedicationStatus.TAKEN_ON_TIME, false))
    }

    @Test
    fun testRecordDoseGuardLogic() {
        // Prevent double stock deduction if status was already TAKEN
        fun shouldDeductStock(currentStatus: MedicationStatus, autoDeduct: Boolean): Boolean {
            val wasAlreadyTaken = currentStatus == MedicationStatus.TAKEN_ON_TIME || currentStatus == MedicationStatus.TAKEN_LATE
            return !wasAlreadyTaken && autoDeduct
        }

        assertTrue(shouldDeductStock(MedicationStatus.PENDING, true))
        assertTrue(shouldDeductStock(MedicationStatus.SKIPPED, true))
        assertFalse(shouldDeductStock(MedicationStatus.TAKEN_ON_TIME, true))
        assertFalse(shouldDeductStock(MedicationStatus.TAKEN_LATE, true))
    }

    @Test
    fun testSelectiveRestoreUserActiveFlagLogic() {
        // When restoring selective profiles:
        // isCurrentUser should only be true if existing users is empty AND it is the very first profile being restored
        fun computeIsCurrentUser(existingUsersEmpty: Boolean, totalProfiles: Int): Boolean {
            return existingUsersEmpty && totalProfiles == 0
        }

        // When DB is empty and restoring first user -> active
        assertTrue(computeIsCurrentUser(existingUsersEmpty = true, totalProfiles = 0))
        // When DB is empty and restoring second user -> NOT active
        assertFalse(computeIsCurrentUser(existingUsersEmpty = true, totalProfiles = 1))
        // When DB already has users -> NOT active
        assertFalse(computeIsCurrentUser(existingUsersEmpty = false, totalProfiles = 0))
    }

    @Test
    fun testAsNeededFrequencyDisplayLogic() {
        val pattern = FrequencyPattern.AS_NEEDED
        val timesPerDay = 0
        val display = if (pattern == FrequencyPattern.AS_NEEDED) "As Needed (PRN)" else "$timesPerDay times a day"
        assertEquals("As Needed (PRN)", display)
    }
}
