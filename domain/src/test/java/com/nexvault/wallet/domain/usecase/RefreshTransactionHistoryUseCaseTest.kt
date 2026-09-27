package com.nexvault.wallet.domain.usecase.token

import com.google.common.truth.Truth.assertThat
import com.nexvault.wallet.domain.model.common.ApiKeyNotConfiguredException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.WalletNotFoundException
import com.nexvault.wallet.domain.model.wallet.Account
import com.nexvault.wallet.domain.model.wallet.Wallet
import com.nexvault.wallet.domain.model.wallet.WalletType
import com.nexvault.wallet.domain.repository.TransactionRepository
import com.nexvault.wallet.domain.repository.WalletRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Roadmap 2.0.6 regression protection for the `RefreshTransactionHistoryUseCase` fix: before
 * this item the use case swallowed the repository's [DataResult] and returned `Unit`, which
 * made `TokenDetailViewModel`'s `is DataResult.Error` branch dead code — the 2.0.4
 * "Not configured" and 2.0.4b plan-gate notices on the token detail screen could never
 * appear. The use case now returns the repository result; these tests lock that in.
 */
class RefreshTransactionHistoryUseCaseTest {
    private val transactionRepository = mockk<TransactionRepository>()
    private val walletRepository = mockk<WalletRepository>()

    private val useCase = RefreshTransactionHistoryUseCase(transactionRepository, walletRepository)

    @Test
    fun withActiveWallet_returnsTheRepositoryResult() = runTest {
        every { walletRepository.getActiveWallet() } returns flowOf(walletWithActiveAccount())
        coEvery { transactionRepository.refreshTransactionHistory(1, "0xABC") } returns
            DataResult.Success(Unit)

        val result = useCase(1)

        assertThat(result).isEqualTo(DataResult.Success(Unit))
    }

    @Test
    fun withActiveWallet_passesAnErrorResultThrough() = runTest {
        every { walletRepository.getActiveWallet() } returns flowOf(walletWithActiveAccount())
        coEvery { transactionRepository.refreshTransactionHistory(1, "0xABC") } returns
            DataResult.Error(ApiKeyNotConfiguredException("no key"))

        val result = useCase(1)

        assertThat((result as? DataResult.Error)?.exception).isInstanceOf(ApiKeyNotConfiguredException::class.java)
    }

    @Test
    fun withoutWallet_returnsWalletNotFoundAndNeverCallsTheRepository() = runTest {
        every { walletRepository.getActiveWallet() } returns flowOf(null)

        val result = useCase(1)

        assertThat((result as? DataResult.Error)?.exception).isInstanceOf(WalletNotFoundException::class.java)
        coVerify(exactly = 0) { transactionRepository.refreshTransactionHistory(any(), any()) }
    }

    private fun walletWithActiveAccount() = Wallet(
        id = "w1",
        name = "Main",
        type = WalletType.HD,
        accounts = listOf(
            Account(
                walletId = "w1",
                index = 0,
                address = "0xABC",
                name = "Account 1",
                derivationPath = "m/44'/60'/0'/0/0",
                isActive = true,
            ),
        ),
        createdAt = 0L,
        isActive = true,
    )
}
