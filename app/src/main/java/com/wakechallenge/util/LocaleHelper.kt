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
        return runBlocking {
            try {
                val prefs = context.settingsDataStore.data.first()
                prefs[LANGUAGE_KEY] ?: LANGUAGE_SYSTEM
            } catch (e: Exception) {
                LANGUAGE_SYSTEM
            }
        }
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
