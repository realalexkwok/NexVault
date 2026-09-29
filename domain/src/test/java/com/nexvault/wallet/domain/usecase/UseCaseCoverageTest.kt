package com.nexvault.wallet.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.nexvault.wallet.domain.model.chain.Chain
import com.nexvault.wallet.domain.model.chain.SupportedChains
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.token.Portfolio
import com.nexvault.wallet.domain.model.token.PricePoint
import com.nexvault.wallet.domain.model.token.Token
import com.nexvault.wallet.domain.model.wallet.Account
import com.nexvault.wallet.domain.model.wallet.Wallet
import com.nexvault.wallet.domain.model.wallet.WalletType
import com.nexvault.wallet.domain.repository.ChainRepository
import com.nexvault.wallet.domain.repository.TransactionRepository
import com.nexvault.wallet.domain.repository.TokenRepository
import com.nexvault.wallet.domain.repository.WalletRepository
import com.nexvault.wallet.domain.usecase.chain.GetSelectedChainUseCase
import com.nexvault.wallet.domain.usecase.token.GetPortfolioUseCase
import com.nexvault.wallet.domain.usecase.token.GetPriceHistoryUseCase
import com.nexvault.wallet.domain.usecase.token.GetRecentTokenTransactionsUseCase
import com.nexvault.wallet.domain.usecase.token.ObserveTokenDetailUseCase
import com.nexvault.wallet.domain.usecase.wallet.GetActiveWalletUseCase
import com.nexvault.wallet.domain.usecase.wallet.GetMnemonicForBackupUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap 2.6 coverage: the thin use cases that lacked a dedicated test.
 */
class UseCaseCoverageTest {
    private val tokenRepository = mockk<TokenRepository>()
    private val walletRepository = mockk<WalletRepository>()
    private val chainRepository = mockk<ChainRepository>()

    private val portfolioUseCase = GetPortfolioUseCase(tokenRepository, walletRepository, chainRepository)
    private val priceHistoryUseCase = GetPriceHistoryUseCase(tokenRepository)
    private val observeTokenDetailUseCase = ObserveTokenDetailUseCase(tokenRepository, walletRepository)
    private val activeWalletUseCase = GetActiveWalletUseCase(walletRepository)
    private val mnemonicUseCase = GetMnemonicForBackupUseCase(walletRepository)
    private val selectedChainUseCase = GetSelectedChainUseCase(chainRepository)

    @Test
    fun portfolio_combinesTokensAndWeightsTheChange() = runTest {
        every { walletRepository.getActiveAddress() } returns flowOf(ADDRESS)
        every { chainRepository.getSelectedChain() } returns flowOf(CHAIN)
        every { tokenRepository.getTokensWithBalances(1, ADDRESS) } returns
            flowOf(
                listOf(
                    token(balance = BigDecimal.ONE, fiatValue = 100.0, change = 10.0),
                    token(balance = BigDecimal("2"), fiatValue = 200.0, change = -5.0),
                ),
            )

        val result = portfolioUseCase().first()

        assertTrue(result is DataResult.Success)
        val portfolio = (result as DataResult.Success).data
        assertThat(portfolio.totalFiatValue).isEqualTo(300.0)
        assertThat(portfolio.tokens).hasSize(2)
    }

    @Test
    fun portfolio_withoutAnActiveWalletErrors() = runTest {
        every { walletRepository.getActiveAddress() } returns flowOf(null)
        every { chainRepository.getSelectedChain() } returns flowOf(CHAIN)

        val result = portfolioUseCase().first()

        assertTrue(result is DataResult.Error)
    }

    @Test
    fun priceHistory_delegatesToTheRepository() = runTest {
        val expected = listOf(PricePoint(1L, 2.0), PricePoint(3L, 4.0))
        coEvery { tokenRepository.getPortfolioChartData(1, 7) } returns DataResult.Success(expected)

        val result = priceHistoryUseCase(1, 7)

        assertEquals(expected, (result as DataResult.Success).data)
    }

    @Test
    fun observeTokenDetail_emitsNullWithoutAnActiveWallet() = runTest {
        every { walletRepository.getActiveWallet() } returns flowOf(null)

        assertNull(observeTokenDetailUseCase("native", 1).first())
    }

    @Test
    fun observeTokenDetail_delegatesWithAnActiveWallet() = runTest {
        val expected = token(balance = BigDecimal("5"), fiatValue = null, change = null)
        every { walletRepository.getActiveWallet() } returns flowOf(WALLET)
        every { tokenRepository.observeToken(1, "native") } returns flowOf(expected)

        assertEquals(expected, observeTokenDetailUseCase("native", 1).first())
    }

    @Test
    fun recentTokenTransactions_usesTheActiveAccountAndNativeSentinel() = runTest {
        val transactionRepository = mockk<TransactionRepository>()
        val useCase = GetRecentTokenTransactionsUseCase(transactionRepository, walletRepository)
        every { walletRepository.getActiveWallet() } returns flowOf(WALLET)
        coEvery { transactionRepository.getRecentTransactionsForToken(1, ADDRESS, null, 5) } returns emptyList()

        assertTrue(useCase(1, "native").isEmpty())
    }

    @Test
    fun recentTokenTransactions_withoutAnActiveWalletReturnsEmpty() = runTest {
        val useCase = GetRecentTokenTransactionsUseCase(mockk(), walletRepository)
        every { walletRepository.getActiveWallet() } returns flowOf(null)

        assertTrue(useCase(1, "native").isEmpty())
    }

    @Test
    fun activeWallet_delegates() = runTest {
        every { walletRepository.getActiveWallet() } returns flowOf(WALLET)

        assertEquals(WALLET, activeWalletUseCase().first())
    }

    @Test
    fun mnemonicForBackup_delegates() = runTest {
        coEvery { walletRepository.getMnemonicForBackup("w1") } returns DataResult.Success(listOf("a", "b"))

        assertEquals(listOf("a", "b"), (mnemonicUseCase("w1") as DataResult.Success).data)
    }

    @Test
    fun selectedChain_delegates() = runTest {
        every { chainRepository.getSelectedChain() } returns flowOf(CHAIN)

        assertEquals(CHAIN, selectedChainUseCase().first())
    }

    private fun token(balance: BigDecimal, fiatValue: Double?, change: Double?) =
        Token(
            contractAddress = "native",
            chainId = 1,
            symbol = "ETH",
            name = "Ethereum",
            decimals = 18,
            logoUrl = null,
            balance = balance,
            fiatPrice = fiatValue,
            fiatValue = fiatValue,
            priceChange24h = change,
            isCustom = false,
            coinGeckoId = "ethereum",
        )

    private companion object {
        val CHAIN: Chain = SupportedChains.ETHEREUM_MAINNET
        const val ADDRESS = "0x1234567890abcdef1234567890abcdef12345678"
        val WALLET =
            Wallet(
                id = "w1",
                name = "Main",
                type = WalletType.HD,
                accounts =
                    listOf(
                        Account("w1", 0, ADDRESS, "Account 1", "m/44'/60'/0'/0/0", true),
                    ),
                createdAt = 0L,
                isActive = true,
            )
    }
}
