package com.example

import com.example.domain.model.ExpenseSummary
import com.example.domain.model.formatCurrency
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.Base64

class ExampleUnitTest {

    @Test
    fun `exact decimal arithmetic with BigDecimal for expense calculations`() {
        val expense1 = BigDecimal("5000.00")
        val expense2 = BigDecimal("1200.50")
        val expense3 = BigDecimal("2000.00")

        val total = expense1.add(expense2).add(expense3)
        assertEquals(BigDecimal("8200.50"), total)

        // Split among 6 members
        val memberCount = BigDecimal(6)
        val share = total.divide(memberCount, 2, RoundingMode.HALF_EVEN)
        assertEquals(BigDecimal("1366.75"), share)

        val summary = ExpenseSummary(
            totalExpense = total,
            yourShare = share,
            yourTotalPaid = expense2,
            netBalance = expense2.subtract(share)
        )

        assertEquals("৳ 8,200.5", summary.formattedTotalExpense)
        assertEquals("৳ 1,366.75", summary.formattedYourShare)
    }

    @Test
    fun `nonce generation and SHA-256 hash validation for Google OAuth`() {
        val rawBytes = ByteArray(32) { 1 }
        val rawNonce = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes)
        assertNotNull(rawNonce)
        assertTrue(rawNonce.isNotEmpty())

        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest(rawNonce.toByteArray())
        val hashedNonce = hashedBytes.joinToString("") { "%02x".format(it) }

        assertEquals(64, hashedNonce.length)
    }

    @Test
    fun `GoogleAuthException types provide appropriate error messages`() {
        val cancelled = com.example.core.security.GoogleAuthException.Cancelled()
        assertEquals("Google Sign-In was cancelled by user", cancelled.message)

        val noCreds = com.example.core.security.GoogleAuthException.NoCredentials()
        assertTrue(noCreds.message?.contains("No Google account found") == true)

        val configErr = com.example.core.security.GoogleAuthException.ConfigurationError("Web Client ID invalid")
        assertEquals("Web Client ID invalid", configErr.message)

        val result = com.example.core.security.GoogleSignInResult("test_token", "test_raw_nonce")
        assertEquals("test_token", result.idToken)
        assertEquals("test_raw_nonce", result.rawNonce)
    }
}
