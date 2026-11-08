package com.example.core.security

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class GoogleSignInResult(val idToken: String, val rawNonce: String)

sealed class GoogleAuthException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Cancelled(message: String = "Google Sign-In was cancelled by user") : GoogleAuthException(message)
    class NoCredentials(message: String = "No Google account is available. Add an account in Android Settings or try again.") : GoogleAuthException(message)
    class ConfigurationError(message: String, cause: Throwable? = null) : GoogleAuthException(message, cause)
    class Other(message: String, cause: Throwable? = null) : GoogleAuthException(message, cause)
}

class GoogleAuthHelper(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    fun generateNonce(): Pair<String, String> {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        val rawNonce = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val digest = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray(Charsets.UTF_8))
        val hashedNonce = digest.joinToString("") { "%02x".format(it) }
        return rawNonce to hashedNonce
    }

    suspend fun signInWithGoogle(
        activityContext: Context = context,
        serverClientId: String
    ): Result<GoogleSignInResult> {
        val clientId = serverClientId.trim()
        if (clientId.isBlank()) {
            return Result.failure(GoogleAuthException.ConfigurationError("Google Web Client ID is not configured"))
        }
        val (rawNonce, hashedNonce) = generateNonce()

        // First: accounts previously authorized for this app.
        val authorizedOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .setNonce(hashedNonce)
            .build()

        try {
            val request = GetCredentialRequest.Builder().addCredentialOption(authorizedOption).build()
            val response = credentialManager.getCredential(activityContext, request)
            return parseCredential(response.credential, rawNonce)
        } catch (e: GetCredentialCancellationException) {
            return Result.failure(GoogleAuthException.Cancelled())
        } catch (_: NoCredentialException) {
            // Continue to the account chooser.
        } catch (e: GetCredentialException) {
            val message = e.message.orEmpty()
            if (!message.contains("no credential", ignoreCase = true)) {
                return Result.failure(GoogleAuthException.Other(message.ifBlank { "Google Sign-In failed" }, e))
            }
        }

        // Second: show all Google accounts, not only previously authorized accounts.
        val allAccountsOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .setNonce(hashedNonce)
            .build()
        try {
            val request = GetCredentialRequest.Builder().addCredentialOption(allAccountsOption).build()
            val response = credentialManager.getCredential(activityContext, request)
            return parseCredential(response.credential, rawNonce)
        } catch (e: GetCredentialCancellationException) {
            return Result.failure(GoogleAuthException.Cancelled())
        } catch (_: NoCredentialException) {
            // Some devices/providers require the explicit Sign in with Google button flow.
        } catch (e: GetCredentialException) {
            val message = e.message.orEmpty()
            if (!message.contains("no credential", ignoreCase = true)) {
                return Result.failure(GoogleAuthException.Other(message.ifBlank { "Google account selection failed" }, e))
            }
        }

        // Final fallback: dedicated Google button credential flow.
        return try {
            val signInOption = GetSignInWithGoogleOption.Builder(clientId)
                .setNonce(hashedNonce)
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
            val response = credentialManager.getCredential(activityContext, request)
            parseCredential(response.credential, rawNonce)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(GoogleAuthException.Cancelled())
        } catch (e: NoCredentialException) {
            Result.failure(GoogleAuthException.NoCredentials())
        } catch (e: GetCredentialException) {
            Result.failure(GoogleAuthException.Other(e.message ?: "Google Sign-In failed", e))
        } catch (e: Exception) {
            Result.failure(GoogleAuthException.Other(e.message ?: "Google Sign-In failed", e))
        }
    }

    private fun parseCredential(credential: Credential, rawNonce: String): Result<GoogleSignInResult> =
        try {
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                if (token.isBlank()) Result.failure(GoogleAuthException.Other("Google returned an empty ID token"))
                else Result.success(GoogleSignInResult(token, rawNonce))
            } else {
                Result.failure(GoogleAuthException.Other("Google returned an unsupported credential type"))
            }
        } catch (e: Exception) {
            Result.failure(GoogleAuthException.Other(e.message ?: "Could not read Google credential", e))
        }
}
