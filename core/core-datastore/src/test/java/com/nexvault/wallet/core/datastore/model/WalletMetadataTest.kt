package com.nexvault.wallet.core.datastore.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WalletMetadataTest {
    @Test
    fun walletMetadata_creationWithAllFields() {
        val wallet = WalletMetadata(
            id = "test-uuid-1234",
            name = "Main Wallet",
            createdAt = 1700000000000L,
            type = WalletType.HD,
            accountCount = 5,
            isActive = true,
        )

        assertEquals("test-uuid-1234", wallet.id)
        assertEquals("Main Wallet", wallet.name)
        assertEquals(1700000000000L, wallet.createdAt)
        assertEquals(WalletType.HD, wallet.type)
        assertEquals(5, wallet.accountCount)
        assertEquals(true, wallet.isActive)
    }

    @Test
    fun accountMetadata_creationWithDerivationPath() {
        val account = AccountMetadata(
            walletId = "wallet-123",
            accountIndex = 0,
            address = "0x742d35Cc6634C0532925a3b844Bc9e7595f",
            name = "Account 1",
            derivationPath = "m/44'/60'/0'/0/0",
            isActive = true,
            addedAt = 1700000000000L,
        )

        assertEquals("wallet-123", account.walletId)
        assertEquals(0, account.accountIndex)
        assertEquals("0x742d35Cc6634C0532925a3b844Bc9e7595f", account.address)
        assertEquals("Account 1", account.name)
        assertEquals("m/44'/60'/0'/0/0", account.derivationPath)
        assertEquals(true, account.isActive)
        assertEquals(1700000000000L, account.addedAt)
    }

    @Test
    fun walletType_hasAllExpectedValues() {
        val values = WalletType.entries
        assertEquals(2, values.size)
        assert(WalletType.HD in values)
        assert(WalletType.IMPORTED in values)
    }

    @Test
    fun importedWallet_metadataCreation() {
        val wallet = WalletMetadata(
            id = "imported-wallet",
            name = "Imported Key",
            createdAt = 1700000000000L,
            type = WalletType.IMPORTED,
            accountCount = 1,
            isActive = false,
        )

        assertEquals(WalletType.IMPORTED, wallet.type)
        assertEquals(1, wallet.accountCount)
    }
}
