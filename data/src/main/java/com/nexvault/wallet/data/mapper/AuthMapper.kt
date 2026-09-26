package com.nexvault.wallet.data.mapper

import com.nexvault.wallet.domain.model.auth.AuthState
import com.nexvault.wallet.core.datastore.security.AuthMethod as DataStoreAuthMethod
import com.nexvault.wallet.domain.model.auth.AuthMethod as DomainAuthMethod

/**
 * Maps datastore auth models to domain auth models.
 */
object AuthMapper {
    fun mapAuthMethod(
        datastoreMethod: DataStoreAuthMethod,
    ): DomainAuthMethod {
        return when (datastoreMethod) {
            DataStoreAuthMethod.PIN -> DomainAuthMethod.PIN
            DataStoreAuthMethod.PASSWORD -> DomainAuthMethod.PASSWORD
        }
    }

    fun mapAuthMethodToDataStore(
        domainMethod: DomainAuthMethod,
    ): DataStoreAuthMethod {
        return when (domainMethod) {
            DomainAuthMethod.PIN -> DataStoreAuthMethod.PIN
            DomainAuthMethod.PASSWORD -> DataStoreAuthMethod.PASSWORD
        }
    }

    fun createAuthState(
        isWalletSetUp: Boolean,
        isLocked: Boolean,
        isLockedOut: Boolean,
        lockoutRemainingSeconds: Long,
        authMethod: DomainAuthMethod,
        isBiometricEnabled: Boolean,
        isBiometricAvailable: Boolean,
    ): AuthState {
        return AuthState(
            isWalletSetUp = isWalletSetUp,
            isLocked = isLocked,
            isLockedOut = isLockedOut,
            lockoutRemainingSeconds = lockoutRemainingSeconds,
            authMethod = authMethod,
            isBiometricEnabled = isBiometricEnabled,
            isBiometricAvailable = isBiometricAvailable,
        )
    }
}
