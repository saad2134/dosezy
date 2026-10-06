package com.example.dosezy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.example.dosezy.MainActivity
import com.example.dosezy.R
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.MedicationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

class DosezyAppWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_CONFIGURATION_CHANGED) {
            updateAppWidgets(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAppWidgets(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            DosezyWidgetPrefs.deleteWidgetProfile(context, appWidgetId)
        }
    }

    companion object {
        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var debounceJob: Job? = null

        /**
         * Resolves whether the widget should render in dark mode.
         * Strictly reads from SharedPreferences without any Room DB queries to ensure 0ms execution
         * and eliminate SQLite lock contention and ANRs.
         */
        fun isWidgetDark(context: Context, targetUserId: String): Boolean {
            val profileTheme = DosezyWidgetPrefs.getWidgetProfileTheme(context, targetUserId)
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val appTheme = prefs.getString("theme", "system")
            val effectiveTheme = profileTheme ?: appTheme ?: "system"

            return when (effectiveTheme.lowercase()) {
                "dark" -> true
                "light" -> false
                else -> {
                    val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    uiMode == Configuration.UI_MODE_NIGHT_YES
                }
            }
        }

        /**
         * Requests widget updates across all active widgets.
         * Debounces calls by 150ms to prevent duplicate IPC storms and SQLite lock exhaustion.
         */
        fun updateAppWidgets(context: Context) {
            val appContext = context.applicationContext
            synchronized(this) {
                debounceJob?.cancel()
                debounceJob = widgetScope.launch {
                    delay(150L) // Debounce rapid back-to-back updates
                    performUpdateAllWidgets(appContext)
                }
            }
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            updateAppWidgets(context)
        }

        private suspend fun performUpdateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, DosezyAppWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                if (appWidgetIds.isEmpty()) return

                val db = DosezyDatabase.getInstance(context)
                val allUsers = db.userDao().getAllUsersDirect()
                val currentUser = allUsers.find { it.isCurrentUser } ?: allUsers.firstOrNull()
                val userMap = allUsers.associateBy { it.userId }

                val today = LocalDate.now()
                val zoneUtc = ZoneOffset.UTC
                val startOfDay = today.atStartOfDay(zoneUtc).toInstant().toEpochMilli()
                val endOfDay = today.plusDays(1).atStartOfDay(zoneUtc).toInstant().toEpochMilli() - 1

                val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                val dateString = dateFormat.format(System.currentTimeMillis())
                val appName = context.getString(R.string.app_name)

                for (appWidgetId in appWidgetIds) {
                    val targetUserId = DosezyWidgetPrefs.getWidgetProfile(context, appWidgetId)
                    val user = if (targetUserId == DosezyWidgetPrefs.ACTIVE_PROFILE_ID) {
                        currentUser
                    } else {
                        userMap[targetUserId] ?: currentUser
                    }

                    val isDark = isWidgetDark(context, targetUserId)
                    val layoutId = if (isDark) R.layout.widget_dosezy_dark else R.layout.widget_dosezy_light
                    val views = RemoteViews(context.packageName, layoutId)

                    // Header click opens MainActivity (switches to bound profile if specific)
                    val clickIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        if (targetUserId != DosezyWidgetPrefs.ACTIVE_PROFILE_ID && user != null) {
                            putExtra("switch_to_user_id", user.userId)
                        }
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        clickIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                    // Connect scrollable ListView to DosezyWidgetService with theme in intent URI
                    val serviceIntent = Intent(context, DosezyWidgetService::class.java).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        putExtra("is_dark", isDark)
                        data = Uri.parse("dosezy://widget/service/$appWidgetId?dark=$isDark&ts=${System.currentTimeMillis()}")
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

                    // Set header title and date
                    val titleText = if (user != null && user.fullName.isNotBlank()) {
                        "$appName \u2022 ${user.fullName}"
                    } else {
                        appName
                    }
                    views.setTextViewText(R.id.widget_title_text, titleText)
                    views.setTextViewText(R.id.widget_date_text, dateString)

                    val defaultMedIcon = if (isDark) R.drawable.ic_widget_medication_dark else R.drawable.ic_widget_medication_light

                    if (user == null) {
                        views.setViewVisibility(R.id.widget_status_container, View.VISIBLE)
                        views.setImageViewResource(R.id.widget_status_icon, defaultMedIcon)
                        views.setTextViewText(R.id.widget_status_message, context.getString(R.string.widget_no_profile))
                    } else {
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
                                    views.setImageViewResource(R.id.widget_status_icon, defaultMedIcon)
                                    views.setTextViewText(R.id.widget_status_message, context.getString(R.string.widget_taken_summary, takenToday, totalToday))
                                }
                            }
                        } else {
                            views.setImageViewResource(R.id.widget_status_icon, defaultMedIcon)
                            views.setTextViewText(R.id.widget_status_message, context.getString(R.string.widget_no_meds))
                        }
                    }

                    // Push update ONCE per widget
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }

                // Batch notify ListView data changed ONCE for all widgets
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_medicine_list)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
