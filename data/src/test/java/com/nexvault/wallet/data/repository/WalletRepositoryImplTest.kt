package com.nexvault.wallet.data.repository

import com.nexvault.wallet.core.datastore.model.AccountMetadata
import com.nexvault.wallet.core.datastore.model.WalletMetadata
import com.nexvault.wallet.core.datastore.model.WalletType
import com.nexvault.wallet.core.datastore.security.SecurityPreferencesDataStore
import com.nexvault.wallet.core.datastore.wallet.WalletMetadataDataStore
import com.nexvault.wallet.core.security.mnemonic.MnemonicManager
import com.nexvault.wallet.core.security.wallet.HDKeyManager
import com.nexvault.wallet.core.security.wallet.WalletStore
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.InvalidMnemonicException
import com.nexvault.wallet.domain.model.common.WalletNotFoundException
import com.nexvault.wallet.domain.repository.TokenRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WalletRepositoryImplTest {

    private lateinit var mnemonicManager: MnemonicManager
    private lateinit var hdKeyManager: HDKeyManager
    private lateinit var walletStore: WalletStore
    private lateinit var securityPreferences: SecurityPreferencesDataStore
    private lateinit var walletMetadataStore: WalletMetadataDataStore
    private lateinit var tokenRepository: TokenRepository
    private lateinit var repository: WalletRepositoryImpl

    @Before
    fun setup() {
        mnemonicManager = mockk(relaxed = true)
        hdKeyManager = mockk(relaxed = true)
        walletStore = mockk(relaxed = true)
        securityPreferences = mockk(relaxed = true)
        walletMetadataStore = mockk(relaxed = true)
        tokenRepository = mockk(relaxed = true)
        coEvery { tokenRepository.seedDefaultTokens(any()) } returns DataResult.Success(Unit)

        repository = WalletRepositoryImpl(
            mnemonicManager,
            hdKeyManager,
            walletStore,
            securityPreferences,
            walletMetadataStore,
            tokenRepository,
        )
    }

    @Test
    fun createWallet_success_returnsResult() = runTest {
        // Given - use valid 12-word mnemonic
        coEvery { mnemonicManager.generateMnemonic() } returns "abandon about after again agent air allow almost always amount angle animal"
        coEvery { mnemonicManager.mnemonicToSeed(any(), any()) } returns ByteArray(64) { 0 }
        every { hdKeyManager.deriveEthereumKeyPair(any(), any(), any()) } returns mockk(relaxed = true)
        every { hdKeyManager.deriveAddress(any()) } returns "0xABC123"
        coEvery { walletStore.storeMnemonic(any(), any()) } returns Unit
        coEvery { walletMetadataStore.addWallet(any()) } returns Unit
        coEvery { walletMetadataStore.addAccount(any()) } returns Unit
        coEvery { securityPreferences.setActiveWalletId(any()) } returns Unit
        coEvery { securityPreferences.setActiveAccountIndex(any()) } returns Unit
        coEvery { securityPreferences.setWalletSetUp(any()) } returns Unit

        // When
        val result = repository.createWallet("My Wallet")

        // Then
        assertTrue(result is DataResult.Success)
        val success = result as DataResult.Success
        assertNotNull(success.data.walletId)
        assertEquals("0xABC123", success.data.address)
        assertEquals(12, success.data.mnemonicWords.size)

        coVerify { walletStore.storeMnemonic(any(), any()) }
        coVerify { securityPreferences.setWalletSetUp(true) }
    }

    @Test
    fun importFromMnemonic_valid_returnsSuccess() = runTest {
        // Given
        every { mnemonicManager.validateMnemonic(any()) } returns true
        coEvery { mnemonicManager.mnemonicToSeed(any(), any()) } returns ByteArray(64) { 0 }
        every { hdKeyManager.deriveEthereumKeyPair(any(), any(), any()) } returns mockk(relaxed = true)
        every { hdKeyManager.deriveAddress(any()) } returns "0xDEF456"
        coEvery { walletStore.storeMnemonic(any(), any()) } returns Unit
        coEvery { walletMetadataStore.addWallet(any()) } returns Unit
        coEvery { walletMetadataStore.addAccount(any()) } returns Unit
        coEvery { securityPreferences.setActiveWalletId(any()) } returns Unit
        coEvery { securityPreferences.setActiveAccountIndex(any()) } returns Unit
        coEvery { securityPreferences.setWalletSetUp(any()) } returns Unit

        // When
        val result = repository.importFromMnemonic(
            "abandon about after again agent air",
            "Imported"
        )

        // Then
        assertTrue(result is DataResult.Success)
        val success = result as DataResult.Success
        assertEquals("0xDEF456", success.data.address)
    }

    @Test
    fun importFromMnemonic_invalid_returnsError() = runTest {
        // Given
        every { mnemonicManager.validateMnemonic(any()) } returns false

        // When
        val result = repository.importFromMnemonic("invalid mnemonic", "Test")

        // Then
        assertTrue(result is DataResult.Error)
        val error = result as DataResult.Error
        assertTrue(error.exception is InvalidMnemonicException)
    }

    @Test
    fun importFromPrivateKey_success_returnsResult() = runTest {
        // Given
        coEvery { walletStore.storePrivateKey(any(), any(), any()) } returns Unit
        coEvery { walletMetadataStore.addWallet(any()) } returns Unit
        coEvery { walletMetadataStore.addAccount(any()) } returns Unit
        coEvery { securityPreferences.setActiveWalletId(any()) } returns Unit
        coEvery { securityPreferences.setActiveAccountIndex(any()) } returns Unit
        coEvery { securityPreferences.setWalletSetUp(any()) } returns Unit

        // When
        val result = repository.importFromPrivateKey(
            "0000000000000000000000000000000000000000000000000000000000000001",
            "PK Wallet"
        )

        // Then
        assertTrue(result is DataResult.Success)
        val success = result as DataResult.Success
        assertTrue(success.data.mnemonicWords.isEmpty())
    }

    @Test
    fun importFromPrivateKey_storesLiveKeyThenWipesIt() = runTest {
        val keyHex = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2"
        var bytesAtCallTime: ByteArray? = null
        var referenceAtCallTime: ByteArray? = null
        coEvery { walletStore.storePrivateKey(any(), any(), any()) } answers {
            referenceAtCallTime = secondArg()
            bytesAtCallTime = secondArg<ByteArray>().copyOf()
        }
        coEvery { walletMetadataStore.addWallet(any()) } returns Unit
        coEvery { walletMetadataStore.addAccount(any()) } returns Unit
        coEvery { securityPreferences.setActiveWalletId(any()) } returns Unit
        coEvery { securityPreferences.setActiveAccountIndex(any()) } returns Unit
        coEvery { securityPreferences.setWalletSetUp(any()) } returns Unit

        val result = repository.importFromPrivateKey(keyHex, "PK Wallet")

        assertTrue(result is DataResult.Success)
        // CR 1.6-2: the store must receive the real key material, not the pre-wiped zeros.
        val storedAtCallTime = bytesAtCallTime ?: error("storePrivateKey was never called")
        assertEquals(32, storedAtCallTime.size)
        assertTrue(
            "the key handed to the store must not be all zeros",
            storedAtCallTime.any { it != 0.toByte() },
        )
        assertTrue(
            "the key must be wiped once the import returns",
            referenceAtCallTime!!.all { it == 0.toByte() },
        )
    }

    @Test
    fun getMnemonicForBackup_success_returnsWords() = runTest {
        // Given - use valid 12-word mnemonic
        coEvery { walletStore.retrieveMnemonic(any()) } returns "abandon about after again agent air allow almost always amount angle animal"

        // When
        val result = repository.getMnemonicForBackup("walletId123")

        // Then
        assertTrue(result is DataResult.Success)
        val success = result as DataResult.Success
        assertEquals(12, success.data.size)
    }

    @Test
    fun getMnemonicForBackup_notFound_returnsError() = runTest {
        // Given
        coEvery { walletStore.retrieveMnemonic(any()) } throws IllegalStateException("No wallet data found")

        // When
        val result = repository.getMnemonicForBackup("nonexistent")

        // Then
        assertTrue(result is DataResult.Error)
    }

    @Test
    fun deleteWallet_success() = runTest {
        // Given
        coEvery { walletStore.wipeWalletData(any()) } returns Unit
        coEvery { walletMetadataStore.removeWallet(any()) } returns Unit
        every { walletMetadataStore.wallets } returns flowOf(emptyList())
        coEvery { securityPreferences.setWalletSetUp(any()) } returns Unit
        coEvery { securityPreferences.setActiveWalletId(any()) } returns Unit
        coEvery { securityPreferences.setActiveAccountIndex(any()) } returns Unit

        // When
        val result = repository.deleteWallet("walletId123")

        // Then
        assertTrue(result is DataResult.Success)
        coVerify { walletStore.wipeWalletData("walletId123") }
        coVerify { walletMetadataStore.removeWallet("walletId123") }
    }

    @Test
    fun hasWallet_reflectsDataStoreState() = runTest {
        // Given
        every { securityPreferences.isWalletSetUp } returns flowOf(true)

        // When & Then
        repository.hasWallet().collect { hasWallet ->
            assertTrue(hasWallet)
        }
    }

    @Test
    fun setActiveWallet_success() = runTest {
        // Given
        coEvery { securityPreferences.setActiveWalletId(any()) } returns Unit
        coEvery { securityPreferences.setActiveAccountIndex(any()) } returns Unit

        // When
        val result = repository.setActiveWallet("walletId123")

        // Then
        assertTrue(result is DataResult.Success)
        coVerify { securityPreferences.setActiveWalletId("walletId123") }
        coVerify { securityPreferences.setActiveAccountIndex(0) }
    }

    @Test
    fun getActiveWallet_returnsWalletWithActiveAccount() = runTest {
        // Given
        val walletMeta = WalletMetadata(
            id = "walletId123",
            name = "My Wallet",
            createdAt = System.currentTimeMillis(),
            type = WalletType.HD,
            accountCount = 1,
            isActive = true
        )
        val accounts = listOf(
            AccountMetadata(
                walletId = "walletId123",
                accountIndex = 0,
                address = "0xABC123",
                name = "Account 1",
                derivationPath = "m/44'/60'/0'/0/0",
                isActive = false,
                addedAt = System.currentTimeMillis()
            )
        )

        every { walletMetadataStore.wallets } returns flowOf(listOf(walletMeta))
        every { securityPreferences.activeWalletId } returns flowOf("walletId123")
        every { securityPreferences.activeAccountIndex } returns flowOf(0)
        every { walletMetadataStore.accountsForWallet(any()) } returns flowOf(accounts)

        // When & Then
        repository.getActiveWallet().collect { wallet ->
            assertNotNull(wallet)
            assertEquals("walletId123", wallet?.id)
            assertTrue(wallet?.accounts?.any { it.isActive } == true)
        }
    }
}
