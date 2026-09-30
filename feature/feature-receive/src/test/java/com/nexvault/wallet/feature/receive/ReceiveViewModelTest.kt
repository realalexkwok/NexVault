package com.nexvault.wallet.feature.receive

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.nexvault.wallet.domain.model.chain.SupportedChains
import com.nexvault.wallet.domain.model.wallet.Account
import com.nexvault.wallet.domain.model.wallet.Wallet
import com.nexvault.wallet.domain.model.wallet.WalletType
import com.nexvault.wallet.domain.usecase.chain.GetSelectedChainUseCase
import com.nexvault.wallet.domain.usecase.wallet.GetActiveWalletUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Roadmap 2.7: the receive screen's state comes from the active wallet and the selected chain. */
class ReceiveViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val getActiveWallet = mockk<GetActiveWalletUseCase>()
    private val getSelectedChain = mockk<GetSelectedChainUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `exposes the active account address and the chain badge`() =
        runTest(dispatcher) {
            every { getActiveWallet() } returns flowOf(wallet())
            every { getSelectedChain() } returns flowOf(SupportedChains.ETHEREUM_MAINNET)

            val viewModel = ReceiveViewModel(getActiveWallet, getSelectedChain)

            viewModel.uiState.test {
                // The first emission is the loading snapshot, before the flows combine.
                assertThat(awaitItem().isLoading).isTrue()
                val state = awaitItem()
                assertThat(state.address).isEqualTo(ADDRESS)
                assertThat(state.hasAddress).isTrue()
                assertThat(state.chain?.name).isEqualTo(SupportedChains.ETHEREUM_MAINNET.name)
                assertThat(state.chain?.isTestnet).isFalse()
                assertThat(state.error).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `marks a testnet chain so the screen can warn about it`() =
        runTest(dispatcher) {
            every { getActiveWallet() } returns flowOf(wallet())
            every { getSelectedChain() } returns flowOf(SupportedChains.ETHEREUM_SEPOLIA)

            val viewModel = ReceiveViewModel(getActiveWallet, getSelectedChain)

            viewModel.uiState.test {
                // The first emission is the loading snapshot, before the flows combine.
                assertThat(awaitItem().isLoading).isTrue()
                val state = awaitItem()
                assertThat(state.chain?.isTestnet).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `reports the no-wallet message when no wallet is active`() =
        runTest(dispatcher) {
            every { getActiveWallet() } returns flowOf(null)
            every { getSelectedChain() } returns flowOf(SupportedChains.ETHEREUM_MAINNET)

            val viewModel = ReceiveViewModel(getActiveWallet, getSelectedChain)

            viewModel.uiState.test {
                // The first emission is the loading snapshot, before the flows combine.
                assertThat(awaitItem().isLoading).isTrue()
                val state = awaitItem()
                assertThat(state.address).isEmpty()
                assertThat(state.hasAddress).isFalse()
                assertThat(state.error).isEqualTo(ReceiveError.NO_ACTIVE_WALLET)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a wallet without any account is treated as having no address`() =
        runTest(dispatcher) {
            every { getActiveWallet() } returns flowOf(wallet(accounts = emptyList()))
            every { getSelectedChain() } returns flowOf(SupportedChains.ETHEREUM_MAINNET)

            val viewModel = ReceiveViewModel(getActiveWallet, getSelectedChain)

            viewModel.uiState.test {
                // The first emission is the loading snapshot, before the flows combine.
                assertThat(awaitItem().isLoading).isTrue()
                val state = awaitItem()
                assertThat(state.hasAddress).isFalse()
                assertThat(state.error).isEqualTo(ReceiveError.NO_ACTIVE_WALLET)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun wallet(accounts: List<Account> = listOf(account())) =
        Wallet(
            id = "w1",
            name = "Main",
            type = WalletType.HD,
            accounts = accounts,
            createdAt = 0L,
            isActive = true,
        )

    private fun account(inactive: Boolean = false) =
        Account(
            walletId = "w1",
            index = 0,
            address = ADDRESS,
            name = "Account 1",
            derivationPath = "m/44'/60'/0'/0/0",
            isActive = !inactive,
        )

    private companion object {
        const val ADDRESS = "0x97B633905380C70B1a4c18DDCd56676589bcE147"
    }

    @Test
    fun `a failing upstream flow ends up as a load error instead of loading forever`() =
        runTest(dispatcher) {
            every { getActiveWallet() } returns flow { throw IllegalStateException("boom") }
            every { getSelectedChain() } returns flowOf(SupportedChains.ETHEREUM_MAINNET)

            val viewModel = ReceiveViewModel(getActiveWallet, getSelectedChain)

            viewModel.uiState.test {
                assertThat(awaitItem().isLoading).isTrue()
                val state = awaitItem()
                assertThat(state.isLoading).isFalse()
                assertThat(state.error).isEqualTo(ReceiveError.LOAD_FAILED)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
