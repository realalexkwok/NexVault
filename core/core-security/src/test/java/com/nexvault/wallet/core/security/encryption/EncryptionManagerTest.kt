package com.nexvault.wallet.core.security.encryption

import com.nexvault.wallet.core.security.keystore.KeyStoreManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import javax.crypto.SecretKey

class EncryptionManagerTest {

    private lateinit var encryptionManager: EncryptionManager
    private lateinit var keyStoreManager: KeyStoreManager

    @Before
    fun setup() {
        // Strict mock on purpose: the password-derived tests below never touch the
        // keystore, so any accidental keystore call should fail loudly rather than
        // silently return a stub.
        keyStoreManager = mockk<KeyStoreManager>()
        encryptionManager = EncryptionManager(keyStoreManager)
    }

    @Test
    fun testEncryptDecryptWithPassword() {
        val plaintext = "Hello, World!".toByteArray(Charsets.UTF_8)
        val password = "SecurePassword123!"

        val encrypted = encryptionManager.encryptWithPassword(plaintext, password)
        val decrypted = encryptionManager.decryptWithPassword(encrypted, password)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test(expected = javax.crypto.AEADBadTagException::class)
    fun testDecryptWithWrongPassword() {
        val plaintext = "Secret message".toByteArray(Charsets.UTF_8)
        val correctPassword = "CorrectPassword123!"
        val wrongPassword = "WrongPassword456!"

        val encrypted = encryptionManager.encryptWithPassword(plaintext, correctPassword)
        encryptionManager.decryptWithPassword(encrypted, wrongPassword)
    }

    @Test
    fun testDifferentPasswordsProduceDifferentCiphertexts() {
        val plaintext = "Test message".toByteArray(Charsets.UTF_8)
        val password1 = "PasswordOne123!"
        val password2 = "PasswordTwo456!"

        val encrypted1 = encryptionManager.encryptWithPassword(plaintext, password1)
        val encrypted2 = encryptionManager.encryptWithPassword(plaintext, password2)

        assertFalse(encrypted1.ciphertext.contentEquals(encrypted2.ciphertext))
    }

    @Test
    fun testSaltIsDifferentEachTime() {
        val plaintext = "Test message".toByteArray(Charsets.UTF_8)
        val password = "SamePassword123!"

        val encrypted1 = encryptionManager.encryptWithPassword(plaintext, password)
        val encrypted2 = encryptionManager.encryptWithPassword(plaintext, password)

        assertFalse(encrypted1.salt.contentEquals(encrypted2.salt))
    }

    @Test
    fun testIvIsDifferentEachTime() {
        val plaintext = "Test message".toByteArray(Charsets.UTF_8)
        val password = "SamePassword123!"

        val encrypted1 = encryptionManager.encryptWithPassword(plaintext, password)
        val encrypted2 = encryptionManager.encryptWithPassword(plaintext, password)

        assertFalse(encrypted1.iv.contentEquals(encrypted2.iv))
    }

    @Test
    fun testDeriveKeyFromPassword() {
        val password = "TestPassword123!"
        val salt = "12345678901234567890123456789012".toByteArray()

        val result1 = encryptionManager.deriveKeyFromPassword(password, salt)
        val result2 = encryptionManager.deriveKeyFromPassword(password, salt)

        // Same password and salt should produce same key
        assertTrue(result1.key.encoded.contentEquals(result2.key.encoded))
        assertTrue(result1.salt.contentEquals(result2.salt))
    }

    @Test
    fun testDeriveKeyFromPasswordDifferentSalts() {
        val password = "TestPassword123!"
        val salt1 = "12345678901234567890123456789012".toByteArray()
        val salt2 = "abcdefghijklmnopqrstuvwxyz12".toByteArray()

        val result1 = encryptionManager.deriveKeyFromPassword(password, salt1)
        val result2 = encryptionManager.deriveKeyFromPassword(password, salt2)

        // Different salts should produce different keys
        assertFalse(result1.key.encoded.contentEquals(result2.key.encoded))
    }

    @Ignore(
        "Requires a real AndroidKeyStore: encryptWithKeystore needs a hardware-backed AES " +
            "key, which a JVM mock cannot provide. Port to app/src/androidTest once a " +
            "device is available — see specs/features/2026-09-20-2.0.0-stabilization/ " +
            "requirements.md (Handoff)."
    )
    @Test
    fun testEncryptWithKeystore() {
        // Setup mock to return a proper key
        val mockKey = mockk<SecretKey>()
        every { keyStoreManager.getOrCreateMasterKey() } returns mockKey

        // This test just verifies the method can be called
        // Full keystore encryption testing requires instrumented tests
    }

    @Test
    fun testPasswordEncryptedDataEquality() {
        val data1 = PasswordEncryptedData(
            ciphertext = "cipher".toByteArray(),
            iv = "iv".toByteArray(),
            salt = "salt".toByteArray()
        )
        val data2 = PasswordEncryptedData(
            ciphertext = "cipher".toByteArray(),
            iv = "iv".toByteArray(),
            salt = "salt".toByteArray()
        )

        assertTrue(data1 == data2)
    }
}
