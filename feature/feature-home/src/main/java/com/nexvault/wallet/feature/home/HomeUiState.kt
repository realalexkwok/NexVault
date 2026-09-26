package com.nexvault.wallet.feature.home

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.nexvault.wallet.domain.model.chain.Chain
import com.nexvault.wallet.domain.model.token.PricePoint
import com.nexvault.wallet.domain.model.token.Token

/**
 * UI state for the Home dashboard.
 */
data class HomeUiState(
    val totalFiatValue: Double = 0.0,
    val change24hPercent: Double = 0.0,
    val tokens: List<Token> = emptyList(),
    val chartData: List<PricePoint> = emptyList(),
    val selectedChartDays: Int = 7,
    val selectedChain: Chain? = null,
    val supportedChains: List<Chain> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    /** True when the RPC API key is missing, so the screen says so instead of showing stale data. */
    val isRpcNotConfigured: Boolean = false,
    @StringRes val errorRes: Int? = null,
    @PluralsRes val errorPluralsRes: Int? = null,
    val errorQuantity: Int = 0,
    val errorArgs: List<Any> = emptyList(),
    val errorMessage: String? = null,
    val showAddTokenDialog: Boolean = false,
    val addTokenLoading: Boolean = false,
    @StringRes val addTokenErrorRes: Int? = null,
    val addTokenErrorMessage: String? = null,
    val addTokenResult: Token? = null,
)
