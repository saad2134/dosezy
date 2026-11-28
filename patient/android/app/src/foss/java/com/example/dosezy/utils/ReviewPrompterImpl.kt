/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.utils

import android.app.Activity
import android.content.Context

/**
 * FOSS distribution review prompter (#146).
 *
 * Guard: Kept as a silent no-op to strictly comply with F-Droid inclusion policies
 * and ensure zero closed-source Google Play SDKs exist in the compiled DEX bytecode.
 */
class ReviewPrompterImpl(private val context: Context) : ReviewPrompter {
    override fun maybePromptReview(activity: Activity, triggerReason: String) {
        // No-op for FOSS / F-Droid builds
    }
}

