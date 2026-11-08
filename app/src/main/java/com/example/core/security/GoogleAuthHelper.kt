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
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class GoogleSignInResult(
    val idToken: String,
    val rawNonce: String
)

sealed class GoogleAuthException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Cancelled(message: String = "Google Sign-In was cancelled by user") : GoogleAuthException(message)
    class NoCredentials(message: String = "No Google account found or credential unavailable on device. Please ensure a Google account is added on your device.") : GoogleAuthException(message)
    class ConfigurationError(message: String, cause: Throwable? = null) : GoogleAuthException(message, cause)
    class Other(message: String, cause: Throwable? = null) : GoogleAuthException(message, cause)
}

class GoogleAuthHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    /**
     * Generates a cryptographically secure raw nonce and its corresponding SHA-256 hash.
     * The SHA-256 hash is passed to Google Credential Manager, and the raw nonce is sent
     * to Supabase Auth to verify token integrity.
     */
    fun generateNonce(): Pair<String, String> {
        val rawNonceBytes = ByteArray(32)
        SecureRandom().nextBytes(rawNonceBytes)
        val rawNonce = Base64.getUrlEncoder().withoutPadding().encodeToString(rawNonceBytes)

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawNonce.toByteArray())
        val hashedNonce = hashBytes.joinToString("") { "%02x".format(it) }

        return Pair(rawNonce, hashedNonce)
    }

    /**
     * Executes Google Sign-In with two-step credential resolution:
     * 1. Attempts with setFilterByAuthorizedAccounts(true) to silently check for previously authorized accounts.
     * 2. If NoCredentialException occurs, automatically retries with setFilterByAuthorizedAccounts(false)
     *    so new users can select any Google account available on their device.
     */
    suspend fun signInWithGoogle(
        activityContext: Context = context,
        serverClientId: String
    ): Result<GoogleSignInResult> {
        val trimmedClientId = serverClientId.trim()
        if (trimmedClientId.isBlank()) {
            return Result.failure(GoogleAuthException.ConfigurationError("Google Web Client ID is not configured"))
        }

        val (rawNonce, hashedNonce) = generateNonce()

        // Phase 1: Try with authorized accounts first
        val authorizedGoogleOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(trimmedClientId)
            .setAutoSelectEnabled(false)
            .setNonce(hashedNonce)
            .build()

        val firstRequest = GetCredentialRequest.Builder()
            .addCredentialOption(authorizedGoogleOption)
            .build()

        try {
            val response = credentialManager.getCredential(activityContext, firstRequest)
            return parseCredential(response.credential, rawNonce)
        } catch (e: GetCredentialCancellationException) {
            return Result.failure(GoogleAuthException.Cancelled())
        } catch (e: NoCredentialException) {
            // Authorized account not found; proceed immediately to Phase 2
        } catch (e: GetCredentialException) {
            val msg = e.message.orEmpty()
            if (!msg.contains("No credential", ignoreCase = true)) {
                // Return if it is another specific fatal failure
                if (e is GetCredentialCancellationException) {
                    return Result.failure(GoogleAuthException.Cancelled())
                }
            }
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            if (!msg.contains("No credential", ignoreCase = true)) {
                return Result.failure(GoogleAuthException.Other(msg.ifBlank { "Sign-in error occurred" }, e))
            }
        }

        // Phase 2: Automatically retry with setFilterByAuthorizedAccounts(false)
        val allAccountsGoogleOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(trimmedClientId)
            .setAutoSelectEnabled(false)
            .setNonce(hashedNonce)
            .build()

        val secondRequest = GetCredentialRequest.Builder()
            .addCredentialOption(allAccountsGoogleOption)
            .build()

        return try {
            val response = credentialManager.getCredential(activityContext, secondRequest)
            parseCredential(response.credential, rawNonce)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(GoogleAuthException.Cancelled())
        } catch (e: NoCredentialException) {
            Result.failure(GoogleAuthException.NoCredentials())
        } catch (e: GetCredentialException) {
            val msg = e.message.orEmpty()
            if (msg.contains("No credential", ignoreCase = true)) {
                Result.failure(GoogleAuthException.NoCredentials())
            } else {
                Result.failure(GoogleAuthException.Other(msg.ifBlank { "Google Sign-In failed" }, e))
            }
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            if (msg.contains("No credential", ignoreCase = true)) {
                Result.failure(GoogleAuthException.NoCredentials())
            } else {
                Result.failure(GoogleAuthException.Other(msg.ifBlank { "Authentication error" }, e))
            }
        }
    }

    private fun parseCredential(
        credential: Credential,
        rawNonce: String
    ): Result<GoogleSignInResult> {
        return when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    Result.success(
                        GoogleSignInResult(
                            idToken = googleIdTokenCredential.idToken,
                            rawNonce = rawNonce
                        )
                    )
                } else {
                    Result.failure(GoogleAuthException.Other("Unexpected credential type: ${credential.type}"))
                }
            }
            else -> Result.failure(GoogleAuthException.Other("Unrecognized credential class: ${credential.javaClass.simpleName}"))
        }
    }
}
