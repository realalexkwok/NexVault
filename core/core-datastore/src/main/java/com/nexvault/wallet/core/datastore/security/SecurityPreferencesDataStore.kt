package com.nexvault.wallet.core.datastore.security

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexvault.wallet.core.security.encryption.EncryptionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.securityPreferencesDataStore by preferencesDataStore(name = "security_preferences")

enum class AuthMethod {
    PIN,
    PASSWORD,
}

@Singleton
class SecurityPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: EncryptionManager,
) {
    private val dataStore: DataStore<Preferences> = context.securityPreferencesDataStore

    companion object {
        private val KEY_IS_WALLET_SET_UP = booleanPreferencesKey("is_wallet_set_up")
        private val KEY_IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
        private val KEY_AUTH_METHOD = stringPreferencesKey("auth_method")
        private val KEY_FAILED_ATTEMPT_COUNT = intPreferencesKey("failed_attempt_count")
        private val KEY_LOCKOUT_END_TIME = longPreferencesKey("lockout_end_time")
        private val KEY_PASSWORD_HASH = stringPreferencesKey("password_hash")
        private val KEY_PASSWORD_SALT = stringPreferencesKey("password_salt")
        private val KEY_ACTIVE_WALLET_ID = stringPreferencesKey("active_wallet_id")
        private val KEY_ACTIVE_ACCOUNT_INDEX = intPreferencesKey("active_account_index")
        private val KEY_LAST_AUTH_TIMESTAMP = longPreferencesKey("last_auth_timestamp")
    }

    val isWalletSetUp: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_IS_WALLET_SET_UP] ?: false
    }

    suspend fun setWalletSetUp(isSetUp: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_IS_WALLET_SET_UP] = isSetUp
        }
    }

    val isBiometricEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_IS_BIOMETRIC_ENABLED] ?: false
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_IS_BIOMETRIC_ENABLED] = enabled
        }
    }

    val authMethod: Flow<AuthMethod> = dataStore.data.map { preferences ->
        val value = preferences[KEY_AUTH_METHOD] ?: AuthMethod.PIN.name
        try {
            AuthMethod.valueOf(value)
        } catch (e: IllegalArgumentException) {
            AuthMethod.PIN
        }
    }

    suspend fun setAuthMethod(method: AuthMethod) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTH_METHOD] = method.name
        }
    }

    val failedAttemptCount: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_FAILED_ATTEMPT_COUNT] ?: 0
    }

    suspend fun incrementFailedAttempts(): Int {
        var newCount = 0
        dataStore.edit { preferences ->
            val current = preferences[KEY_FAILED_ATTEMPT_COUNT] ?: 0
            newCount = current + 1
            preferences[KEY_FAILED_ATTEMPT_COUNT] = newCount
        }
        return newCount
    }

    suspend fun resetFailedAttempts() {
        dataStore.edit { preferences ->
            preferences[KEY_FAILED_ATTEMPT_COUNT] = 0
        }
    }

    val lockoutEndTime: Flow<Long> = dataStore.data.map { preferences ->
        preferences[KEY_LOCKOUT_END_TIME] ?: 0L
    }

    suspend fun setLockoutEndTime(time: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_LOCKOUT_END_TIME] = time
        }
    }

    suspend fun storePasswordHash(hash: ByteArray, salt: ByteArray) {
        val encryptedHash = encryptionManager.encryptWithKeystore(hash)
        val encryptedSalt = encryptionManager.encryptWithKeystore(salt)

        dataStore.edit { preferences ->
            preferences[KEY_PASSWORD_HASH] = Base64.encodeToString(encryptedHash, Base64.NO_WRAP)
            preferences[KEY_PASSWORD_SALT] = Base64.encodeToString(encryptedSalt, Base64.NO_WRAP)
        }
    }

    suspend fun getPasswordHash(): Pair<ByteArray, ByteArray>? {
        return try {
            val preferences = dataStore.data.first()
            val encryptedHashStr = preferences[KEY_PASSWORD_HASH]
            val encryptedSaltStr = preferences[KEY_PASSWORD_SALT]

            if (encryptedHashStr != null && encryptedSaltStr != null) {
                val encryptedHash = Base64.decode(encryptedHashStr, Base64.NO_WRAP)
                val encryptedSalt = Base64.decode(encryptedSaltStr, Base64.NO_WRAP)

                val hash = encryptionManager.decryptWithKeystore(encryptedHash)
                val salt = encryptionManager.decryptWithKeystore(encryptedSalt)

                Pair(hash, salt)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun hasPasswordHash(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_PASSWORD_HASH] != null
    }

    val activeWalletId: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_WALLET_ID] ?: ""
    }

    suspend fun setActiveWalletId(walletId: String) {
        dataStore.edit { preferences ->
            preferences[KEY_ACTIVE_WALLET_ID] = walletId
        }
    }

    val activeAccountIndex: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_ACCOUNT_INDEX] ?: 0
    }

    suspend fun setActiveAccountIndex(index: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_ACTIVE_ACCOUNT_INDEX] = index
        }
    }

    val lastAuthTimestamp: Flow<Long> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_AUTH_TIMESTAMP] ?: 0L
    }

    suspend fun setLastAuthTimestamp(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_AUTH_TIMESTAMP] = timestamp
        }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
