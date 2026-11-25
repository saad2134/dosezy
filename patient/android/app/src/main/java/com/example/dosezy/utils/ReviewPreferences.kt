/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.utils

import android.content.Context

/**
 * Manages engagement milestones and rate-limiting guardrails for in-app review prompts (#146).
 *
 * Enforces Google Play and OEM policy guidelines:
 * - Minimum engagement: At least 5 days since install and 10 doses logged.
 * - Cooldown: At least 30 days between successive review prompts.
 * - Lifetime ceiling: At most 2 review prompts ever presented to the user.
 */
object ReviewPreferences {
    const val PREFS_NAME = "review_prefs"
    const val KEY_FIRST_LAUNCH_TIME = "first_launch_time"
    const val KEY_COMPLETED_DOSES_COUNT = "completed_doses_count"
    const val KEY_LAST_PROMPT_TIME = "last_prompt_time"
    const val KEY_PROMPT_COUNT = "prompt_count"
    const val KEY_REVIEW_FINISHED = "review_finished"
    const val KEY_LAST_CELEBRATED_STREAK = "last_celebrated_streak"

    const val MIN_DAYS_BEFORE_FIRST_PROMPT = 5L
    const val MIN_COMPLETED_DOSES = 10
    const val COOLDOWN_DAYS_BETWEEN_PROMPTS = 30L
    const val MAX_LIFETIME_PROMPTS = 2

    fun recordAppLaunch(context: Context, currentTimeMillis: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_FIRST_LAUNCH_TIME)) {
            // Guard: Record initial install epoch timestamp on cold start to gate review prompt eligibility
            prefs.edit().putLong(KEY_FIRST_LAUNCH_TIME, currentTimeMillis).apply()
        }
    }

    fun incrementCompletedDoses(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_COMPLETED_DOSES_COUNT, 0)
        // Guard: Atomically increment intake count so review eligibility reflects real adherence activity
        prefs.edit().putInt(KEY_COMPLETED_DOSES_COUNT, current + 1).apply()
    }

    fun decrementCompletedDoses(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_COMPLETED_DOSES_COUNT, 0)
        if (current > 0) {
            // Guard: Decrement intake count on dose undo to maintain strict engagement metric integrity
            prefs.edit().putInt(KEY_COMPLETED_DOSES_COUNT, current - 1).apply()
        }
    }

    fun getCompletedDoses(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_COMPLETED_DOSES_COUNT, 0)
    }

    fun isEligibleForPrompt(context: Context, currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Guard: Abort if user has already completed review flow or permanently dismissed it
        if (prefs.getBoolean(KEY_REVIEW_FINISHED, false)) return false

        // Guard: Enforce maximum lifetime prompt ceiling (max 2 prompts) to prevent review fatigue
        val promptCount = prefs.getInt(KEY_PROMPT_COUNT, 0)
        if (promptCount >= MAX_LIFETIME_PROMPTS) return false

        // Guard: Require minimum 5 days since first app launch before triggering first review prompt
        val firstLaunchTime = prefs.getLong(KEY_FIRST_LAUNCH_TIME, currentTimeMillis)
        // Guard: If device clock was rolled back prior to first launch, synchronize firstLaunchTime to prevent permanent lockout (#BUG-REVIEW-CLOCK-ROLLBACK)
        if (firstLaunchTime > currentTimeMillis) {
            prefs.edit().putLong(KEY_FIRST_LAUNCH_TIME, currentTimeMillis).apply()
            return false
        }
        val daysSinceInstall = (currentTimeMillis - firstLaunchTime) / (1000L * 60 * 60 * 24)
        if (daysSinceInstall < MIN_DAYS_BEFORE_FIRST_PROMPT) return false

        // Guard: Require minimum 10 completed medication doses before asking for store review
        val completedDoses = prefs.getInt(KEY_COMPLETED_DOSES_COUNT, 0)
        if (completedDoses < MIN_COMPLETED_DOSES) return false

        // Guard: Enforce 30-day cooldown between successive prompt attempts
        val lastPromptTime = prefs.getLong(KEY_LAST_PROMPT_TIME, 0L)
        if (lastPromptTime > 0L) {
            // Guard: If device clock was rolled back after last prompt, enforce cooldown safely without underflow (#BUG-REVIEW-CLOCK-ROLLBACK)
            if (lastPromptTime > currentTimeMillis) {
                return false
            }
            val daysSinceLastPrompt = (currentTimeMillis - lastPromptTime) / (1000L * 60 * 60 * 24)
            if (daysSinceLastPrompt < COOLDOWN_DAYS_BETWEEN_PROMPTS) return false
        }

        return true
    }

    fun recordPromptShown(context: Context, currentTimeMillis: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_PROMPT_COUNT, 0) + 1
        val editor = prefs.edit()
            .putLong(KEY_LAST_PROMPT_TIME, currentTimeMillis)
            .putInt(KEY_PROMPT_COUNT, count)
        if (count >= MAX_LIFETIME_PROMPTS) {
            editor.putBoolean(KEY_REVIEW_FINISHED, true)
        }
        editor.apply()
    }

    fun markReviewFinished(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Guard: Mark review flow permanently completed so no further prompts occur
        prefs.edit().putBoolean(KEY_REVIEW_FINISHED, true).apply()
    }

    fun shouldCelebrateStreak(context: Context, streakDays: Int): Boolean {
        val milestone = when {
            streakDays >= 14 -> 14
            streakDays >= 7 -> 7
            else -> return false
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCelebrated = prefs.getInt(KEY_LAST_CELEBRATED_STREAK, 0)
        return milestone > lastCelebrated
    }

    fun recordStreakCelebrated(context: Context, streakDays: Int) {
        val milestone = when {
            streakDays >= 14 -> 14
            streakDays >= 7 -> 7
            else -> streakDays
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_LAST_CELEBRATED_STREAK, milestone).apply()
    }
}
