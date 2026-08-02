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
        val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            putExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID, entry.entryId)
            putExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(
                MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME,
                entry.scheduledDateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            )
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entry.entryId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = entry.scheduledDateTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000

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

        Log.d(TAG, "Scheduled alarm for medicine: $medicineName at ${entry.scheduledDateTime} (exact=${canScheduleExact()})")
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleSnooze(entryId: String, minutes: Int, medicineName: String) {
        val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            putExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID, entryId)
            putExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME, medicineName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entryId.hashCode() + 1000, // Different request code for snooze
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

        Log.d(TAG, "Scheduled snooze for entry: $entryId in $minutes minutes (exact=${canScheduleExact()})")
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