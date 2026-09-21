// ScheduleRepository.kt
package com.example.dosezy.data.repository

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.ScheduleWithMedicine
import com.example.dosezy.notifications.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime

class ScheduleRepository(private val database: DosezyDatabase) {

    companion object {
        private const val TAG = "ScheduleRepository"
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
            val medicines = database.medicineDao().getMedicinesByUser(userId).first()
            medicines.forEach { medicine ->
                val scheduleEntries = database.scheduleDao().getScheduleEntriesByMedicine(medicine.medicineId)
                val latestEntry = scheduleEntries.maxByOrNull { it.scheduledDateTime }

                // If no entries exist, or the latest entry is less than 15 days in the future,
                // auto-generate/append next 30 days of schedules
                if (latestEntry == null || latestEntry.scheduledDateTime.isBefore(LocalDateTime.now().plusDays(15))) {
                    val startGenerateFrom = latestEntry?.scheduledDateTime?.toLocalDate()?.plusDays(1) ?: LocalDate.now()
                    val newEntries = medicine.generateScheduleEntries(startGenerateFrom, 30)
                    if (newEntries.isNotEmpty()) {
                        database.scheduleDao().insertScheduleEntries(newEntries)
                        Log.d(TAG, "Auto-extended schedule for medicine: ${medicine.medicationName} by 30 days starting from $startGenerateFrom")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error auto-extending schedules for user: $userId", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun scheduleAlarmsForMedicine(medicineId: String, context: Context) {
        val alarmScheduler = AlarmScheduler(context)
        val medicine = database.medicineDao().getMedicineById(medicineId).first()

        if (medicine != null) {
            // First check and extend schedules for this user
            autoExtendSchedules(medicine.userId)

            val scheduleEntries = database.scheduleDao().getScheduleEntriesByMedicine(medicineId)
            var scheduledCount = 0
            val limitTime = LocalDateTime.now().plusDays(7)

            scheduleEntries.forEach { entry ->
                if (entry.status == MedicationStatus.PENDING &&
                    entry.scheduledDateTime.isAfter(LocalDateTime.now()) &&
                    entry.scheduledDateTime.isBefore(limitTime)) {

                    alarmScheduler.scheduleMedicineAlarm(entry, medicine.medicationName)
                    scheduledCount++
                } else {
                    // Cancel alarms that are further in the future or no longer pending
                    alarmScheduler.cancelAlarm(entry.entryId)
                    alarmScheduler.cancelSnooze(entry.entryId)
                }
            }
            Log.d(TAG, "Scheduled alarms for medicine: ${medicine.medicationName} ($scheduledCount/${scheduleEntries.size} entries, limit 7 days)")
        } else {
            Log.w(TAG, "Medicine not found for ID: $medicineId - cannot schedule alarms")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun cancelAlarmsForMedicine(medicineId: String, context: Context) {
        val alarmScheduler = AlarmScheduler(context)
        val scheduleEntries = database.scheduleDao().getScheduleEntriesByMedicine(medicineId)
        scheduleEntries.forEach { entry ->
            alarmScheduler.cancelAlarm(entry.entryId)
            alarmScheduler.cancelSnooze(entry.entryId)
            alarmScheduler.cancelSlotAlarm(entry.userId, entry.scheduledDateTime)
        }
        Log.d(TAG, "Cancelled alarms for medicine ID: $medicineId (${scheduleEntries.size} entries)")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun rescheduleAllAlarms(userId: String, context: Context) {
        val alarmScheduler = AlarmScheduler(context)

        try {
            // First check and extend schedules
            autoExtendSchedules(userId)

            // Get all schedule entries for the user
            val allEntries = database.scheduleDao().getAllScheduleEntries(userId)

            // Cancel all existing alarms first
            allEntries.forEach { entry ->
                alarmScheduler.cancelAlarm(entry.entryId)
                alarmScheduler.cancelSnooze(entry.entryId)
                alarmScheduler.cancelSlotAlarm(entry.userId, entry.scheduledDateTime)
            }

            // Schedule grouped alarms for pending future entries within the 7-day window
            var scheduledCount = 0
            val limitTime = LocalDateTime.now().plusDays(7)
            val nowMinus2 = LocalDateTime.now().minusMinutes(2)
            val pendingEntries = allEntries.filter { 
                it.status == MedicationStatus.PENDING &&
                it.scheduledDateTime.isAfter(nowMinus2) &&
                it.scheduledDateTime.isBefore(limitTime)
            }

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
                    alarmScheduler.scheduleGroupedMedicineAlarm(slotDateTime, entriesList, namesList)
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
            entries.forEach { entry ->
                if (entry.status == MedicationStatus.PENDING &&
                    entry.scheduledDateTime.isAfter(LocalDateTime.now())) {

                    val medicine = database.medicineDao().getMedicineById(entry.medicineId).first()
                    medicine?.let {
                        alarmScheduler.scheduleMedicineAlarm(entry, it.medicationName)
                        scheduledCount++
                    }
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

    // method to get schedules as list (not Flow)
    suspend fun getSchedulesByUserSync(userId: String): List<ScheduleEntry> =
        database.scheduleDao().getScheduleForUser(userId).first()

    suspend fun insertScheduleEntry(entry: ScheduleEntry) =
        database.scheduleDao().insertScheduleEntry(entry)

    suspend fun updateMedicationStatus(entryId: String, status: String, takenAt: String?) {
        database.scheduleDao().updateMedicationStatus(entryId, status, takenAt)
        com.example.dosezy.notifications.AlarmActivity.stopActiveAlarm()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun recordDoseTaken(
        entryId: String,
        status: String = "TAKEN_ON_TIME",
        takenAt: String = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        context: Context? = null
    ) {
        // Inspect previous status to prevent double stock deduction
        val previousEntry = database.scheduleDao().getScheduleEntryById(entryId)
        val alreadyTaken = previousEntry?.status == MedicationStatus.TAKEN_ON_TIME || previousEntry?.status == MedicationStatus.TAKEN_LATE

        // 1. Update schedule entry status in database
        database.scheduleDao().updateMedicationStatus(entryId, status, takenAt)

        // 2. Stop any active alarm sound / popup
        com.example.dosezy.notifications.AlarmActivity.stopActiveAlarm()

        // 3. Stock Auto-Decrement Logic (only if not already taken)
        if (!alreadyTaken && previousEntry != null) {
            try {
                val medicine = database.medicineDao().getMedicineByIdDirect(previousEntry.medicineId)
                if (medicine != null && medicine.currentStock != null && medicine.autoDeductOnTake) {
                    val deductAmount = medicine.getStockDeductionAmount(previousEntry.scheduledDateTime.toLocalTime())
                    val newStock = (medicine.currentStock - deductAmount).coerceAtLeast(0)
                    val updatedMedicine = medicine.copy(currentStock = newStock)
                    database.medicineDao().updateMedicine(updatedMedicine)
                    Log.d(TAG, "Decremented stock for ${medicine.medicationName}: ${medicine.currentStock} -> $newStock (deducted $deductAmount)")

                    // Trigger refill warning notification if stock is below threshold
                    if (context != null && medicine.refillThreshold != null && newStock <= medicine.refillThreshold) {
                        val savedLanguage = com.example.dosezy.utils.LocaleHelper.getSavedLanguage(context)
                        val localizedContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, savedLanguage)
                        val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

                        val contentIntent = android.app.PendingIntent.getActivity(
                            context,
                            (previousEntry.medicineId + "_refill_click").hashCode(),
                            Intent(context, com.example.dosezy.MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            },
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )

                        val builder = androidx.core.app.NotificationCompat.Builder(context, com.example.dosezy.notifications.MedicineAlarmReceiver.REFILL_CHANNEL_ID)
                            .setSmallIcon(com.example.dosezy.R.drawable.loader_icon)
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

        // 4. Cancel active notification for this entry and update widgets
        if (context != null) {
            try {
                val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                nManager.cancel(entryId.hashCode())
            } catch (_: Exception) {}
            try {
                com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
            } catch (_: Exception) {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun recordDoseSkipped(
        entryId: String,
        skipReason: String,
        context: Context? = null
    ) {
        // 1. Update schedule entry status in database
        database.scheduleDao().updateMedicationStatusWithReason(entryId, "SKIPPED", null, skipReason)

        // 2. Stop any active alarm sound / popup
        com.example.dosezy.notifications.AlarmActivity.stopActiveAlarm()

        // 3. Cancel active notification for this entry and update widgets
        if (context != null) {
            try {
                val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                nManager.cancel(entryId.hashCode())
            } catch (_: Exception) {}
            try {
                com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
            } catch (_: Exception) {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun undoDoseTaken(entryId: String, context: Context? = null) {
        // Inspect previous status to only restore stock if it was actually taken
        val previousEntry = database.scheduleDao().getScheduleEntryById(entryId)
        val wasTaken = previousEntry?.status == MedicationStatus.TAKEN_ON_TIME || previousEntry?.status == MedicationStatus.TAKEN_LATE

        // 1. Revert status to PENDING and clear takenAt and skipReason
        database.scheduleDao().updateMedicationStatusWithReason(entryId, "PENDING", null, null)

        // 2. Revert stock auto-decrement only if previously taken
        if (wasTaken && previousEntry != null) {
            try {
                val medicine = database.medicineDao().getMedicineByIdDirect(previousEntry.medicineId)
                if (medicine != null && medicine.currentStock != null && medicine.autoDeductOnTake) {
                    val addAmount = medicine.getStockDeductionAmount(previousEntry.scheduledDateTime.toLocalTime())
                    val restoredStock = medicine.currentStock + addAmount
                    val updatedMedicine = medicine.copy(currentStock = restoredStock)
                    database.medicineDao().updateMedicine(updatedMedicine)
                    Log.d(TAG, "Restored stock for ${medicine.medicationName}: ${medicine.currentStock} -> $restoredStock on undo (restored $addAmount)")
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Error reverting stock in undoDoseTaken", ex)
            }
        }

        // 3. Update app widgets
        if (context != null) {
            try {
                com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
            } catch (_: Exception) {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun logAsNeededDose(
        medicine: com.example.dosezy.data.model.Medicine,
        userId: String,
        dateTime: LocalDateTime = LocalDateTime.now(),
        context: Context? = null
    ) {
        val entryId = "PRN_${medicine.medicineId}_${System.currentTimeMillis()}"
        val entry = ScheduleEntry(
            entryId = entryId,
            userId = userId,
            medicineId = medicine.medicineId,
            scheduledDateTime = dateTime,
            takenAt = dateTime,
            status = MedicationStatus.TAKEN_ON_TIME
        )
        database.scheduleDao().insertScheduleEntry(entry)

        // Decrement stock if enabled
        if (medicine.currentStock != null && medicine.autoDeductOnTake) {
            val deductAmount = medicine.getStockDeductionAmount()
            val newStock = (medicine.currentStock - deductAmount).coerceAtLeast(0)
            val updatedMedicine = medicine.copy(currentStock = newStock)
            database.medicineDao().updateMedicine(updatedMedicine)

            if (context != null && medicine.refillThreshold != null && newStock <= medicine.refillThreshold) {
                val savedLanguage = com.example.dosezy.utils.LocaleHelper.getSavedLanguage(context)
                val localizedContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, savedLanguage)
                val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val builder = androidx.core.app.NotificationCompat.Builder(context, com.example.dosezy.notifications.MedicineAlarmReceiver.CHANNEL_ID)
                    .setSmallIcon(com.example.dosezy.R.drawable.loader_icon)
                    .setContentTitle(localizedContext.getString(com.example.dosezy.R.string.notif_refill_alert_title, medicine.medicationName))
                    .setContentText(localizedContext.getString(com.example.dosezy.R.string.notif_refill_alert_text, newStock))
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                nManager.notify((medicine.medicineId + "_refill").hashCode(), builder.build())
            }
        }

        if (context != null) {
            try {
                com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
            } catch (_: Exception) {}
        }
    }
}