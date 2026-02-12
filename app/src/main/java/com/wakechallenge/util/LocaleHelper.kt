package com.wakechallenge.util

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wakechallenge.presentation.settings.settingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

object LocaleHelper {
    val LANGUAGE_KEY = stringPreferencesKey("app_language")

    const val LANGUAGE_SYSTEM = "system"
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_THAI = "th"

    fun getPersistedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString(LANGUAGE_KEY.name, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
    }

    fun persistLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(LANGUAGE_KEY.name, language).apply()
    }

    fun setLocale(context: Context, languageCode: String): Context {
        val locale = when (languageCode) {
            LANGUAGE_ENGLISH -> Locale.ENGLISH
            LANGUAGE_THAI -> Locale("th", "TH")
            else -> Locale.getDefault()
        }

        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    fun wrapContext(context: Context): Context {
        val language = getPersistedLanguage(context)
        return if (language == LANGUAGE_SYSTEM) {
            context
        } else {
            setLocale(context, language)
        }
    }
}
