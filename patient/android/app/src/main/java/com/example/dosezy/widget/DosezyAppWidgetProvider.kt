package com.example.dosezy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.dosezy.MainActivity
import com.example.dosezy.R
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.MedicationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DosezyAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, DosezyAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, DosezyAppWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_dosezy)

            // Click to open MainActivity
            val clickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            views.setTextViewText(R.id.widget_date_text, dateFormat.format(System.currentTimeMillis()))
            
            // Push initial synchronous update so launcher immediately has a valid layout
            appWidgetManager.updateAppWidget(appWidgetId, views)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = DosezyDatabase.getInstance(context)
                    val users = db.userDao().getAllUsersDirect()
                    val user = users.find { it.isCurrentUser } ?: users.firstOrNull()

                    views.setTextViewText(R.id.widget_date_text, dateFormat.format(System.currentTimeMillis()))

                    if (user == null) {
                        views.setViewVisibility(R.id.widget_item_1, View.GONE)
                        views.setViewVisibility(R.id.widget_item_2, View.GONE)
                        views.setViewVisibility(R.id.widget_item_3, View.GONE)
                        views.setViewVisibility(R.id.widget_status_message, View.VISIBLE)
                        views.setTextViewText(R.id.widget_status_message, context.getString(R.string.widget_no_profile))
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        return@launch
                    }

                    val today = java.time.LocalDate.now()
                    val zoneId = java.time.ZoneId.systemDefault()
                    val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
                    val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
                    val next7Days = today.plusDays(7).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

                    val todayEntries = db.scheduleDao().getScheduleForDateRangeDirect(user.userId, startOfDay, endOfDay)
                    val todayPending = todayEntries.filter { it.status == MedicationStatus.PENDING }.sortedBy { it.scheduledDateTime }
                    val medicines = db.medicineDao().getMedicinesByUserDirect(user.userId).associateBy { it.medicineId }

                    val displayEntries: List<com.example.dosezy.data.model.ScheduleEntry>
                    if (todayPending.isNotEmpty()) {
                        displayEntries = todayPending
                    } else if (todayEntries.isNotEmpty() && todayEntries.any { it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE }) {
                        // All scheduled doses for today are completed
                        displayEntries = emptyList()
                    } else {
                        // No pending doses today (e.g. future medicine starting tomorrow or later)
                        val upcomingEntries = db.scheduleDao().getScheduleForDateRangeDirect(user.userId, startOfDay, next7Days)
                        displayEntries = upcomingEntries.filter {
                            it.status == MedicationStatus.PENDING && (it.scheduledDateTime.isAfter(java.time.LocalDateTime.now()) || it.scheduledDateTime.toLocalDate() == today)
                        }.sortedBy { it.scheduledDateTime }
                    }

                    if (displayEntries.isEmpty()) {
                        views.setViewVisibility(R.id.widget_item_1, View.GONE)
                        views.setViewVisibility(R.id.widget_item_2, View.GONE)
                        views.setViewVisibility(R.id.widget_item_3, View.GONE)
                        views.setViewVisibility(R.id.widget_status_message, View.VISIBLE)

                        if (todayEntries.isNotEmpty() && todayEntries.any { it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE }) {
                            views.setTextViewText(R.id.widget_status_message, context.getString(R.string.widget_all_taken))
                        } else {
                            views.setTextViewText(R.id.widget_status_message, context.getString(R.string.widget_no_meds))
                        }
                    } else {
                        views.setViewVisibility(R.id.widget_status_message, View.GONE)

                        fun formatTimeLabel(entry: com.example.dosezy.data.model.ScheduleEntry): String {
                            val itemDate = entry.scheduledDateTime.toLocalDate()
                            val dateObj = java.util.Date.from(entry.scheduledDateTime.atZone(zoneId).toInstant())
                            return when (itemDate) {
                                today -> timeFormat.format(dateObj)
                                today.plusDays(1) -> "Tmrw " + timeFormat.format(dateObj)
                                else -> {
                                    val dayName = itemDate.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
                                    "$dayName " + timeFormat.format(dateObj)
                                }
                            }
                        }

                        // Item 1
                        val item1 = displayEntries.getOrNull(0)
                        if (item1 != null) {
                            val med1 = medicines[item1.medicineId]
                            views.setViewVisibility(R.id.widget_item_1, View.VISIBLE)
                            views.setTextViewText(R.id.widget_item_1_time, formatTimeLabel(item1))
                            views.setTextViewText(R.id.widget_item_1_name, med1?.medicationName ?: "Medicine")
                            views.setTextViewText(R.id.widget_item_1_dose, if (med1 != null) "${med1.dosage.toInt()} ${med1.dosageUnit}" else "")
                        } else {
                            views.setViewVisibility(R.id.widget_item_1, View.GONE)
                        }

                        // Item 2
                        val item2 = displayEntries.getOrNull(1)
                        if (item2 != null) {
                            val med2 = medicines[item2.medicineId]
                            views.setViewVisibility(R.id.widget_item_2, View.VISIBLE)
                            views.setTextViewText(R.id.widget_item_2_time, formatTimeLabel(item2))
                            views.setTextViewText(R.id.widget_item_2_name, med2?.medicationName ?: "Medicine")
                            views.setTextViewText(R.id.widget_item_2_dose, if (med2 != null) "${med2.dosage.toInt()} ${med2.dosageUnit}" else "")
                        } else {
                            views.setViewVisibility(R.id.widget_item_2, View.GONE)
                        }

                        // Item 3
                        val item3 = displayEntries.getOrNull(2)
                        if (item3 != null) {
                            val med3 = medicines[item3.medicineId]
                            views.setViewVisibility(R.id.widget_item_3, View.VISIBLE)
                            views.setTextViewText(R.id.widget_item_3_time, formatTimeLabel(item3))
                            views.setTextViewText(R.id.widget_item_3_name, med3?.medicationName ?: "Medicine")
                            views.setTextViewText(R.id.widget_item_3_dose, if (med3 != null) "${med3.dosage.toInt()} ${med3.dosageUnit}" else "")
                        } else {
                            views.setViewVisibility(R.id.widget_item_3, View.GONE)
                        }
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    e.printStackTrace()
                    try {
                        views.setViewVisibility(R.id.widget_item_1, View.GONE)
                        views.setViewVisibility(R.id.widget_item_2, View.GONE)
                        views.setViewVisibility(R.id.widget_item_3, View.GONE)
                        views.setViewVisibility(R.id.widget_status_message, View.VISIBLE)
                        views.setTextViewText(R.id.widget_status_message, context.getString(R.string.widget_no_meds))
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
