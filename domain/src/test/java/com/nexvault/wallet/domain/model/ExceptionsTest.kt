package com.nexvault.wallet.domain.model

import com.nexvault.wallet.domain.model.common.AuthenticationException
import com.nexvault.wallet.domain.model.common.EncryptionException
import com.nexvault.wallet.domain.model.common.InsufficientBalanceException
import com.nexvault.wallet.domain.model.common.InvalidAddressException
import com.nexvault.wallet.domain.model.common.InvalidMnemonicException
import com.nexvault.wallet.domain.model.common.InvalidPrivateKeyException
import com.nexvault.wallet.domain.model.common.NetworkException
import com.nexvault.wallet.domain.model.common.NexVaultException
import com.nexvault.wallet.domain.model.common.TransactionFailedException
import com.nexvault.wallet.domain.model.common.WalletAlreadyExistsException
import com.nexvault.wallet.domain.model.common.WalletNotFoundException
import org.junit.Assert.*
import org.junit.Test

class ExceptionsTest {
    @Test
    fun testNexVaultExceptionDefaultMessage() {
        val exception = NexVaultException("test message")
        assertEquals("test message", exception.message)
    }

    @Test
    fun testNexVaultExceptionWithCause() {
        val cause = RuntimeException("cause")
        val exception = NexVaultException("test message", cause)
        assertEquals("test message", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun testInvalidMnemonicExceptionDefaultMessage() {
        val exception = InvalidMnemonicException()
        assertEquals("Invalid mnemonic phrase", exception.message)
    }

    @Test
    fun testInvalidMnemonicExceptionCustomMessage() {
        val exception = InvalidMnemonicException("Custom error")
        assertEquals("Custom error", exception.message)
    }

    @Test
    fun testInvalidPrivateKeyExceptionDefaultMessage() {
        val exception = InvalidPrivateKeyException()
        assertEquals("Invalid private key", exception.message)
    }

    @Test
    fun testInvalidAddressExceptionDefaultMessage() {
        val exception = InvalidAddressException()
        assertEquals("Invalid Ethereum address", exception.message)
    }

    @Test
    fun testWalletNotFoundExceptionDefaultMessage() {
        val exception = WalletNotFoundException()
        assertEquals("Wallet not found", exception.message)
    }

    @Test
    fun testWalletAlreadyExistsExceptionDefaultMessage() {
        val exception = WalletAlreadyExistsException()
        assertEquals("Wallet already exists", exception.message)
    }

    @Test
    fun testInsufficientBalanceExceptionContainsAmounts() {
        val exception = InsufficientBalanceException("1.0", "2.0")
        assertEquals("1.0", exception.available)
        assertEquals("2.0", exception.required)
        assertTrue(exception.message?.contains("1.0") == true)
        assertTrue(exception.message?.contains("2.0") == true)
    }

    @Test
    fun testTransactionFailedExceptionDefaultMessage() {
        val exception = TransactionFailedException("Transaction failed")
        assertEquals("Transaction failed", exception.message)
    }

    @Test
    fun testTransactionFailedExceptionWithCause() {
        val cause = RuntimeException("cause")
        val exception = TransactionFailedException("Transaction failed", cause)
        assertEquals("Transaction failed", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun testAuthenticationExceptionDefaultMessage() {
        val exception = AuthenticationException()
        assertEquals("Authentication failed", exception.message)
    }

    @Test
    fun testNetworkExceptionDefaultMessage() {
        val exception = NetworkException()
        assertEquals("Network error", exception.message)
    }

    @Test
    fun testNetworkExceptionWithCause() {
        val cause = RuntimeException("cause")
        val exception = NetworkException("Network error", cause)
        assertEquals("Network error", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun testEncryptionExceptionDefaultMessage() {
        val exception = EncryptionException()
        assertEquals("Encryption error", exception.message)
    }

    @Test
    fun testEncryptionExceptionWithCause() {
        val cause = RuntimeException("cause")
        val exception = EncryptionException("Encryption error", cause)
        assertEquals("Encryption error", exception.message)
        assertEquals(cause, exception.cause)
    }
}
