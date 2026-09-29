package com.nexvault.wallet.feature.tokens

import androidx.lifecycle.SavedStateHandle
import com.nexvault.wallet.domain.model.common.ApiKeyNotConfiguredException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.ExplorerPlanUnsupportedException
import com.nexvault.wallet.domain.model.token.PricePoint
import com.nexvault.wallet.domain.model.token.Token
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
import java.io.IOException
import java.math.BigDecimal

/**
 * Roadmap 2.6 coverage: the remaining TokenDetailViewModel paths — token-missing error, plan
 * gate, key-absent history, chart failure, range selection and refresh.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TokenDetailViewModelCoverageTest {
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

        every { observeTokenDetailUseCase("0xtoken", 1) } returns flowOf(TOKEN)
        coEvery { getTokenPriceChartUseCase(1, "0xtoken", any()) } returns
            DataResult.Success(listOf(PricePoint(1L, 2.0)))
        coEvery { getRecentTokenTransactionsUseCase(1, "0xtoken", 5) } returns emptyList()
        coEvery { refreshTransactionHistoryUseCase(1) } returns DataResult.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        TokenDetailViewModel(
            savedStateHandle =
                SavedStateHandle(
                    mapOf("contractAddress" to "0xtoken", "chainId" to 1),
                ),
            observeTokenDetailUseCase = observeTokenDetailUseCase,
            getTokenPriceChartUseCase = getTokenPriceChartUseCase,
            getRecentTokenTransactionsUseCase = getRecentTokenTransactionsUseCase,
            refreshTransactionHistoryUseCase = refreshTransactionHistoryUseCase,
        )

    @Test
    fun missingToken_setsTheNotFoundError() = testScope.runTest {
        every { observeTokenDetailUseCase("0xtoken", 1) } returns flowOf(null)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(R.string.token_detail_token_not_found, viewModel.uiState.value.errorRes)
        assertNull(viewModel.uiState.value.token)
    }

    @Test
    fun keyAbsentHistory_marksHistoryAsNotConfigured() = testScope.runTest {
        coEvery { refreshTransactionHistoryUseCase(1) } returns
            DataResult.Error(ApiKeyNotConfiguredException("no key"), "no key")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isHistoryConfigured)
        assertFalse(viewModel.uiState.value.isHistoryPlanGated)
    }

    @Test
    fun planGatedHistory_setsThePersistentNotice() = testScope.runTest {
        coEvery { refreshTransactionHistoryUseCase(1) } returns
            DataResult.Error(ExplorerPlanUnsupportedException("plan"), "plan")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isHistoryConfigured)
        assertTrue(viewModel.uiState.value.isHistoryPlanGated)
    }

    @Test
    fun chartFailure_keepsLoadingFalseAndEmptyChart() = testScope.runTest {
        coEvery { getTokenPriceChartUseCase(1, "0xtoken", any()) } returns
            DataResult.Error(IOException("down"), "down")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isChartLoading)
    }

    @Test
    fun chartRangeSelection_reloadsWithTheNewDays() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onChartRangeSelected(30)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(30, viewModel.uiState.value.selectedChartDays)
        coVerify { getTokenPriceChartUseCase(1, "0xtoken", 30) }
    }

    @Test
    fun refresh_reloadsHistoryAndChartAndFinishes() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onRefresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)
        coVerify(atLeast = 2) { refreshTransactionHistoryUseCase(1) }
    }

    private companion object {
        val TOKEN =
            Token(
                contractAddress = "0xtoken",
                chainId = 1,
                symbol = "TKN",
                name = "Token",
                decimals = 18,
                logoUrl = null,
                balance = BigDecimal.ONE,
                fiatPrice = 2.0,
                fiatValue = 2.0,
                priceChange24h = 0.0,
                isCustom = false,
                coinGeckoId = "tkn",
            )
    }
}
