package com.nexvault.wallet.data.mapper

import com.nexvault.wallet.core.datastore.model.AccountMetadata
import com.nexvault.wallet.core.datastore.model.WalletMetadata
import com.nexvault.wallet.domain.model.wallet.Account
import com.nexvault.wallet.domain.model.wallet.Wallet
import com.nexvault.wallet.core.datastore.model.WalletType as DataStoreWalletType
import com.nexvault.wallet.domain.model.wallet.WalletType as DomainWalletType

/**
 * Maps datastore WalletMetadata + AccountMetadata to domain Wallet model.
 */
object WalletMapper {
    fun mapToDomain(
        walletMetadata: WalletMetadata,
        accounts: List<AccountMetadata>,
        activeWalletId: String?,
    ): Wallet {
        return Wallet(
            id = walletMetadata.id,
            name = walletMetadata.name,
            type = mapWalletType(walletMetadata.type),
            accounts = accounts.map { mapAccountToDomain(it) },
            createdAt = walletMetadata.createdAt,
            isActive = walletMetadata.id == activeWalletId,
        )
    }

    fun mapAccountToDomain(
        accountMetadata: AccountMetadata,
        activeAccountIndex: Int? = null,
    ): Account {
        return Account(
            walletId = accountMetadata.walletId,
            index = accountMetadata.accountIndex,
            address = accountMetadata.address,
            name = accountMetadata.name,
            derivationPath = accountMetadata.derivationPath,
            isActive = activeAccountIndex?.let { accountMetadata.accountIndex == it } ?: accountMetadata.isActive,
        )
    }

    fun mapWalletType(
        datastoreType: DataStoreWalletType,
    ): DomainWalletType {
        return when (datastoreType) {
            DataStoreWalletType.HD -> DomainWalletType.HD
            DataStoreWalletType.IMPORTED -> DomainWalletType.IMPORTED
        }
    }

    fun mapWalletTypeToDataStore(
        domainType: DomainWalletType,
    ): DataStoreWalletType {
        return when (domainType) {
            DomainWalletType.HD -> DataStoreWalletType.HD
            DomainWalletType.IMPORTED -> DataStoreWalletType.IMPORTED
        }
    }

    fun mapToDataStore(
        wallet: Wallet,
    ): WalletMetadata {
        return WalletMetadata(
            id = wallet.id,
            name = wallet.name,
            createdAt = wallet.createdAt,
            type = mapWalletTypeToDataStore(wallet.type),
            accountCount = wallet.accounts.size,
            isActive = wallet.isActive,
        )
    }

    fun mapAccountToDataStore(
        account: Account,
    ): AccountMetadata {
        return AccountMetadata(
            walletId = account.walletId,
            accountIndex = account.index,
            address = account.address,
            name = account.name,
            derivationPath = account.derivationPath,
            isActive = account.isActive,
            addedAt = System.currentTimeMillis(),
        )
    }
}
