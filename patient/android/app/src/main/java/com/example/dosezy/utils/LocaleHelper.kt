package com.example.dosezy.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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

    fun getSavedLanguage(context: Context): Language {
        return try {
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val langName = prefs.getString("selected_language", Language.SYSTEM.name) ?: Language.SYSTEM.name
            Language.valueOf(langName)
        } catch (e: Exception) {
            Language.SYSTEM
        }
    }

    fun applyLanguage(context: Context, language: Language, forceRecreate: Boolean = false) {
        try {
            // Persist selection so attachBaseContext uses it on subsequent launches
            try {
                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("selected_language", language.name)
                    .apply()
            } catch (_: Throwable) {}

            val targetLocale = getLocale(language)
            Locale.setDefault(targetLocale)

            val resources = context.resources
            val config = Configuration(resources.configuration)
            config.setLocale(targetLocale)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val localeList = LocaleList(targetLocale)
                LocaleList.setDefault(localeList)
                config.setLocales(localeList)
            }
            config.setLayoutDirection(targetLocale)

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

            // MainActivity is a ComponentActivity (not AppCompatActivity) and does not automatically recreate
            // on AppCompatDelegate locale changes. We explicitly recreate it on user-triggered language changes
            // so Compose resets string caches and flips layout direction immediately.
            if (forceRecreate) {
                val activity = context.findActivity()
                if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            activity.recreate()
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("LocaleHelper", "Error applying language $language, falling back to System default", e)
            try {
                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("selected_language", Language.SYSTEM.name)
                    .apply()
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            } catch (_: Throwable) {}
        }
    }

    fun updateContextLocale(context: Context, language: Language): Context {
        return try {
            val locale = getLocale(language)
            Locale.setDefault(locale)

            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocales(LocaleList(locale))
            }
            config.setLayoutDirection(locale)
            context.createConfigurationContext(config)
        } catch (e: Throwable) {
            android.util.Log.e("LocaleHelper", "Failed to updateContextLocale for $language", e)
            context
        }
    }

    private fun Context.findActivity(): Activity? {
        var currentContext = this
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }
}
