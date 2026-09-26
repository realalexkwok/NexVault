package com.nexvault.wallet.domain.model.auth

/**
 * Authentication-related domain models.
 */
data class AuthState(
    val isWalletSetUp: Boolean,
    val isLocked: Boolean,
    val isLockedOut: Boolean,
    val lockoutRemainingSeconds: Long,
    val authMethod: AuthMethod,
    val isBiometricEnabled: Boolean,
    val isBiometricAvailable: Boolean,
)

enum class AuthMethod {
    PIN,
    PASSWORD,
}

sealed class AuthResult {
    data object Success : AuthResult()

    data class Failed(val remainingAttempts: Int?, val message: String) : AuthResult()

    data class LockedOut(val durationSeconds: Long) : AuthResult()

    data object WalletWiped : AuthResult()
}

/**
 * Result of wallet creation or import.
 */
data class WalletCreationResult(
    val walletId: String,
    val address: String,
    val mnemonicWords: List<String>,
)
