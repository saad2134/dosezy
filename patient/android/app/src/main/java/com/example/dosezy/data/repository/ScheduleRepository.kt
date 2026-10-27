// ScheduleRepository.kt
package com.example.dosezy.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.ScheduleWithMedicine
import com.example.dosezy.notifications.AlarmScheduler
import com.example.dosezy.notifications.MedicineAlarmReceiver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime

class ScheduleRepository(private val database: DosezyDatabase) {

    companion object {
        private const val TAG = "ScheduleRepository"
        val ENTRY_ID_DATETIME_REGEX = Regex("""_(\d{4})_(\d{2})_(\d{2})_(\d{2})_(\d{2})(?:_(\d{2}))?$""")

        @RequiresApi(Build.VERSION_CODES.O)
        fun parseDateTimeFromEntryId(entryId: String): LocalDateTime? {
            if (entryId.startsWith("PRN_")) return null
            val match = ENTRY_ID_DATETIME_REGEX.find(entryId) ?: return null
            return try {
                val year = match.groupValues[1].toInt()
                val month = match.groupValues[2].toInt()
                val day = match.groupValues[3].toInt()
                val hour = match.groupValues[4].toInt()
                val minute = match.groupValues[5].toInt()
                val second = if (match.groupValues.size > 6 && match.groupValues[6].isNotBlank()) {
                    match.groupValues[6].toInt()
                } else 0
                LocalDateTime.of(year, month, day, hour, minute, second)
            } catch (_: Exception) {
                null
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun findClosestScheduledTime(entryDateTime: LocalDateTime, scheduledTimes: List<java.time.LocalTime>): LocalDateTime? {
            if (scheduledTimes.isEmpty()) return null
            val entryTime = entryDateTime.toLocalTime()
            if (entryTime in scheduledTimes) return entryDateTime

            var bestCandidate: LocalDateTime? = null
            var minDiffMinutes = Long.MAX_VALUE

            for (slot in scheduledTimes) {
                for (dayOffset in listOf(0L, 1L, -1L)) {
                    val candidate = LocalDateTime.of(entryDateTime.toLocalDate().plusDays(dayOffset), slot)
                    val diff = kotlin.math.abs(java.time.Duration.between(entryDateTime, candidate).toMinutes())
                    if (diff < minDiffMinutes) {
                        minDiffMinutes = diff
                        bestCandidate = candidate
                    }
                }
            }
            return if (minDiffMinutes <= 14 * 60) bestCandidate else null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getScheduleForDate(userId: String, date: LocalDate): Flow<List<ScheduleEntry>> {
        val startOfDay = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val endOfDay = date.atTime(java.time.LocalTime.MAX).atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        return database.scheduleDao().getScheduleForDateRange(userId, startOfDay, endOfDay)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getScheduleWithMedicineForDate(userId: String, date: LocalDate): Flow<List<ScheduleWithMedicine>> {
        val startOfDay = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val endOfDay = date.atTime(java.time.LocalTime.MAX).atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        return database.scheduleDao().getScheduleWithMedicineForDateRange(userId, startOfDay, endOfDay)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun autoExtendSchedules(userId: String) {
        try {
            reconcileLegacyScheduleEntries(userId)
            com.example.dosezy.domain.usecases.GenerateSchedulesUseCase(database).autoExtendSchedules(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error auto-extending schedules for user: $userId", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun scheduleAlarmsForMedicine(medicineId: String, context: Context) {
        val medicine = database.medicineDao().getMedicineByIdDirect(medicineId)
        if (medicine != null) {
            rescheduleAllAlarms(medicine.userId, context)
            Log.d(TAG, "Rescheduled slot alarms for medicine: ${medicine.medicationName}")
        } else {
            Log.w(TAG, "Medicine not found for ID: $medicineId - cannot schedule alarms")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun cancelAlarmsForMedicine(medicineId: String, context: Context) {
        val alarmScheduler = AlarmScheduler(context)
        val scheduleEntries = database.scheduleDao().getScheduleEntriesByMedicine(medicineId)
        val now = LocalDateTime.now()
        val activeWindowStart = now.minusHours(24)
        val activeWindowEnd = now.plusDays(8)
        // Guard: Only cancel alarms within the active window; looping over months of past history triggers thousands of redundant Binder IPC calls
        val entriesToCancel = scheduleEntries.filter { entry ->
            entry.scheduledDateTime.isAfter(activeWindowStart) &&
            entry.scheduledDateTime.isBefore(activeWindowEnd)
        }
        val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        entriesToCancel.forEach { entry ->
            // Guard: Pass entry directly to cancelAlarm to bypass sequential blocking DB lookups
            alarmScheduler.cancelAlarm(entry)
            alarmScheduler.cancelSnooze(entry.entryId)
            alarmScheduler.cancelNagging(entry.entryId)
            alarmScheduler.cancelSlotAlarm(entry.userId, entry.scheduledDateTime)
            // Guard: Cancel any active intake safeguard alarms for this entry to prevent orphaned re-alerts
            alarmScheduler.cancelIntakeSafeguardAlarm(entry.entryId)
            // Guard: Dismiss active reminder notifications from shade when cancelling medicine alarms
            nManager?.cancel(entry.entryId.hashCode())
        }
        // Guard: Dismiss any active refill alert notification for deleted/archived medicine
        nManager?.cancel((medicineId + "_refill").hashCode())
        // Guard: Reconcile active ADHD intake countdown notification if active countdown references deleted medicine (#BUG-SAFEGUARD-PARTIAL-COHORT)
        try {
            val activeCountdown = com.example.dosezy.notifications.IntakeSafeguardManager.activeCountdown.value
            if (activeCountdown != null && (scheduleEntries.any { it.entryId == activeCountdown.entryId } || activeCountdown.entryIds.any { id -> scheduleEntries.any { it.entryId == id } })) {
                val matchingIds = scheduleEntries.map { it.entryId }.filter { activeCountdown.entryIds.contains(it) || it == activeCountdown.entryId }
                com.example.dosezy.notifications.IntakeSafeguardManager.reconcileCountdown(context, matchingIds)
            }
        } catch (_: Exception) {}
        Log.d(TAG, "Cancelled active alarms for medicine ID: $medicineId (${entriesToCancel.size} active entries)")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun rescheduleAllAlarms(userId: String, context: Context) {
        val alarmScheduler = AlarmScheduler(context)

        try {
            // First check and extend schedules
            autoExtendSchedules(userId)

            // Get all schedule entries for the user
            val allEntries = database.scheduleDao().getAllScheduleEntries(userId)

            val now = LocalDateTime.now()
            val limitTime = now.plusDays(7)
            val activeWindowStart = now.minusHours(24)
            val activeWindowEnd = limitTime.plusDays(1)

            // Guard: Only cancel slot alarms within active window; preserve active snoozes and nagging reminders so rebuilding alarms does not wipe out active snooze timers
            val entriesToCancel = allEntries.filter { entry ->
                entry.scheduledDateTime.isAfter(activeWindowStart) &&
                entry.scheduledDateTime.isBefore(activeWindowEnd)
            }
            entriesToCancel.forEach { entry ->
                // Guard: Pass entry directly to cancelAlarm to bypass sequential blocking DB lookups
                alarmScheduler.cancelAlarm(entry)
                alarmScheduler.cancelSlotAlarm(entry.userId, entry.scheduledDateTime)
            }

            // Schedule grouped alarms for strictly future pending entries within the 7-day window
            var scheduledCount = 0
            val pendingEntries = allEntries.filter { 
                it.status == MedicationStatus.PENDING &&
                it.scheduledDateTime.isAfter(now) &&
                it.scheduledDateTime.isBefore(limitTime)
            }

            val user = database.userDao().getUserByIdDirect(userId)
            val userTimeFormat = user?.timeFormat

            // Group entries by exact scheduled time slot
            val groupedByTime = pendingEntries.groupBy { it.scheduledDateTime.withSecond(0).withNano(0) }

            groupedByTime.forEach { (slotDateTime, entriesInSlot) ->
                val entriesWithNames = entriesInSlot.mapNotNull { entry ->
                    val med = database.medicineDao().getMedicineByIdDirect(entry.medicineId)
                    med?.let { Pair(entry, it.medicationName) }
                }

                if (entriesWithNames.isNotEmpty()) {
                    val entriesList = entriesWithNames.map { it.first }
                    val namesList = entriesWithNames.map { it.second }
                    // Guard: Pass user's timeFormat so scheduled alarm intent contains user's preferred 12h/24h time string
                    alarmScheduler.scheduleGroupedMedicineAlarm(slotDateTime, entriesList, namesList, timeFormat = userTimeFormat)
                    scheduledCount += entriesWithNames.size
                }
            }

            Log.d(TAG, "Rescheduled $scheduledCount alarms for user: $userId (from ${allEntries.size} total entries, limit 7 days)")
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling alarms for user: $userId", e)
            throw e
        }
    }

    // method to schedule alarms for a specific date range
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun scheduleAlarmsForDateRange(userId: String, startDate: LocalDate, endDate: LocalDate, context: Context) {
        val alarmScheduler = AlarmScheduler(context)

        try {
            val startMillis = startDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            val endMillis = endDate.atTime(java.time.LocalTime.MAX).atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            val entries = database.scheduleDao().getScheduleForDateRange(userId, startMillis, endMillis).first()

            var scheduledCount = 0
            val pendingEntries = entries.filter {
                it.status == MedicationStatus.PENDING &&
                it.scheduledDateTime.isAfter(LocalDateTime.now())
            }
            val user = database.userDao().getUserByIdDirect(userId)
            val userTimeFormat = user?.timeFormat
            val groupedByTime = pendingEntries.groupBy { it.scheduledDateTime.withSecond(0).withNano(0) }
            groupedByTime.forEach { (slotDateTime, entriesInSlot) ->
                val entriesWithNames = entriesInSlot.mapNotNull { entry ->
                    val med = database.medicineDao().getMedicineByIdDirect(entry.medicineId)
                    med?.let { Pair(entry, it.medicationName) }
                }
                if (entriesWithNames.isNotEmpty()) {
                    val entriesList = entriesWithNames.map { it.first }
                    val namesList = entriesWithNames.map { it.second }
                    // Guard: Pass user's timeFormat so scheduled alarm intent contains user's preferred 12h/24h time string
                    alarmScheduler.scheduleGroupedMedicineAlarm(slotDateTime, entriesList, namesList, timeFormat = userTimeFormat)
                    scheduledCount += entriesWithNames.size
                }
            }

            Log.d(TAG, "Scheduled $scheduledCount alarms for date range $startDate to $endDate")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarms for date range", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getScheduleForDateRange(userId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<ScheduleEntry>> {
        val startMillis = startDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val endMillis = endDate.atTime(java.time.LocalTime.MAX).atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        return database.scheduleDao().getScheduleForDateRange(userId, startMillis, endMillis)
    }

    // method to get all schedules for a user as Flow
    fun getScheduleForUser(userId: String): Flow<List<ScheduleEntry>> =
        database.scheduleDao().getScheduleForUser(userId)

    // Guard: Use getAllScheduleEntries to execute one-shot query directly without InvalidationTracker Flow observer overhead
    suspend fun getSchedulesByUserSync(userId: String): List<ScheduleEntry> =
        database.scheduleDao().getAllScheduleEntries(userId)

    suspend fun insertScheduleEntry(entry: ScheduleEntry) =
        database.scheduleDao().insertScheduleEntry(entry)

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun updateMedicationStatus(entryId: String, status: String, takenAt: LocalDateTime?) {
        val takenAtMillis = takenAt?.atZone(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        database.scheduleDao().updateMedicationStatus(entryId, status, takenAtMillis)
        com.example.dosezy.notifications.AlarmActivity.stopActiveAlarm()
    }

    suspend fun updateMedicationStatus(entryId: String, status: String, takenAtMillis: Long?) {
        database.scheduleDao().updateMedicationStatus(entryId, status, takenAtMillis)
        com.example.dosezy.notifications.AlarmActivity.stopActiveAlarm()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getScheduleEntryById(entryId: String): ScheduleEntry? {
        return database.scheduleDao().getScheduleEntryById(entryId)
    }

    suspend fun updateMedicationStatus(entryId: String, status: String, takenAtStr: String?) {
        val takenAt = takenAtStr?.let {
            try { LocalDateTime.parse(it) } catch (_: Exception) { LocalDateTime.now() }
        }
        val takenAtMillis = takenAt?.atZone(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        database.scheduleDao().updateMedicationStatus(entryId, status, takenAtMillis)
        com.example.dosezy.notifications.AlarmActivity.stopActiveAlarm()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun recordDoseTaken(
        entryId: String,
        status: String = "TAKEN_ON_TIME",
        takenAt: LocalDateTime = LocalDateTime.now(),
        context: Context? = null,
        notes: String? = null,
        syncSlotNotification: Boolean = true
    ) {
        // Inspect previous status to prevent double stock deduction
        val previousEntry = database.scheduleDao().getScheduleEntryById(entryId)
        val alreadyTaken = previousEntry?.status == MedicationStatus.TAKEN_ON_TIME || previousEntry?.status == MedicationStatus.TAKEN_LATE

        // 1. Update schedule entry status in database with UTC epoch millis
        val takenAtMillis = takenAt.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        if (notes != null) {
            database.scheduleDao().updateMedicationStatusWithNotes(entryId, status, takenAtMillis, notes)
        } else {
            database.scheduleDao().updateMedicationStatus(entryId, status, takenAtMillis)
        }

        // 2. Stop any active alarm sound / popup
        // Guard: Only dismiss AlarmActivity on single-dose actions; during batch cohort processing (syncSlotNotification == false), stopping activity cancels ViewModel scope and aborts remaining doses (#115)
        if (syncSlotNotification) {
            com.example.dosezy.notifications.AlarmActivity.stopActiveAlarm()
        }

        // 3. Stock Auto-Decrement Logic (only if not already taken)
        if (!alreadyTaken && previousEntry != null) {
            try {
                val medicine = database.medicineDao().getMedicineByIdDirect(previousEntry.medicineId)
                if (medicine != null && medicine.currentStock != null && medicine.autoDeductOnTake) {
                    // Guard: Pass previousEntry.dosage to getStockDeductionAmount to prevent inventory calculation drift on custom dose overrides
                    val deductAmount = medicine.getStockDeductionAmount(previousEntry.scheduledDateTime.toLocalTime(), previousEntry.dosage)
                    val actualDeducted = minOf(deductAmount, medicine.currentStock)
                    val newStock = (medicine.currentStock - deductAmount).coerceAtLeast(0)
                    val updatedMedicine = medicine.copy(currentStock = newStock)
                    database.medicineDao().updateMedicine(updatedMedicine)
                    Log.d(TAG, "Decremented stock for ${medicine.medicationName}: ${medicine.currentStock} -> $newStock (deducted $actualDeducted of $deductAmount)")

                    // Guard: If partial stock was consumed due to stock exhaustion, record note to inform medical reports and preserve inventory symmetry on undo
                    if (actualDeducted < deductAmount) {
                        val partialTag = "[Partial Stock: Took $actualDeducted of $deductAmount]"
                        val mergedNotes = if (!notes.isNullOrBlank()) "$notes | $partialTag" else partialTag
                        database.scheduleDao().updateDoseNotes(entryId, mergedNotes)
                    }

                    // Trigger refill warning notification when crossing threshold boundary or running out
                    val threshold = medicine.refillThreshold
                    // Guard: Check medicine.currentStock >= threshold and currentStock > newStock to catch boundary crossings when initial stock equals threshold
                    val shouldNotifyRefill = threshold != null && ((medicine.currentStock >= threshold && newStock <= threshold && medicine.currentStock > newStock) || (medicine.currentStock > 0 && newStock == 0))
                    if (context != null && shouldNotifyRefill) {
                        // Guard: Ensure notification channels are registered before posting refill alerts to prevent Android 8.0+ (API 26+) from dropping notifications
                        com.example.dosezy.notifications.MedicineAlarmReceiver.createNotificationChannels(context)
                        // Guard: Resolve notification strings using target profile language to prevent English or mismatched language in multi-profile refill alerts (#BUG-REFILL-LOCALE)
                        val userLang = database.userDao().getUserByIdDirect(previousEntry.userId)?.language
                            ?: com.example.dosezy.utils.LocaleHelper.getSavedLanguage(context)
                        val localizedContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, userLang)
                        val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

                        val contentIntent = android.app.PendingIntent.getActivity(
                            context,
                            (previousEntry.medicineId + "_refill_click").hashCode(),
                            Intent(context, com.example.dosezy.MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                // Guard: Pass target user ID and route to medicines screen so tapping refill alert switches profile and navigates to medication list
                                putExtra("switch_to_user_id", previousEntry.userId)
                                putExtra("fragment", "medicines")
                            },
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )

                        // Guard: Instantiate NotificationCompat.Builder with localizedContext so internal resource inflation and layout direction align with target profile locale (#BUG-REFILL-NOTIF-BUILDER)
                        val refillIconBitmap = if (context != null) {
                            runCatching { BitmapFactory.decodeResource(context.resources, com.example.dosezy.R.mipmap.ic_launcher) }.getOrNull()
                        } else null

                        val builder = androidx.core.app.NotificationCompat.Builder(localizedContext, com.example.dosezy.notifications.MedicineAlarmReceiver.REFILL_CHANNEL_ID)
                            .setSmallIcon(com.example.dosezy.R.mipmap.ic_launcher)
                            .apply {
                                if (refillIconBitmap != null) {
                                    setLargeIcon(refillIconBitmap)
                                }
                            }
                            .setContentTitle(localizedContext.getString(com.example.dosezy.R.string.notif_refill_alert_title, medicine.medicationName))
                            .setContentText(localizedContext.getString(com.example.dosezy.R.string.notif_refill_alert_text, newStock))
                            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(contentIntent)
                            .setAutoCancel(true)
                        nManager.notify((previousEntry.medicineId + "_refill").hashCode(), builder.build())
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Error performing stock auto-decrement in recordDoseTaken", ex)
            }
        }

        // 4. Synchronize active slot notifications, cancel armed alarms, and update widgets
        // Guard: Allow suppressing intermediate slot synchronization during coordinated batch processing (e.g. IntakeSafeguardManager) to avoid redundant notification churn (#BUG-01)
        if (context != null && syncSlotNotification) {
            syncSlotNotificationAndAlarms(entryId, previousEntry, context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun recordDoseTaken(
        entryId: String,
        status: String,
        takenAtStr: String,
        context: Context? = null,
        notes: String? = null,
        syncSlotNotification: Boolean = true
    ) {
        val takenAt = try { LocalDateTime.parse(takenAtStr) } catch (_: Exception) { LocalDateTime.now() }
        recordDoseTaken(entryId, status, takenAt, context, notes, syncSlotNotification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun recordDoseSkipped(
        entryId: String,
        skipReason: String,
        context: Context? = null
    ) {
        // 1. Update schedule entry status in database
        val entry = database.scheduleDao().getScheduleEntryById(entryId)
        database.scheduleDao().updateMedicationStatusWithReason(entryId, "SKIPPED", null, skipReason)

        // 2. Stop any active alarm sound / popup
        com.example.dosezy.notifications.AlarmActivity.stopActiveAlarm()

        // 3. Synchronize active slot notifications, cancel armed alarms, and update widgets
        if (context != null) {
            syncSlotNotificationAndAlarms(entryId, entry, context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun recordDoseMissed(
        entryId: String,
        context: Context? = null
    ) {
        // 1. Update schedule entry status in database
        val entry = database.scheduleDao().getScheduleEntryById(entryId)
        database.scheduleDao().updateMedicationStatus(entryId, "MISSED", null as Long?)

        // 2. Stop any active alarm sound / popup
        com.example.dosezy.notifications.AlarmActivity.stopActiveAlarm()

        // 3. Synchronize active slot notifications, cancel armed alarms, and update widgets
        if (context != null) {
            syncSlotNotificationAndAlarms(entryId, entry, context)
        }
    }

    // Guard: Synchronize slot notification when taking/skipping/missing a dose to prevent dropping cohort reminders or leaving stale shade entries
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun syncSlotNotificationAndAlarms(
        entryId: String,
        entry: ScheduleEntry?,
        context: Context
    ) {
        val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager

        // Guard: Resolve primary cohort ID before cancelling snooze/nagging alarms to prevent wiping membership mapping before notification dismissal (#BUG-SLOT-NULL-ENTRY)
        val prefs = try {
            context.getSharedPreferences(
                AlarmScheduler.PREFS_COORDINATION,
                Context.MODE_PRIVATE
            )
        } catch (_: Exception) { null }
        val primaryNagging = prefs?.getString("nagging_member_$entryId", null)
        val primarySnooze = prefs?.getString("snooze_member_$entryId", null)
        val primaryId = primaryNagging ?: primarySnooze

        // Cancel armed snooze, nagging, and intake safeguard alarms so taking in-app prevents phantom alarms from ringing later
        try {
            val alarmScheduler = AlarmScheduler(context)
            alarmScheduler.cancelSnooze(entryId)
            alarmScheduler.cancelNagging(entryId)
            // Guard: Cancel intake safeguard re-alert alarm for this entry to prevent sounding alarm after dose was taken (#104)
            alarmScheduler.cancelIntakeSafeguardAlarm(entryId)
        } catch (_: Exception) {}

        // Guard: Reconcile active ADHD intake countdown notification if confirmed entry matches active countdown to prevent dropping remaining pending medications (#BUG-SAFEGUARD-PARTIAL-COHORT)
        try {
            val activeCountdown = com.example.dosezy.notifications.IntakeSafeguardManager.activeCountdown.value
            if (activeCountdown != null && (activeCountdown.entryId == entryId || activeCountdown.entryIds.contains(entryId))) {
                com.example.dosezy.notifications.IntakeSafeguardManager.reconcileCountdown(context, listOf(entryId))
            }
        } catch (_: Exception) {}

        if (nManager != null) {
            try {
                if (entry != null) {
                    val slotMillis = entry.scheduledDateTime.withSecond(0).withNano(0)
                        .atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
                    val slotEntries = database.scheduleDao().getScheduleForDateRangeDirect(entry.userId, slotMillis, slotMillis + 59999L)
                    // Guard: Include entry in remainingPending if its status is PENDING (e.g. on undo) so cohort notification is restored (#BUG-03)
                    val remainingPending = slotEntries.filter { !it.entryId.startsWith("PRN_") && it.status == MedicationStatus.PENDING && (entry.status == MedicationStatus.PENDING || it.entryId != entryId) }

                    if (remainingPending.isEmpty()) {
                        // Guard: Cancel exact slot alarm and entry alarms from AlarmManager when all slot doses are resolved to remove lockscreen alarm icon and prevent phantom CPU wakeups
                        // Guard: Disarm snooze, nagging, and intake safeguard alarms for all sibling slot entries to prevent phantom Doze wakeups (#BUG-SIBLING-CLEANUP)
                        try {
                            val alarmScheduler = AlarmScheduler(context)
                            alarmScheduler.cancelSlotAlarm(entry.userId, entry.scheduledDateTime)
                            slotEntries.forEach { slotEntry ->
                                alarmScheduler.cancelAlarm(slotEntry)
                                alarmScheduler.cancelSnooze(slotEntry.entryId)
                                alarmScheduler.cancelNagging(slotEntry.entryId)
                                alarmScheduler.cancelIntakeSafeguardAlarm(slotEntry.entryId)
                            }
                        } catch (_: Exception) {}

                        // All medications in this slot are handled: cancel notification for entry, primary, and all slot items
                        nManager.cancel(entryId.hashCode())
                        if (primaryId != null) nManager.cancel(primaryId.hashCode())
                        slotEntries.forEach { nManager.cancel(it.entryId.hashCode()) }
                    } else {
                        // Other medications remain pending in this slot; update active notification to remove taken medication
                        // Guard: Check active notifications against all slot entry IDs and stored primaryId; if entry is PENDING on today's schedule, force active alert restoration (#BUG-03, #BUG-UNDO-PAST-DATE)
                        val isScheduledToday = entry.scheduledDateTime.toLocalDate() == java.time.LocalDate.now()
                        val hasActiveNotification = (entry.status == MedicationStatus.PENDING && isScheduledToday) || try {
                            val active = nManager.activeNotifications
                            active.any { notif ->
                                slotEntries.any { it.entryId.hashCode() == notif.id } || (primaryId != null && notif.id == primaryId.hashCode())
                            }
                        } catch (_: Exception) { false }

                        // Guard: Cancel prior notifications for the taken entry, stored primary, and all slot items before re-issuing cohort notification to prevent orphan/duplicate notifications in shade
                        nManager.cancel(entryId.hashCode())
                        if (primaryId != null) {
                            nManager.cancel(primaryId.hashCode())
                        }
                        slotEntries.forEach { nManager.cancel(it.entryId.hashCode()) }

                        if (hasActiveNotification) {
                            val newPrimary = remainingPending.first()
                            val medDetails = remainingPending.mapNotNull { e ->
                                val m = database.medicineDao().getMedicineByIdDirect(e.medicineId) ?: return@mapNotNull null
                                val dose = m.getDosageDisplay(e.scheduledDateTime.toLocalTime())
                                Pair(m.medicationName, dose)
                            }
                            val activeMedNames = ArrayList(remainingPending.mapNotNull { e ->
                                database.medicineDao().getMedicineByIdDirect(e.medicineId)?.medicationName
                            }.distinct())
                            val effectiveMedName = if (activeMedNames.isNotEmpty()) activeMedNames.joinToString(", ") else "Medicine"
                            val activeEntryIds = ArrayList(remainingPending.map { it.entryId })

                            val user = database.userDao().getUserByIdDirect(entry.userId)
                            // Guard: Derive locale from user's explicit profile language so scheduled time matches profile preferences (#BUG-NOTIF-LOCALE)
                            val targetLanguage = user?.language ?: com.example.dosezy.utils.LocaleHelper.getSavedLanguage(context)
                            val currentLocale = com.example.dosezy.utils.LocaleHelper.getLocale(targetLanguage)
                            val formattedScheduledTime = if (user != null) {
                                try {
                                    com.example.dosezy.utils.TimeFormatUtils.formatTime(newPrimary.scheduledDateTime, user.timeFormat, currentLocale)
                                } catch (_: Exception) { null }
                            } else null

                            MedicineAlarmReceiver.updateCohortNotification(
                                context = context,
                                entryId = newPrimary.entryId,
                                entryIds = activeEntryIds,
                                medicineName = effectiveMedName,
                                medicineNames = activeMedNames,
                                medicineDetails = medDetails,
                                scheduledTime = formattedScheduledTime,
                                snoozeMinutes = user?.snoozeDuration ?: 10,
                                userId = entry.userId,
                                userLanguage = user?.language
                            )

                            // Guard: Re-arm nagging reminder only if undone dose belongs to today's schedule and nagging reminders are enabled to prevent phantom alarms for historical past-date doses (#BUG-03, #BUG-UNDO-PAST-DATE)
                            if (entry.status == MedicationStatus.PENDING && isScheduledToday && user != null && user.naggingRemindersEnabled && entry.scheduledDateTime.isBefore(LocalDateTime.now())) {
                                try {
                                    val alarmScheduler = AlarmScheduler(context)
                                    alarmScheduler.scheduleNaggingReminder(
                                        entryId = newPrimary.entryId,
                                        minutes = user.naggingIntervalMinutes,
                                        medicineName = effectiveMedName,
                                        naggingCount = 1,
                                        entryIds = activeEntryIds,
                                        medicineNames = activeMedNames,
                                        scheduledTime = formattedScheduledTime,
                                        userId = newPrimary.userId
                                    )
                                } catch (ex: Exception) {
                                    Log.e(TAG, "Error scheduling nagging reminder on undoDoseTaken", ex)
                                }
                            }
                        }
                    }
                } else {
                    // Guard: When entry is null (deleted or invalid), disarm alarms and cancel notification (#BUG-SLOT-NULL-ENTRY)
                    try {
                        val alarmScheduler = AlarmScheduler(context)
                        alarmScheduler.cancelAlarm(entryId)
                        alarmScheduler.cancelSnooze(entryId)
                        alarmScheduler.cancelNagging(entryId)
                        alarmScheduler.cancelIntakeSafeguardAlarm(entryId)
                        if (primaryId != null) {
                            alarmScheduler.cancelAlarm(primaryId)
                            alarmScheduler.cancelSnooze(primaryId)
                            alarmScheduler.cancelNagging(primaryId)
                            alarmScheduler.cancelIntakeSafeguardAlarm(primaryId)
                        }
                    } catch (_: Exception) {}
                    nManager.cancel(entryId.hashCode())
                    if (primaryId != null) nManager.cancel(primaryId.hashCode())
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Error synchronizing slot notification in ScheduleRepository", ex)
                try { nManager.cancel(entryId.hashCode()) } catch (_: Exception) {}
                if (primaryId != null) try { nManager.cancel(primaryId.hashCode()) } catch (_: Exception) {}
            }
        }

        try {
            com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
        } catch (_: Exception) {}
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun undoDoseTaken(entryId: String, context: Context? = null) {
        // Inspect previous status to only restore stock if it was actually taken
        val previousEntry = database.scheduleDao().getScheduleEntryById(entryId)
        val wasTaken = previousEntry?.status == MedicationStatus.TAKEN_ON_TIME || previousEntry?.status == MedicationStatus.TAKEN_LATE

        // 1. Revert status to PENDING or delete if it was an as-needed (PRN) dose
        if (entryId.startsWith("PRN_")) {
            database.scheduleDao().deleteScheduleEntry(entryId)
        } else {
            // Guard: Strip automated partial stock tag and retrospective logging audit tags while preserving user-authored clinical notes (#BUG-04, #109)
            val cleanedNotes = previousEntry?.doseNotes
                ?.replace(Regex("\\[Partial Stock: Took \\d+ of \\d+\\]"), "")
                ?.replace(Regex("\\[Logged retrospectively\\]"), "")
                ?.trim(' ', '|', '-')
                ?.ifBlank { null }
            database.scheduleDao().resetScheduleEntryToPendingWithNotes(entryId, cleanedNotes)
        }

        // 2. Revert stock auto-decrement only if previously taken
        if (wasTaken && previousEntry != null) {
            try {
                val medicine = database.medicineDao().getMedicineByIdDirect(previousEntry.medicineId)
                if (medicine != null && medicine.currentStock != null && medicine.autoDeductOnTake) {
                    // Guard: Check for partial stock tag in doseNotes to restore only the quantity actually consumed and prevent artificial inventory inflation
                    val partialMatch = previousEntry.doseNotes?.let { Regex("\\[Partial Stock: Took (\\d+) of (\\d+)\\]").find(it) }
                    val actualConsumed = partialMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
                    val addAmount = actualConsumed ?: medicine.getStockDeductionAmount(previousEntry.scheduledDateTime.toLocalTime(), previousEntry.dosage)
                    val restoredStock = medicine.currentStock + addAmount
                    val updatedMedicine = medicine.copy(currentStock = restoredStock)
                    database.medicineDao().updateMedicine(updatedMedicine)
                    Log.d(TAG, "Restored stock for ${medicine.medicationName}: ${medicine.currentStock} -> $restoredStock on undo (restored $addAmount)")

                    // Guard: Dismiss active refill alert notification from shade when undo restores inventory above refill threshold
                    if (restoredStock > (medicine.refillThreshold ?: 0)) {
                        val nManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                        nManager?.cancel((previousEntry.medicineId + "_refill").hashCode())
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Error reverting stock in undoDoseTaken", ex)
            }
        }

        // 3. Update app widgets and alarms
        if (context != null && previousEntry != null) {
            val now = LocalDateTime.now()
            val isScheduledToday = previousEntry.scheduledDateTime.toLocalDate() == java.time.LocalDate.now()
            if (previousEntry.scheduledDateTime.isAfter(now)) {
                // Guard: Reschedule alarms for upcoming doses so undoing a mistakenly marked future dose restores future AlarmManager reminders
                try {
                    rescheduleAllAlarms(previousEntry.userId, context)
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms on undoDoseTaken", e)
                }
            } else if (isScheduledToday) {
                // Guard: Re-post shade notification and re-arm reminder for current or overdue undone dose on today's schedule (#BUG-03, #BUG-UNDO-PAST-DATE)
                try {
                    syncSlotNotificationAndAlarms(entryId, previousEntry.copy(status = MedicationStatus.PENDING), context)
                } catch (e: Exception) {
                    Log.e(TAG, "Error resyncing slot notification on undoDoseTaken", e)
                }
            } else {
                // Guard: For historical past-date doses, disarm any lingering notifications or alarms without posting new ones (#BUG-UNDO-PAST-DATE)
                try {
                    val alarmScheduler = AlarmScheduler(context)
                    alarmScheduler.cancelAlarm(previousEntry)
                    alarmScheduler.cancelNagging(previousEntry.entryId)
                    alarmScheduler.cancelSnooze(previousEntry.entryId)
                    alarmScheduler.cancelIntakeSafeguardAlarm(previousEntry.entryId)
                    val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                    nManager?.cancel(previousEntry.entryId.hashCode())
                } catch (_: Exception) {}
            }
            try {
                com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
            } catch (_: Exception) {}
        }
    }

    suspend fun updateDoseNotes(entryId: String, notes: String?) {
        database.scheduleDao().updateDoseNotes(entryId, notes)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun logAsNeededDose(
        medicine: com.example.dosezy.data.model.Medicine,
        userId: String,
        dateTime: LocalDateTime = LocalDateTime.now(),
        context: Context? = null
    ) {
        // Guard: Append random UUID suffix to prevent primary key collision and silent record overwrite on rapid PRN logging
        val entryId = "PRN_${medicine.medicineId}_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(8)}"
        val entry = ScheduleEntry(
            entryId = entryId,
            userId = userId,
            medicineId = medicine.medicineId,
            scheduledDateTime = dateTime,
            takenAt = dateTime,
            status = MedicationStatus.TAKEN_ON_TIME,
            dosage = medicine.getDosageForTime(dateTime.toLocalTime())
        )
        database.scheduleDao().insertScheduleEntry(entry)

        // Guard: Query fresh entity from Room and pass slot time to getStockDeductionAmount to prevent stale UI stock overwrites and symmetry drift with undoDoseTaken
        val freshMedicine = database.medicineDao().getMedicineByIdDirect(medicine.medicineId) ?: medicine
        if (freshMedicine.currentStock != null && freshMedicine.autoDeductOnTake) {
            val deductAmount = freshMedicine.getStockDeductionAmount(dateTime.toLocalTime())
            val actualDeducted = minOf(deductAmount, freshMedicine.currentStock)
            val newStock = (freshMedicine.currentStock - deductAmount).coerceAtLeast(0)
            val updatedMedicine = freshMedicine.copy(currentStock = newStock)
            database.medicineDao().updateMedicine(updatedMedicine)

            // Guard: Record partial stock tag on as-needed dose when stock is exhausted to preserve inventory audit trail
            if (actualDeducted < deductAmount) {
                val partialTag = "[Partial Stock: Took $actualDeducted of $deductAmount]"
                database.scheduleDao().updateDoseNotes(entryId, partialTag)
            }

            val threshold = freshMedicine.refillThreshold
            // Guard: Check freshMedicine.currentStock >= threshold and currentStock > newStock to catch boundary crossings when initial stock equals threshold
            val shouldNotifyRefill = threshold != null && ((freshMedicine.currentStock >= threshold && newStock <= threshold && freshMedicine.currentStock > newStock) || (freshMedicine.currentStock > 0 && newStock == 0))
            if (context != null && shouldNotifyRefill) {
                // Guard: Ensure notification channels are registered before posting refill alerts to prevent Android 8.0+ (API 26+) from dropping notifications
                com.example.dosezy.notifications.MedicineAlarmReceiver.createNotificationChannels(context)
                // Guard: Resolve notification strings using target profile language to prevent English or mismatched language in multi-profile refill alerts (#BUG-REFILL-LOCALE)
                val userLang = database.userDao().getUserByIdDirect(freshMedicine.userId)?.language
                    ?: com.example.dosezy.utils.LocaleHelper.getSavedLanguage(context)
                val localizedContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, userLang)
                val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val contentIntent = android.app.PendingIntent.getActivity(
                    context,
                    (freshMedicine.medicineId + "_refill_click").hashCode(),
                    Intent(context, com.example.dosezy.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        // Guard: Pass target user ID and route to medicines screen so tapping refill alert switches profile and navigates to medication list
                        putExtra("switch_to_user_id", freshMedicine.userId)
                        putExtra("fragment", "medicines")
                    },
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                // Guard: Instantiate NotificationCompat.Builder with localizedContext so internal resource inflation and layout direction align with target profile locale (#BUG-REFILL-NOTIF-BUILDER)
                val refillIconBitmap = if (context != null) {
                    runCatching { BitmapFactory.decodeResource(context.resources, com.example.dosezy.R.mipmap.ic_launcher) }.getOrNull()
                } else null

                val builder = androidx.core.app.NotificationCompat.Builder(localizedContext, com.example.dosezy.notifications.MedicineAlarmReceiver.REFILL_CHANNEL_ID)
                    .setSmallIcon(com.example.dosezy.R.mipmap.ic_launcher)
                    .apply {
                        if (refillIconBitmap != null) {
                            setLargeIcon(refillIconBitmap)
                        }
                    }
                    .setContentTitle(localizedContext.getString(com.example.dosezy.R.string.notif_refill_alert_title, freshMedicine.medicationName))
                    .setContentText(localizedContext.getString(com.example.dosezy.R.string.notif_refill_alert_text, newStock))
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true)
                nManager.notify((freshMedicine.medicineId + "_refill").hashCode(), builder.build())
            }
        }

        if (context != null) {
            try {
                com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
            } catch (_: Exception) {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getPendingEntriesBefore(userId: String, cutoffTime: LocalDateTime): List<ScheduleEntry> {
        val cutoffMillis = cutoffTime.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        return database.scheduleDao().getPendingEntriesBefore(userId, cutoffMillis)
    }

    /**
     * Automatically detects and corrects schedule entries that were serialized using legacy
     * pre-v2.5.2 ZoneId.systemDefault() epoch converters.
     * Uses entryId timestamp encoding and Medicine.scheduledTimes as untainted sources of truth.
     *
     * @return Number of schedule entries that were corrected.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun reconcileLegacyScheduleEntries(userId: String, context: Context? = null): Int {
        var correctedCount = 0
        try {
            val allEntries = database.scheduleDao().getAllScheduleEntries(userId)
            if (allEntries.isEmpty()) return 0

            val medicines = database.medicineDao().getMedicinesByUserDirect(userId)
            val medMap = medicines.associateBy { it.medicineId }

            for (entry in allEntries) {
                if (entry.entryId.startsWith("PRN_")) continue

                // 1. Try parsing intended LocalDateTime directly from entryId
                var intendedDateTime = parseDateTimeFromEntryId(entry.entryId)

                // 2. Fallback: match against parent Medicine.scheduledTimes
                if (intendedDateTime == null) {
                    val med = medMap[entry.medicineId]
                    if (med != null && med.scheduledTimes.isNotEmpty()) {
                        intendedDateTime = findClosestScheduledTime(entry.scheduledDateTime, med.scheduledTimes)
                    }
                }

                // 3. If intendedDateTime differs from scheduledDateTime, recalibrate and update
                if (intendedDateTime != null && intendedDateTime != entry.scheduledDateTime) {
                    val duration = java.time.Duration.between(entry.scheduledDateTime, intendedDateTime)
                    val correctedTakenAt = entry.takenAt?.plus(duration)

                    val correctedEntry = entry.copy(
                        scheduledDateTime = intendedDateTime,
                        takenAt = correctedTakenAt
                    )
                    database.scheduleDao().updateScheduleEntry(correctedEntry)
                    correctedCount++
                    Log.d(TAG, "Reconciled legacy entry ${entry.entryId}: ${entry.scheduledDateTime} -> $intendedDateTime")
                }
            }

            if (correctedCount > 0 && context != null) {
                rescheduleAllAlarms(userId, context)
                try {
                    com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reconciling legacy schedule entries for user: $userId", e)
        }
        return correctedCount
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun reconcileAllProfiles(context: Context? = null): Int {
        var total = 0
        try {
            val users = database.userDao().getAllUsersDirect()
            users.forEach { user ->
                total += reconcileLegacyScheduleEntries(user.userId, context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reconciling all profiles", e)
        }
        return total
    }
}