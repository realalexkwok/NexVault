package com.nexvault.wallet.feature.tokens

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.nexvault.wallet.domain.model.token.PricePoint
import com.nexvault.wallet.domain.model.token.Token
import com.nexvault.wallet.domain.model.transaction.Transaction

/**
 * UI state for the token detail screen.
 *
 * @param token Current token row from Room, or null if missing.
 * @param chartData Price history points for the chart.
 * @param selectedChartDays Selected range in days (1, 7, 30, or 365).
 * @param isChartLoading Whether the chart request is in flight.
 * @param recentTransactions Recent transactions for this token.
 * @param isHistoryConfigured False when the block-explorer API key is missing, so the screen can
 *        say so explicitly instead of showing an empty history (roadmap 2.0.4).
 * @param isHistoryPlanGated True when the explorer answered that the current API plan does not
 *        cover this chain (roadmap 2.0.4b, for example BSC on the free Etherscan plan), so the
 *        screen shows a persistent notice instead of a misleading empty history.
 * @param isLoading Initial token observation not yet completed.
 * @param isRefreshing Pull-to-refresh in progress.
 * @param errorRes Static error text when the token cannot be shown.
 * @param errorPluralsRes Plural error text; used when [errorRes] is null.
 * @param errorQuantity Quantity for [errorPluralsRes].
 * @param errorArgs Positional args for [errorRes] or [errorPluralsRes].
 * @param errorMessage Dynamic error text; wins over the resource fields when non-null.
 */
data class TokenDetailUiState(
    val token: Token? = null,
    val chartData: List<PricePoint> = emptyList(),
    val selectedChartDays: Int = 7,
    val isChartLoading: Boolean = false,
    val recentTransactions: List<Transaction> = emptyList(),
    val isHistoryConfigured: Boolean = true,
    val isHistoryPlanGated: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    @StringRes val errorRes: Int? = null,
    @PluralsRes val errorPluralsRes: Int? = null,
    val errorQuantity: Int = 0,
    val errorArgs: List<Any> = emptyList(),
    val errorMessage: String? = null,
)
