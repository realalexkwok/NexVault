package com.nexvault.wallet.core.security.util

import com.nexvault.wallet.core.security.util.SecureUtils.secureWipe
import com.nexvault.wallet.core.security.util.SecurityUtils.hexToByteArray
import com.nexvault.wallet.core.security.util.SecurityUtils.toHex
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.web3j.crypto.Keys

class SecurityUtilsTest {

    @Test
    fun testSecureWipe() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        data.secureWipe()
        assertTrue(data.all { it == 0.toByte() })
    }

    @Test
    fun testConstantTimeEqualsEqualArrays() {
        val a = byteArrayOf(1, 2, 3, 4, 5)
        val b = byteArrayOf(1, 2, 3, 4, 5)
        assertTrue(SecurityUtils.constantTimeEquals(a, b))
    }

    @Test
    fun testConstantTimeEqualsDifferentArrays() {
        val a = byteArrayOf(1, 2, 3, 4, 5)
        val b = byteArrayOf(1, 2, 3, 4, 6)
        assertFalse(SecurityUtils.constantTimeEquals(a, b))
    }

    @Test
    fun testConstantTimeEqualsDifferentLengths() {
        val a = byteArrayOf(1, 2, 3)
        val b = byteArrayOf(1, 2, 3, 4)
        assertFalse(SecurityUtils.constantTimeEquals(a, b))
    }

    @Test
    fun testHexRoundTrip() {
        val original = byteArrayOf(1, 2, 3, 4, 5, 255.toByte(), 0, 128.toByte())
        val hex = original.toHex()
        val recovered = hex.hexToByteArray()
        assertArrayEquals(original, recovered)
    }

    @Test
    fun testHexToByteArray() {
        val hex = "0123456789abcdef"
        val bytes = hex.hexToByteArray()
        assertEquals(8, bytes.size)
        assertEquals(0x01.toByte(), bytes[0])
        assertEquals(0x23.toByte(), bytes[1])
    }

    @Test
    fun testValidEthereumAddress() {
        assertTrue(SecurityUtils.isValidEthereumAddress("0x742d35Cc6634C0532925a3b844Bc9e7595f0eB1E"))
        assertTrue(SecurityUtils.isValidEthereumAddress("0x" + "a".repeat(40)))
        assertTrue(SecurityUtils.isValidEthereumAddress("0x" + "A".repeat(40)))
    }

    @Test
    fun testInvalidEthereumAddressNoPrefix() {
        assertFalse(SecurityUtils.isValidEthereumAddress("742d35Cc6634C0532925a3b844Bc9e7595f0eB1E"))
    }

    @Test
    fun testInvalidEthereumAddressTooShort() {
        assertFalse(SecurityUtils.isValidEthereumAddress("0x" + "a".repeat(39)))
    }

    @Test
    fun testInvalidEthereumAddressTooLong() {
        assertFalse(SecurityUtils.isValidEthereumAddress("0x" + "a".repeat(41)))
    }

    @Test
    fun testInvalidEthereumAddressNonHex() {
        assertFalse(SecurityUtils.isValidEthereumAddress("0x742d35Cc6634C0532925a3b844Bc9e7595f0eB1G"))
    }

    @Test
    fun testChecksumAddress() {
        // EIP-55 reference vectors — a format-only assertion cannot catch a wrong hash
        // algorithm (NIST SHA3-256 vs Ethereum Keccak-256 produce different casing).
        val vectors =
            listOf(
                "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed",
                "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359",
                "0xdbF03B407c01E7cD3CBea99509d93f8DDDC8C6FB",
                "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb",
            )

        vectors.forEach { expected ->
            assertEquals(expected, SecurityUtils.checksumAddress(expected.lowercase()))
        }
    }

    @Test
    fun testChecksumAddressKeccakMatchesWeb3j() {
        // Cross-check against web3j's canonical EIP-55 implementation.
        val address = "0x742d35Cc6634C0532925a3b844Bc9e7595f0eB1E".lowercase()

        assertEquals(Keys.toChecksumAddress(address), SecurityUtils.checksumAddress(address))
    }

    @Test
    fun testChecksumAddressInvalid() {
        val invalid = "invalid"
        assertEquals(invalid, SecurityUtils.checksumAddress(invalid))
    }

    @Test
    fun testGenerateSecureRandom() {
        val bytes1 = SecurityUtils.generateSecureRandom(32)
        val bytes2 = SecurityUtils.generateSecureRandom(32)

        assertEquals(32, bytes1.size)
        assertEquals(32, bytes2.size)
        assertFalse(bytes1.contentEquals(bytes2))
    }

    @Test
    fun testCharArraySecureWipe() {
        val chars = charArrayOf('a', 'b', 'c', 'd')
        chars.secureWipe()
        assertTrue(chars.all { it == '\u0000' })
    }
}
