package com.nexvault.wallet.feature.home

import com.nexvault.wallet.domain.model.chain.Chain
import com.nexvault.wallet.domain.model.chain.SupportedChains
import com.nexvault.wallet.domain.model.common.ApiKeyNotConfiguredException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.token.PricePoint
import com.nexvault.wallet.domain.model.token.Token
import com.nexvault.wallet.domain.usecase.chain.GetSelectedChainUseCase
import com.nexvault.wallet.domain.usecase.chain.GetSupportedChainsUseCase
import com.nexvault.wallet.domain.usecase.chain.SetSelectedChainUseCase
import com.nexvault.wallet.domain.usecase.token.AddCustomTokenUseCase
import com.nexvault.wallet.domain.usecase.token.GetPortfolioUseCase
import com.nexvault.wallet.domain.usecase.token.GetPriceHistoryUseCase
import com.nexvault.wallet.domain.usecase.token.RefreshBalancesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

/**
 * Roadmap 2.6 coverage: the HomeViewModel paths beyond the two mapped TCs — chain selection,
 * chart ranges, add-token dialog, refresh error mapping and error dismissal.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelCoverageTest {
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

        every { getPortfolioUseCase() } returns flowOf(DataResult.Success(emptyPortfolio()))
        every { getSupportedChainsUseCase() } returns flowOf(listOf(CHAIN))
        every { getSelectedChainUseCase() } returns flowOf(CHAIN)
        coEvery { refreshBalancesUseCase() } returns DataResult.Success(Unit)
        coEvery { getPriceHistoryUseCase(any(), any()) } returns
            DataResult.Success(listOf(PricePoint(1L, 2.0)))
        coEvery { setSelectedChainUseCase(any()) } returns DataResult.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        HomeViewModel(
            getPortfolioUseCase = getPortfolioUseCase,
            refreshBalancesUseCase = refreshBalancesUseCase,
            addCustomTokenUseCase = addCustomTokenUseCase,
            getPriceHistoryUseCase = getPriceHistoryUseCase,
            getSupportedChainsUseCase = getSupportedChainsUseCase,
            getSelectedChainUseCase = getSelectedChainUseCase,
            setSelectedChainUseCase = setSelectedChainUseCase,
        )

    @Test
    fun initialLoad_marksKeyAbsentWhenTheRpcKeyIsMissing() = testScope.runTest {
        coEvery { refreshBalancesUseCase() } returns
            DataResult.Error(ApiKeyNotConfiguredException("no key"), "no key")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isRpcNotConfigured)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun refresh_withIOException_showsTheNetworkError() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        coEvery { refreshBalancesUseCase() } returns
            DataResult.Error(IOException("down"), "down")

        viewModel.onRefresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(R.string.home_error_network, viewModel.uiState.value.errorRes)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun chainSelection_succeedsAndRefreshes() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onChainSelected(11155111)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { setSelectedChainUseCase(11155111) }
        coVerify(atLeast = 2) { refreshBalancesUseCase() }
    }

    @Test
    fun chartRangeSelection_reloadsTheChartWithTheNewDays() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onChartRangeSelected(30)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(30, viewModel.uiState.value.selectedChartDays)
        coVerify { getPriceHistoryUseCase(1, 30) }
    }

    @Test
    fun chartLoadFailure_clearsTheChart() = testScope.runTest {
        coEvery { getPriceHistoryUseCase(any(), any()) } returns DataResult.Error(IOException("down"))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptyList<PricePoint>(), viewModel.uiState.value.chartData)
    }

    @Test
    fun addCustomToken_successClosesTheDialogAndStoresTheResult() = testScope.runTest {
        val added =
            Token(
                contractAddress = "0xnew",
                chainId = 1,
                symbol = "NEW",
                name = "New Token",
                decimals = 18,
                logoUrl = null,
                balance = BigDecimal.ZERO,
                fiatPrice = null,
                fiatValue = null,
                priceChange24h = null,
                isCustom = true,
                coinGeckoId = null,
            )
        coEvery { addCustomTokenUseCase(1, "0xnew") } returns DataResult.Success(added)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onShowAddTokenDialog()

        viewModel.onAddCustomToken("0xnew")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showAddTokenDialog)
        assertFalse(viewModel.uiState.value.addTokenLoading)
        assertNotNull(viewModel.uiState.value.addTokenResult)
    }

    @Test
    fun addCustomToken_failureKeepsTheDialogAndShowsTheError() = testScope.runTest {
        coEvery { addCustomTokenUseCase(1, "bad") } returns DataResult.Error(IllegalArgumentException("bad"))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onShowAddTokenDialog()

        viewModel.onAddCustomToken("bad")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showAddTokenDialog)
        assertEquals(
            R.string.home_add_token_error_invalid_contract,
            viewModel.uiState.value.addTokenErrorRes,
        )
    }

    @Test
    fun dialogShowAndDismiss_cycleTheState() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onShowAddTokenDialog()
        assertTrue(viewModel.uiState.value.showAddTokenDialog)

        viewModel.onDismissAddTokenDialog()
        assertFalse(viewModel.uiState.value.showAddTokenDialog)
    }

    @Test
    fun errorDismissed_clearsBothErrorChannels() = testScope.runTest {
        coEvery { refreshBalancesUseCase() } returns DataResult.Error(IOException("down"), "down")
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onErrorDismissed()

        assertNull(viewModel.uiState.value.errorRes)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    private fun emptyPortfolio() =
        com.nexvault.wallet.domain.model.token.Portfolio(
            totalFiatValue = 0.0,
            change24hPercent = 0.0,
            tokens = emptyList(),
            chartData = emptyList(),
        )

    private companion object {
        val CHAIN: Chain = SupportedChains.ETHEREUM_MAINNET
    }
}
