package com.nexvault.wallet.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexvault.wallet.core.ui.components.EmptyStateView
import com.nexvault.wallet.core.ui.components.NexVaultTopBar
import com.nexvault.wallet.core.ui.components.TransactionRow
import com.nexvault.wallet.core.ui.theme.NexVaultDimens
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

/** Distance from the end (in items) at which the next page is requested. */
private const val LOAD_MORE_THRESHOLD = 2

/**
 * Transaction history tab (roadmap 2.8, AC-2.8): date-grouped list with sticky headers, filter
 * chips, explorer-backed pagination on scroll and a detail screen per row.
 */
@Composable
fun HistoryScreen(
    onNavigateToDetail: (txHash: String, chainId: Int) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LoadMoreOnScroll(listState = listState, onLoadMore = viewModel::loadMore)

    Scaffold(
        topBar = { NexVaultTopBar(title = stringResource(R.string.history_title)) },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            FilterChipsRow(
                selected = uiState.filter,
                onSelected = viewModel::onFilterSelected,
            )
            HistoryNotice(uiState)
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    uiState.isLoading -> HistoryLoading()
                    uiState.isEmpty -> HistoryEmpty(uiState)
                    else -> HistoryList(
                        uiState = uiState,
                        listState = listState,
                        onNavigateToDetail = onNavigateToDetail,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryList(
    uiState: HistoryUiState,
    listState: LazyListState,
    onNavigateToDetail: (String, Int) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = NexVaultDimens.spacingXl),
    ) {
        uiState.groups.forEach { group ->
            stickyHeader(key = group.label) {
                DateHeader(label = group.label)
            }
            items(
                count = group.transactions.size,
                key = { index -> group.transactions[index].txHash + group.transactions[index].chainId },
            ) { index ->
                val transaction = group.transactions[index]
                TransactionRow(
                    transaction = transaction,
                    tokenSymbol = transaction.tokenSymbol ?: uiState.chain?.symbol.orEmpty(),
                    tokenDecimals = transaction.tokenDecimals ?: NATIVE_DECIMALS,
                    modifier =
                        Modifier.clickable {
                            onNavigateToDetail(transaction.txHash, transaction.chainId)
                        },
                )
            }
        }
        item(key = "history_footer") {
            HistoryFooter(uiState)
        }
    }
}

@Composable
private fun DateHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = NexVaultDimens.spacingMd, vertical = NexVaultDimens.spacingSm),
    )
}

@Composable
private fun FilterChipsRow(
    selected: HistoryFilter,
    onSelected: (HistoryFilter) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NexVaultDimens.spacingSm),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = NexVaultDimens.spacingMd, vertical = NexVaultDimens.spacingSm),
    ) {
        HistoryFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelected(filter) },
                label = { Text(stringResource(filter.labelRes)) },
            )
        }
    }
}

/** The explorer's standing conditions, surfaced the way the token detail screen does. */
@Composable
private fun HistoryNotice(uiState: HistoryUiState) {
    val message =
        when {
            !uiState.isExplorerConfigured -> stringResource(R.string.history_explorer_not_configured)
            uiState.isPlanGated -> stringResource(R.string.history_explorer_plan_gated)
            else -> null
        } ?: return
    Row(
        horizontalArrangement = Arrangement.spacedBy(NexVaultDimens.spacingSm),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = NexVaultDimens.spacingMd, vertical = NexVaultDimens.spacingSm),
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = NexVaultDimens.spacingXxs),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun HistoryEmpty(uiState: HistoryUiState) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (uiState.errorMessage != null && uiState.isExplorerConfigured) {
            EmptyStateView(
                title = stringResource(R.string.history_error_title),
                subtitle = uiState.errorMessage,
            )
        } else {
            EmptyStateView(
                title = stringResource(R.string.history_empty_title),
                subtitle = stringResource(R.string.history_empty_subtitle),
            )
        }
    }
}

@Composable
private fun HistoryFooter(uiState: HistoryUiState) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(NexVaultDimens.spacingMd),
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.isLoadingMore -> CircularProgressIndicator()
            !uiState.hasMore && uiState.groups.isNotEmpty() ->
                Text(
                    text = stringResource(R.string.history_all_loaded),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
        }
    }
}

/** Requests the next page once the list is within [LOAD_MORE_THRESHOLD] items of its end. */
@Composable
private fun LoadMoreOnScroll(
    listState: LazyListState,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { lastVisible ->
                val total = listState.layoutInfo.totalItemsCount
                if (total > 0 && lastVisible >= total - 1 - LOAD_MORE_THRESHOLD) onLoadMore()
            }
    }
}

/** Native coin decimals, used when a row carries no token metadata. */
private const val NATIVE_DECIMALS = 18
