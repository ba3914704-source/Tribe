package com.example.core.security

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tribe_session_prefs")

class SessionManager(private val context: Context) {

    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_AVATAR_URL = stringPreferencesKey("avatar_url")
        private val KEY_IS_GUEST = booleanPreferencesKey("is_guest")
    }

    val sessionFlow: Flow<UserSession?> = context.dataStore.data.map { prefs ->
        val token = prefs[KEY_ACCESS_TOKEN]
        val userId = prefs[KEY_USER_ID]
        if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
            UserSession(
                userId = userId,
                email = prefs[KEY_USER_EMAIL] ?: "",
                fullName = prefs[KEY_USER_NAME] ?: "Traveler",
                accessToken = token,
                refreshToken = prefs[KEY_REFRESH_TOKEN],
                avatarUrl = prefs[KEY_AVATAR_URL]
            )
        } else {
            null
        }
    }

    val isGuestFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_GUEST] ?: false
    }

    suspend fun getAccessToken(): String? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_ACCESS_TOKEN]
    }

    suspend fun saveSession(session: UserSession) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = session.accessToken
            session.refreshToken?.let { prefs[KEY_REFRESH_TOKEN] = it }
            prefs[KEY_USER_ID] = session.userId
            prefs[KEY_USER_EMAIL] = session.email
            prefs[KEY_USER_NAME] = session.fullName
            session.avatarUrl?.let { prefs[KEY_AVATAR_URL] = it }
            prefs[KEY_IS_GUEST] = false
        }
    }

    suspend fun setGuestMode(isGuest: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_GUEST] = isGuest
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_EMAIL)
            prefs.remove(KEY_USER_NAME)
            prefs.remove(KEY_AVATAR_URL)
            prefs.remove(KEY_IS_GUEST)
        }
    }
}
