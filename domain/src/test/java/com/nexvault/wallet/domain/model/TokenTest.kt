package com.nexvault.wallet.domain.model

import com.nexvault.wallet.domain.model.token.Token
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class TokenTest {
    @Test
    fun testNativeTokenIsNative() {
        val token = Token(
            contractAddress = "native",
            chainId = 1,
            symbol = "ETH",
            name = "Ethereum",
            decimals = 18,
            logoUrl = null,
            balance = BigDecimal("1.0"),
            fiatPrice = null,
            fiatValue = null,
            priceChange24h = null,
        )
        assertTrue(token.isNative)
    }

    @Test
    fun testErc20TokenIsNotNative() {
        val token = Token(
            contractAddress = "0x1234567890123456789012345678901234567890",
            chainId = 1,
            symbol = "USDC",
            name = "USD Coin",
            decimals = 6,
            logoUrl = null,
            balance = BigDecimal("100.0"),
            fiatPrice = null,
            fiatValue = null,
            priceChange24h = null,
        )
        assertFalse(token.isNative)
    }

    @Test
    fun testNativeTokenAddressConstant() {
        assertEquals("native", Token.NATIVE_TOKEN_ADDRESS)
    }
}
