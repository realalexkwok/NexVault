package com.nexvault.wallet.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.usecase.chain.GetSelectedChainUseCase
import com.nexvault.wallet.domain.usecase.chain.GetSupportedChainsUseCase
import com.nexvault.wallet.domain.usecase.chain.SetSelectedChainUseCase
import com.nexvault.wallet.domain.usecase.token.AddCustomTokenUseCase
import com.nexvault.wallet.domain.usecase.token.GetPortfolioUseCase
import com.nexvault.wallet.domain.usecase.token.GetPriceHistoryUseCase
import com.nexvault.wallet.domain.usecase.token.RefreshBalancesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel for the Home dashboard: portfolio, chart, chain selection, and custom tokens.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getPortfolioUseCase: GetPortfolioUseCase,
    private val refreshBalancesUseCase: RefreshBalancesUseCase,
    private val addCustomTokenUseCase: AddCustomTokenUseCase,
    private val getPriceHistoryUseCase: GetPriceHistoryUseCase,
    private val getSupportedChainsUseCase: GetSupportedChainsUseCase,
    private val getSelectedChainUseCase: GetSelectedChainUseCase,
    private val setSelectedChainUseCase: SetSelectedChainUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val chartDays = MutableStateFlow(7)

    init {
        observePortfolio()
        observeChains()
        loadInitialData()
    }

    private fun observePortfolio() {
        viewModelScope.launch {
            getPortfolioUseCase().collect { result ->
                when (result) {
                    is DataResult.Success -> {
                        val p = result.data
                        _uiState.update { state ->
                            state.copy(
                                totalFiatValue = p.totalFiatValue,
                                change24hPercent = p.change24hPercent,
                                tokens = p.tokens,
                                isLoading = false,
                            )
                        }
                    }
                    is DataResult.Error -> {
                        _uiState.update {
                            it.copy(
                                totalFiatValue = 0.0,
                                change24hPercent = 0.0,
                                tokens = emptyList(),
                                isLoading = false,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun observeChains() {
        viewModelScope.launch {
            combine(
                getSupportedChainsUseCase(),
                getSelectedChainUseCase(),
            ) { supported, selected ->
                supported to selected
            }.collect { (supported, selected) ->
                _uiState.update { state ->
                    state.copy(
                        supportedChains = supported,
                        selectedChain = selected,
                    )
                }
                loadChartData()
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorRes = null, errorMessage = null) }
            when (val result = refreshBalancesUseCase()) {
                is DataResult.Error -> {
                    val message = result.message
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorRes = if (message == null) R.string.home_error_load_balances else null,
                            errorMessage = message,
                        )
                    }
                }
                is DataResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorRes = null, errorMessage = null) }
            try {
                when (val result = refreshBalancesUseCase()) {
                    is DataResult.Error -> {
                        val errorRes =
                            when (result.exception) {
                                is IOException -> R.string.home_error_network
                                else -> R.string.home_error_refresh_failed
                            }
                        _uiState.update { it.copy(errorRes = errorRes) }
                    }
                    is DataResult.Success -> { }
                }
            } catch (e: IOException) {
                _uiState.update { it.copy(errorRes = R.string.home_error_network) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorRes = R.string.home_error_refresh_failed) }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun onChainSelected(chainId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorRes = null, errorMessage = null) }
            when (setSelectedChainUseCase(chainId)) {
                is DataResult.Error -> { }
                is DataResult.Success -> {
                    refreshBalancesUseCase()
                    loadChartData()
                }
            }
        }
    }

    fun onChartRangeSelected(days: Int) {
        chartDays.value = days
        loadChartData()
    }

    private fun loadChartData() {
        viewModelScope.launch {
            val chain = _uiState.value.selectedChain ?: return@launch
            val days = chartDays.value
            when (val result = getPriceHistoryUseCase(chain.chainId, days)) {
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(
                            chartData = result.data,
                            selectedChartDays = days,
                        )
                    }
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(chartData = emptyList(), selectedChartDays = days) }
                }
            }
        }
    }

    fun onAddCustomToken(contractAddress: String) {
        viewModelScope.launch {
            val chainId = _uiState.value.selectedChain?.chainId ?: return@launch
            _uiState.update {
                it.copy(
                    addTokenLoading = true,
                    addTokenErrorRes = null,
                    addTokenErrorMessage = null,
                )
            }
            when (val result = addCustomTokenUseCase(chainId, contractAddress)) {
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(
                            addTokenLoading = false,
                            showAddTokenDialog = false,
                            addTokenResult = result.data,
                        )
                    }
                    refreshBalancesUseCase()
                }
                is DataResult.Error -> {
                    _uiState.update {
                        it.copy(
                            addTokenLoading = false,
                            addTokenErrorRes = R.string.home_add_token_error_invalid_contract,
                        )
                    }
                }
            }
        }
    }

    fun onShowAddTokenDialog() {
        _uiState.update {
            it.copy(
                showAddTokenDialog = true,
                addTokenErrorRes = null,
                addTokenErrorMessage = null,
                addTokenResult = null,
            )
        }
    }

    fun onDismissAddTokenDialog() {
        _uiState.update {
            it.copy(
                showAddTokenDialog = false,
                addTokenErrorRes = null,
                addTokenErrorMessage = null,
            )
        }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorRes = null, errorMessage = null) }
    }
}
