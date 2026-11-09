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

    private fun AuthResponseDto.toValidSession(
        emailFallback: String? = null,
        nameFallback: String? = null
    ): UserSession {
        val authenticatedUser = user
            ?: throw IllegalStateException("Supabase returned no user for this authentication response.")
        val userId = authenticatedUser.id.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Supabase returned an invalid user ID.")
        val token = accessToken?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("No active session was returned. If email confirmation is enabled, check your inbox to verify your account, then sign in.")
        val resolvedEmail = authenticatedUser.email?.takeIf { it.isNotBlank() }
            ?: emailFallback?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Supabase did not return an email address.")
        val metadataName = authenticatedUser.userMetadata?.get("full_name") as? String
        val resolvedName = metadataName?.takeIf { it.isNotBlank() }
            ?: nameFallback?.takeIf { it.isNotBlank() }
            ?: resolvedEmail.substringBefore("@").ifBlank { "Traveler" }

        return UserSession(
            userId = userId,
            email = resolvedEmail,
            fullName = resolvedName,
            accessToken = token,
            refreshToken = refreshToken?.takeIf { it.isNotBlank() },
            avatarUrl = authenticatedUser.userMetadata?.get("avatar_url") as? String
        )
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<UserSession> {
        return try {
            val response = authApi.signInWithPassword(EmailPasswordRequest(email.trim(), password))
            if (!response.isSuccessful) {
                val errorMsg = response.errorBody()?.string()
                    ?: "Sign in failed (${response.code()}). Please check your credentials and try again."
                return Result.failure(Exception(errorMsg))
            }
            val body = response.body()
                ?: return Result.failure(Exception("Supabase returned an empty sign-in response."))
            val session = body.toValidSession(emailFallback = email)
            sessionManager.saveSession(session)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String, fullName: String): Result<UserSession> {
        return try {
            val response = authApi.signUpWithEmail(
                SignUpRequest(email = email.trim(), password = password, data = mapOf("full_name" to fullName.trim()))
            )
            if (!response.isSuccessful) {
                val errorMsg = response.errorBody()?.string()
                    ?: "Registration failed (${response.code()}). Please try again."
                return Result.failure(Exception(errorMsg))
            }
            val body = response.body()
                ?: return Result.failure(Exception("Supabase returned an empty registration response."))
            val session = body.toValidSession(emailFallback = email, nameFallback = fullName)
            sessionManager.saveSession(session)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogleToken(idToken: String, rawNonce: String): Result<UserSession> {
        return try {
            if (idToken.isBlank() || rawNonce.isBlank()) {
                return Result.failure(Exception("Google Sign-In returned an invalid token. Please try again."))
            }
            val response = authApi.signInWithIdToken(
                IdTokenRequest(provider = "google", idToken = idToken, nonce = rawNonce)
            )
            if (!response.isSuccessful) {
                val errorMsg = response.errorBody()?.string()
                    ?: "Google sign-in token exchange failed (${response.code()})."
                return Result.failure(Exception(errorMsg))
            }
            val body = response.body()
                ?: return Result.failure(Exception("Supabase returned an empty Google sign-in response."))
            val session = body.toValidSession()
            sessionManager.saveSession(session)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recoverPassword(email: String): Result<Unit> {
        return try {
            val response = authApi.recoverPassword(RecoverPasswordRequest(email.trim()))
            if (response.isSuccessful) Result.success(Unit)
            else {
                val errorMsg = response.errorBody()?.string()
                    ?: "Could not send the password recovery email (${response.code()})."
                Result.failure(Exception(errorMsg))
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
            // Always clear local credentials, including when the device is offline.
        } finally {
            sessionManager.clearSession()
        }
    }
}
