package com.example.data.repository

import com.example.core.network.SupabaseAuthApi
import com.example.core.security.SessionManager
import com.example.data.remote.dto.*
import com.example.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val sessionFlow: Flow<UserSession?>
    val isGuestFlow: Flow<Boolean>

    suspend fun signInWithEmail(email: String, password: String): Result<UserSession>
    suspend fun signUpWithEmail(email: String, password: String, fullName: String): Result<UserSession>
    suspend fun signInWithGoogleToken(idToken: String, rawNonce: String): Result<UserSession>
    suspend fun recoverPassword(email: String): Result<Unit>
    suspend fun continueAsGuest()
    suspend fun logout()
}

class SupabaseAuthRepository(
    private val authApi: SupabaseAuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {

    override val sessionFlow: Flow<UserSession?> = sessionManager.sessionFlow
    override val isGuestFlow: Flow<Boolean> = sessionManager.isGuestFlow

    override suspend fun signInWithEmail(email: String, password: String): Result<UserSession> {
        return try {
            val response = authApi.signInWithPassword(EmailPasswordRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val user = body.user
                val session = UserSession(
                    userId = user?.id ?: "user_default",
                    email = user?.email ?: email,
                    fullName = (user?.userMetadata?.get("full_name") as? String)
                        ?: email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    accessToken = body.accessToken ?: "",
                    refreshToken = body.refreshToken,
                    avatarUrl = user?.userMetadata?.get("avatar_url") as? String
                )
                sessionManager.saveSession(session)
                Result.success(session)
            } else {
                // If demo or backend response is error, handle or map error
                val errorMsg = response.errorBody()?.string() ?: "Authentication failed (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        fullName: String
    ): Result<UserSession> {
        return try {
            val response = authApi.signUpWithEmail(
                SignUpRequest(
                    email = email,
                    password = password,
                    data = mapOf("full_name" to fullName)
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val user = body.user
                val session = UserSession(
                    userId = user?.id ?: "user_new",
                    email = user?.email ?: email,
                    fullName = fullName.ifBlank { "Traveler" },
                    accessToken = body.accessToken ?: "local_token_${System.currentTimeMillis()}",
                    refreshToken = body.refreshToken,
                    avatarUrl = null
                )
                sessionManager.saveSession(session)
                Result.success(session)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Registration failed (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogleToken(
        idToken: String,
        rawNonce: String
    ): Result<UserSession> {
        return try {
            val response = authApi.signInWithIdToken(
                IdTokenRequest(
                    provider = "google",
                    idToken = idToken,
                    nonce = rawNonce
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val user = body.user
                val session = UserSession(
                    userId = user?.id ?: "google_user",
                    email = user?.email ?: "user@google.com",
                    fullName = (user?.userMetadata?.get("full_name") as? String) ?: "Google Traveler",
                    accessToken = body.accessToken ?: "",
                    refreshToken = body.refreshToken,
                    avatarUrl = user?.userMetadata?.get("avatar_url") as? String
                )
                sessionManager.saveSession(session)
                Result.success(session)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Google sign-in token exchange failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recoverPassword(email: String): Result<Unit> {
        return try {
            val response = authApi.recoverPassword(RecoverPasswordRequest(email))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Could not send recovery email"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun continueAsGuest() {
        sessionManager.setGuestMode(true)
    }

    override suspend fun logout() {
        try {
            authApi.logout()
        } catch (_: Exception) {
            // Ignore network errors on logout
        } finally {
            sessionManager.clearSession()
        }
    }
}
