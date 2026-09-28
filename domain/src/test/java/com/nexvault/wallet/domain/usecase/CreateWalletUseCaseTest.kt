package com.nexvault.wallet.domain.usecase.wallet

import com.nexvault.wallet.domain.model.auth.WalletCreationResult
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.wallet.WalletDraft
import com.nexvault.wallet.domain.repository.WalletRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap 2.0.6 (TC traceability): TC-UC-001 had no direct unit-test home — the only
 * coverage was indirect, through `CreateWalletViewModelTest` in feature-onboarding.
 *
 * Deviation recorded in the 2.0.6 matrix: `doc/07` describes `CreateWalletUseCase()`
 * returning `Result.success(Wallet(...))` from a `WalletRepository.createWallet()` with no
 * arguments; the shipped contract is `CreateWalletUseCase(walletName, draft)` returning
 * `DataResult<WalletCreationResult>`. The TC's intent — the use case yields a valid wallet
 * with a non-empty address — is asserted against the real contract.
 */
class CreateWalletUseCaseTest {
    private val walletRepository = mockk<WalletRepository>()

    private val useCase = CreateWalletUseCase(walletRepository)

    // TC-UC-001: the use case returns success with a valid wallet carrying a non-empty address.
    @Test
    fun createWallet_success_returnsWalletWithNonEmptyAddress() = runTest {
        val draft = WalletDraft(
            mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
            mnemonicWords = List(11) { "abandon" } + listOf("about"),
            address = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
        )
        val created = WalletCreationResult(
            walletId = "w1",
            address = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
            mnemonicWords = draft.mnemonicWords,
        )
        coEvery { walletRepository.createWallet("Main", draft) } returns DataResult.Success(created)

        val result = useCase("Main", draft)

        val data = (result as DataResult.Success).data
        assertEquals("w1", data.walletId)
        assertTrue(data.address.isNotEmpty())
        assertEquals(12, data.mnemonicWords.size)
        coVerify(exactly = 1) { walletRepository.createWallet("Main", draft) }
    }
}
