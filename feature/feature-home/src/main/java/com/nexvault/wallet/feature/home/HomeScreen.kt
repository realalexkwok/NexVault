package com.nexvault.wallet.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexvault.wallet.core.ui.components.ChainSelectorDropdown
import com.nexvault.wallet.core.ui.components.NexVaultCard
import com.nexvault.wallet.core.ui.components.SimpleLineChart
import com.nexvault.wallet.core.ui.components.TokenIcon
import com.nexvault.wallet.core.ui.theme.NexVaultDimens
import com.nexvault.wallet.core.ui.util.formatFiatValue
import com.nexvault.wallet.core.ui.util.formatTokenBalance
import com.nexvault.wallet.core.ui.mapper.toChainUi
import com.nexvault.wallet.domain.model.chain.Chain
import com.nexvault.wallet.domain.model.token.PricePoint
import com.nexvault.wallet.domain.model.token.Token
import com.nexvault.wallet.feature.home.R
import java.math.BigDecimal

/**
 * Home dashboard: portfolio value, chart, token list, and quick actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTokenDetail: (contractAddress: String, chainId: Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorText =
        uiState.errorMessage
            ?: uiState.errorRes?.let { stringResource(it, *uiState.errorArgs.toTypedArray()) }
            ?: uiState.errorPluralsRes?.let {
                pluralStringResource(it, uiState.errorQuantity, *uiState.errorArgs.toTypedArray())
            }

    LaunchedEffect(errorText) {
        if (errorText != null) {
            snackbarHostState.showSnackbar(errorText)
            viewModel.onErrorDismissed()
        }
    }

    if (uiState.showAddTokenDialog) {
        AddTokenDialog(
            isLoading = uiState.addTokenLoading,
            errorRes = uiState.addTokenErrorRes,
            onConfirm = { viewModel.onAddCustomToken(it) },
            onDismiss = { viewModel.onDismissAddTokenDialog() },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.onRefresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = NexVaultDimens.spacingMd),
            ) {
                item(key = "chain_selector") {
                    ChainSelectorHeader(
                        selectedChain = uiState.selectedChain,
                        supportedChains = uiState.supportedChains,
                        onChainSelected = { viewModel.onChainSelected(it) },
                    )
                }
                item(key = "balance") {
                    PortfolioBalanceSection(
                        totalFiatValue = uiState.totalFiatValue,
                        change24hPercent = uiState.change24hPercent,
                        isLoading = uiState.isLoading,
                    )
                }
                item(key = "chart") {
                    PortfolioChartSection(
                        chartData = uiState.chartData,
                        selectedDays = uiState.selectedChartDays,
                        onRangeSelected = { viewModel.onChartRangeSelected(it) },
                    )
                }
                item(key = "actions") {
                    QuickActionsRow()
                }
                item(key = "tokens_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = NexVaultDimens.spacingMd, vertical = NexVaultDimens.spacingSm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.home_tokens_header),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        TextButton(onClick = { viewModel.onShowAddTokenDialog() }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.home_add_token_icon_description),
                                modifier = Modifier.size(NexVaultDimens.iconSizeXs),
                            )
                            Spacer(modifier = Modifier.width(NexVaultDimens.spacingXs))
                            Text(stringResource(R.string.home_add_token))
                        }
                    }
                }
                if (uiState.isLoading && uiState.tokens.isEmpty()) {
                    items(5, key = { "shimmer_$it" }) {
                        TokenRowShimmer()
                    }
                }
                items(
                    items = uiState.tokens,
                    key = { "${it.contractAddress}-${it.chainId}" },
                ) { token ->
                    TokenRow(
                        token = token,
                        onClick = {
                            onNavigateToTokenDetail(token.contractAddress, token.chainId)
                        },
                    )
                }
                if (!uiState.isLoading && uiState.tokens.isEmpty()) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(NexVaultDimens.spacingXl),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.home_empty_tokens),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChainSelectorHeader(
    selectedChain: Chain?,
    supportedChains: List<Chain>,
    onChainSelected: (Int) -> Unit,
) {
    val chain = selectedChain ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NexVaultDimens.spacingMd, vertical = NexVaultDimens.spacingSm),
    ) {
        ChainSelectorDropdown(
            selectedChain = chain.toChainUi(),
            supportedChains = supportedChains.map { it.toChainUi() },
            onChainSelected = onChainSelected,
        )
    }
}

@Composable
private fun PortfolioBalanceSection(
    totalFiatValue: Double,
    change24hPercent: Double,
    isLoading: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NexVaultDimens.spacingMd, vertical = NexVaultDimens.spacingSm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_total_balance),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(NexVaultDimens.spacingXs))
        if (isLoading && totalFiatValue == 0.0) {
            Box(
                modifier = Modifier
                    .width(NexVaultDimens.skeletonBlockWidthWide)
                    .height(NexVaultDimens.skeletonBlockHeightLarge)
                    .padding(NexVaultDimens.spacingXs)
                    .clip(RoundedCornerShape(NexVaultDimens.cornerRadiusSmall))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        } else {
            Text(
                text = formatFiatValue(totalFiatValue),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(NexVaultDimens.spacingXs))
        val changeColor = when {
            change24hPercent > 0 -> MaterialTheme.colorScheme.primary
            change24hPercent < 0 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        val changePrefix = if (change24hPercent > 0) "+" else ""
        Text(
            text = "${changePrefix}${String.format("%.2f", change24hPercent)}% (24h)",
            style = MaterialTheme.typography.bodyMedium,
            color = changeColor,
        )
    }
}

@Composable
private fun PortfolioChartSection(
    chartData: List<PricePoint>,
    selectedDays: Int,
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
                .height(NexVaultDimens.chartHeight),
        ) {
            if (chartData.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.home_chart_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                SimpleLineChart(
                    dataPoints = chartData,
                    modifier = Modifier.fillMaxSize(),
                    lineColor = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(modifier = Modifier.height(NexVaultDimens.spacingSm))
        val ranges =
            listOf(
                1 to stringResource(R.string.home_chart_range_1d),
                7 to stringResource(R.string.home_chart_range_7d),
                30 to stringResource(R.string.home_chart_range_1m),
                90 to stringResource(R.string.home_chart_range_3m),
                365 to stringResource(R.string.home_chart_range_all),
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
private fun QuickActionsRow() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NexVaultDimens.spacingMd, vertical = NexVaultDimens.spacing12),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            QuickActionButton(
                icon = Icons.Default.ArrowUpward,
                label = stringResource(R.string.home_action_send),
            )
            QuickActionButton(
                icon = Icons.Default.ArrowDownward,
                label = stringResource(R.string.home_action_receive),
            )
            QuickActionButton(
                icon = Icons.Default.SwapHoriz,
                label = stringResource(R.string.home_action_swap),
            )
        }
        // Roadmap 2.0.3: these actions have no destination yet (Send 2.6, Receive 2.7,
        // Swap 3.3), so the buttons are disabled and the reason is stated right under them.
        Text(
            text = stringResource(R.string.home_actions_coming_soon),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = NexVaultDimens.spacingMd),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilledTonalIconButton(
            onClick = { },
            enabled = false,
            modifier = Modifier.size(NexVaultDimens.actionButtonSize),
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(NexVaultDimens.spacingXs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}

@Composable
fun TokenRow(
    token: Token,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = NexVaultDimens.spacingMd, vertical = NexVaultDimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TokenIcon(
            imageUrl = token.logoUrl,
            symbol = token.symbol,
            modifier = Modifier.size(NexVaultDimens.tokenIconSize),
        )
        Spacer(modifier = Modifier.width(NexVaultDimens.spacing12))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = token.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = token.symbol,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${formatTokenBalance(token.balance)} ${token.symbol}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatFiatValue(token.fiatValue ?: 0.0),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                token.priceChange24h?.let { change ->
                    Spacer(modifier = Modifier.width(NexVaultDimens.spacingXs))
                    val changeColor = if (change >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                    val prefix = if (change >= 0) "+" else ""
                    Text(
                        text = "${prefix}${String.format("%.1f", change)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = changeColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun TokenRowShimmer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NexVaultDimens.spacingMd, vertical = NexVaultDimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(NexVaultDimens.tokenIconSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(modifier = Modifier.width(NexVaultDimens.spacing12))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(NexVaultDimens.skeletonBlockWidthMedium)
                    .height(NexVaultDimens.spacingMd)
                    .clip(RoundedCornerShape(NexVaultDimens.cornerRadiusXSmall))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(modifier = Modifier.height(NexVaultDimens.spacingXs))
            Box(
                modifier = Modifier
                    .width(NexVaultDimens.skeletonBlockWidth)
                    .height(NexVaultDimens.skeletonBlockHeight)
                    .clip(RoundedCornerShape(NexVaultDimens.cornerRadiusXSmall))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}
