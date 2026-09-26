package com.nexvault.wallet.core.datastore.wallet

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexvault.wallet.core.datastore.model.AccountMetadata
import com.nexvault.wallet.core.datastore.model.WalletMetadata
import com.nexvault.wallet.core.datastore.util.DataStoreSerializer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.walletMetadataDataStore by preferencesDataStore(name = "wallet_metadata")

@Singleton
class WalletMetadataDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore: DataStore<Preferences> = context.walletMetadataDataStore

    companion object {
        private val KEY_WALLETS = stringPreferencesKey("wallets")
        private val KEY_ACCOUNTS_PREFIX = "accounts_"
    }

    val wallets: Flow<List<WalletMetadata>> = dataStore.data.map { preferences ->
        val json = preferences[KEY_WALLETS] ?: ""
        DataStoreSerializer.deserializeWallets(json)
    }

    suspend fun addWallet(wallet: WalletMetadata) {
        dataStore.edit { preferences ->
            val currentJson = preferences[KEY_WALLETS] ?: ""
            val currentList = DataStoreSerializer.deserializeWallets(currentJson).toMutableList()
            currentList.add(wallet)
            preferences[KEY_WALLETS] = DataStoreSerializer.serializeWallets(currentList)
        }
    }

    suspend fun updateWallet(wallet: WalletMetadata) {
        dataStore.edit { preferences ->
            val currentJson = preferences[KEY_WALLETS] ?: ""
            val currentList = DataStoreSerializer.deserializeWallets(currentJson).toMutableList()
            val index = currentList.indexOfFirst { it.id == wallet.id }
            if (index >= 0) {
                currentList[index] = wallet
            }
            preferences[KEY_WALLETS] = DataStoreSerializer.serializeWallets(currentList)
        }
    }

    suspend fun removeWallet(walletId: String) {
        dataStore.edit { preferences ->
            val currentJson = preferences[KEY_WALLETS] ?: ""
            val currentList = DataStoreSerializer.deserializeWallets(currentJson).toMutableList()
            currentList.removeAll { it.id == walletId }
            preferences[KEY_WALLETS] = DataStoreSerializer.serializeWallets(currentList)

            preferences.remove(stringPreferencesKey("$KEY_ACCOUNTS_PREFIX$walletId"))
        }
    }

    private fun accountsKey(walletId: String) = stringPreferencesKey("$KEY_ACCOUNTS_PREFIX$walletId")

    val accounts: Flow<List<AccountMetadata>> = dataStore.data.map { preferences ->
        val allAccounts = mutableListOf<AccountMetadata>()
        preferences.asMap().forEach { (key, value) ->
            if (key.name.startsWith(KEY_ACCOUNTS_PREFIX) && value is String) {
                allAccounts.addAll(DataStoreSerializer.deserializeAccounts(value))
            }
        }
        allAccounts.sortedBy { it.accountIndex }
    }

    fun accountsForWallet(walletId: String): Flow<List<AccountMetadata>> {
        return dataStore.data.map { preferences ->
            val json = preferences[accountsKey(walletId)] ?: ""
            DataStoreSerializer.deserializeAccounts(json).sortedBy { it.accountIndex }
        }
    }

    suspend fun addAccount(account: AccountMetadata) {
        dataStore.edit { preferences ->
            val key = accountsKey(account.walletId)
            val currentJson = preferences[key] ?: ""
            val currentList = DataStoreSerializer.deserializeAccounts(currentJson).toMutableList()
            currentList.add(account)
            preferences[key] = DataStoreSerializer.serializeAccounts(currentList)

            val walletsJson = preferences[KEY_WALLETS] ?: ""
            val walletList = DataStoreSerializer.deserializeWallets(walletsJson).toMutableList()
            val walletIndex = walletList.indexOfFirst { it.id == account.walletId }
            if (walletIndex >= 0) {
                val wallet = walletList[walletIndex]
                walletList[walletIndex] = wallet.copy(accountCount = currentList.size)
                preferences[KEY_WALLETS] = DataStoreSerializer.serializeWallets(walletList)
            }
        }
    }

    suspend fun updateAccount(account: AccountMetadata) {
        dataStore.edit { preferences ->
            val key = accountsKey(account.walletId)
            val currentJson = preferences[key] ?: ""
            val currentList = DataStoreSerializer.deserializeAccounts(currentJson).toMutableList()
            val index = currentList.indexOfFirst { it.accountIndex == account.accountIndex }
            if (index >= 0) {
                currentList[index] = account
            }
            preferences[key] = DataStoreSerializer.serializeAccounts(currentList)
        }
    }

    suspend fun removeAccount(walletId: String, accountIndex: Int) {
        dataStore.edit { preferences ->
            val key = accountsKey(walletId)
            val currentJson = preferences[key] ?: ""
            val currentList = DataStoreSerializer.deserializeAccounts(currentJson).toMutableList()
            currentList.removeAll { it.accountIndex == accountIndex }
            preferences[key] = DataStoreSerializer.serializeAccounts(currentList)

            val walletsJson = preferences[KEY_WALLETS] ?: ""
            val walletList = DataStoreSerializer.deserializeWallets(walletsJson).toMutableList()
            val walletIndex = walletList.indexOfFirst { it.id == walletId }
            if (walletIndex >= 0) {
                val wallet = walletList[walletIndex]
                walletList[walletIndex] = wallet.copy(accountCount = currentList.size)
                preferences[KEY_WALLETS] = DataStoreSerializer.serializeWallets(walletList)
            }
        }
    }

    suspend fun getActiveWalletWithAccounts(): Pair<WalletMetadata, List<AccountMetadata>>? {
        val preferences = dataStore.data.first()
        val walletsJson = preferences[KEY_WALLETS] ?: ""
        val walletList = DataStoreSerializer.deserializeWallets(walletsJson)

        val activeWallet = walletList.find { it.isActive } ?: return null
        val accountsJson = preferences[accountsKey(activeWallet.id)] ?: ""
        val accountsList = DataStoreSerializer.deserializeAccounts(accountsJson)

        return Pair(activeWallet, accountsList)
    }

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
