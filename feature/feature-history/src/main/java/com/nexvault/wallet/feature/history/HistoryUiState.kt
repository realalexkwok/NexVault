package com.nexvault.wallet.feature.history

import com.nexvault.wallet.core.ui.components.ChainUi
import com.nexvault.wallet.domain.model.transaction.Transaction
import com.nexvault.wallet.domain.model.transaction.TransactionStatus
import com.nexvault.wallet.domain.model.transaction.TransactionType

/** Filter chips of the history screen (AC-2.8: All, Sent, Received, Swap, Failed). */
enum class HistoryFilter {
    ALL,
    SENT,
    RECEIVED,
    SWAP,
    FAILED,
    ;

    /** Whether [transaction] belongs to this chip. */
    fun matches(transaction: Transaction): Boolean =
        when (this) {
            ALL -> true
            SENT -> transaction.type == TransactionType.SEND
            RECEIVED -> transaction.type == TransactionType.RECEIVE
            SWAP -> transaction.type == TransactionType.SWAP
            FAILED -> transaction.status == TransactionStatus.FAILED
        }
}

/** Chip label for each filter (AC-2.8's five chips). */
val HistoryFilter.labelRes: Int
    get() =
        when (this) {
            HistoryFilter.ALL -> R.string.history_filter_all
            HistoryFilter.SENT -> R.string.history_filter_sent
            HistoryFilter.RECEIVED -> R.string.history_filter_received
            HistoryFilter.SWAP -> R.string.history_filter_swap
            HistoryFilter.FAILED -> R.string.history_filter_failed
        }

/** One date section of the history list; [label] is the sticky header. */
data class HistoryDateGroup(
    val label: String,
    val transactions: List<Transaction>,
)

/** State of the history screen. */
data class HistoryUiState(
    val groups: List<HistoryDateGroup> = emptyList(),
    val filter: HistoryFilter = HistoryFilter.ALL,
    val chain: ChainUi? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val isExplorerConfigured: Boolean = true,
    val isPlanGated: Boolean = false,
    val errorMessage: String? = null,
) {
    /** True once the first page arrived and the filter left nothing to show. */
    val isEmpty: Boolean get() = !isLoading && groups.isEmpty()
}
