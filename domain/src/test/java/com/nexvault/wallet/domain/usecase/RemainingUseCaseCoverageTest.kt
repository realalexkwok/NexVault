package com.nexvault.wallet.domain.usecase

import com.nexvault.wallet.domain.model.auth.AuthMethod
import com.nexvault.wallet.domain.model.auth.AuthState
import com.nexvault.wallet.domain.model.chain.Chain
import com.nexvault.wallet.domain.model.chain.SupportedChains
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.token.PricePoint
import com.nexvault.wallet.domain.model.wallet.WalletDraft
import com.nexvault.wallet.domain.repository.AuthRepository
import com.nexvault.wallet.domain.repository.ChainRepository
import com.nexvault.wallet.domain.repository.TokenRepository
import com.nexvault.wallet.domain.repository.WalletRepository
import com.nexvault.wallet.domain.usecase.auth.CheckBiometricAvailabilityUseCase
import com.nexvault.wallet.domain.usecase.auth.GetAuthStateUseCase
import com.nexvault.wallet.domain.usecase.chain.GetSupportedChainsUseCase
import com.nexvault.wallet.domain.usecase.token.GetTokenPriceChartUseCase
import com.nexvault.wallet.domain.usecase.wallet.CompleteOnboardingUseCase
import com.nexvault.wallet.domain.usecase.wallet.GenerateWalletDraftUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap 2.6 coverage: the last thin use cases without a dedicated test.
 */
class RemainingUseCaseCoverageTest {
    private val authRepository = mockk<AuthRepository>()
    private val chainRepository = mockk<ChainRepository>()
    private val tokenRepository = mockk<TokenRepository>()
    private val walletRepository = mockk<WalletRepository>()

    @Test
    fun biometricAvailability_delegatesToTheRepository() = runTest {
        coEvery { authRepository.isBiometricAvailable() } returns true

        assertTrue(CheckBiometricAvailabilityUseCase(authRepository)())
    }

    @Test
    fun authState_observesTheRepository() = runTest {
        val state =
            AuthState(
                isWalletSetUp = true,
                isLocked = false,
                isLockedOut = false,
                lockoutRemainingSeconds = 0L,
                authMethod = AuthMethod.PIN,
                isBiometricEnabled = false,
                isBiometricAvailable = true,
            )
        every { authRepository.getAuthState() } returns flowOf(state)

        assertEquals(state, GetAuthStateUseCase(authRepository)().first())
    }

    @Test
    fun supportedChains_observesTheRepository() = runTest {
        val chains: List<Chain> = listOf(SupportedChains.ETHEREUM_MAINNET, SupportedChains.BSC_MAINNET)
        every { chainRepository.getSupportedChains() } returns flowOf(chains)

        assertEquals(chains, GetSupportedChainsUseCase(chainRepository)().first())
    }

    @Test
    fun tokenPriceChart_delegates() = runTest {
        val expected = DataResult.Success(listOf(PricePoint(1L, 2.0)))
        coEvery { tokenRepository.getTokenPriceChart(1, "0xtoken", 7) } returns expected

        assertEquals(expected, GetTokenPriceChartUseCase(tokenRepository)(1, "0xtoken", 7))
    }

    @Test
    fun completeOnboarding_delegates() = runTest {
        coEvery { walletRepository.completeOnboarding() } returns DataResult.Success(Unit)

        assertTrue(CompleteOnboardingUseCase(walletRepository)() is DataResult.Success)
    }

    @Test
    fun generateWalletDraft_delegates() = runTest {
        val draft =
            WalletDraft(
                mnemonic = "a b",
                mnemonicWords = listOf("a", "b"),
                address = "0x1234",
            )
        coEvery { walletRepository.generateWallet() } returns DataResult.Success(draft)

        assertEquals(draft, (GenerateWalletDraftUseCase(walletRepository)() as DataResult.Success).data)
    }
}
