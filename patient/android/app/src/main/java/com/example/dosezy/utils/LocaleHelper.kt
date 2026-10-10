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

    private val supportedLanguageCodes = setOf("en", "es", "hi", "zh", "pt", "ar", "fr", "de", "ja", "ru", "it", "bn")

    /**
     * Gets the true device-level system locale from Android OS Resources,
     * immune to any app-level configuration or Locale.setDefault() mutations.
     */
    fun getSystemDefaultLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val locales = android.content.res.Resources.getSystem().configuration.locales
            if (!locales.isEmpty) locales.get(0) else Locale.getDefault()
        } else {
            @Suppress("DEPRECATION")
            android.content.res.Resources.getSystem().configuration.locale ?: Locale.getDefault()
        }
    }

    fun isSystemLanguageSupported(): Boolean {
        return isLanguageSupported(getSystemDefaultLocale().language)
    }

    private fun isLanguageSupported(langCode: String): Boolean {
        return supportedLanguageCodes.contains(langCode.lowercase(Locale.ROOT))
    }

    fun getLocale(language: Language): Locale {
        return when (language) {
            Language.SYSTEM -> {
                val sysLocale = getSystemDefaultLocale()
                if (isLanguageSupported(sysLocale.language)) {
                    sysLocale
                } else {
                    // Fallback to English for date/time formatting when system language is unsupported
                    Locale.ENGLISH
                }
            }
            // Guard: Locale(String) constructor deprecated in Java 19+; Locale.forLanguageTag is supported on API 21+ across all Android targets
            Language.ENGLISH -> Locale.forLanguageTag("en")
            Language.SPANISH -> Locale.forLanguageTag("es")
            Language.HINDI -> Locale.forLanguageTag("hi")
            Language.CHINESE -> Locale.forLanguageTag("zh")
            Language.PORTUGUESE -> Locale.forLanguageTag("pt")
            Language.ARABIC -> Locale.forLanguageTag("ar")
            Language.FRENCH -> Locale.forLanguageTag("fr")
            Language.GERMAN -> Locale.forLanguageTag("de")
            Language.JAPANESE -> Locale.forLanguageTag("ja")
            Language.RUSSIAN -> Locale.forLanguageTag("ru")
            Language.ITALIAN -> Locale.forLanguageTag("it")
            Language.BENGALI -> Locale.forLanguageTag("bn")
        }
    }

    fun getSystemLanguageDisplayName(): String {
        val sysLocale = getSystemDefaultLocale()
        val langName = sysLocale.getDisplayLanguage(Locale.ENGLISH).replaceFirstChar { it.titlecase(Locale.ROOT) }
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

    fun getCurrentLocale(context: Context): Locale {
        return getLocale(getSavedLanguage(context))
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
            if (language != Language.SYSTEM) {
                Locale.setDefault(targetLocale)
            } else {
                Locale.setDefault(getSystemDefaultLocale())
            }

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

            try {
                @Suppress("DEPRECATION")
                context.applicationContext.resources.updateConfiguration(config, context.applicationContext.resources.displayMetrics)
            } catch (_: Throwable) {}

            val activity = findActivity(context)
            if (activity != null) {
                try {
                    @Suppress("DEPRECATION")
                    activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
                } catch (_: Throwable) {}
            }

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

            // On Android 13+ (API 33+), AppCompatDelegate / LocaleManager natively handles activity recreation.
            // Calling activity.recreate() on top of setApplicationLocales causes a double-destroy race condition that kicks the user out of the app.
            if (forceRecreate && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
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

    fun getBaseContext(context: Context): Context {
        var ctx = context
        while (ctx is LocalizedContextWrapper) {
            ctx = ctx.baseContext
        }
        return ctx
    }

    fun updateContextLocale(context: Context, language: Language): Context {
        return try {
            val base = getBaseContext(context)
            val locale = getLocale(language)
            if (language != Language.SYSTEM) {
                Locale.setDefault(locale)
            } else {
                Locale.setDefault(getSystemDefaultLocale())
            }

            val config = Configuration(base.resources.configuration)
            config.setLocale(locale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocales(LocaleList(locale))
            }
            config.setLayoutDirection(locale)
            val configurationContext = base.createConfigurationContext(config)
            LocalizedContextWrapper(base, configurationContext)
        } catch (e: Throwable) {
            android.util.Log.e("LocaleHelper", "Failed to updateContextLocale for $language", e)
            context
        }
    }

    fun findActivity(context: Context): Activity? {
        var currentContext: Context? = context
        while (currentContext != null) {
            if (currentContext is Activity) {
                return currentContext
            }
            if (currentContext is ContextWrapper) {
                currentContext = currentContext.baseContext
            } else {
                break
            }
        }
        return null
    }
}

class LocalizedContextWrapper(
    base: Context,
    private val localizedContext: Context
) : ContextWrapper(base) {
    override fun getResources(): android.content.res.Resources = localizedContext.resources
    override fun getAssets(): android.content.res.AssetManager = localizedContext.assets
    override fun getApplicationContext(): Context = baseContext.applicationContext
}
