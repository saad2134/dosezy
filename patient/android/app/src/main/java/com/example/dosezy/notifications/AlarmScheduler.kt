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
import com.example.dosezy.MainActivity
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.TimeFormat
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {

    companion object {
        private const val TAG = "AlarmScheduler"
        const val PREFS_COORDINATION = "dosezy_alarm_coordination"
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
    fun scheduleMedicineAlarm(entry: ScheduleEntry, medicineName: String, timeFormat: TimeFormat? = null) {
        scheduleGroupedMedicineAlarm(
            scheduledDateTime = entry.scheduledDateTime,
            entries = listOf(entry),
            medicineNames = listOf(medicineName),
            timeFormat = timeFormat
        )
    }

    @SuppressLint("ScheduleExactAlarm")
    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleGroupedMedicineAlarm(
        scheduledDateTime: LocalDateTime,
        entries: List<ScheduleEntry>,
        medicineNames: List<String>,
        timeFormat: TimeFormat? = null
    ) {
        if (entries.isEmpty() || medicineNames.isEmpty()) return

        val entryIds = ArrayList(entries.map { it.entryId })
        val medNames = ArrayList(medicineNames)
        val cleanDateTime = scheduledDateTime.withSecond(0).withNano(0)

        // Guard: Format scheduled time respecting 24h vs 12h user preference and active locale instead of hardcoded 12h pattern
        val is24 = timeFormat == TimeFormat.HOUR_24 || (timeFormat == null && android.text.format.DateFormat.is24HourFormat(context))
        val pattern = if (is24) "HH:mm" else "h:mm a"
        val savedLang = com.example.dosezy.utils.LocaleHelper.getSavedLanguage(context)
        val locale = com.example.dosezy.utils.LocaleHelper.getLocale(savedLang)
        val timeFormatted = cleanDateTime.format(java.time.format.DateTimeFormatter.ofPattern(pattern, locale))
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

        val triggerTime = cleanDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        if (triggerTime <= now) {
            Log.d(TAG, "Skipping past alarm trigger for $cleanDateTime (triggerTime=$triggerTime, now=$now)")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && canScheduleExact()) {
            val showIntent = PendingIntent.getActivity(
                context,
                slotKey.hashCode() + 500,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, showIntent),
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
    fun scheduleSnooze(entryId: String, minutes: Int, medicineName: String, timeFormat: TimeFormat? = null) {
        scheduleGroupedSnooze(listOf(entryId), minutes, listOf(medicineName), timeFormat = timeFormat)
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleGroupedSnooze(
        entryIds: List<String>,
        minutes: Int,
        medicineNames: List<String>,
        explicitTriggerTime: Long? = null,
        timeFormat: TimeFormat? = null
    ) {
        if (entryIds.isEmpty()) return
        val primaryId = entryIds.first()
        val medNameSummary = if (medicineNames.isNotEmpty()) medicineNames.joinToString(", ") else "Medicine"

        // Guard: Support explicitTriggerTime so transferred group alarms preserve exact original target timestamp
        val triggerTime = explicitTriggerTime ?: (System.currentTimeMillis() + (minutes * 60 * 1000))
        // Guard: Format snooze time respecting 24h vs 12h user preference and active locale instead of hardcoded 12h pattern
        val is24 = timeFormat == TimeFormat.HOUR_24 || (timeFormat == null && android.text.format.DateFormat.is24HourFormat(context))
        val pattern = if (is24) "HH:mm" else "h:mm a"
        val savedLang = com.example.dosezy.utils.LocaleHelper.getSavedLanguage(context)
        val locale = com.example.dosezy.utils.LocaleHelper.getLocale(savedLang)
        val snoozeTimeFormatted = java.text.SimpleDateFormat(pattern, locale).format(java.util.Date(triggerTime))

        val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            putExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID, primaryId)
            putStringArrayListExtra(MedicineAlarmReceiver.EXTRA_ENTRY_IDS, ArrayList(entryIds))
            putExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME, medNameSummary)
            putStringArrayListExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAMES, ArrayList(medicineNames))
            // Guard: Pass formatted snooze time to avoid blank time extra and double reminder title in AlarmActivity
            putExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME, snoozeTimeFormatted)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            primaryId.hashCode() + 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Guard: Persist grouped snooze mapping in preferences to prevent dropping cohort alarms when primary entry is marked taken
        try {
            val prefs = context.getSharedPreferences(PREFS_COORDINATION, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString("snooze_group_ids_$primaryId", entryIds.joinToString(","))
            editor.putString("snooze_group_names_$primaryId", medicineNames.joinToString("|||"))
            editor.putLong("snooze_group_trigger_$primaryId", triggerTime)
            entryIds.forEach { id ->
                editor.putString("snooze_member_$id", primaryId)
            }
            editor.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving snooze group coordination", e)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && canScheduleExact()) {
            val showIntent = PendingIntent.getActivity(
                context,
                primaryId.hashCode() + 1500,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, showIntent),
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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

        Log.d(TAG, "Scheduled grouped snooze for ${entryIds.size} entries in $minutes minutes (primaryId=$primaryId)")
    }

    fun cancelAlarm(entryId: String, userId: String? = null, scheduledDateTime: LocalDateTime? = null) {
        val intent = Intent(context, MedicineAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entryId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Cancelled alarm for entry: $entryId")

        if (userId != null && scheduledDateTime != null) {
            cancelSlotAlarm(userId, scheduledDateTime)
        } else {
            val parsed = com.example.dosezy.data.repository.ScheduleRepository.parseDateTimeFromEntryId(entryId)
            if (parsed != null && userId != null) {
                cancelSlotAlarm(userId, parsed)
            }
        }
    }

    fun cancelAlarm(entry: ScheduleEntry) {
        cancelAlarm(entry.entryId, entry.userId, entry.scheduledDateTime)
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
        pendingIntent.cancel()

        val showIntent = PendingIntent.getActivity(
            context,
            slotKey.hashCode() + 500,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        showIntent.cancel()

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

        // Guard: Cancel and release both pendingIntent and showIntent tokens to prevent system PendingIntent token leaks
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        val showIntent = PendingIntent.getActivity(
            context,
            entryId.hashCode() + 1500,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        showIntent.cancel()

        // Guard: Coordinate grouped snooze so taking the primary dose transfers remaining untaken doses rather than dropping them
        try {
            val prefs = context.getSharedPreferences(PREFS_COORDINATION, Context.MODE_PRIVATE)
            val primaryId = prefs.getString("snooze_member_$entryId", null)
            if (primaryId != null) {
                val idsStr = prefs.getString("snooze_group_ids_$primaryId", "") ?: ""
                val namesStr = prefs.getString("snooze_group_names_$primaryId", "") ?: ""
                val triggerTime = prefs.getLong("snooze_group_trigger_$primaryId", 0L)

                val currentIds = idsStr.split(",").filter { it.isNotBlank() }.toMutableList()
                val currentNames = namesStr.split("|||").filter { it.isNotBlank() }.toMutableList()

                val idx = currentIds.indexOf(entryId)
                if (idx >= 0) {
                    currentIds.removeAt(idx)
                    if (idx < currentNames.size) {
                        currentNames.removeAt(idx)
                    }
                }
                prefs.edit().remove("snooze_member_$entryId").apply()

                if (currentIds.isEmpty()) {
                    prefs.edit()
                        .remove("snooze_group_ids_$primaryId")
                        .remove("snooze_group_names_$primaryId")
                        .remove("snooze_group_trigger_$primaryId")
                        .apply()
                    if (primaryId != entryId) {
                        val pIntent = PendingIntent.getBroadcast(
                            context,
                            primaryId.hashCode() + 1000,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        alarmManager.cancel(pIntent)
                        pIntent.cancel()
                        val sIntent = PendingIntent.getActivity(
                            context,
                            primaryId.hashCode() + 1500,
                            Intent(context, MainActivity::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        sIntent.cancel()
                    }
                } else {
                    if (entryId == primaryId) {
                        val newPrimary = currentIds.first()
                        val editor = prefs.edit()
                        editor.remove("snooze_group_ids_$primaryId")
                        editor.remove("snooze_group_names_$primaryId")
                        editor.remove("snooze_group_trigger_$primaryId")
                        currentIds.forEach { id ->
                            editor.putString("snooze_member_$id", newPrimary)
                        }
                        editor.putString("snooze_group_ids_$newPrimary", currentIds.joinToString(","))
                        editor.putString("snooze_group_names_$newPrimary", currentNames.joinToString("|||"))
                        editor.putLong("snooze_group_trigger_$newPrimary", triggerTime)
                        editor.apply()

                        if (triggerTime > System.currentTimeMillis()) {
                            val remMins = ((triggerTime - System.currentTimeMillis()) / (60 * 1000)).toInt().coerceAtLeast(1)
                            scheduleGroupedSnooze(currentIds, remMins, currentNames, triggerTime)
                        }
                    } else {
                        prefs.edit()
                            .putString("snooze_group_ids_$primaryId", currentIds.joinToString(","))
                            .putString("snooze_group_names_$primaryId", currentNames.joinToString("|||"))
                            .apply()

                        val medNameSummary = if (currentNames.isNotEmpty()) currentNames.joinToString(", ") else "Medicine"
                        // Guard: Format snooze time respecting 24h vs 12h user preference and active locale instead of hardcoded 12h pattern
                        val is24 = android.text.format.DateFormat.is24HourFormat(context)
                        val pattern = if (is24) "HH:mm" else "h:mm a"
                        val savedLang = com.example.dosezy.utils.LocaleHelper.getSavedLanguage(context)
                        val locale = com.example.dosezy.utils.LocaleHelper.getLocale(savedLang)
                        val snoozeTimeFormatted = java.text.SimpleDateFormat(pattern, locale).format(java.util.Date(triggerTime))
                        val updateIntent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                            putExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID, primaryId)
                            putStringArrayListExtra(MedicineAlarmReceiver.EXTRA_ENTRY_IDS, ArrayList(currentIds))
                            putExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME, medNameSummary)
                            putStringArrayListExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAMES, ArrayList(currentNames))
                            putExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME, snoozeTimeFormatted)
                        }
                        PendingIntent.getBroadcast(
                            context,
                            primaryId.hashCode() + 1000,
                            updateIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error coordinating snooze cancellation", e)
        }

        Log.d(TAG, "Cancelled snooze for entry: $entryId")
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNaggingReminder(
        entryId: String,
        minutes: Int,
        medicineName: String,
        naggingCount: Int,
        entryIds: ArrayList<String>? = null,
        medicineNames: ArrayList<String>? = null,
        scheduledTime: String? = null,
        explicitTriggerTime: Long? = null
    ) {
        val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            putExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID, entryId)
            putExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(MedicineAlarmReceiver.EXTRA_NAGGING_COUNT, naggingCount)
            if (!entryIds.isNullOrEmpty()) {
                putStringArrayListExtra(MedicineAlarmReceiver.EXTRA_ENTRY_IDS, entryIds)
            }
            if (!medicineNames.isNullOrEmpty()) {
                putStringArrayListExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAMES, medicineNames)
            }
            if (!scheduledTime.isNullOrBlank()) {
                putExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME, scheduledTime)
            }
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entryId.hashCode() + 2000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Guard: Support explicitTriggerTime so transferred nagging alarms preserve exact target timestamp
        val triggerTime = explicitTriggerTime ?: (System.currentTimeMillis() + (minutes * 60 * 1000))

        // Guard: Persist grouped nagging mapping in preferences to prevent dropping cohort alarms when primary entry is marked taken
        if (!entryIds.isNullOrEmpty()) {
            try {
                val prefs = context.getSharedPreferences(PREFS_COORDINATION, Context.MODE_PRIVATE)
                val editor = prefs.edit()
                editor.putString("nagging_group_ids_$entryId", entryIds.joinToString(","))
                editor.putString("nagging_group_names_$entryId", (medicineNames ?: arrayListOf()).joinToString("|||"))
                editor.putLong("nagging_group_trigger_$entryId", triggerTime)
                editor.putInt("nagging_group_count_$entryId", naggingCount)
                editor.putString("nagging_group_sched_$entryId", scheduledTime ?: "")
                entryIds.forEach { id ->
                    editor.putString("nagging_member_$id", entryId)
                }
                editor.apply()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving nagging group coordination", e)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && canScheduleExact()) {
            val showIntent = PendingIntent.getActivity(
                context,
                entryId.hashCode() + 2500,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, showIntent),
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
        Log.d(TAG, "Scheduled nagging reminder #$naggingCount for entry: $entryId in $minutes minutes")
    }

    fun cancelNagging(entryId: String) {
        val intent = Intent(context, MedicineAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entryId.hashCode() + 2000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Guard: Cancel and release both pendingIntent and showIntent tokens to prevent system token leaks
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        val showIntent = PendingIntent.getActivity(
            context,
            entryId.hashCode() + 2500,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        showIntent.cancel()

        // Guard: Coordinate grouped nagging so taking primary dose transfers nagging to remaining entries instead of dropping them
        try {
            val prefs = context.getSharedPreferences(PREFS_COORDINATION, Context.MODE_PRIVATE)
            val primaryId = prefs.getString("nagging_member_$entryId", null)
            if (primaryId != null) {
                val idsStr = prefs.getString("nagging_group_ids_$primaryId", "") ?: ""
                val namesStr = prefs.getString("nagging_group_names_$primaryId", "") ?: ""
                val triggerTime = prefs.getLong("nagging_group_trigger_$primaryId", 0L)
                val count = prefs.getInt("nagging_group_count_$primaryId", 1)
                val schedTime = prefs.getString("nagging_group_sched_$primaryId", "") ?: ""

                val currentIds = idsStr.split(",").filter { it.isNotBlank() }.toMutableList()
                val currentNames = namesStr.split("|||").filter { it.isNotBlank() }.toMutableList()

                val idx = currentIds.indexOf(entryId)
                if (idx >= 0) {
                    currentIds.removeAt(idx)
                    if (idx < currentNames.size) {
                        currentNames.removeAt(idx)
                    }
                }
                prefs.edit().remove("nagging_member_$entryId").apply()

                if (currentIds.isEmpty()) {
                    prefs.edit()
                        .remove("nagging_group_ids_$primaryId")
                        .remove("nagging_group_names_$primaryId")
                        .remove("nagging_group_trigger_$primaryId")
                        .remove("nagging_group_count_$primaryId")
                        .remove("nagging_group_sched_$primaryId")
                        .apply()
                    if (primaryId != entryId) {
                        val pIntent = PendingIntent.getBroadcast(
                            context,
                            primaryId.hashCode() + 2000,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        alarmManager.cancel(pIntent)
                        pIntent.cancel()
                        val sIntent = PendingIntent.getActivity(
                            context,
                            primaryId.hashCode() + 2500,
                            Intent(context, MainActivity::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        sIntent.cancel()
                    }
                } else {
                    if (entryId == primaryId) {
                        val newPrimary = currentIds.first()
                        val editor = prefs.edit()
                        editor.remove("nagging_group_ids_$primaryId")
                        editor.remove("nagging_group_names_$primaryId")
                        editor.remove("nagging_group_trigger_$primaryId")
                        editor.remove("nagging_group_count_$primaryId")
                        editor.remove("nagging_group_sched_$primaryId")
                        currentIds.forEach { id ->
                            editor.putString("nagging_member_$id", newPrimary)
                        }
                        editor.putString("nagging_group_ids_$newPrimary", currentIds.joinToString(","))
                        editor.putString("nagging_group_names_$newPrimary", currentNames.joinToString("|||"))
                        editor.putLong("nagging_group_trigger_$newPrimary", triggerTime)
                        editor.putInt("nagging_group_count_$newPrimary", count)
                        editor.putString("nagging_group_sched_$newPrimary", schedTime)
                        editor.apply()

                        if (triggerTime > System.currentTimeMillis()) {
                            val remMins = ((triggerTime - System.currentTimeMillis()) / (60 * 1000)).toInt().coerceAtLeast(1)
                            val summaryName = if (currentNames.isNotEmpty()) currentNames.joinToString(", ") else "Medicine"
                            scheduleNaggingReminder(
                                entryId = newPrimary,
                                minutes = remMins,
                                medicineName = summaryName,
                                naggingCount = count,
                                entryIds = ArrayList(currentIds),
                                medicineNames = ArrayList(currentNames),
                                scheduledTime = schedTime,
                                explicitTriggerTime = triggerTime
                            )
                        }
                    } else {
                        prefs.edit()
                            .putString("nagging_group_ids_$primaryId", currentIds.joinToString(","))
                            .putString("nagging_group_names_$primaryId", currentNames.joinToString("|||"))
                            .apply()

                        val summaryName = if (currentNames.isNotEmpty()) currentNames.joinToString(", ") else "Medicine"
                        val updateIntent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                            putExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID, primaryId)
                            putExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME, summaryName)
                            putExtra(MedicineAlarmReceiver.EXTRA_NAGGING_COUNT, count)
                            putStringArrayListExtra(MedicineAlarmReceiver.EXTRA_ENTRY_IDS, ArrayList(currentIds))
                            putStringArrayListExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAMES, ArrayList(currentNames))
                            if (schedTime.isNotBlank()) {
                                putExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME, schedTime)
                            }
                        }
                        PendingIntent.getBroadcast(
                            context,
                            primaryId.hashCode() + 2000,
                            updateIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error coordinating nagging cancellation", e)
        }

        Log.d(TAG, "Cancelled nagging reminder for entry: $entryId")
    }
}
