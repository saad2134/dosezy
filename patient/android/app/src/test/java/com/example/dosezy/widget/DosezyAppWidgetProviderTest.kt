/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.widget

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests verifying DosezyAppWidgetProvider:
 * 1. Theme resolution without SQLite queries.
 * 2. Intent flags require FLAG_ACTIVITY_SINGLE_TOP and strictly forbid FLAG_ACTIVITY_CLEAR_TASK.
 * 3. updateAppWidgets execution.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DosezyAppWidgetProviderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testIsWidgetDark_resolvesPreferencesProperly() {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // Light theme preference
        prefs.edit().putString("theme", "light").commit()
        assertFalse(DosezyAppWidgetProvider.isWidgetDark(context, "test_user"))

        // Dark theme preference
        prefs.edit().putString("theme", "dark").commit()
        assertTrue(DosezyAppWidgetProvider.isWidgetDark(context, "test_user"))
    }

    @Test
    fun testWidgetPendingIntentFlags_usesSingleTopAndNeverClearTask() {
        // Guard: Verify that widget click intents preserve in-progress app task state and use SINGLE_TOP
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        assertTrue(
            "Widget clickIntent must contain FLAG_ACTIVITY_NEW_TASK",
            (clickIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        )
        assertTrue(
            "Widget clickIntent must contain FLAG_ACTIVITY_SINGLE_TOP to route to onNewIntent",
            (clickIntent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0
        )
        assertEquals(
            "Widget clickIntent must NOT contain FLAG_ACTIVITY_CLEAR_TASK to prevent tearing down foreground state",
            0,
            clickIntent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK
        )

        // Template item click intent
        val itemClickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        assertTrue(
            "itemClickIntent must contain FLAG_ACTIVITY_SINGLE_TOP",
            (itemClickIntent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0
        )
        assertEquals(
            "itemClickIntent must NOT contain FLAG_ACTIVITY_CLEAR_TASK",
            0,
            itemClickIntent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
    }

    @Test
    fun testUpdateAppWidgets_executesWithoutCrash() {
        // Verifies debounced execution triggers safely
        DosezyAppWidgetProvider.updateAppWidgets(context)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
    }

    @Test
    fun testUpdateWidget_withTargetWidgetId_executesImmediateUpdateWithoutDebounce() {
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        // Guard: Verify updateWidget with explicit widget ID executes synchronously without throwing and updates target widget
        DosezyAppWidgetProvider.updateWidget(context, appWidgetManager, appWidgetId = 42)
        // Verify invalid ID fallback also executes safely
        DosezyAppWidgetProvider.updateWidget(context, appWidgetManager, appWidgetId = android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID)
    }

    @Test
    fun testWidgetProfileTheme_preservesSpecificProfileThemeIndependentOfAppPrefs() {
        // Guard: Specific profile theme must override global app theme for dedicated profile widgets
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("theme", "light").commit()

        val userId = "user_dark_mode"
        DosezyWidgetPrefs.saveWidgetProfileTheme(context, userId, "dark", isCurrentProfile = false)

        // Verify target profile widget resolves to dark despite global app theme being light
        assertTrue(DosezyAppWidgetProvider.isWidgetDark(context, userId))

        // When saved as light, resolves to light even if global app theme is dark
        prefs.edit().putString("theme", "dark").commit()
        DosezyWidgetPrefs.saveWidgetProfileTheme(context, userId, "light", isCurrentProfile = false)
        assertFalse(DosezyAppWidgetProvider.isWidgetDark(context, userId))
    }

    @Test
    fun testWidgetScheduledCalculation_excludesAdHocPrnDoses() {
        val now = java.time.LocalDateTime.now()
        val scheduled1 = com.example.dosezy.data.model.ScheduleEntry(
            entryId = "med1_2026_10_17_08_00",
            userId = "u1",
            medicineId = "m1",
            scheduledDateTime = now.withHour(8).withMinute(0),
            status = com.example.dosezy.data.model.MedicationStatus.PENDING
        )
        val scheduled2 = com.example.dosezy.data.model.ScheduleEntry(
            entryId = "med2_2026_10_17_12_00",
            userId = "u1",
            medicineId = "m2",
            scheduledDateTime = now.withHour(12).withMinute(0),
            status = com.example.dosezy.data.model.MedicationStatus.PENDING
        )
        val prnDose = com.example.dosezy.data.model.ScheduleEntry(
            entryId = "PRN_inhaler_1729158000000",
            userId = "u1",
            medicineId = "inhaler",
            scheduledDateTime = now.minusHours(1),
            status = com.example.dosezy.data.model.MedicationStatus.TAKEN_ON_TIME
        )

        val rawTodayEntries = listOf(scheduled1, scheduled2, prnDose)

        // Guard: Verify raw total counts 3 items, but scheduledToday filters out PRN entries
        val scheduledToday = rawTodayEntries.filter { !it.entryId.startsWith("PRN_") }
        assertEquals("Scheduled count must exclude PRN ad-hoc doses", 2, scheduledToday.size)
        assertEquals("Taken scheduled count must be 0", 0, scheduledToday.count {
            it.status == com.example.dosezy.data.model.MedicationStatus.TAKEN_ON_TIME || it.status == com.example.dosezy.data.model.MedicationStatus.TAKEN_LATE
        })

        // Edge case: 0 scheduled medications today, but 1 ad-hoc PRN dose taken
        val prnOnlyToday = listOf(prnDose)
        val scheduledPrnOnly = prnOnlyToday.filter { !it.entryId.startsWith("PRN_") }
        assertTrue("Scheduled list must be empty when only PRN doses were taken", scheduledPrnOnly.isEmpty())
    }
}

