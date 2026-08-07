package com.example.dosezy.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.dosezy.data.model.Language
import java.util.Locale

object LocaleHelper {

    private val systemDefaultLocale: Locale by lazy { Locale.getDefault() }
    private val supportedLanguageCodes = setOf("en", "es", "hi", "zh", "pt", "ar", "fr", "de", "ja", "ru", "it", "bn")

    fun isSystemLanguageSupported(): Boolean {
        return supportedLanguageCodes.contains(systemDefaultLocale.language.lowercase(Locale.ROOT))
    }

    fun getLocale(language: Language): Locale {
        return when (language) {
            Language.SYSTEM -> {
                if (isSystemLanguageSupported()) {
                    systemDefaultLocale
                } else {
                    // Fallback to English for date/time formatting when system language is unsupported
                    Locale.ENGLISH
                }
            }
            Language.ENGLISH -> Locale("en")
            Language.SPANISH -> Locale("es")
            Language.HINDI -> Locale("hi")
            Language.CHINESE -> Locale("zh")
            Language.PORTUGUESE -> Locale("pt")
            Language.ARABIC -> Locale("ar")
            Language.FRENCH -> Locale("fr")
            Language.GERMAN -> Locale("de")
            Language.JAPANESE -> Locale("ja")
            Language.RUSSIAN -> Locale("ru")
            Language.ITALIAN -> Locale("it")
            Language.BENGALI -> Locale("bn")
        }
    }

    fun getSystemLanguageDisplayName(): String {
        val langName = systemDefaultLocale.getDisplayLanguage(Locale.ENGLISH).replaceFirstChar { it.titlecase(Locale.ROOT) }
        return if (isSystemLanguageSupported()) {
            langName
        } else {
            "$langName - English Fallback"
        }
    }

    fun applyLanguage(context: Context, language: Language, forceRecreate: Boolean = false) {
        val targetLocale = getLocale(language)
        val currentLocale = context.resources.configuration.locales.get(0)

        Locale.setDefault(targetLocale)

        val resources = context.resources
        val config = Configuration(resources.configuration)
        config.setLocale(targetLocale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(targetLocale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
        }

        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        val languageTag = when (language) {
            Language.SYSTEM -> ""
            Language.ENGLISH -> "en"
            Language.SPANISH -> "es"
            Language.HINDI -> "hi"
            Language.CHINESE -> "zh"
            Language.PORTUGUESE -> "pt"
            Language.ARABIC -> "ar"
            Language.FRENCH -> "fr"
            Language.GERMAN -> "de"
            Language.JAPANESE -> "ja"
            Language.RUSSIAN -> "ru"
            Language.ITALIAN -> "it"
            Language.BENGALI -> "bn"
        }

        if (languageTag.isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
        }

        if (forceRecreate || (currentLocale.language != targetLocale.language)) {
            (context as? Activity)?.recreate()
        }
    }

    fun updateContextLocale(context: Context, language: Language): Context {
        val locale = getLocale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
