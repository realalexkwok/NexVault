package com.nexvault.wallet.feature.tokens

import androidx.lifecycle.SavedStateHandle
import com.nexvault.wallet.domain.model.common.ApiKeyNotConfiguredException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.ExplorerPlanUnsupportedException
import com.nexvault.wallet.domain.model.token.PricePoint
import com.nexvault.wallet.domain.model.token.Token
import com.nexvault.wallet.domain.model.transaction.Transaction
import com.nexvault.wallet.domain.model.transaction.TransactionStatus
import com.nexvault.wallet.domain.model.transaction.TransactionType
import com.nexvault.wallet.domain.usecase.token.GetRecentTokenTransactionsUseCase
import com.nexvault.wallet.domain.usecase.token.GetTokenPriceChartUseCase
import com.nexvault.wallet.domain.usecase.token.ObserveTokenDetailUseCase
import com.nexvault.wallet.domain.usecase.token.RefreshTransactionHistoryUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Roadmap 2.0.6 (TC traceability): feature-tokens had no test source set at all (legacy CR
 * finding 2.5-1). `doc/07` defines no TC for the token detail screen, so these tests are
 * listed in the 2.0.6 matrix appendix rather than under a TC id.
 *
 * The configured/plan-gate cases lock in the 2.0.4 / 2.0.4b persistent notices, which were
 * dead states until this item fixed `RefreshTransactionHistoryUseCase` to return the
 * repository result instead of `Unit`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TokenDetailViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var observeTokenDetailUseCase: ObserveTokenDetailUseCase
    private lateinit var getTokenPriceChartUseCase: GetTokenPriceChartUseCase
    private lateinit var getRecentTokenTransactionsUseCase: GetRecentTokenTransactionsUseCase
    private lateinit var refreshTransactionHistoryUseCase: RefreshTransactionHistoryUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        observeTokenDetailUseCase = mockk()
        getTokenPriceChartUseCase = mockk()
        getRecentTokenTransactionsUseCase = mockk()
        refreshTransactionHistoryUseCase = mockk()

        every { observeTokenDetailUseCase(any(), any()) } returns flowOf(TOKEN)
        coEvery { getTokenPriceChartUseCase(any(), any(), any()) } returns DataResult.Success(emptyList())
        coEvery { getRecentTokenTransactionsUseCase(any(), any(), any()) } returns emptyList()
        coEvery { refreshTransactionHistoryUseCase(any()) } returns DataResult.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TokenDetailViewModel {
        return TokenDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("contractAddress" to "native", "chainId" to 1)),
            observeTokenDetailUseCase = observeTokenDetailUseCase,
            getTokenPriceChartUseCase = getTokenPriceChartUseCase,
            getRecentTokenTransactionsUseCase = getRecentTokenTransactionsUseCase,
            refreshTransactionHistoryUseCase = refreshTransactionHistoryUseCase,
        )
    }

    @Test
    fun initialLoad_populatesTokenChartAndTransactions() = testScope.runTest {
        coEvery { getTokenPriceChartUseCase(1, "native", 7) } returns DataResult.Success(CHART)
        coEvery { getRecentTokenTransactionsUseCase(1, "native", 5) } returns listOf(TRANSACTION)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TOKEN, state.token)
        assertFalse(state.isLoading)
        assertEquals(CHART, state.chartData)
        assertEquals(listOf(TRANSACTION), state.recentTransactions)
        assertTrue(state.isHistoryConfigured)
        assertFalse(state.isHistoryPlanGated)
        assertNull(state.errorRes)
    }

    @Test
    fun missingToken_showsTheNotFoundError() = testScope.runTest {
        every { observeTokenDetailUseCase(any(), any()) } returns flowOf(null)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.token)
        assertEquals(R.string.token_detail_token_not_found, state.errorRes)
    }

    @Test
    fun keyNotConfigured_setsTheHistoryConfiguredNotice() = testScope.runTest {
        coEvery { refreshTransactionHistoryUseCase(any()) } returns
            DataResult.Error(ApiKeyNotConfiguredException("no key"))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isHistoryConfigured)
    }

    @Test
    fun planGate_setsThePlanGateNotice() = testScope.runTest {
        coEvery { refreshTransactionHistoryUseCase(any()) } returns
            DataResult.Error(ExplorerPlanUnsupportedException("NOTOK: plan does not cover this chain"))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isHistoryPlanGated)
    }

    @Test
    fun chartRangeSelection_reloadsTheChartForTheSelectedDays() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onChartRangeSelected(30)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(30, state.selectedChartDays)
        coVerify { getTokenPriceChartUseCase(1, "native", 30) }
    }

    @Test
    fun pullToRefresh_togglesRefreshingAndReloads() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onRefresh()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        coVerify(atLeast = 2) { refreshTransactionHistoryUseCase(1) }
    }

    private companion object {
        val TOKEN = Token(
            contractAddress = "native",
            chainId = 1,
            symbol = "ETH",
            name = "Ethereum",
            decimals = 18,
            logoUrl = null,
            balance = BigDecimal.ONE,
            fiatPrice = 3000.0,
            fiatValue = 3000.0,
            priceChange24h = 0.5,
        )

        val CHART = listOf(PricePoint(timestamp = 1L, value = 3000.0))

        val TRANSACTION = Transaction(
            txHash = "0xtx",
            chainId = 1,
            fromAddress = "0xA",
            toAddress = "0xB",
            value = BigDecimal.ONE,
            gasUsed = BigInteger.ONE,
            gasPrice = BigInteger.ONE,
            tokenSymbol = null,
            tokenContractAddress = null,
            tokenDecimals = null,
            blockNumber = 1L,
            timestamp = 1L,
            status = TransactionStatus.CONFIRMED,
            type = TransactionType.SEND,
        )
    }
}
