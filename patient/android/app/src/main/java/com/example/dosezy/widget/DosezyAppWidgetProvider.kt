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

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            DosezyWidgetPrefs.deleteWidgetProfile(context, appWidgetId)
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
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_medicine_list)
            }
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val targetUserId = DosezyWidgetPrefs.getWidgetProfile(context, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_dosezy)

            // Click header to open MainActivity (switches to bound profile if specific)
            val clickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                if (targetUserId != DosezyWidgetPrefs.ACTIVE_PROFILE_ID) {
                    putExtra("switch_to_user_id", targetUserId)
                }
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // Connect scrollable ListView to DosezyWidgetService
            val serviceIntent = Intent(context, DosezyWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_medicine_list, serviceIntent)
            views.setEmptyView(R.id.widget_medicine_list, R.id.widget_status_container)

            // Template PendingIntent for items in the scrollable ListView
            val itemClickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val itemPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 100000,
                itemClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_medicine_list, itemPendingIntent)

            val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            views.setTextViewText(R.id.widget_date_text, dateFormat.format(System.currentTimeMillis()))
            views.setTextViewText(R.id.widget_title_text, context.getString(R.string.app_name))
            
            // Push initial synchronous update so launcher immediately has a valid layout
            appWidgetManager.updateAppWidget(appWidgetId, views)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = DosezyDatabase.getInstance(context)
                    val users = db.userDao().getAllUsersDirect()
                    val user = if (targetUserId == DosezyWidgetPrefs.ACTIVE_PROFILE_ID) {
                        users.find { it.isCurrentUser } ?: users.firstOrNull()
                    } else {
                        users.find { it.userId == targetUserId } ?: users.find { it.isCurrentUser } ?: users.firstOrNull()
                    }

                    val appName = context.getString(R.string.app_name)
                    val titleText = if (user != null && user.fullName.isNotBlank()) {
                        "$appName • ${user.fullName}"
                    } else {
                        appName
                    }
                    views.setTextViewText(R.id.widget_title_text, titleText)
                    views.setTextViewText(R.id.widget_date_text, dateFormat.format(System.currentTimeMillis()))

                    if (user == null) {
                        views.setViewVisibility(R.id.widget_status_container, View.VISIBLE)
                        views.setImageViewResource(R.id.widget_status_icon, R.drawable.ic_widget_medication)
                        views.setTextViewText(R.id.widget_status_message, context.getString(R.string.widget_no_profile))
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        return@launch
                    }

                    val today = java.time.LocalDate.now()
                    val zoneUtc = java.time.ZoneOffset.UTC
                    val startOfDay = today.atStartOfDay(zoneUtc).toInstant().toEpochMilli()
                    val endOfDay = today.plusDays(1).atStartOfDay(zoneUtc).toInstant().toEpochMilli() - 1

                    val todayEntries = db.scheduleDao().getScheduleForDateRangeDirect(user.userId, startOfDay, endOfDay)
                    val totalToday = todayEntries.size
                    val takenToday = todayEntries.count { it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE }
                    val skippedToday = todayEntries.count { it.status == MedicationStatus.SKIPPED }

                    if (todayEntries.isNotEmpty()) {
                        when {
                            takenToday == totalToday -> {
                                views.setImageViewResource(R.id.widget_status_icon, R.drawable.ic_widget_check_circle)
                                views.setTextViewText(R.id.widget_status_message, context.getString(R.string.widget_all_taken))
                            }
                            takenToday + skippedToday == totalToday -> {
                                views.setImageViewResource(R.id.widget_status_icon, R.drawable.ic_widget_check_circle)
                                views.setTextViewText(R.id.widget_status_message, context.getString(R.string.widget_all_completed))
                            }
                            else -> {
                                views.setImageViewResource(R.id.widget_status_icon, R.drawable.ic_widget_medication)
                                views.setTextViewText(R.id.widget_status_message, context.getString(R.string.widget_taken_summary, takenToday, totalToday))
                            }
                        }
                    } else {
                        views.setImageViewResource(R.id.widget_status_icon, R.drawable.ic_widget_medication)
                        views.setTextViewText(R.id.widget_status_message, context.getString(R.string.widget_no_meds))
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_medicine_list)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
