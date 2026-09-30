package com.nexvault.wallet.core.security.biometric

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.StringRes
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.nexvault.wallet.core.security.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isBiometricAvailable(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)

        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            else -> BiometricStatus.NOT_ENROLLED
        }
    }

    fun isBiometricEnrolled(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Builds the system biometric prompt. All copy comes from string resources so callers can
     * override it without introducing hardcoded user-facing text.
     *
     * @param title Prompt title resource.
     * @param subtitle Prompt subtitle resource.
     * @param negativeButtonText Resource for the fallback button label.
     */
    fun createPromptInfo(
        @StringRes title: Int = R.string.biometric_prompt_title,
        @StringRes subtitle: Int = R.string.biometric_prompt_subtitle,
        @StringRes negativeButtonText: Int = R.string.biometric_prompt_use_pin,
    ): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(title))
            .setSubtitle(context.getString(subtitle))
            .setNegativeButtonText(context.getString(negativeButtonText))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
    }

    fun createCryptoObject(cipher: Cipher): BiometricPrompt.CryptoObject {
        return BiometricPrompt.CryptoObject(cipher)
    }

    /**
     * Cipher for a crypto-bound biometric prompt (roadmap 2.6 scan fix, kotlin:S6293).
     *
     * The gate key is generated in the AndroidKeyStore with `setUserAuthenticationRequired(true)`,
     * so the system prompt only succeeds through a genuine biometric authentication — the key is
     * used as an authentication gate only and never encrypts wallet material. When enrolled
     * biometrics change and invalidate the key, it is recreated once.
     */
    fun getBiometricGateCipher(): Cipher? {
        return try {
            initGateCipher()
        } catch (_: Exception) {
            try {
                deleteBiometricGateKey()
                initGateCipher()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun initGateCipher(): Cipher {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val key =
            keyStore.getKey(BIOMETRIC_GATE_KEY_ALIAS, null) as? SecretKey ?: generateBiometricGateKey()
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
    }

    private fun generateBiometricGateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec =
            KeyGenParameterSpec.Builder(
                BIOMETRIC_GATE_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun deleteBiometricGateKey() {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            keyStore.deleteEntry(BIOMETRIC_GATE_KEY_ALIAS)
        } catch (_: Exception) {
            // Best-effort: the next generation attempt reports the real problem.
        }
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val BIOMETRIC_GATE_KEY_ALIAS = "nexvault_biometric_gate_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

enum class BiometricStatus {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NOT_ENROLLED,
}
