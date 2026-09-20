package com.nexvault.wallet.core.security.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordValidatorTest {

    @Test
    fun testValidPassword() {
        val result = PasswordValidator.validate("SecurePass1!")
        assertTrue(result.isValid)
        assertTrue(result.hasMinLength)
        assertTrue(result.hasUppercase)
        assertTrue(result.hasLowercase)
        assertTrue(result.hasDigit)
        assertTrue(result.hasSpecialChar)
    }

    @Test
    fun testShortPassword() {
        val result = PasswordValidator.validate("Aa1!")
        assertFalse(result.isValid)
        assertFalse(result.hasMinLength)
    }

    @Test
    fun testPasswordNoUppercase() {
        val result = PasswordValidator.validate("securepass1!")
        assertFalse(result.isValid)
        assertFalse(result.hasUppercase)
    }

    @Test
    fun testPasswordNoLowercase() {
        val result = PasswordValidator.validate("SECUREPASS1!")
        assertFalse(result.isValid)
        assertFalse(result.hasLowercase)
    }

    @Test
    fun testPasswordNoDigit() {
        val result = PasswordValidator.validate("SecurePass!!")
        assertFalse(result.isValid)
        assertFalse(result.hasDigit)
    }

    @Test
    fun testPasswordNoSpecialChar() {
        val result = PasswordValidator.validate("SecurePass12")
        assertFalse(result.isValid)
        assertFalse(result.hasSpecialChar)
    }

    @Test
    fun testValidPin() {
        assertTrue(PasswordValidator.validatePin("123456"))
        assertTrue(PasswordValidator.validatePin("000000"))
        assertTrue(PasswordValidator.validatePin("999999"))
    }

    @Test
    fun testInvalidPinTooShort() {
        assertFalse(PasswordValidator.validatePin("12345"))
    }

    @Test
    fun testInvalidPinTooLong() {
        assertFalse(PasswordValidator.validatePin("1234567"))
    }

    @Test
    fun testInvalidPinAlphabetic() {
        assertFalse(PasswordValidator.validatePin("abcdef"))
    }

    @Test
    fun testInvalidPinAlphanumeric() {
        assertFalse(PasswordValidator.validatePin("12345a"))
    }

    @Test
    fun testStrengthCalculationWeak() {
        val strength = PasswordValidator.calculateStrength("abc")
        assertTrue(strength < 40)
    }

    @Test
    fun testStrengthCalculationStrong() {
        val strength = PasswordValidator.calculateStrength("MyV3ryStr0ng!P@ssw0rd")
        // The documented maximum is 75 (25 length + 50 variety), not 100: the
        // coerceIn(0, 100) is only a clamp. This password covers all four character
        // classes at length 21 (+75) and contains the "ss" pair (-5), so the top band
        // lands at exactly 70. Pinning the exact value documents that ceiling; the
        // compressed scale is logged for Phase 4.
        assertEquals(70, strength)
    }

    @Test
    fun testStrengthCalculationEmpty() {
        val strength = PasswordValidator.calculateStrength("")
        assertEquals(0, strength)
    }

    @Test
    fun testStrengthLengthContribution() {
        val short = PasswordValidator.calculateStrength("Abc1!")
        val long = PasswordValidator.calculateStrength("Abcdefghijkl1!")
        assertTrue(long > short)
    }

    @Test
    fun testStrengthCommonPatternPenalty() {
        val withoutPattern = PasswordValidator.calculateStrength("XyZ1234!@#")
        val withPattern = PasswordValidator.calculateStrength("abc123!@#")
        assertTrue(withoutPattern >= withPattern)
    }
}
