package com.nexvault.wallet.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexvault.wallet.core.ui.mapper.toChainUi
import com.nexvault.wallet.domain.model.common.ApiKeyNotConfiguredException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.ExplorerPlanUnsupportedException
import com.nexvault.wallet.domain.model.transaction.Transaction
import com.nexvault.wallet.domain.model.transaction.TransactionStatus
import com.nexvault.wallet.domain.usecase.chain.GetSelectedChainUseCase
import com.nexvault.wallet.domain.usecase.token.RefreshTransactionHistoryUseCase
import com.nexvault.wallet.domain.usecase.transaction.GetTransactionHistoryUseCase
import com.nexvault.wallet.domain.usecase.transaction.UpdateTransactionStatusUseCase
import com.nexvault.wallet.domain.usecase.wallet.GetActiveWalletUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for the transaction history tab (roadmap 2.8, AC-2.8).
 *
 * The list is a window over the stored rows: [refresh] fetches explorer page 1, [loadMore] fetches
 * the next page when the user reaches the end (capped by [MAX_PAGES]), and every fetch is followed
 * by a receipt check for the pending rows so a mined transaction stops showing as pending.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getActiveWalletUseCase: GetActiveWalletUseCase,
    private val getSelectedChainUseCase: GetSelectedChainUseCase,
    private val getTransactionHistoryUseCase: GetTransactionHistoryUseCase,
    private val refreshTransactionHistoryUseCase: RefreshTransactionHistoryUseCase,
    private val updateTransactionStatusUseCase: UpdateTransactionStatusUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var chainId: Int? = null
    private var address: String? = null
    private var currentPage = 0
    private val loadedRows = mutableListOf<Transaction>()

    init {
        viewModelScope.launch {
            combine(getActiveWalletUseCase(), getSelectedChainUseCase()) { wallet, chain ->
                wallet?.accounts?.firstOrNull { it.isActive }?.address to chain
            }.collect { (activeAddress, chain) ->
                val chainChanged = chainId != chain.chainId
                address = activeAddress
                chainId = chain.chainId
                _uiState.update { it.copy(chain = chain.toChainUi()) }
                if (chainChanged) {
                    loadedRows.clear()
                    currentPage = 0
                    _uiState.update {
                        it.copy(
                            groups = emptyList(),
                            hasMore = true,
                            errorMessage = null,
                            isLoading = true,
                        )
                    }
                    refresh()
                }
            }
        }
    }

    /** Pull-to-refresh: reload page 1 and re-check the pending rows. */
    fun refresh() {
        viewModelScope.launch {
            val chain = chainId ?: return@launch
            if (address == null) return@launch
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            val result = refreshTransactionHistoryUseCase(chain, page = 1, pageSize = PAGE_SIZE)
            applyExplorerResult(result)
            currentPage = 0
            loadPage(chain, page = 1, replace = true)
            checkPendingReceipts(chain)
            _uiState.update { it.copy(isRefreshing = false, isLoading = false) }
        }
    }

    /** Called when the list reaches its end; fetches the next explorer page until [MAX_PAGES]. */
    fun loadMore() {
        val chain = chainId ?: return
        if (address == null) return
        val state = _uiState.value
        val busy = state.isLoadingMore || state.isRefreshing
        if (busy || !state.hasMore) return
        if (currentPage >= MAX_PAGES) {
            _uiState.update { it.copy(hasMore = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val nextPage = currentPage + 1
            val result = refreshTransactionHistoryUseCase(chain, page = nextPage, pageSize = PAGE_SIZE)
            applyExplorerResult(result)
            loadPage(chain, page = nextPage, replace = false)
            checkPendingReceipts(chain)
            _uiState.update { it.copy(isLoadingMore = false) }
        }
    }

    /** Applies a filter chip; the loaded rows are re-grouped, no network call is made. */
    fun onFilterSelected(filter: HistoryFilter) {
        _uiState.update { it.copy(filter = filter) }
        regroup()
    }

    private suspend fun loadPage(
        chain: Int,
        page: Int,
        replace: Boolean,
    ) {
        when (val result = getTransactionHistoryUseCase(chain, page = page, pageSize = PAGE_SIZE)) {
            is DataResult.Success -> {
                if (replace) loadedRows.clear()
                loadedRows += result.data
                currentPage = page
                val full = result.data.size == PAGE_SIZE
                _uiState.update { it.copy(hasMore = full) }
                regroup()
            }

            is DataResult.Error -> {
                _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    /** Re-checks every pending row that is currently loaded. */
    private suspend fun checkPendingReceipts(chain: Int) {
        val pending = loadedRows.filter { it.status == TransactionStatus.PENDING }
        if (pending.isEmpty()) return
        for (row in pending) {
            when (val result = updateTransactionStatusUseCase(row.txHash, chain)) {
                is DataResult.Success -> {
                    val index = loadedRows.indexOfFirst { it.txHash == row.txHash && it.chainId == row.chainId }
                    if (index >= 0) loadedRows[index] = result.data
                }

                is DataResult.Error -> Unit
            }
        }
        regroup()
    }

    private fun applyExplorerResult(result: DataResult<Unit>) {
        when (result) {
            is DataResult.Success ->
                _uiState.update {
                    it.copy(isExplorerConfigured = true, isPlanGated = false, errorMessage = null)
                }

            is DataResult.Error -> {
                val exception = result.exception
                _uiState.update {
                    it.copy(
                        isExplorerConfigured = exception !is ApiKeyNotConfiguredException,
                        isPlanGated = exception is ExplorerPlanUnsupportedException,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    /** Re-derives the visible groups from [loadedRows] and the active filter. */
    private fun regroup() {
        val filter = _uiState.value.filter
        val visible = loadedRows.filter(filter::matches)
        _uiState.update { it.copy(groups = groupByLocalDate(visible)) }
    }

    private companion object {
        /** Rows fetched per explorer page (roadmap 2.8 decision: cap the walks at 50 pages). */
        const val PAGE_SIZE = 20

        /** Hard cap on explorer pages, protecting the API key's rate limit. */
        const val MAX_PAGES = 50
    }
}

/** Header format for older dates; today and yesterday get their own labels. */
private val DATE_HEADER_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

/**
 * Groups [transactions] into date sections, newest first, labelled Today/Yesterday/date.
 *
 * Extracted from the ViewModel so the grouping rules are unit-testable with a fixed clock.
 */
internal fun groupByLocalDate(
    transactions: List<Transaction>,
    zoneId: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zoneId),
): List<HistoryDateGroup> =
    transactions
        .sortedByDescending { it.timestamp }
        .groupBy { transaction ->
            Instant.ofEpochSecond(transaction.timestamp).atZone(zoneId).toLocalDate()
        }
        .map { (date, rows) ->
            HistoryDateGroup(
                label =
                    when (date) {
                        today -> TODAY_LABEL
                        today.minusDays(1) -> YESTERDAY_LABEL
                        else -> date.format(DATE_HEADER_FORMAT)
                    },
                transactions = rows,
            )
        }

/** Header labels; kept as constants so the strings live with the screen's copy. */
internal const val TODAY_LABEL = "Today"
internal const val YESTERDAY_LABEL = "Yesterday"
