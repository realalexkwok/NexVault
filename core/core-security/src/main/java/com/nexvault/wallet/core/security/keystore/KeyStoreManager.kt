package com.nexvault.wallet.core.security.keystore

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "nexvault_master_key"
        private const val BIOMETRIC_KEY_ALIAS = "nexvault_biometric_key"
        private const val STRONGBOX_PROBE_ALIAS = "test_strongbox_key"
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }
    }

    fun getOrCreateMasterKey(): SecretKey {
        return if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
        } else {
            generateMasterKey()
        }
    }

    private fun generateMasterKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val builder = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)

        if (isStrongBoxAvailable()) {
            builder.setIsStrongBoxBacked(true)
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    fun getOrCreateBiometricKey(): SecretKey {
        return if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
            keyStore.getKey(BIOMETRIC_KEY_ALIAS, null) as SecretKey
        } else {
            generateBiometricKey()
        }
    }

    private fun generateBiometricKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val builder = KeyGenParameterSpec.Builder(
            BIOMETRIC_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
            .setInvalidatedByBiometricEnrollment(true)

        if (isStrongBoxAvailable()) {
            builder.setIsStrongBoxBacked(true)
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    /**
     * Probes StrongBox support without leaving state behind.
     *
     * The platform feature check short-circuits devices that cannot have StrongBox at all
     * (the attribute only exists from API 28), so no probe key is generated there.
     */
    fun isStrongBoxAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)) {
            return false
        }

        return try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            val spec = KeyGenParameterSpec.Builder(
                STRONGBOX_PROBE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT
            )
                .setIsStrongBoxBacked(true)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
            true
        } catch (e: Exception) {
            false
        } finally {
            // A probe must not persist a key in AndroidKeyStore.
            try {
                if (keyStore.containsAlias(STRONGBOX_PROBE_ALIAS)) {
                    keyStore.deleteEntry(STRONGBOX_PROBE_ALIAS)
                }
            } catch (e: Exception) {
                // Best effort; a failed cleanup must not change the probe result.
            }
        }
    }

    fun deleteAllKeys() {
        if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            keyStore.deleteEntry(MASTER_KEY_ALIAS)
        }
        if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
            keyStore.deleteEntry(BIOMETRIC_KEY_ALIAS)
        }
    }

    fun hasMasterKey(): Boolean {
        return keyStore.containsAlias(MASTER_KEY_ALIAS)
    }
}
