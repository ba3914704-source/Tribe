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
                    ?: return Result.failure(Exception("Supabase did not return a user for this session"))
                val accessToken = body.accessToken
                    ?.takeIf(String::isNotBlank)
                    ?: return Result.failure(Exception("Supabase did not return an access token"))
                val session = UserSession(
                    userId = user.id,
                    email = user.email ?: email,
                    fullName = (user.userMetadata?.get("full_name") as? String).orEmpty(),
                    accessToken = accessToken,
                    refreshToken = body.refreshToken,
                    avatarUrl = user.userMetadata?.get("avatar_url") as? String
                )
                sessionManager.saveSession(session)
                Result.success(session)
            } else {
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
                    ?: return Result.failure(Exception("Account created. Check your email to verify it before signing in."))
                val accessToken = body.accessToken
                    ?.takeIf(String::isNotBlank)
                    ?: return Result.failure(Exception("Account created. Check your email to verify it before signing in."))
                val session = UserSession(
                    userId = user.id,
                    email = user.email ?: email,
                    fullName = fullName.ifBlank { user.userMetadata?.get("full_name") as? String ?: "" },
                    accessToken = accessToken,
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
                    ?: return Result.failure(Exception("Supabase did not return a user for this session"))
                val accessToken = body.accessToken
                    ?.takeIf(String::isNotBlank)
                    ?: return Result.failure(Exception("Supabase did not return an access token"))
                val session = UserSession(
                    userId = user.id,
                    email = user.email.orEmpty(),
                    fullName = (user.userMetadata?.get("full_name") as? String).orEmpty(),
                    accessToken = accessToken,
                    refreshToken = body.refreshToken,
                    avatarUrl = user.userMetadata?.get("avatar_url") as? String
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
