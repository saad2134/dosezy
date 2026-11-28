/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.utils.ReviewPreferences
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
class ReviewPreferencesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(ReviewPreferences.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun testRecordAppLaunch_retainsFirstLaunchTimestamp() {
        val firstTime = 1000000L
        val secondTime = 2000000L

        ReviewPreferences.recordAppLaunch(context, firstTime)
        val prefs = context.getSharedPreferences(ReviewPreferences.PREFS_NAME, Context.MODE_PRIVATE)
        assertEquals(firstTime, prefs.getLong(ReviewPreferences.KEY_FIRST_LAUNCH_TIME, 0L))

        // Subsequent launches should not overwrite the original install timestamp
        ReviewPreferences.recordAppLaunch(context, secondTime)
        assertEquals(firstTime, prefs.getLong(ReviewPreferences.KEY_FIRST_LAUNCH_TIME, 0L))
    }

    @Test
    fun testCompletedDosesIncrementAndDecrement() {
        assertEquals(0, ReviewPreferences.getCompletedDoses(context))

        ReviewPreferences.incrementCompletedDoses(context)
        ReviewPreferences.incrementCompletedDoses(context)
        assertEquals(2, ReviewPreferences.getCompletedDoses(context))

        ReviewPreferences.decrementCompletedDoses(context)
        assertEquals(1, ReviewPreferences.getCompletedDoses(context))

        ReviewPreferences.decrementCompletedDoses(context)
        assertEquals(0, ReviewPreferences.getCompletedDoses(context))

        // Ensure decrementing does not underflow below zero
        ReviewPreferences.decrementCompletedDoses(context)
        assertEquals(0, ReviewPreferences.getCompletedDoses(context))
    }

    @Test
    fun testIsEligibleForPrompt_requiresMinimum5DaysAnd10Doses() {
        val baseTime = 10_000_000_000L
        val oneDayMillis = 24 * 60 * 60 * 1000L

        // Record launch on Day 0
        ReviewPreferences.recordAppLaunch(context, baseTime)

        // Day 0, 0 doses logged -> Not eligible
        assertFalse(ReviewPreferences.isEligibleForPrompt(context, baseTime))

        // Day 4 (under 5 days), log 15 doses -> Not eligible due to duration
        repeat(15) { ReviewPreferences.incrementCompletedDoses(context) }
        val day4Time = baseTime + (4 * oneDayMillis)
        assertFalse(ReviewPreferences.isEligibleForPrompt(context, day4Time))

        // Day 6 (exceeds 5 days), 15 doses -> Eligible!
        val day6Time = baseTime + (6 * oneDayMillis)
        assertTrue(ReviewPreferences.isEligibleForPrompt(context, day6Time))
    }

    @Test
    fun testIsEligibleForPrompt_enforcesCooldownAndMaxPromptLimit() {
        val baseTime = 10_000_000_000L
        val oneDayMillis = 24 * 60 * 60 * 1000L
        ReviewPreferences.recordAppLaunch(context, baseTime)
        repeat(12) { ReviewPreferences.incrementCompletedDoses(context) }

        val promptTime1 = baseTime + (6 * oneDayMillis)
        assertTrue(ReviewPreferences.isEligibleForPrompt(context, promptTime1))

        // Record first prompt
        ReviewPreferences.recordPromptShown(context, promptTime1)

        // Day 10 (4 days after first prompt) -> Ineligible due to 30-day cooldown
        val promptTimeTooSoon = promptTime1 + (4 * oneDayMillis)
        assertFalse(ReviewPreferences.isEligibleForPrompt(context, promptTimeTooSoon))

        // Day 38 (32 days after first prompt) -> Eligible for second prompt
        val promptTime2 = promptTime1 + (32 * oneDayMillis)
        assertTrue(ReviewPreferences.isEligibleForPrompt(context, promptTime2))

        // Record second prompt (reaching MAX_LIFETIME_PROMPTS = 2)
        ReviewPreferences.recordPromptShown(context, promptTime2)

        // Day 80 (40 days later) -> Ineligible permanently due to lifetime limit
        val promptTime3 = promptTime2 + (40 * oneDayMillis)
        assertFalse(ReviewPreferences.isEligibleForPrompt(context, promptTime3))
    }

    @Test
    fun testMarkReviewFinished_permanentlyDisablesPrompt() {
        val baseTime = 10_000_000_000L
        val oneDayMillis = 24 * 60 * 60 * 1000L
        ReviewPreferences.recordAppLaunch(context, baseTime)
        repeat(15) { ReviewPreferences.incrementCompletedDoses(context) }

        val eligibleTime = baseTime + (10 * oneDayMillis)
        assertTrue(ReviewPreferences.isEligibleForPrompt(context, eligibleTime))

        ReviewPreferences.markReviewFinished(context)
        assertFalse(ReviewPreferences.isEligibleForPrompt(context, eligibleTime))
    }

    @Test
    fun testStreakCelebrationMilestones() {
        // Less than 7 days -> Not celebrated
        assertFalse(ReviewPreferences.shouldCelebrateStreak(context, 5))
        assertFalse(ReviewPreferences.shouldCelebrateStreak(context, 6))

        // 7-day milestone
        assertTrue(ReviewPreferences.shouldCelebrateStreak(context, 7))
        ReviewPreferences.recordStreakCelebrated(context, 7)

        // After celebrating 7-day milestone, 7 through 13 do not re-celebrate
        assertFalse(ReviewPreferences.shouldCelebrateStreak(context, 7))
        assertFalse(ReviewPreferences.shouldCelebrateStreak(context, 10))
        assertFalse(ReviewPreferences.shouldCelebrateStreak(context, 13))

        // 14-day milestone
        assertTrue(ReviewPreferences.shouldCelebrateStreak(context, 14))
        ReviewPreferences.recordStreakCelebrated(context, 14)

        // After celebrating 14-day milestone, future days do not re-celebrate
        assertFalse(ReviewPreferences.shouldCelebrateStreak(context, 14))
        assertFalse(ReviewPreferences.shouldCelebrateStreak(context, 21))
    }

    @Test
    fun testIsEligibleForPrompt_clockRollback_recoversFromFutureLaunchTimestamp() {
        val currentTime = 10_000_000_000L
        val futureTime = currentTime + (100 * 24 * 60 * 60 * 1000L) // Clock was set 100 days in the future

        // 1. Initial launch recorded in future
        ReviewPreferences.recordAppLaunch(context, futureTime)
        repeat(15) { ReviewPreferences.incrementCompletedDoses(context) }

        // 2. User/NTP sync fixes clock back to currentTime (firstLaunchTime > currentTime)
        // Guard: Verify clock rollback resets firstLaunchTime and prevents permanent lockout (#BUG-REVIEW-CLOCK-ROLLBACK)
        assertFalse(ReviewPreferences.isEligibleForPrompt(context, currentTime))

        val prefs = context.getSharedPreferences(ReviewPreferences.PREFS_NAME, Context.MODE_PRIVATE)
        assertEquals(currentTime, prefs.getLong(ReviewPreferences.KEY_FIRST_LAUNCH_TIME, 0L))

        // 3. Advancing 6 days in real time should now become eligible
        val sixDaysLater = currentTime + (6 * 24 * 60 * 60 * 1000L)
        assertTrue(ReviewPreferences.isEligibleForPrompt(context, sixDaysLater))
    }

    @Test
    fun testIsEligibleForPrompt_clockRollback_handlesFutureLastPromptTime() {
        val baseTime = 10_000_000_000L
        val oneDayMillis = 24 * 60 * 60 * 1000L
        ReviewPreferences.recordAppLaunch(context, baseTime)
        repeat(15) { ReviewPreferences.incrementCompletedDoses(context) }

        // Future prompt time
        val futurePromptTime = baseTime + (50 * oneDayMillis)
        val prefs = context.getSharedPreferences(ReviewPreferences.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(ReviewPreferences.KEY_LAST_PROMPT_TIME, futurePromptTime).apply()

        // Current time is before last prompt time (clock rollback)
        val testTime = baseTime + (10 * oneDayMillis)
        assertFalse(ReviewPreferences.isEligibleForPrompt(context, testTime))
    }
}

