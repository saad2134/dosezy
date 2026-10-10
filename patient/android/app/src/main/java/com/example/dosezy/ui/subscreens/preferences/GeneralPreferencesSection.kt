/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.subscreens.preferences

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dosezy.R
import com.example.dosezy.data.model.Language
import com.example.dosezy.data.model.User
import com.example.dosezy.ui.components.PreferenceItem
import com.example.dosezy.utils.LocaleHelper

fun LazyListScope.generalPreferencesSection(
    currentUser: User?,
    onLanguageClick: () -> Unit
) {
    item {
        Text(
            text = stringResource(R.string.pref_section_general),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
        )
    }

    item {
        val sysLangName = LocaleHelper.getSystemLanguageDisplayName()
        PreferenceItem(
            title = stringResource(R.string.pref_language),
            currentValue = currentUser?.language?.let {
                when (it) {
                    Language.SYSTEM -> "${stringResource(R.string.system_default)} ($sysLangName)"
                    Language.ENGLISH -> "English"
                    Language.SPANISH -> "Español"
                    Language.HINDI -> "हिन्दी"
                    Language.CHINESE -> "中文"
                    Language.PORTUGUESE -> "Português"
                    Language.ARABIC -> "العربية"
                    Language.FRENCH -> "Français"
                    Language.GERMAN -> "Deutsch"
                    Language.JAPANESE -> "日本語"
                    Language.RUSSIAN -> "Русский"
                    Language.ITALIAN -> "Italiano"
                    Language.BENGALI -> "বাংলা"
                }
            } ?: "${stringResource(R.string.system_default)} ($sysLangName)",
            iconName = "language",
            onClick = onLanguageClick
        )
    }
}
