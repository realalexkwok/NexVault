package com.nexvault.wallet.feature.home

import com.nexvault.wallet.domain.model.chain.Chain
import com.nexvault.wallet.domain.model.chain.SupportedChains
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.token.Portfolio
import com.nexvault.wallet.domain.model.token.Token
import com.nexvault.wallet.domain.usecase.chain.GetSelectedChainUseCase
import com.nexvault.wallet.domain.usecase.chain.GetSupportedChainsUseCase
import com.nexvault.wallet.domain.usecase.chain.SetSelectedChainUseCase
import com.nexvault.wallet.domain.usecase.token.AddCustomTokenUseCase
import com.nexvault.wallet.domain.usecase.token.GetPortfolioUseCase
import com.nexvault.wallet.domain.usecase.token.GetPriceHistoryUseCase
import com.nexvault.wallet.domain.usecase.token.RefreshBalancesUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

/**
 * Roadmap 2.0.6 (TC traceability): feature-home had no test source set at all (legacy CR
 * finding 2.4-1). TC-VM-001 and TC-VM-002 get their mapped home here.
 *
 * Deviation recorded in the 2.0.6 matrix: TC-VM-002 as written in `doc/07` blames
 * `GetPortfolioUseCase` throwing, but the real pull-to-refresh path is
 * `RefreshBalancesUseCase` failing with an `IOException` while the already-collected
 * portfolio stays in state. The TC's intent — refresh shows the network error and keeps
 * cached data — is what this test asserts against the real path.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var getPortfolioUseCase: GetPortfolioUseCase
    private lateinit var refreshBalancesUseCase: RefreshBalancesUseCase
    private lateinit var addCustomTokenUseCase: AddCustomTokenUseCase
    private lateinit var getPriceHistoryUseCase: GetPriceHistoryUseCase
    private lateinit var getSupportedChainsUseCase: GetSupportedChainsUseCase
    private lateinit var getSelectedChainUseCase: GetSelectedChainUseCase
    private lateinit var setSelectedChainUseCase: SetSelectedChainUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getPortfolioUseCase = mockk()
        refreshBalancesUseCase = mockk()
        addCustomTokenUseCase = mockk()
        getPriceHistoryUseCase = mockk()
        getSupportedChainsUseCase = mockk()
        getSelectedChainUseCase = mockk()
        setSelectedChainUseCase = mockk()

        every { getSupportedChainsUseCase() } returns flowOf(listOf(ETH))
        every { getSelectedChainUseCase() } returns flowOf(ETH)
        coEvery { getPriceHistoryUseCase(any(), any()) } returns DataResult.Success(emptyList())
        coEvery { refreshBalancesUseCase() } returns DataResult.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            getPortfolioUseCase = getPortfolioUseCase,
            refreshBalancesUseCase = refreshBalancesUseCase,
            addCustomTokenUseCase = addCustomTokenUseCase,
            getPriceHistoryUseCase = getPriceHistoryUseCase,
            getSupportedChainsUseCase = getSupportedChainsUseCase,
            getSelectedChainUseCase = getSelectedChainUseCase,
            setSelectedChainUseCase = setSelectedChainUseCase,
        )
    }

    // TC-VM-001: initial load transitions from Loading to Success with 3 tokens and the total.
    @Test
    fun initialLoad_transitionsFromLoadingToSuccessWithThreeTokensAndTotalValue() = testScope.runTest {
        val states = mutableListOf<HomeUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testDispatcher.scheduler)) {
            createViewModel().uiState.collect { states.add(it) }
        }
        every { getPortfolioUseCase() } returns
            flowOf(
                DataResult.Success(
                    Portfolio(
                        totalFiatValue = 4125.0,
                        change24hPercent = 1.5,
                        tokens = listOf(token("ETH", 4000.0), token("USDC", 100.0), token("LINK", 25.0)),
                        chartData = emptyList(),
                    ),
                ),
            )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = states.last()
        assertTrue("must start loading", states.first().isLoading)
        assertFalse(state.isLoading)
        assertEquals(3, state.tokens.size)
        assertEquals(4125.0, state.totalFiatValue, 0.0)
        assertEquals(1.5, state.change24hPercent, 0.0)
    }

    // TC-VM-002: refresh error shows the network message and keeps the cached data.
    @Test
    fun refreshError_showsNetworkMessageAndKeepsCachedData() = testScope.runTest {
        every { getPortfolioUseCase() } returns
            flowOf(
                DataResult.Success(
                    Portfolio(
                        totalFiatValue = 4000.0,
                        change24hPercent = 0.0,
                        tokens = listOf(token("ETH", 4000.0)),
                        chartData = emptyList(),
                    ),
                ),
            )
        coEvery { refreshBalancesUseCase() } returns
            DataResult.Success(Unit) andThen DataResult.Error(IOException("network down"))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.tokens.size)

        viewModel.onRefresh()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.home_error_network, state.errorRes)
        assertEquals(1, state.tokens.size)
        assertEquals(4000.0, state.totalFiatValue, 0.0)
        assertFalse(state.isRefreshing)
    }

    private fun token(symbol: String, fiatValue: Double) = Token(
        contractAddress = "native",
        chainId = 1,
        symbol = symbol,
        name = symbol,
        decimals = 18,
        logoUrl = null,
        balance = BigDecimal.ONE,
        fiatPrice = fiatValue,
        fiatValue = fiatValue,
        priceChange24h = 0.0,
    )

    private companion object {
        val ETH: Chain = SupportedChains.ETHEREUM_MAINNET
    }
}
