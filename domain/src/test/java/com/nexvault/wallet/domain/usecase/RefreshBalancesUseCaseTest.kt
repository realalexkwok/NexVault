package com.nexvault.wallet.domain.usecase

import com.nexvault.wallet.domain.model.chain.SupportedChains
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.WalletNotFoundException
import com.nexvault.wallet.domain.repository.ChainRepository
import com.nexvault.wallet.domain.repository.TokenRepository
import com.nexvault.wallet.domain.repository.WalletRepository
import com.nexvault.wallet.domain.usecase.token.RefreshBalancesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RefreshBalancesUseCaseTest {
    private lateinit var tokenRepository: TokenRepository
    private lateinit var chainRepository: ChainRepository
    private lateinit var walletRepository: WalletRepository
    private lateinit var useCase: RefreshBalancesUseCase

    @Before
    fun setup() {
        tokenRepository = mockk()
        chainRepository = mockk()
        walletRepository = mockk()
        useCase = RefreshBalancesUseCase(tokenRepository, chainRepository, walletRepository)
    }

    @Test
    fun seedsDefaultTokensForSelectedChainBeforeRefreshing() = runTest {
        every { chainRepository.getSelectedChain() } returns flowOf(SupportedChains.BSC_MAINNET)
        every { walletRepository.getActiveAddress() } returns flowOf("0xabc")
        coEvery { tokenRepository.seedDefaultTokens(any()) } returns DataResult.Success(Unit)
        coEvery { tokenRepository.refreshBalances(any(), any()) } returns DataResult.Success(Unit)

        val result = useCase()

        assertTrue(result is DataResult.Success)
        coVerify { tokenRepository.seedDefaultTokens(SupportedChains.BSC_MAINNET.chainId) }
        coVerify { tokenRepository.refreshBalances(SupportedChains.BSC_MAINNET.chainId, "0xabc") }
    }

    @Test
    fun seedsTheNewlySelectedChainAfterAChainSwitch() = runTest {
        every { chainRepository.getSelectedChain() } returns flowOf(SupportedChains.POLYGON_MAINNET)
        every { walletRepository.getActiveAddress() } returns flowOf("0xabc")
        coEvery { tokenRepository.seedDefaultTokens(any()) } returns DataResult.Success(Unit)
        coEvery { tokenRepository.refreshBalances(any(), any()) } returns DataResult.Success(Unit)

        useCase()

        coVerify { tokenRepository.seedDefaultTokens(SupportedChains.POLYGON_MAINNET.chainId) }
    }

    @Test
    fun seedingFailureDoesNotFailTheRefresh() = runTest {
        every { chainRepository.getSelectedChain() } returns flowOf(SupportedChains.ETHEREUM_SEPOLIA)
        every { walletRepository.getActiveAddress() } returns flowOf("0xabc")
        coEvery { tokenRepository.seedDefaultTokens(any()) } returns
            DataResult.Error(RuntimeException("db unavailable"))
        coEvery { tokenRepository.refreshBalances(any(), any()) } returns DataResult.Success(Unit)

        val result = useCase()

        assertTrue(result is DataResult.Success)
    }

    @Test
    fun withoutActiveAddressReturnsErrorAndDoesNotSeed() = runTest {
        every { chainRepository.getSelectedChain() } returns flowOf(SupportedChains.ETHEREUM_MAINNET)
        every { walletRepository.getActiveAddress() } returns flowOf(null)

        val result = useCase()

        assertTrue(result is DataResult.Error)
        assertTrue((result as DataResult.Error).exception is WalletNotFoundException)
        coVerify(exactly = 0) { tokenRepository.seedDefaultTokens(any()) }
    }
}
