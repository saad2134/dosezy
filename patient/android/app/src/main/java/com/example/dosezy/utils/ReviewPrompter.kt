/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.utils

import android.app.Activity

/**
 * Common contract for presenting in-app review prompts across store distributions (#146).
 *
 * Implemented flavor-specifically:
 * - Play flavor: Uses Google Play In-App Review API with multi-store (Galaxy Store, Amazon) fallback.
 * - FOSS flavor: Zero-dependency no-op to maintain full F-Droid privacy compliance.
 */
interface ReviewPrompter {
    fun maybePromptReview(activity: Activity, triggerReason: String)
}

