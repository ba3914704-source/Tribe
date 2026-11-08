package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.formatCurrency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context matches TRIBE`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TRIBE", appName)
    }

    @Test
    fun `currency formatting produces correct Bangladeshi Taka format`() {
        val amount = BigDecimal("18500")
        val formatted = formatCurrency(amount)
        assertTrue(formatted.contains("৳"))
        assertTrue(formatted.contains("18,500"))
    }
}
