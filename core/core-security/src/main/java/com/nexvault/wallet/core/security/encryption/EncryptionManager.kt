package com.nexvault.wallet.core.security.encryption

import com.nexvault.wallet.core.security.keystore.KeyStoreManager
import com.nexvault.wallet.core.security.util.SecurityUtils
import com.nexvault.wallet.core.security.util.SecureUtils.secureWipe
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionManager @Inject constructor(
    private val keyStoreManager: KeyStoreManager
) {
    companion object {
        private const val AES_GCM_ALGORITHM = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val PBKDF2_ITERATIONS = 100000
        private const val KEY_LENGTH = 256
        private const val SALT_LENGTH = 32
    }

    private val secureRandom = SecureRandom()

    fun encryptWithKeystore(plaintext: ByteArray): ByteArray {
        val masterKey = keyStoreManager.getOrCreateMasterKey()
        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)

        return iv + ciphertext
    }

    fun decryptWithKeystore(ciphertext: ByteArray): ByteArray {
        val masterKey = keyStoreManager.getOrCreateMasterKey()
        val iv = ciphertext.copyOfRange(0, GCM_IV_LENGTH)
        val encryptedData = ciphertext.copyOfRange(GCM_IV_LENGTH, ciphertext.size)

        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)

        return cipher.doFinal(encryptedData)
    }

    fun deriveKeyFromPassword(password: String, salt: ByteArray?): DerivedKeyResult {
        val saltBytes = salt ?: SecurityUtils.generateSecureRandom(SALT_LENGTH)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, PBKDF2_ITERATIONS, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        val keyBytes = tmp.encoded
        spec.clearPassword()

        val key = SecretKeySpec(keyBytes, "AES")
        keyBytes.secureWipe()

        return DerivedKeyResult(key, saltBytes)
    }

    fun encryptWithPassword(plaintext: ByteArray, password: String): PasswordEncryptedData {
        val salt = SecurityUtils.generateSecureRandom(SALT_LENGTH)
        val derivedKeyResult = deriveKeyFromPassword(password, salt)
        val iv = SecurityUtils.generateSecureRandom(GCM_IV_LENGTH)

        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, derivedKeyResult.key, spec)

        val ciphertext = cipher.doFinal(plaintext)

        derivedKeyResult.key.encoded?.let { keyBytes ->
            keyBytes.secureWipe()
        }

        return PasswordEncryptedData(ciphertext, iv, salt)
    }

    fun decryptWithPassword(encryptedData: PasswordEncryptedData, password: String): ByteArray {
        val derivedKeyResult = deriveKeyFromPassword(password, encryptedData.salt)

        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, derivedKeyResult.key, spec)

        val plaintext = cipher.doFinal(encryptedData.ciphertext)

        derivedKeyResult.key.encoded?.let { keyBytes ->
            keyBytes.secureWipe()
        }

        return plaintext
    }

    fun encryptWithBiometric(plaintext: ByteArray, cipher: Cipher): ByteArray {
        return cipher.doFinal(plaintext)
    }

    fun decryptWithBiometric(ciphertext: ByteArray, cipher: Cipher): ByteArray {
        return cipher.doFinal(ciphertext)
    }

    fun getBiometricCipher(forEncryption: Boolean, iv: ByteArray? = null): Cipher {
        val key = keyStoreManager.getOrCreateBiometricKey()
        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)

        if (iv != null) {
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(
                if (forEncryption) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE,
                key,
                spec
            )
        } else {
            cipher.init(
                if (forEncryption) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE,
                key
            )
        }

        return cipher
    }
}
