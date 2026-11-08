/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.data.model

import androidx.annotation.StringRes
import com.example.dosezy.R

/**
 * Defines the reminder alert delivery mechanism:
 * - ALARM: Full-screen alarm activity, continuous sound stream, bypasses Do Not Disturb (#111)
 * - NOTIFICATION: Standard notification shade alert with chime, respects Do Not Disturb (#111)
 */
enum class ReminderType(@StringRes val titleRes: Int, @StringRes val descRes: Int) {
    ALARM(R.string.reminder_type_alarm, R.string.reminder_type_alarm_desc),
    NOTIFICATION(R.string.reminder_type_notification, R.string.reminder_type_notification_desc)
}
