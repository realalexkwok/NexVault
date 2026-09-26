package com.nexvault.wallet.data.repository

import com.nexvault.wallet.core.datastore.model.AutoLockTimeout
import com.nexvault.wallet.core.datastore.preferences.UserPreferencesDataStore
import com.nexvault.wallet.core.datastore.security.SecurityPreferencesDataStore
import com.nexvault.wallet.core.datastore.state.AppStateManager
import com.nexvault.wallet.core.datastore.state.AuthFailureResult
import com.nexvault.wallet.core.security.biometric.BiometricHelper
import com.nexvault.wallet.core.security.util.SecurityUtils
import com.nexvault.wallet.data.mapper.AuthMapper
import com.nexvault.wallet.domain.model.auth.AuthMethod
import com.nexvault.wallet.domain.model.auth.AuthResult
import com.nexvault.wallet.domain.model.auth.AuthState
import com.nexvault.wallet.domain.model.common.AuthenticationException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val securityPreferences: SecurityPreferencesDataStore,
    private val appStateManager: AppStateManager,
    private val biometricHelper: BiometricHelper,
    private val userPreferences: UserPreferencesDataStore,
) : AuthRepository {
    override suspend fun setPin(pin: String): DataResult<Unit> {
        return try {
            val hash = SecurityUtils.hashPassword(pin)
            val hashBytes = hash.toByteArray(Charsets.UTF_8)
            val salt = ByteArray(16) // Salt is embedded in hash string
            securityPreferences.storePasswordHash(hashBytes, salt)
            securityPreferences.setAuthMethod(com.nexvault.wallet.core.datastore.security.AuthMethod.PIN)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(AuthenticationException("Failed to set PIN: ${e.message}"))
        }
    }

    override suspend fun verifyPin(pin: String): AuthResult {
        return try {
            val isLockedOut = appStateManager.isLockedOut.first()
            if (isLockedOut) {
                val remaining = appStateManager.lockoutRemainingSeconds.first()
                return AuthResult.LockedOut(remaining)
            }

            val storedHashPair = securityPreferences.getPasswordHash()
            if (storedHashPair == null) {
                return AuthResult.Failed(
                    remainingAttempts = null,
                    message = "No PIN set",
                )
            }

            val (storedHash, _) = storedHashPair
            val storedHashString = String(storedHash, Charsets.UTF_8)
            val isValid = SecurityUtils.verifyPassword(pin, storedHashString)

            if (isValid) {
                appStateManager.onAuthenticationSuccess()
                securityPreferences.setLastAuthTimestamp(System.currentTimeMillis())
                AuthResult.Success
            } else {
                val failureResult = appStateManager.onAuthenticationFailure()
                mapAuthFailureResult(failureResult)
            }
        } catch (e: Exception) {
            AuthResult.Failed(
                remainingAttempts = null,
                message = "Verification error: ${e.message}",
            )
        }
    }

    private fun mapAuthFailureResult(result: AuthFailureResult): AuthResult {
        return when (result) {
            is AuthFailureResult.WalletWiped -> AuthResult.WalletWiped
            is AuthFailureResult.TemporaryLockout -> AuthResult.LockedOut(result.seconds)
            is AuthFailureResult.Warning -> AuthResult.Failed(
                remainingAttempts = result.attemptCount,
                message = result.message,
            )
            is AuthFailureResult.NoLockout -> AuthResult.Failed(
                remainingAttempts = result.attemptCount,
                message = "Incorrect PIN",
            )
        }
    }

    override suspend fun changePin(
        currentPin: String,
        newPin: String,
    ): DataResult<Unit> {
        val verifyResult = verifyPin(currentPin)
        if (verifyResult !is AuthResult.Success) {
            return DataResult.Error(AuthenticationException("Current PIN is incorrect"))
        }

        return setPin(newPin)
    }

    override suspend fun isBiometricAvailable(): Boolean {
        return biometricHelper.isBiometricAvailable() == com.nexvault.wallet.core.security.biometric.BiometricStatus.AVAILABLE
    }

    override suspend fun setBiometricEnabled(enabled: Boolean): DataResult<Unit> {
        return try {
            securityPreferences.setBiometricEnabled(enabled)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e, "Failed to update biometric setting")
        }
    }

    override fun isBiometricEnabled(): Flow<Boolean> {
        return securityPreferences.isBiometricEnabled
    }

    override fun getAuthState(): Flow<AuthState> {
        return combine(
            securityPreferences.isWalletSetUp,
            securityPreferences.isBiometricEnabled,
            securityPreferences.authMethod,
            securityPreferences.failedAttemptCount,
            securityPreferences.lockoutEndTime,
        ) { isWalletSetUp, isBiometricEnabled, authMethod, _, _ ->
            val isLockedOut = appStateManager.isLockedOut.first()
            val lockoutRemaining = if (isLockedOut) {
                appStateManager.lockoutRemainingSeconds.first()
            } else {
                0L
            }

            AuthMapper.createAuthState(
                isWalletSetUp = isWalletSetUp,
                isLocked = true,
                isLockedOut = isLockedOut,
                lockoutRemainingSeconds = lockoutRemaining,
                authMethod = AuthMapper.mapAuthMethod(authMethod),
                isBiometricEnabled = isBiometricEnabled,
                isBiometricAvailable = isBiometricAvailable(),
            )
        }
    }

    override suspend fun onAuthSuccess() {
        try {
            appStateManager.onAuthenticationSuccess()
            securityPreferences.setLastAuthTimestamp(System.currentTimeMillis())
        } catch (_: Exception) {
            // Best effort
        }
    }

    override suspend fun isAuthRequired(): Boolean {
        return try {
            val lastAuthTimestamp = securityPreferences.lastAuthTimestamp.first()
            val autoLockTimeout = userPreferences.autoLockTimeout.first()

            val timeoutMillis = autoLockTimeout.seconds * 1000L
            if (timeoutMillis <= 0) {
                return true // Immediate lock
            }

            val elapsed = System.currentTimeMillis() - lastAuthTimestamp
            elapsed > timeoutMillis
        } catch (_: Exception) {
            true
        }
    }

    override fun hasPinSet(): Flow<Boolean> {
        return securityPreferences.hasPasswordHash()
    }

    override fun getAutoLockTimeout(): Flow<Long> {
        return userPreferences.autoLockTimeout.map { timeout ->
            timeout.seconds
        }
    }

    override suspend fun setAutoLockTimeout(timeoutSeconds: Long): DataResult<Unit> {
        return try {
            val timeout = AutoLockTimeout.fromSeconds(timeoutSeconds)
            userPreferences.setAutoLockTimeout(timeout)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e, "Failed to set auto-lock timeout")
        }
    }
}
