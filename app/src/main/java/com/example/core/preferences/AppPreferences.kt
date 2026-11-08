package com.example.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPrefsDataStore by preferencesDataStore(name = "tribe_app_preferences")

class AppPreferences(private val context: Context) {

    companion object {
        private val KEY_THEME = stringPreferencesKey("app_theme")
        private val KEY_LANGUAGE = stringPreferencesKey("app_language")
    }

    val themeModeFlow: Flow<ThemeMode> = context.appPrefsDataStore.data.map { prefs ->
        when (prefs[KEY_THEME]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val languageFlow: Flow<String> = context.appPrefsDataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: "en"
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[KEY_THEME] = mode.name
        }
    }

    suspend fun setLanguage(langCode: String) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = langCode
        }
    }
}
