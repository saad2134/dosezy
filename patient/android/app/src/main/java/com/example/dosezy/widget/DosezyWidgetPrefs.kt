/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.widget

import android.content.Context

/**
 * SharedPreferences storage managing the associated profile/user ID for each home screen widget instance.
 */
object DosezyWidgetPrefs {
    private const val PREFS_NAME = "com.example.dosezy.widget.DosezyWidgetPrefs"
    private const val PREF_PREFIX_KEY = "appwidget_user_"
    const val ACTIVE_PROFILE_ID = "ACTIVE_PROFILE"

    fun saveWidgetProfile(context: Context, appWidgetId: Int, userId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_PREFIX_KEY + appWidgetId, userId)
            .apply()
    }

    fun getWidgetProfile(context: Context, appWidgetId: Int): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_PREFIX_KEY + appWidgetId, ACTIVE_PROFILE_ID) ?: ACTIVE_PROFILE_ID
    }

    fun deleteWidgetProfile(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(PREF_PREFIX_KEY + appWidgetId)
            .apply()
    }

    fun saveWidgetProfileTheme(context: Context, userId: String, theme: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("profile_theme_" + userId, theme)
            .putString("profile_theme_" + ACTIVE_PROFILE_ID, theme)
            .apply()
    }

    fun getWidgetProfileTheme(context: Context, userId: String): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("profile_theme_" + userId, null)
    }

    // Guard: Remove orphaned widget profile theme entries when a profile is deleted to prevent shared preference bloat
    fun deleteWidgetProfileTheme(context: Context, userId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove("profile_theme_" + userId)
            .apply()
    }
}
