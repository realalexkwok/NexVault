package com.nexvault.wallet.core.security.wallet

import android.content.Context
import com.nexvault.wallet.core.security.encryption.EncryptionManager
import com.nexvault.wallet.core.security.util.SecureUtils.secureWipe
import com.nexvault.wallet.core.security.util.SecurityUtils.hexToByteArray
import com.nexvault.wallet.core.security.util.SecurityUtils.toHex
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores wallet material for the single active wallet.
 *
 * Wallet material is protected by the **KeyStore-wrapped AES-GCM layer alone**
 * (`EncryptionManager.encryptWithKeystore`): the wrapping key never leaves the
 * AndroidKeyStore. The former inner PBKDF2 layer used the wallet UUID — a value stored
 * in plaintext in `wallet_metadata` — as its password, so it held no secret; it was
 * removed in roadmap item 2.0.2b (legacy CR finding 1.6-1).
 *
 * Authentication is enforced by the caller (`WalletRepositoryImpl`), which refuses to
 * retrieve material while the session is locked.
 */
@Singleton
class WalletStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: EncryptionManager
) {

    companion object {
        private const val WALLET_DIR = "wallet"
        private const val MNEMONIC_FILE = "mnemonic.enc"
        private const val PRIVATE_KEYS_FILE = "private_keys.enc"
        private const val BIOMETRIC_FILE = "biometric.enc"
        private const val KEY_CIPHERTEXT = "ciphertext"
    }

    private val walletDir: File by lazy {
        File(context.filesDir, WALLET_DIR).also { it.mkdirs() }
    }

    suspend fun storeMnemonic(mnemonic: String) = withContext(Dispatchers.IO) {
        val mnemonicBytes = mnemonic.toByteArray(Charsets.UTF_8)
        val encryptedData = try {
            encryptionManager.encryptWithKeystore(mnemonicBytes)
        } finally {
            mnemonicBytes.secureWipe()
        }

        val json = JSONObject().apply { put(KEY_CIPHERTEXT, encryptedData.toHex()) }
        File(walletDir, MNEMONIC_FILE).writeText(json.toString())
    }

    suspend fun retrieveMnemonic(): String = withContext(Dispatchers.IO) {
        val file = File(walletDir, MNEMONIC_FILE)
        if (!file.exists()) {
            throw IllegalStateException("No wallet data found")
        }

        val ciphertext = JSONObject(file.readText()).getString(KEY_CIPHERTEXT).hexToByteArray()
        val decrypted = encryptionManager.decryptWithKeystore(ciphertext)
        String(decrypted, Charsets.UTF_8)
    }

    /**
     * Stores the encrypted private key for [address].
     *
     * The call is deliberately destructive: [privateKey] is zeroed as soon as encryption
     * finishes (also when it throws), so no live plaintext copy survives the call. Callers
     * must not reuse the array afterwards.
     */
    suspend fun storePrivateKey(address: String, privateKey: ByteArray) = withContext(Dispatchers.IO) {
        val keysFile = File(walletDir, PRIVATE_KEYS_FILE)

        val existingKeys = if (keysFile.exists()) {
            JSONObject(keysFile.readText())
        } else {
            JSONObject()
        }

        val encryptedData =
            try {
                encryptionManager.encryptWithKeystore(privateKey)
            } finally {
                privateKey.secureWipe()
            }

        existingKeys.put(address, JSONObject().apply { put(KEY_CIPHERTEXT, encryptedData.toHex()) })
        keysFile.writeText(existingKeys.toString())
    }

    suspend fun retrievePrivateKey(address: String): ByteArray = withContext(Dispatchers.IO) {
        val file = File(walletDir, PRIVATE_KEYS_FILE)
        if (!file.exists()) {
            throw IllegalStateException("No private key found for address")
        }

        val json = JSONObject(file.readText())
        if (!json.has(address)) {
            throw IllegalStateException("No private key found for address")
        }

        val ciphertext = json.getJSONObject(address).getString(KEY_CIPHERTEXT).hexToByteArray()
        encryptionManager.decryptWithKeystore(ciphertext)
    }

    suspend fun storeBiometricEncryptedPassword(password: String, cipher: Cipher) = withContext(Dispatchers.IO) {
        val passwordBytes = password.toByteArray(Charsets.UTF_8)
        val encrypted = try {
            encryptionManager.encryptWithBiometric(passwordBytes, cipher)
        } finally {
            passwordBytes.secureWipe()
        }

        val json = JSONObject().apply {
            put(KEY_CIPHERTEXT, encrypted.toHex())
            put("iv", cipher.iv.toHex())
        }

        File(walletDir, BIOMETRIC_FILE).writeText(json.toString())
    }

    suspend fun retrieveBiometricEncryptedPassword(cipher: Cipher): String = withContext(Dispatchers.IO) {
        val file = File(walletDir, BIOMETRIC_FILE)
        if (!file.exists()) {
            throw IllegalStateException("No biometric data found")
        }

        val json = JSONObject(file.readText())
        val ciphertext = json.getString(KEY_CIPHERTEXT).hexToByteArray()

        val decrypted = encryptionManager.decryptWithBiometric(ciphertext, cipher)
        String(decrypted, Charsets.UTF_8)
    }

    fun hasWalletData(): Boolean {
        return File(walletDir, MNEMONIC_FILE).exists()
    }

    fun hasBiometricData(): Boolean {
        return File(walletDir, BIOMETRIC_FILE).exists()
    }

    suspend fun wipeAll() = withContext(Dispatchers.IO) {
        walletDir.listFiles()?.forEach { it.delete() }
    }

    suspend fun wipeWalletData(walletId: String) = withContext(Dispatchers.IO) {
        // For single-file design, wipe all (only one wallet supported at a time)
        walletDir.listFiles()?.forEach { it.delete() }
    }
}
