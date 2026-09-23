package com.nexvault.wallet.core.security.wallet

import android.content.Context
import com.nexvault.wallet.core.security.encryption.DoubleEncryptedData
import com.nexvault.wallet.core.security.encryption.EncryptionManager
import com.nexvault.wallet.core.security.encryption.PasswordEncryptedData
import com.nexvault.wallet.core.security.util.SecureUtils.secureWipe
import com.nexvault.wallet.core.security.util.SecurityUtils
import com.nexvault.wallet.core.security.util.SecurityUtils.toHex
import com.nexvault.wallet.core.security.util.SecurityUtils.hexToByteArray
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton

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
    }

    private val walletDir: File by lazy {
        File(context.filesDir, WALLET_DIR).also { it.mkdirs() }
    }

    suspend fun storeMnemonic(mnemonic: String, password: String) = withContext(Dispatchers.IO) {
        val mnemonicBytes = mnemonic.toByteArray(Charsets.UTF_8)
        val encryptedData = try {
            encryptionManager.doubleEncrypt(mnemonicBytes, password)
        } finally {
            mnemonicBytes.secureWipe()
        }

        val json = JSONObject().apply {
            put("outerCiphertext", encryptedData.outerCiphertext.toHex())
            put("innerIv", encryptedData.innerIv.toHex())
            put("outerIv", encryptedData.outerIv.toHex())
            put("salt", encryptedData.salt.toHex())
        }

        File(walletDir, MNEMONIC_FILE).writeText(json.toString())
    }

    suspend fun retrieveMnemonic(password: String): String = withContext(Dispatchers.IO) {
        val file = File(walletDir, MNEMONIC_FILE)
        if (!file.exists()) {
            throw IllegalStateException("No wallet data found")
        }

        val json = JSONObject(file.readText())
        val encryptedData = DoubleEncryptedData(
            outerCiphertext = json.getString("outerCiphertext").hexToByteArray(),
            innerIv = json.getString("innerIv").hexToByteArray(),
            outerIv = json.getString("outerIv").hexToByteArray(),
            salt = json.getString("salt").hexToByteArray()
        )

        val decrypted = encryptionManager.doubleDecrypt(encryptedData, password)
        String(decrypted, Charsets.UTF_8)
    }

    /**
     * Stores the encrypted private key for [address].
     *
     * The call is deliberately destructive: [privateKey] is zeroed as soon as encryption
     * finishes (also when it throws), so no live plaintext copy survives the call. Callers
     * must not reuse the array afterwards.
     */
    suspend fun storePrivateKey(address: String, privateKey: ByteArray, password: String) = withContext(Dispatchers.IO) {
        val keysFile = File(walletDir, PRIVATE_KEYS_FILE)

        val existingKeys = if (keysFile.exists()) {
            JSONObject(keysFile.readText())
        } else {
            JSONObject()
        }

        val encryptedData =
            try {
                encryptionManager.encryptWithPassword(privateKey, password)
            } finally {
                privateKey.secureWipe()
            }

        val keyData = JSONObject().apply {
            put("ciphertext", encryptedData.ciphertext.toHex())
            put("iv", encryptedData.iv.toHex())
            put("salt", encryptedData.salt.toHex())
        }

        existingKeys.put(address, keyData)
        keysFile.writeText(existingKeys.toString())
    }

    suspend fun retrievePrivateKey(address: String, password: String): ByteArray = withContext(Dispatchers.IO) {
        val file = File(walletDir, PRIVATE_KEYS_FILE)
        if (!file.exists()) {
            throw IllegalStateException("No private key found for address")
        }

        val json = JSONObject(file.readText())
        if (!json.has(address)) {
            throw IllegalStateException("No private key found for address")
        }

        val keyData = json.getJSONObject(address)
        val encryptedData = PasswordEncryptedData(
            ciphertext = keyData.getString("ciphertext").hexToByteArray(),
            iv = keyData.getString("iv").hexToByteArray(),
            salt = keyData.getString("salt").hexToByteArray()
        )

        encryptionManager.decryptWithPassword(encryptedData, password)
    }

    suspend fun storeBiometricEncryptedPassword(password: String, cipher: Cipher) = withContext(Dispatchers.IO) {
        val passwordBytes = password.toByteArray(Charsets.UTF_8)
        val encrypted = try {
            encryptionManager.encryptWithBiometric(passwordBytes, cipher)
        } finally {
            passwordBytes.secureWipe()
        }

        val json = JSONObject().apply {
            put("ciphertext", encrypted.toHex())
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
        val ciphertext = json.getString("ciphertext").hexToByteArray()

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
