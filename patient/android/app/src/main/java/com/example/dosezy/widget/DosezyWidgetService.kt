/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.dosezy.R
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.utils.LocaleHelper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class DosezyWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val isDark = intent.getBooleanExtra("is_dark", false)
        return DosezyRemoteViewsFactory(applicationContext, appWidgetId, isDark)
    }
}

class DosezyRemoteViewsFactory(
    private val context: Context,
    private val appWidgetId: Int,
    private var isDark: Boolean = false
) : RemoteViewsService.RemoteViewsFactory {

    data class WidgetItem(
        val entryId: String,
        val timeLabel: String,
        val medName: String,
        val doseLabel: String,
        val userId: String
    )

    private val items = mutableListOf<WidgetItem>()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        items.clear()
        try {
            val targetUserId = DosezyWidgetPrefs.getWidgetProfile(context, appWidgetId)
            isDark = DosezyAppWidgetProvider.isWidgetDark(context, targetUserId)
            kotlinx.coroutines.runBlocking {
                kotlinx.coroutines.withTimeoutOrNull(2500L) {
                    val db = DosezyDatabase.getInstance(context)
                    val users = db.userDao().getAllUsersDirect()
                    val user = if (targetUserId == DosezyWidgetPrefs.ACTIVE_PROFILE_ID) {
                        users.find { it.isCurrentUser } ?: users.firstOrNull()
                    } else {
                        users.find { it.userId == targetUserId } ?: users.find { it.isCurrentUser } ?: users.firstOrNull()
                    } ?: return@withTimeoutOrNull

                // Guard: Use user's selected locale and DateTimeFormatter directly to prevent timezone drift and incorrect language formatting on home widget
                val targetLocale = LocaleHelper.getLocale(user.language)
                val is24Hour = user.timeFormat == TimeFormat.HOUR_24
                val timePattern = if (is24Hour) "HH:mm" else "h:mm a"
                val userTimeFormatter = DateTimeFormatter.ofPattern(timePattern, targetLocale)

                val today = LocalDate.now()
                val zoneUtc = ZoneOffset.UTC
                val startOfDay = today.atStartOfDay(zoneUtc).toInstant().toEpochMilli()
                val endOfDay = today.plusDays(1).atStartOfDay(zoneUtc).toInstant().toEpochMilli() - 1
                val next7Days = today.plusDays(7).atStartOfDay(zoneUtc).toInstant().toEpochMilli() - 1

                val todayEntries = db.scheduleDao().getScheduleForDateRangeDirect(user.userId, startOfDay, endOfDay)
                val todayPending = todayEntries.filter { it.status == MedicationStatus.PENDING }.sortedBy { it.scheduledDateTime }
                val medicines = db.medicineDao().getMedicinesByUserDirect(user.userId).associateBy { it.medicineId }

                val displayEntries: List<ScheduleEntry>
                if (todayPending.isNotEmpty()) {
                    displayEntries = todayPending
                } else if (todayEntries.isNotEmpty()) {
                    displayEntries = emptyList()
                } else {
                    val upcomingEntries = db.scheduleDao().getScheduleForDateRangeDirect(user.userId, startOfDay, next7Days)
                    displayEntries = upcomingEntries.filter {
                        it.status == MedicationStatus.PENDING && (it.scheduledDateTime.isAfter(LocalDateTime.now()) || it.scheduledDateTime.toLocalDate() == today)
                    }.sortedBy { it.scheduledDateTime }
                }

                val tomorrowLabel = context.getString(R.string.widget_tomorrow)
                fun formatTimeLabel(entry: ScheduleEntry): String {
                    val itemDate = entry.scheduledDateTime.toLocalDate()
                    val formattedTime = entry.scheduledDateTime.format(userTimeFormatter)
                    return when (itemDate) {
                        today -> formattedTime
                        today.plusDays(1) -> "${tomorrowLabel.trim()} $formattedTime"
                        else -> {
                            val dayName = itemDate.dayOfWeek.getDisplayName(TextStyle.SHORT, targetLocale)
                            "$dayName $formattedTime"
                        }
                    }
                }

                displayEntries.forEach { entry ->
                    val med = medicines[entry.medicineId]
                    val doseStr = if (med != null) med.getDosageDisplay(entry.scheduledDateTime.toLocalTime()) else ""
                    items.add(
                        WidgetItem(
                            entryId = entry.entryId,
                            timeLabel = formatTimeLabel(entry),
                            medName = med?.medicationName ?: context.getString(R.string.nav_medicines),
                            doseLabel = doseStr,
                            userId = user.userId
                        )
                    )
                }
            }
        }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        items.clear()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position !in items.indices) return null
        val item = items[position]

        val layoutId = if (isDark) R.layout.widget_medicine_item_dark else R.layout.widget_medicine_item_light
        val views = RemoteViews(context.packageName, layoutId)
        views.setTextViewText(R.id.widget_item_time, item.timeLabel)
        views.setTextViewText(R.id.widget_item_name, item.medName)
        views.setTextViewText(R.id.widget_item_dose, item.doseLabel)

        // Fill-in intent for item click
        val fillInIntent = Intent().apply {
            putExtra("switch_to_user_id", item.userId)
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long = (position + if (isDark) 10000 else 0).toLong()

    override fun hasStableIds(): Boolean = false
}
