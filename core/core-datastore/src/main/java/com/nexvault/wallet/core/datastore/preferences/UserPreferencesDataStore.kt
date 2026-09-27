package com.nexvault.wallet.core.datastore.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexvault.wallet.core.datastore.model.AutoLockTimeout
import com.nexvault.wallet.core.datastore.model.NetworkType
import com.nexvault.wallet.core.datastore.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore: DataStore<Preferences> = context.userPreferencesDataStore

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_FIAT_CURRENCY = stringPreferencesKey("fiat_currency")
        private val KEY_SELECTED_NETWORK = stringPreferencesKey("selected_network")
        private val KEY_CUSTOM_RPC_URL = stringPreferencesKey("custom_rpc_url")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_TRANSACTION_ALERTS = booleanPreferencesKey("transaction_alerts")
        private val KEY_PRICE_ALERTS = booleanPreferencesKey("price_alerts")
        private val KEY_HIDE_BALANCES = booleanPreferencesKey("hide_balances")
        private val KEY_SHOW_TEST_NETWORKS = booleanPreferencesKey("show_test_networks")
        private val KEY_AUTO_LOCK_TIMEOUT = stringPreferencesKey("auto_lock_timeout")
        private val KEY_REQUIRE_AUTH_FOR_TRANSACTIONS = booleanPreferencesKey("require_auth_for_transactions")
        private val KEY_HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        private val KEY_HAS_BACKED_UP_MNEMONIC = booleanPreferencesKey("has_backed_up_mnemonic")
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        val value = preferences[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }

    val fiatCurrency: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_FIAT_CURRENCY] ?: "USD"
    }

    suspend fun setFiatCurrency(currency: String) {
        dataStore.edit { preferences ->
            preferences[KEY_FIAT_CURRENCY] = currency
        }
    }

    val selectedNetwork: Flow<NetworkType> = dataStore.data.map { preferences ->
        val value = preferences[KEY_SELECTED_NETWORK] ?: NetworkType.MAINNET.name
        try {
            NetworkType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            NetworkType.MAINNET
        }
    }

    suspend fun setSelectedNetwork(network: NetworkType) {
        dataStore.edit { preferences ->
            preferences[KEY_SELECTED_NETWORK] = network.name
        }
    }

    val customRpcUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_CUSTOM_RPC_URL] ?: ""
    }

    suspend fun setCustomRpcUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[KEY_CUSTOM_RPC_URL] = url
        }
    }

    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    val transactionAlerts: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_TRANSACTION_ALERTS] ?: true
    }

    suspend fun setTransactionAlerts(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_TRANSACTION_ALERTS] = enabled
        }
    }

    val priceAlerts: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_PRICE_ALERTS] ?: true
    }

    suspend fun setPriceAlerts(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_PRICE_ALERTS] = enabled
        }
    }

    val hideBalances: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_HIDE_BALANCES] ?: false
    }

    suspend fun setHideBalances(hide: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HIDE_BALANCES] = hide
        }
    }

    val showTestNetworks: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_SHOW_TEST_NETWORKS] ?: false
    }

    suspend fun setShowTestNetworks(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_TEST_NETWORKS] = show
        }
    }

    val autoLockTimeout: Flow<AutoLockTimeout> = dataStore.data.map { preferences ->
        val value = preferences[KEY_AUTO_LOCK_TIMEOUT] ?: AutoLockTimeout.FIVE_MINUTES.name
        try {
            AutoLockTimeout.valueOf(value)
        } catch (e: IllegalArgumentException) {
            AutoLockTimeout.FIVE_MINUTES
        }
    }

    suspend fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_LOCK_TIMEOUT] = timeout.name
        }
    }

    val requireAuthForTransactions: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_REQUIRE_AUTH_FOR_TRANSACTIONS] ?: true
    }

    suspend fun setRequireAuthForTransactions(required: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_REQUIRE_AUTH_FOR_TRANSACTIONS] = required
        }
    }

    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_HAS_COMPLETED_ONBOARDING] ?: false
    }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    val hasBackedUpMnemonic: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_HAS_BACKED_UP_MNEMONIC] ?: false
    }

    suspend fun setHasBackedUpMnemonic(backedUp: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HAS_BACKED_UP_MNEMONIC] = backedUp
        }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
