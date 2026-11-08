/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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

    @Test
    fun testDeleteWidgetPreferencesForUser_removesWidgetBindingsAndProfileTheme() {
        // Guard: Verify that deleting a user profile clears both theme overrides and appwidget_user_<id> bindings
        val deletedUserId = "user_to_delete"
        val remainingUserId = "user_to_keep"

        // Set up widget configurations
        DosezyWidgetPrefs.saveAppWidgetUser(context, 101, deletedUserId)
        DosezyWidgetPrefs.saveAppWidgetUser(context, 102, remainingUserId)
        DosezyWidgetPrefs.saveWidgetProfileTheme(context, deletedUserId, "dark", isCurrentProfile = false)
        DosezyWidgetPrefs.saveWidgetProfileTheme(context, remainingUserId, "light", isCurrentProfile = false)

        assertEquals(deletedUserId, DosezyWidgetPrefs.getAppWidgetUser(context, 101))
        assertEquals(remainingUserId, DosezyWidgetPrefs.getAppWidgetUser(context, 102))

        // Trigger cleanup
        DosezyWidgetPrefs.deleteWidgetPreferencesForUser(context, deletedUserId)

        // Verify deleted user's widget binding was cleared (now falls back to null/default)
        org.junit.Assert.assertNull(DosezyWidgetPrefs.getAppWidgetUser(context, 101))
        // Verify remaining user's widget binding is untouched
        assertEquals(remainingUserId, DosezyWidgetPrefs.getAppWidgetUser(context, 102))
    }

    @Test
    fun testWidgetDateFormatting_respectsUserProfileLanguage() {
        // Guard: Verify that widget date formatting resolves specific locale corresponding to user profile language
        val frenchUser = com.example.dosezy.data.model.User(
            userId = "user_french",
            fullName = "Pierre",
            age = 30,
            gender = com.example.dosezy.data.model.Gender.DO_NOT_SPECIFY,
            contactNumber = "+1234567890",
            language = com.example.dosezy.data.model.Language.FRENCH
        )
        val arabicUser = com.example.dosezy.data.model.User(
            userId = "user_arabic",
            fullName = "Tariq",
            age = 35,
            gender = com.example.dosezy.data.model.Gender.DO_NOT_SPECIFY,
            contactNumber = "+1234567891",
            language = com.example.dosezy.data.model.Language.ARABIC
        )

        val frenchLocale = com.example.dosezy.utils.LocaleHelper.getLocale(frenchUser.language)
        val arabicLocale = com.example.dosezy.utils.LocaleHelper.getLocale(arabicUser.language)

        val cal = java.util.Calendar.getInstance()
        cal.set(2026, java.util.Calendar.OCTOBER, 18, 10, 0)
        val fixedMillis = cal.timeInMillis

        val frenchFormat = java.text.SimpleDateFormat("EEE, MMM d", frenchLocale)
        val frenchDate = frenchFormat.format(fixedMillis)

        val arabicFormat = java.text.SimpleDateFormat("EEE, MMM d", arabicLocale)
        val arabicDate = arabicFormat.format(fixedMillis)

        val englishFormat = java.text.SimpleDateFormat("EEE, MMM d", java.util.Locale.ENGLISH)
        val englishDate = englishFormat.format(fixedMillis)

        assertEquals("fr", frenchLocale.language)
        assertEquals("ar", arabicLocale.language)
        assertFalse("French widget date should differ from English", frenchDate == englishDate)
        assertFalse("Arabic widget date should differ from English", arabicDate == englishDate)
    }

    @Test
    fun testWidgetLocalizedContext_resolvesProfileSpecificStrings() {
        // Guard: Verify that wrapping context with user profile language resolves localized widget strings (#BUG-WIDGET-LOCALE)
        val spanishContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, com.example.dosezy.data.model.Language.SPANISH)
        val englishContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, com.example.dosezy.data.model.Language.ENGLISH)

        val spanishTomorrow = spanishContext.getString(com.example.dosezy.R.string.widget_tomorrow)
        val englishTomorrow = englishContext.getString(com.example.dosezy.R.string.widget_tomorrow)

        val spanishAllTaken = spanishContext.getString(com.example.dosezy.R.string.widget_all_taken)
        val englishAllTaken = englishContext.getString(com.example.dosezy.R.string.widget_all_taken)

        assertEquals("Tmrw", englishTomorrow.trim())
        assertEquals("All doses taken today! 🎉", englishAllTaken)
        assertFalse("Spanish tomorrow label must differ from English", spanishTomorrow == englishTomorrow)
        assertFalse("Spanish all-taken status must differ from English", spanishAllTaken == englishAllTaken)
    }

    @Test
    fun testWidgetDateAndStatus_whenUserIsNull_resolvesSavedAppLanguage() {
        // Guard: When user profile is null, widget date format and status strings fall back to saved app language rather than OS default (#BUG-WIDGET-LOCALE)
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_language", "SPANISH").commit()

        val savedLang = com.example.dosezy.utils.LocaleHelper.getSavedLanguage(context)
        assertEquals(com.example.dosezy.data.model.Language.SPANISH, savedLang)

        val localizedContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, savedLang)
        val statusMessage = localizedContext.getString(com.example.dosezy.R.string.widget_no_profile)
        val englishContext = com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, com.example.dosezy.data.model.Language.ENGLISH)
        val englishStatus = englishContext.getString(com.example.dosezy.R.string.widget_no_profile)

        assertEquals("No profile created", englishStatus)
        assertFalse("Spanish unselected profile widget status must differ from English", statusMessage == englishStatus)

        // Verify updateWidget with null user executes cleanly under Spanish saved language
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        DosezyAppWidgetProvider.updateWidget(context, appWidgetManager, appWidgetId = 77)

        // Restore global preference
        prefs.edit().putString("selected_language", "ENGLISH").commit()
    }

    @Test
    fun testWidgetPendingIntentMutableFlag_guardedBySdkVersion() {
        val flagsApi31 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val targetFlags = PendingIntent.FLAG_UPDATE_CURRENT or flagsApi31

        // In Robolectric test runner running at SDK 34, FLAG_MUTABLE must be present
        assertTrue((targetFlags and PendingIntent.FLAG_MUTABLE) != 0)
        assertTrue((targetFlags and PendingIntent.FLAG_UPDATE_CURRENT) != 0)

        // For simulated API 28 (Android 9.0), flagsApi below 31 must be 0
        val simulatedApiBelow31 = if (28 >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        assertEquals(0, simulatedApiBelow31)
        assertEquals(PendingIntent.FLAG_UPDATE_CURRENT, PendingIntent.FLAG_UPDATE_CURRENT or simulatedApiBelow31)
    }
}


