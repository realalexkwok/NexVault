package com.nexvault.wallet.data.repository

import com.nexvault.wallet.core.datastore.model.AccountMetadata
import com.nexvault.wallet.core.datastore.model.WalletMetadata
import com.nexvault.wallet.core.datastore.model.WalletType
import com.nexvault.wallet.core.datastore.preferences.UserPreferencesDataStore
import com.nexvault.wallet.core.datastore.security.SecurityPreferencesDataStore
import com.nexvault.wallet.core.datastore.state.AppStateManager
import com.nexvault.wallet.core.datastore.wallet.WalletMetadataDataStore
import com.nexvault.wallet.core.security.mnemonic.MnemonicManager
import com.nexvault.wallet.core.security.wallet.HDKeyManager
import com.nexvault.wallet.core.security.wallet.WalletStore
import com.nexvault.wallet.domain.model.common.AuthenticationException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.InvalidMnemonicException
import com.nexvault.wallet.domain.model.common.WalletNotFoundException
import com.nexvault.wallet.domain.model.wallet.WalletDraft
import com.nexvault.wallet.domain.repository.TokenRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
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
    private lateinit var userPreferences: UserPreferencesDataStore
    private lateinit var appStateManager: AppStateManager
    private lateinit var tokenRepository: TokenRepository
    private lateinit var repository: WalletRepositoryImpl

    private val isUnlocked = MutableStateFlow(false)

    private val testMnemonic = "abandon about after again agent air allow almost always amount angle animal"
    private val testDraft = WalletDraft(
        mnemonic = testMnemonic,
        mnemonicWords = testMnemonic.split(" "),
        address = "0xABC123",
    )

    @Before
    fun setup() {
        mnemonicManager = mockk(relaxed = true)
        hdKeyManager = mockk(relaxed = true)
        walletStore = mockk(relaxed = true)
        securityPreferences = mockk(relaxed = true)
        walletMetadataStore = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
        appStateManager = mockk(relaxed = true)
        tokenRepository = mockk(relaxed = true)
        coEvery { tokenRepository.seedDefaultTokens(any()) } returns DataResult.Success(Unit)

        // Default: onboarding not finished, session locked.
        every { securityPreferences.isWalletSetUp } returns flowOf(false)
        every { appStateManager.isUnlocked } returns isUnlocked
        isUnlocked.value = false

        repository = WalletRepositoryImpl(
            mnemonicManager,
            hdKeyManager,
            walletStore,
            securityPreferences,
            walletMetadataStore,
            userPreferences,
            appStateManager,
            tokenRepository,
        )
    }

    @Test
    fun generateWallet_returnsDraftWithoutPersisting() = runTest {
        coEvery { mnemonicManager.generateMnemonic() } returns testMnemonic
        coEvery { mnemonicManager.mnemonicToSeed(any(), any()) } returns ByteArray(64) { 0 }
        every { hdKeyManager.deriveEthereumKeyPair(any(), any(), any()) } returns mockk(relaxed = true)
        every { hdKeyManager.deriveAddress(any()) } returns testDraft.address

        val result = repository.generateWallet()

        assertTrue(result is DataResult.Success)
        val draft = (result as DataResult.Success).data
        assertEquals(testMnemonic, draft.mnemonic)
        assertEquals(12, draft.mnemonicWords.size)
        assertEquals("0xABC123", draft.address)

        // Roadmap 2.0.2b option B: generation must not touch the disk.
        coVerify(exactly = 0) { walletStore.storeMnemonic(any()) }
        coVerify(exactly = 0) { walletMetadataStore.addWallet(any()) }
    }

    @Test
    fun createWallet_persistsDraftWithoutCompletingOnboarding() = runTest {
        coEvery { walletStore.storeMnemonic(any()) } returns Unit
        coEvery { walletMetadataStore.addWallet(any()) } returns Unit
        coEvery { walletMetadataStore.addAccount(any()) } returns Unit
        coEvery { securityPreferences.setActiveWalletId(any()) } returns Unit
        coEvery { securityPreferences.setActiveAccountIndex(any()) } returns Unit

        val result = repository.createWallet("My Wallet", testDraft)

        assertTrue(result is DataResult.Success)
        val success = result as DataResult.Success
        assertNotNull(success.data.walletId)
        assertEquals("0xABC123", success.data.address)
        assertEquals(12, success.data.mnemonicWords.size)

        coVerify { walletStore.storeMnemonic(testMnemonic) }
        // Option A: the flag is written by completeOnboarding, never here.
        coVerify(exactly = 0) { securityPreferences.setWalletSetUp(any()) }
    }

    @Test
    fun createWallet_whileOnboardingIncomplete_wipesAbandonedAttempt() = runTest {
        every { securityPreferences.isWalletSetUp } returns flowOf(false)

        repository.createWallet("My Wallet", testDraft)

        coVerify { walletStore.wipeAll() }
        coVerify { walletMetadataStore.clearAll() }
    }

    @Test
    fun createWallet_whenWalletAlreadySetUp_keepsExistingWallet() = runTest {
        every { securityPreferences.isWalletSetUp } returns flowOf(true)

        repository.createWallet("My Wallet", testDraft)

        coVerify(exactly = 0) { walletStore.wipeAll() }
        coVerify(exactly = 0) { walletMetadataStore.clearAll() }
    }

    @Test
    fun completeOnboarding_setsFlagRecordsCompletionAndUnlocks() = runTest {
        coEvery { securityPreferences.setWalletSetUp(any()) } returns Unit
        coEvery { userPreferences.setHasCompletedOnboarding(any()) } returns Unit

        val result = repository.completeOnboarding()

        assertTrue(result is DataResult.Success)
        coVerify { securityPreferences.setWalletSetUp(true) }
        coVerify { userPreferences.setHasCompletedOnboarding(true) }
        verify { appStateManager.unlock() }
    }

    @Test
    fun completeOnboarding_unlocksBeforeWritingTheFlag() = runTest {
        // Device-walk regression (2026-09-25): writing the flag first flipped the root router to
        // the auth graph, destroying the Set PIN screen and cancelling the ViewModel scope
        // mid-write — the flag landed but the session never unlocked. The session must lead.
        coEvery { securityPreferences.setWalletSetUp(any()) } returns Unit
        coEvery { userPreferences.setHasCompletedOnboarding(any()) } returns Unit

        repository.completeOnboarding()

        coVerifyOrder {
            appStateManager.unlock()
            securityPreferences.setWalletSetUp(true)
            userPreferences.setHasCompletedOnboarding(true)
        }
    }

    @Test
    fun importFromMnemonic_valid_persistsWithoutCompletingOnboarding() = runTest {
        every { mnemonicManager.validateMnemonic(any()) } returns true
        coEvery { mnemonicManager.mnemonicToSeed(any(), any()) } returns ByteArray(64) { 0 }
        every { hdKeyManager.deriveEthereumKeyPair(any(), any(), any()) } returns mockk(relaxed = true)
        every { hdKeyManager.deriveAddress(any()) } returns "0xDEF456"
        coEvery { walletStore.storeMnemonic(any()) } returns Unit
        coEvery { walletMetadataStore.addWallet(any()) } returns Unit
        coEvery { walletMetadataStore.addAccount(any()) } returns Unit
        coEvery { securityPreferences.setActiveWalletId(any()) } returns Unit
        coEvery { securityPreferences.setActiveAccountIndex(any()) } returns Unit

        val result = repository.importFromMnemonic(
            "abandon about after again agent air",
            "Imported"
        )

        assertTrue(result is DataResult.Success)
        val success = result as DataResult.Success
        assertEquals("0xDEF456", success.data.address)
        coVerify(exactly = 0) { securityPreferences.setWalletSetUp(any()) }
    }

    @Test
    fun importFromMnemonic_invalid_returnsError() = runTest {
        every { mnemonicManager.validateMnemonic(any()) } returns false

        val result = repository.importFromMnemonic("invalid mnemonic", "Test")

        assertTrue(result is DataResult.Error)
        val error = result as DataResult.Error
        assertTrue(error.exception is InvalidMnemonicException)
    }

    @Test
    fun importFromPrivateKey_success_returnsResult() = runTest {
        coEvery { walletStore.storePrivateKey(any(), any()) } returns Unit
        coEvery { walletMetadataStore.addWallet(any()) } returns Unit
        coEvery { walletMetadataStore.addAccount(any()) } returns Unit
        coEvery { securityPreferences.setActiveWalletId(any()) } returns Unit
        coEvery { securityPreferences.setActiveAccountIndex(any()) } returns Unit

        val result = repository.importFromPrivateKey(
            "0000000000000000000000000000000000000000000000000000000000000001",
            "PK Wallet"
        )

        assertTrue(result is DataResult.Success)
        val success = result as DataResult.Success
        assertTrue(success.data.mnemonicWords.isEmpty())
        coVerify(exactly = 0) { securityPreferences.setWalletSetUp(any()) }
    }

    @Test
    fun importFromPrivateKey_storesLiveKeyThenWipesIt() = runTest {
        val keyHex = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2"
        var bytesAtCallTime: ByteArray? = null
        var referenceAtCallTime: ByteArray? = null
        coEvery { walletStore.storePrivateKey(any(), any()) } answers {
            referenceAtCallTime = secondArg()
            bytesAtCallTime = secondArg<ByteArray>().copyOf()
        }
        coEvery { walletMetadataStore.addWallet(any()) } returns Unit
        coEvery { walletMetadataStore.addAccount(any()) } returns Unit
        coEvery { securityPreferences.setActiveWalletId(any()) } returns Unit
        coEvery { securityPreferences.setActiveAccountIndex(any()) } returns Unit

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
    fun getMnemonicForBackup_whenUnlocked_returnsWords() = runTest {
        isUnlocked.value = true
        every { securityPreferences.isWalletSetUp } returns flowOf(true)
        coEvery { walletStore.retrieveMnemonic() } returns testMnemonic

        val result = repository.getMnemonicForBackup("walletId123")

        assertTrue(result is DataResult.Success)
        assertEquals(12, (result as DataResult.Success).data.size)
    }

    @Test
    fun getMnemonicForBackup_whenLockedAfterOnboarding_returnsAuthError() = runTest {
        isUnlocked.value = false
        every { securityPreferences.isWalletSetUp } returns flowOf(true)

        val result = repository.getMnemonicForBackup("walletId123")

        assertTrue(result is DataResult.Error)
        assertTrue((result as DataResult.Error).exception is AuthenticationException)
        coVerify(exactly = 0) { walletStore.retrieveMnemonic() }
    }

    @Test
    fun getMnemonicForBackup_whenLockedDuringOnboarding_allowsVerifyStep() = runTest {
        isUnlocked.value = false
        every { securityPreferences.isWalletSetUp } returns flowOf(false)
        coEvery { walletStore.retrieveMnemonic() } returns testMnemonic

        val result = repository.getMnemonicForBackup("walletId123")

        assertTrue(result is DataResult.Success)
    }

    @Test
    fun getMnemonicForBackup_notFound_returnsError() = runTest {
        isUnlocked.value = true
        coEvery { walletStore.retrieveMnemonic() } throws IllegalStateException("No wallet data found")

        val result = repository.getMnemonicForBackup("nonexistent")

        assertTrue(result is DataResult.Error)
        assertTrue((result as DataResult.Error).exception is WalletNotFoundException)
    }

    @Test
    fun addAccount_whenLocked_returnsAuthError() = runTest {
        isUnlocked.value = false

        val result = repository.addAccount("walletId123", "Account 2")

        assertTrue(result is DataResult.Error)
        assertTrue((result as DataResult.Error).exception is AuthenticationException)
        coVerify(exactly = 0) { walletStore.retrieveMnemonic() }
    }

    @Test
    fun deleteWallet_success() = runTest {
        coEvery { walletStore.wipeWalletData(any()) } returns Unit
        coEvery { walletMetadataStore.removeWallet(any()) } returns Unit
        every { walletMetadataStore.wallets } returns flowOf(emptyList())
        coEvery { securityPreferences.setWalletSetUp(any()) } returns Unit
        coEvery { securityPreferences.setActiveWalletId(any()) } returns Unit
        coEvery { securityPreferences.setActiveAccountIndex(any()) } returns Unit

        val result = repository.deleteWallet("walletId123")

        assertTrue(result is DataResult.Success)
        coVerify { walletStore.wipeWalletData("walletId123") }
        coVerify { walletMetadataStore.removeWallet("walletId123") }
        // Last wallet gone → session locked so the root router falls back to onboarding.
        verify { appStateManager.lock() }
    }

    @Test
    fun deleteAllWallets_locksTheSession() = runTest {
        coEvery { walletStore.wipeAll() } returns Unit
        coEvery { walletMetadataStore.clearAll() } returns Unit
        coEvery { securityPreferences.setWalletSetUp(any()) } returns Unit
        coEvery { securityPreferences.setActiveWalletId(any()) } returns Unit
        coEvery { securityPreferences.setActiveAccountIndex(any()) } returns Unit

        val result = repository.deleteAllWallets()

        assertTrue(result is DataResult.Success)
        coVerify { securityPreferences.setWalletSetUp(false) }
        verify { appStateManager.lock() }
    }

    @Test
    fun hasWallet_reflectsDataStoreState() = runTest {
        every { securityPreferences.isWalletSetUp } returns flowOf(true)

        repository.hasWallet().collect { hasWallet ->
            assertTrue(hasWallet)
        }
    }

    @Test
    fun setActiveWallet_success() = runTest {
        coEvery { securityPreferences.setActiveWalletId(any()) } returns Unit
        coEvery { securityPreferences.setActiveAccountIndex(any()) } returns Unit

        val result = repository.setActiveWallet("walletId123")

        assertTrue(result is DataResult.Success)
        coVerify { securityPreferences.setActiveWalletId("walletId123") }
        coVerify { securityPreferences.setActiveAccountIndex(0) }
    }

    @Test
    fun getActiveWallet_returnsWalletWithActiveAccount() = runTest {
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

        repository.getActiveWallet().collect { wallet ->
            assertNotNull(wallet)
            assertEquals("walletId123", wallet?.id)
            assertTrue(wallet?.accounts?.any { it.isActive } == true)
        }
    }

    @Test
    fun hasWallet_returnsFalseWhenNotSetUp() = runTest {
        every { securityPreferences.isWalletSetUp } returns flowOf(false)

        repository.hasWallet().collect { hasWallet ->
            assertFalse(hasWallet)
        }
    }
}
