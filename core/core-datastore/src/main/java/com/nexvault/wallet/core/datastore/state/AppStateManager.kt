package com.nexvault.wallet.core.datastore.state

import com.nexvault.wallet.core.datastore.model.AutoLockTimeout
import com.nexvault.wallet.core.datastore.preferences.UserPreferencesDataStore
import com.nexvault.wallet.core.datastore.security.SecurityPreferencesDataStore
import com.nexvault.wallet.core.datastore.wallet.WalletMetadataDataStore
import com.nexvault.wallet.core.security.keystore.KeyStoreManager
import com.nexvault.wallet.core.security.wallet.WalletStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthFailureResult {
    data class TemporaryLockout(val seconds: Long, val attemptCount: Int) : AuthFailureResult()
    data class Warning(val message: String, val attemptCount: Int) : AuthFailureResult()
    object WalletWiped : AuthFailureResult()
    data class NoLockout(val attemptCount: Int) : AuthFailureResult()
}

@Singleton
class AppStateManager @Inject constructor(
    private val userPreferences: UserPreferencesDataStore,
    private val securityPreferences: SecurityPreferencesDataStore,
    private val walletMetadata: WalletMetadataDataStore,
    private val walletStore: WalletStore,
    private val keyStoreManager: KeyStoreManager
) {
    companion object {
        private const val LOCKOUT_5_ATTEMPTS = 30L
        private const val LOCKOUT_8_ATTEMPTS = 300L
        private const val LOCKOUT_10_ATTEMPTS = 900L
        private const val LOCKOUT_15_ATTEMPTS = 3600L
        private const val WIPE_ATTEMPT_THRESHOLD = 20
    }

    val isFirstRun: Flow<Boolean> = combine(
        securityPreferences.isWalletSetUp,
        userPreferences.hasCompletedOnboarding
    ) { isWalletSetUp, hasCompletedOnboarding ->
        !isWalletSetUp && !hasCompletedOnboarding
    }

    val isWalletLocked: Flow<Boolean> = combine(
        securityPreferences.isWalletSetUp,
        securityPreferences.lastAuthTimestamp,
        userPreferences.autoLockTimeout
    ) { isSetUp, lastAuth, timeout ->
        if (!isSetUp) return@combine false

        if (timeout == AutoLockTimeout.NEVER) {
            return@combine false
        }

        if (timeout == AutoLockTimeout.IMMEDIATE) {
            return@combine true
        }

        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = (currentTime - lastAuth) / 1000
        elapsedSeconds > timeout.seconds
    }

    val isLockedOut: Flow<Boolean> = combine(
        securityPreferences.failedAttemptCount,
        securityPreferences.lockoutEndTime
    ) { attemptCount, lockoutEndTime ->
        if (attemptCount < 5) return@combine false

        val currentTime = System.currentTimeMillis()
        if (lockoutEndTime > currentTime) {
            return@combine true
        }
        false
    }

    val lockoutRemainingSeconds: Flow<Long> = securityPreferences.lockoutEndTime.map { lockoutEndTime ->
        val currentTime = System.currentTimeMillis()
        if (lockoutEndTime > currentTime) {
            (lockoutEndTime - currentTime) / 1000
        } else {
            0L
        }
    }

    suspend fun onAuthenticationSuccess() {
        securityPreferences.resetFailedAttempts()
        securityPreferences.setLockoutEndTime(0L)
        securityPreferences.setLastAuthTimestamp(System.currentTimeMillis())
    }

    suspend fun onAuthenticationFailure(): AuthFailureResult {
        val newCount = securityPreferences.incrementFailedAttempts()
        val currentTime = System.currentTimeMillis()

        return when {
            newCount >= WIPE_ATTEMPT_THRESHOLD -> {
                wipeWallet()
                AuthFailureResult.WalletWiped
            }
            newCount >= 15 -> {
                val lockoutEnd = currentTime + (LOCKOUT_15_ATTEMPTS * 1000)
                securityPreferences.setLockoutEndTime(lockoutEnd)
                AuthFailureResult.Warning(
                    message = "Too many failed attempts. You have 1 hour before wallet wipe warning.",
                    attemptCount = newCount
                )
            }
            newCount >= 10 -> {
                val lockoutEnd = currentTime + (LOCKOUT_10_ATTEMPTS * 1000)
                securityPreferences.setLockoutEndTime(lockoutEnd)
                AuthFailureResult.TemporaryLockout(
                    seconds = LOCKOUT_10_ATTEMPTS,
                    attemptCount = newCount
                )
            }
            newCount >= 8 -> {
                val lockoutEnd = currentTime + (LOCKOUT_8_ATTEMPTS * 1000)
                securityPreferences.setLockoutEndTime(lockoutEnd)
                AuthFailureResult.TemporaryLockout(
                    seconds = LOCKOUT_8_ATTEMPTS,
                    attemptCount = newCount
                )
            }
            newCount >= 5 -> {
                val lockoutEnd = currentTime + (LOCKOUT_5_ATTEMPTS * 1000)
                securityPreferences.setLockoutEndTime(lockoutEnd)
                AuthFailureResult.TemporaryLockout(
                    seconds = LOCKOUT_5_ATTEMPTS,
                    attemptCount = newCount
                )
            }
            else -> {
                AuthFailureResult.NoLockout(newCount)
            }
        }
    }

    suspend fun isAuthenticationRequired(): Boolean {
        val isSetUp = securityPreferences.isWalletSetUp.first()
        if (!isSetUp) return false

        val autoLockTimeout = userPreferences.autoLockTimeout.first()
        val lastAuth = securityPreferences.lastAuthTimestamp.first()
        val currentTime = System.currentTimeMillis()

        return when (autoLockTimeout) {
            AutoLockTimeout.NEVER -> false
            AutoLockTimeout.IMMEDIATE -> true
            else -> {
                val elapsedSeconds = (currentTime - lastAuth) / 1000
                elapsedSeconds > autoLockTimeout.seconds
            }
        }
    }

    // Terminal state after WIPE_ATTEMPT_THRESHOLD failed attempts: destroy the wallet rather
    // than merely locking it out. Encrypted material, KeyStore keys and every DataStore are
    // cleared — including the failed-attempt counter and the wallet-set-up flag — so the app
    // restarts in onboarding with no wallet and no credentials.
    private suspend fun wipeWallet() {
        walletStore.wipeAll()
        keyStoreManager.deleteAllKeys()
        resetApp()
    }

    suspend fun resetApp() {
        userPreferences.clearAll()
        securityPreferences.clearAll()
        walletMetadata.clearAll()
    }
}
