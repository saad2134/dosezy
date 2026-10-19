/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dosezy_preferences",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = "app_prefs",
                keysToMigrate = setOf("theme", "selected_language", "timeline_mode_enabled", "thicker_calendar_day_highlight", "slide_actions_enabled", "battery_prompt_dismissed", "overlay_prompt_dismissed")
            )
        )
    }
)

/**
 * Type-safe, reactive preferences store backed by Jetpack DataStore.
 * Automatically migrates existing legacy SharedPreferences keys from "app_prefs" without data loss.
 */
@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_LANGUAGE = stringPreferencesKey("selected_language")
        val KEY_TIMELINE_MODE = booleanPreferencesKey("timeline_mode_enabled")
        val KEY_THICKER_CALENDAR = booleanPreferencesKey("thicker_calendar_day_highlight")
        val KEY_SLIDE_ACTIONS = booleanPreferencesKey("slide_actions_enabled")
        val KEY_BATTERY_PROMPT_DISMISSED = booleanPreferencesKey("battery_prompt_dismissed")
        val KEY_OVERLAY_PROMPT_DISMISSED = booleanPreferencesKey("overlay_prompt_dismissed")
    }

    private val preferencesFlow: Flow<Preferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val themeFlow: Flow<String> = preferencesFlow.map { preferences ->
        preferences[KEY_THEME] ?: "system"
    }

    val languageFlow: Flow<String> = preferencesFlow.map { preferences ->
        preferences[KEY_LANGUAGE] ?: "SYSTEM"
    }

    val timelineModeFlow: Flow<Boolean> = preferencesFlow.map { preferences ->
        preferences[KEY_TIMELINE_MODE] ?: false
    }

    val thickerCalendarFlow: Flow<Boolean> = preferencesFlow.map { preferences ->
        preferences[KEY_THICKER_CALENDAR] ?: false
    }

    val slideActionsFlow: Flow<Boolean> = preferencesFlow.map { preferences ->
        preferences[KEY_SLIDE_ACTIONS] ?: true
    }

    val batteryPromptDismissedFlow: Flow<Boolean> = preferencesFlow.map { preferences ->
        preferences[KEY_BATTERY_PROMPT_DISMISSED] ?: false
    }

    val overlayPromptDismissedFlow: Flow<Boolean> = preferencesFlow.map { preferences ->
        preferences[KEY_OVERLAY_PROMPT_DISMISSED] ?: false
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME] = theme
        }
        // Guard: Mirror to legacy SharedPreferences for widgets and synchronous startup
        try {
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("theme", theme)
                .apply()
        } catch (_: Exception) {}
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = language
        }
        // Guard: Mirror to legacy SharedPreferences for synchronous LocaleHelper baseContext attachment
        try {
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("selected_language", language)
                .apply()
        } catch (_: Exception) {}
    }

    suspend fun setTimelineMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TIMELINE_MODE] = enabled
        }
    }

    suspend fun setThickerCalendar(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THICKER_CALENDAR] = enabled
        }
    }

    suspend fun setSlideActions(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SLIDE_ACTIONS] = enabled
        }
    }

    suspend fun setBatteryPromptDismissed(dismissed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BATTERY_PROMPT_DISMISSED] = dismissed
        }
        // Guard: Mirror to legacy SharedPreferences for backwards compatibility
        try {
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("battery_prompt_dismissed", dismissed)
                .apply()
        } catch (_: Exception) {}
    }

    suspend fun setOverlayPromptDismissed(dismissed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_OVERLAY_PROMPT_DISMISSED] = dismissed
        }
        // Guard: Mirror to legacy SharedPreferences for backwards compatibility
        try {
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("overlay_prompt_dismissed", dismissed)
                .apply()
        } catch (_: Exception) {}
    }
}
