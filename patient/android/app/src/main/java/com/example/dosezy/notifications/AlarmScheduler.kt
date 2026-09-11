// AlarmScheduler.kt
package com.example.dosezy.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.dosezy.data.model.ScheduleEntry
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {

    companion object {
        private const val TAG = "AlarmScheduler"
    }

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun canScheduleExact(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleMedicineAlarm(entry: ScheduleEntry, medicineName: String) {
        scheduleGroupedMedicineAlarm(
            scheduledDateTime = entry.scheduledDateTime,
            entries = listOf(entry),
            medicineNames = listOf(medicineName)
        )
    }

    @SuppressLint("ScheduleExactAlarm")
    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleGroupedMedicineAlarm(
        scheduledDateTime: LocalDateTime,
        entries: List<ScheduleEntry>,
        medicineNames: List<String>
    ) {
        if (entries.isEmpty() || medicineNames.isEmpty()) return

        val entryIds = ArrayList(entries.map { it.entryId })
        val medNames = ArrayList(medicineNames)
        val cleanDateTime = scheduledDateTime.withSecond(0).withNano(0)
        val timeFormatted = cleanDateTime.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"))
        val slotKey = "${entries.first().userId}_${cleanDateTime}"

        val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            putExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID, entryIds.first())
            putStringArrayListExtra(MedicineAlarmReceiver.EXTRA_ENTRY_IDS, entryIds)
            putExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME, medNames.joinToString(", "))
            putStringArrayListExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAMES, medNames)
            putExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME, timeFormatted)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            slotKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = cleanDateTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }

        Log.d(TAG, "Scheduled grouped alarm for ${medNames.size} medicines at $scheduledDateTime (slotKey=$slotKey)")
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleSnooze(entryId: String, minutes: Int, medicineName: String) {
        val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            putExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID, entryId)
            putExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME, medicineName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entryId.hashCode() + 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }

        Log.d(TAG, "Scheduled snooze for entry: $entryId in $minutes minutes")
    }

    fun cancelAlarm(entryId: String) {
        val intent = Intent(context, MedicineAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entryId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled alarm for entry: $entryId")
    }

    fun cancelSlotAlarm(userId: String, scheduledDateTime: LocalDateTime) {
        val slotKey = "${userId}_${scheduledDateTime.withSecond(0).withNano(0)}"
        val intent = Intent(context, MedicineAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            slotKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled grouped slot alarm: $slotKey")
    }

    fun cancelSnooze(entryId: String) {
        val intent = Intent(context, MedicineAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entryId.hashCode() + 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled snooze for entry: $entryId")
    }
}
