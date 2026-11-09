package com.example.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.dataStore by preferencesDataStore(name = "tribe_session_prefs")

class SessionManager(private val context: Context) {
    companion object {
        private const val KEY_ALIAS = "tribe_session_aes_key"
        private const val ENCRYPTED_PREFIX = "enc:"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"

        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_AVATAR_URL = stringPreferencesKey("avatar_url")
        private val KEY_IS_GUEST = booleanPreferencesKey("is_guest")
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        if (value.isBlank()) return value
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val payload = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, payload, 0, iv.size)
        System.arraycopy(ciphertext, 0, payload, iv.size, ciphertext.size)
        return ENCRYPTED_PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decryptOrLegacy(value: String?): String? {
        if (value.isNullOrBlank()) return value
        if (!value.startsWith(ENCRYPTED_PREFIX)) return value // Migrated by getAccessToken/saveSession.
        return try {
            val payload = Base64.decode(value.removePrefix(ENCRYPTED_PREFIX), Base64.NO_WRAP)
            require(payload.size > 12) { "Invalid encrypted session value" }
            val iv = payload.copyOfRange(0, 12)
            val ciphertext = payload.copyOfRange(12, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    val sessionFlow: Flow<UserSession?> = context.dataStore.data.map { prefs ->
        val token = decryptOrLegacy(prefs[KEY_ACCESS_TOKEN])
        val userId = prefs[KEY_USER_ID]
        if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
            UserSession(
                userId = userId,
                email = prefs[KEY_USER_EMAIL] ?: "",
                fullName = prefs[KEY_USER_NAME] ?: "Traveler",
                accessToken = token,
                refreshToken = decryptOrLegacy(prefs[KEY_REFRESH_TOKEN]),
                avatarUrl = prefs[KEY_AVATAR_URL]
            )
        } else null
    }

    val isGuestFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_GUEST] ?: false }

    suspend fun getAccessToken(): String? {
        val prefs = context.dataStore.data.first()
        val stored = prefs[KEY_ACCESS_TOKEN] ?: return null
        if (stored.startsWith(ENCRYPTED_PREFIX)) return decryptOrLegacy(stored)
        // One-time migration from the previous plaintext DataStore format.
        context.dataStore.edit { current ->
            current[KEY_ACCESS_TOKEN] = encrypt(stored)
            current[KEY_REFRESH_TOKEN]?.let { legacy ->
                if (!legacy.startsWith(ENCRYPTED_PREFIX)) current[KEY_REFRESH_TOKEN] = encrypt(legacy)
            }
        }
        return stored
    }

    suspend fun getRefreshToken(): String? {
        val prefs = context.dataStore.data.first()
        val stored = prefs[KEY_REFRESH_TOKEN] ?: return null
        if (stored.startsWith(ENCRYPTED_PREFIX)) return decryptOrLegacy(stored)
        context.dataStore.edit { it[KEY_REFRESH_TOKEN] = encrypt(stored) }
        return stored
    }

    suspend fun saveSession(session: UserSession) {
        require(session.userId.isNotBlank() && session.accessToken.isNotBlank()) {
            "A valid authenticated user ID and access token are required."
        }
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = encrypt(session.accessToken)
            if (!session.refreshToken.isNullOrBlank()) {
                prefs[KEY_REFRESH_TOKEN] = encrypt(session.refreshToken)
            } else {
                prefs.remove(KEY_REFRESH_TOKEN)
            }
            prefs[KEY_USER_ID] = session.userId
            prefs[KEY_USER_EMAIL] = session.email
            prefs[KEY_USER_NAME] = session.fullName
            if (!session.avatarUrl.isNullOrBlank()) prefs[KEY_AVATAR_URL] = session.avatarUrl
            else prefs.remove(KEY_AVATAR_URL)
            prefs[KEY_IS_GUEST] = false
        }
    }

    suspend fun setGuestMode(isGuest: Boolean) {
        context.dataStore.edit { it[KEY_IS_GUEST] = isGuest }
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
