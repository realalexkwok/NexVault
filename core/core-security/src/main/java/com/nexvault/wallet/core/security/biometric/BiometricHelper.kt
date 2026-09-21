package com.nexvault.wallet.core.security.biometric

import android.content.Context
import androidx.annotation.StringRes
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.nexvault.wallet.core.security.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricHelper @Inject constructor(
    @ApplicationContext private val context: Context
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

    fun createCryptoObject(cipher: javax.crypto.Cipher): BiometricPrompt.CryptoObject {
        return BiometricPrompt.CryptoObject(cipher)
    }
}

enum class BiometricStatus {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NOT_ENROLLED
}
