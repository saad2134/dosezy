/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.preferences.UserPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserPreferencesDataStoreTest {

    private lateinit var context: Context
    private lateinit var dataStore: UserPreferencesDataStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStore = UserPreferencesDataStore(context)
    }

    @Test
    fun testDefaultPreferences() = runTest {
        assertEquals("system", dataStore.themeFlow.first())
        assertEquals("SYSTEM", dataStore.languageFlow.first())
        assertFalse(dataStore.timelineModeFlow.first())
        assertFalse(dataStore.thickerCalendarFlow.first())
        assertTrue(dataStore.slideActionsFlow.first())
        assertFalse(dataStore.batteryPromptDismissedFlow.first())
        assertFalse(dataStore.overlayPromptDismissedFlow.first())
    }

    @Test
    fun testUpdatingPreferences() = runTest {
        dataStore.setTheme("dark")
        assertEquals("dark", dataStore.themeFlow.first())

        dataStore.setLanguage("ARABIC")
        assertEquals("ARABIC", dataStore.languageFlow.first())

        dataStore.setTimelineMode(true)
        assertTrue(dataStore.timelineModeFlow.first())

        dataStore.setThickerCalendar(true)
        assertTrue(dataStore.thickerCalendarFlow.first())

        dataStore.setSlideActions(false)
        assertFalse(dataStore.slideActionsFlow.first())

        dataStore.setBatteryPromptDismissed(true)
        assertTrue(dataStore.batteryPromptDismissedFlow.first())

        dataStore.setOverlayPromptDismissed(true)
        assertTrue(dataStore.overlayPromptDismissedFlow.first())
    }
}
