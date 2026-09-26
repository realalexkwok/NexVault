package com.nexvault.wallet.feature.tokens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexvault.wallet.core.ui.components.NexVaultCard
import com.nexvault.wallet.core.ui.components.SimpleLineChart
import com.nexvault.wallet.core.ui.components.TokenIcon
import com.nexvault.wallet.core.ui.components.TransactionRow
import com.nexvault.wallet.core.ui.theme.NexVaultDimens
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import com.nexvault.wallet.core.ui.util.formatFiatValue
import com.nexvault.wallet.core.ui.util.formatTokenBalance
import com.nexvault.wallet.domain.model.token.PricePoint
import com.nexvault.wallet.domain.model.token.Token
import com.nexvault.wallet.feature.tokens.R

/**
 * Token detail: balance, chart, stats, actions, and recent transactions.
 *
 * Send / Receive / See All are **disabled with a visible explanation** until their
 * destinations land (roadmap 2.0.3: Send 2.6, Receive 2.7, History 2.8) — the navigation
 * callbacks return with those items.
 *
 * @param onNavigateBack Back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: TokenDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val token = uiState.token

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = token?.symbol ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.token_detail_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.onRefresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                uiState.isLoading && token == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                token != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = NexVaultDimens.spacingLg),
                    ) {
                        item(key = "header") {
                            TokenDetailHeader(token = token)
                        }
                        item(key = "chart") {
                            TokenPriceChartSection(
                                chartData = uiState.chartData,
                                selectedDays = uiState.selectedChartDays,
                                isLoading = uiState.isChartLoading,
                                onRangeSelected = { viewModel.onChartRangeSelected(it) },
                            )
                        }
                        item(key = "stats") {
                            TokenPriceStats(token = token)
                        }
                        item(key = "actions") {
                            TokenActionButtons()
                        }
                        if (!uiState.isHistoryConfigured) {
                            // Roadmap 2.0.4: say why the history is empty instead of showing nothing.
                            item(key = "history_not_configured") {
                                Text(
                                    text = stringResource(R.string.token_detail_history_not_configured),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.fillMaxWidth().padding(NexVaultDimens.spacingMd),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        if (uiState.isHistoryPlanGated) {
                            // Roadmap 2.0.4b: the key works, the plan does not cover this chain
                            // (BSC on the free Etherscan plan) — a standing notice, not a snackbar.
                            item(key = "history_plan_gated") {
                                Text(
                                    text = stringResource(R.string.token_detail_history_plan_gated),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.fillMaxWidth().padding(NexVaultDimens.spacingMd),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        if (uiState.recentTransactions.isNotEmpty()) {
                            item(key = "tx_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = NexVaultDimens.spacingMd)
                                        .padding(vertical = NexVaultDimens.spacingSm),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(R.string.token_detail_recent_transactions),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    TextButton(
                                        onClick = { },
                                        enabled = false,
                                    ) {
                                        Text(stringResource(R.string.token_detail_see_all))
                                        Spacer(modifier = Modifier.width(NexVaultDimens.spacingXs))
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription =
                                                stringResource(R.string.token_detail_see_all_transactions),
                                            modifier = Modifier.size(NexVaultDimens.spacingMd),
                                        )
                                    }
                                }
                            }
                            items(
                                items = uiState.recentTransactions,
                                key = { "${it.txHash}-${it.chainId}" },
                            ) { transaction ->
                                TransactionRow(
                                    transaction = transaction,
                                    tokenSymbol = token.symbol,
                                    tokenDecimals = token.decimals,
                                )
                            }
                        }
                        if (uiState.recentTransactions.isEmpty() && !uiState.isLoading) {
                            item(key = "no_tx") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(NexVaultDimens.spacingXl),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = stringResource(R.string.token_detail_no_transactions),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    val errorText =
                        uiState.errorMessage
                            ?: uiState.errorRes?.let { stringResource(it, *uiState.errorArgs.toTypedArray()) }
                            ?: uiState.errorPluralsRes?.let {
                                pluralStringResource(it, uiState.errorQuantity, *uiState.errorArgs.toTypedArray())
                            }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(NexVaultDimens.spacingLg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = errorText ?: stringResource(R.string.token_detail_unable_to_load_token),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TokenDetailHeader(token: Token) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(NexVaultDimens.spacingMd),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TokenIcon(
            imageUrl = token.logoUrl,
            symbol = token.symbol,
            modifier = Modifier.size(NexVaultDimens.tokenLogoSize),
        )
        Spacer(modifier = Modifier.height(NexVaultDimens.spacing12))
        Text(
            text = token.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(NexVaultDimens.spacingSm))
        Text(
            text = "${formatTokenBalance(token.balance)} ${token.symbol}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(NexVaultDimens.spacingXs))
        Text(
            text = formatFiatValue(token.fiatValue ?: 0.0),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TokenPriceChartSection(
    chartData: List<PricePoint>,
    selectedDays: Int,
    isLoading: Boolean,
    onRangeSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(NexVaultDimens.spacingMd),
    ) {
        NexVaultCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(NexVaultDimens.chartHeightLarge),
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(NexVaultDimens.iconSizeMedium))
                    }
                }

                chartData.size < 2 -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.token_detail_no_chart_data),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    SimpleLineChart(
                        dataPoints = chartData,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(NexVaultDimens.spacing12),
                        lineColor = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(NexVaultDimens.spacingSm))
        val ranges =
            listOf(
                1 to stringResource(R.string.token_detail_range_1d),
                7 to stringResource(R.string.token_detail_range_7d),
                30 to stringResource(R.string.token_detail_range_1m),
                365 to stringResource(R.string.token_detail_range_1y),
            )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ranges.forEach { (days, label) ->
                val isSelected = days == selectedDays
                FilterChip(
                    selected = isSelected,
                    onClick = { onRangeSelected(days) },
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
private fun TokenPriceStats(token: Token) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NexVaultDimens.spacingMd, vertical = NexVaultDimens.spacingSm),
    ) {
        val fiatPrice = token.fiatPrice
        TokenStatRow(
            label = stringResource(R.string.token_detail_price),
            value = if (fiatPrice != null) formatFiatValue(fiatPrice) else "—",
        )
        val change24h = token.priceChange24h
        val changeColor = when {
            (change24h ?: 0.0) > 0 -> NexVaultTheme.colors.positive
            (change24h ?: 0.0) < 0 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        val changePrefix = if ((change24h ?: 0.0) > 0) "+" else ""
        TokenStatRow(
            label = stringResource(R.string.token_detail_24h_change),
            value = if (change24h != null) {
                "$changePrefix${String.format("%.2f", change24h)}%"
            } else {
                "—"
            },
            valueColor = changeColor,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = NexVaultDimens.spacingSm))
    }
}

@Composable
private fun TokenStatRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NexVaultDimens.spacing6),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor,
        )
    }
}

@Composable
private fun TokenActionButtons() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NexVaultDimens.spacingMd, vertical = NexVaultDimens.spacing12),
            horizontalArrangement = Arrangement.spacedBy(NexVaultDimens.spacing12),
        ) {
            Button(
                onClick = { },
                enabled = false,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier.size(NexVaultDimens.iconSizeXs),
                )
                Spacer(modifier = Modifier.width(NexVaultDimens.spacingSm))
                Text(stringResource(R.string.token_detail_send))
            }
            OutlinedButton(
                onClick = { },
                enabled = false,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(NexVaultDimens.iconSizeXs),
                )
                Spacer(modifier = Modifier.width(NexVaultDimens.spacingSm))
                Text(stringResource(R.string.token_detail_receive))
            }
        }
        // Roadmap 2.0.3: Send (2.6), Receive (2.7) and full history (2.8) have no destination
        // yet, so their controls are disabled and the reason is stated right under them.
        Text(
            text = stringResource(R.string.token_detail_actions_coming_soon),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = NexVaultDimens.spacingMd),
            textAlign = TextAlign.Center,
        )
    }
}
