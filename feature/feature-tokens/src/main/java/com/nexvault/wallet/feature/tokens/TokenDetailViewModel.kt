package com.nexvault.wallet.feature.tokens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexvault.wallet.domain.model.common.ApiKeyNotConfiguredException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.ExplorerPlanUnsupportedException
import com.nexvault.wallet.domain.usecase.token.GetRecentTokenTransactionsUseCase
import com.nexvault.wallet.domain.usecase.token.GetTokenPriceChartUseCase
import com.nexvault.wallet.domain.usecase.token.ObserveTokenDetailUseCase
import com.nexvault.wallet.domain.usecase.token.RefreshTransactionHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the token detail screen.
 *
 * Reads [contractAddress] and [chainId] from navigation [SavedStateHandle] keys matching the route.
 */
@HiltViewModel
class TokenDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeTokenDetailUseCase: ObserveTokenDetailUseCase,
    private val getTokenPriceChartUseCase: GetTokenPriceChartUseCase,
    private val getRecentTokenTransactionsUseCase: GetRecentTokenTransactionsUseCase,
    private val refreshTransactionHistoryUseCase: RefreshTransactionHistoryUseCase,
) : ViewModel() {

    private val contractAddress: String = checkNotNull(savedStateHandle.get<String>("contractAddress"))

    private val chainId: Int = checkNotNull(
        savedStateHandle.get<Int>("chainId"),
    )

    private val _selectedChartDays = MutableStateFlow(7)

    private val _uiState = MutableStateFlow(TokenDetailUiState())
    val uiState: StateFlow<TokenDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeTokenDetailUseCase(contractAddress, chainId).collect { token ->
                _uiState.update { state ->
                    state.copy(
                        token = token,
                        isLoading = false,
                        errorRes = if (token == null) R.string.token_detail_token_not_found else null,
                        errorMessage = null,
                    )
                }
            }
        }
        viewModelScope.launch {
            loadChartDataSync()
        }
        viewModelScope.launch {
            loadRecentTransactionsSync()
        }
    }

    private suspend fun loadChartDataSync() {
        _uiState.update { it.copy(isChartLoading = true) }
        val days = _selectedChartDays.value
        when (
            val result = getTokenPriceChartUseCase(chainId, contractAddress, days)
        ) {
            is DataResult.Success -> {
                _uiState.update {
                    it.copy(
                        chartData = result.data,
                        selectedChartDays = days,
                        isChartLoading = false,
                    )
                }
            }

            is DataResult.Error -> {
                _uiState.update { it.copy(isChartLoading = false) }
            }
        }
    }

    private suspend fun loadRecentTransactionsSync() {
        var historyConfigured = true
        var historyPlanGated = false
        try {
            val result = refreshTransactionHistoryUseCase(chainId)
            if (result is DataResult.Error) {
                historyConfigured = result.exception !is ApiKeyNotConfiguredException
                // Roadmap 2.0.4b: the explorer told us the current plan does not cover this
                // chain — a standing condition, so it becomes a persistent notice.
                historyPlanGated = result.exception is ExplorerPlanUnsupportedException
            }
        } catch (_: Exception) {
        }
        val transactions = getRecentTokenTransactionsUseCase(
            chainId = chainId,
            tokenContractAddress = contractAddress,
            limit = 5,
        )
        _uiState.update {
            it.copy(
                recentTransactions = transactions,
                isHistoryConfigured = historyConfigured,
                isHistoryPlanGated = historyPlanGated,
            )
        }
    }

    /**
     * Called when the user selects a chart range chip.
     *
     * @param days One of 1, 7, 30, or 365.
     */
    fun onChartRangeSelected(days: Int) {
        _selectedChartDays.value = days
        viewModelScope.launch {
            loadChartDataSync()
        }
    }

    /**
     * Pull-to-refresh: reload explorer cache, chart, and recent transactions.
     */
    fun onRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                refreshTransactionHistoryUseCase(chainId)
            } catch (_: Exception) {
            }
            loadChartDataSync()
            loadRecentTransactionsSync()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
}
