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
        listOf(SharedPreferencesMigration(context, "app_prefs"))
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

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME] = theme
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = language
        }
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
}
