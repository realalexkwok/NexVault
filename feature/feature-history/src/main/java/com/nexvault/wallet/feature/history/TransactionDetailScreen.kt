package com.nexvault.wallet.feature.history

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexvault.wallet.core.ui.components.NexVaultButton
import com.nexvault.wallet.core.ui.components.NexVaultCard
import com.nexvault.wallet.core.ui.components.NexVaultTopBar
import com.nexvault.wallet.core.ui.theme.NexVaultDimens
import com.nexvault.wallet.core.ui.util.formatTimestamp
import com.nexvault.wallet.core.ui.util.formatTransactionAmount
import com.nexvault.wallet.domain.model.transaction.TransactionStatus

/**
 * Full details of one transaction (roadmap 2.8, AC-2.8), with the explorer link and — for pending
 * rows — a receipt re-check.
 */
@Composable
fun TransactionDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            NexVaultTopBar(
                title = stringResource(R.string.history_detail_title),
                showBackButton = true,
                onBackClick = onNavigateBack,
            )
        },
    ) { paddingValues ->
        val transaction = uiState.transaction
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            transaction == null -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = uiState.errorMessage ?: stringResource(R.string.history_detail_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            else -> Column(
                verticalArrangement = Arrangement.spacedBy(NexVaultDimens.spacingMd),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = NexVaultDimens.spacingMd)
                        .verticalScroll(rememberScrollState()),
            ) {
                NexVaultCard(modifier = Modifier.fillMaxWidth()) {
                    DetailRow(
                        label = stringResource(R.string.history_detail_status),
                        value = stringResource(transaction.status.labelRes),
                    )
                    DetailRow(
                        label = stringResource(R.string.history_detail_amount),
                        value =
                            "${formatTransactionAmount(transaction.value)} " +
                                (transaction.tokenSymbol ?: uiState.chain?.symbol.orEmpty()),
                    )
                    DetailRow(
                        label = stringResource(R.string.history_detail_date),
                        value = formatTimestamp(transaction.timestamp),
                    )
                    DetailRow(
                        label = stringResource(R.string.history_detail_from),
                        value = transaction.fromAddress,
                        wrap = true,
                    )
                    DetailRow(
                        label = stringResource(R.string.history_detail_to),
                        value = transaction.toAddress,
                        wrap = true,
                    )
                    DetailRow(
                        label = stringResource(R.string.history_detail_block),
                        value = transaction.blockNumber.toString(),
                    )
                    transaction.gasUsed?.let { gasUsed ->
                        DetailRow(
                            label = stringResource(R.string.history_detail_gas_used),
                            value = gasUsed.toString(),
                        )
                    }
                    DetailRow(
                        label = stringResource(R.string.history_detail_hash),
                        value = transaction.txHash,
                        wrap = true,
                    )
                }

                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                if (uiState.canCheckReceipt) {
                    NexVaultButton(
                        text = stringResource(R.string.history_detail_check_receipt),
                        onClick = viewModel::onCheckReceipt,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                val explorerUrl = uiState.chain?.explorerUrl
                if (explorerUrl != null) {
                    NexVaultButton(
                        text = stringResource(R.string.history_detail_view_on_explorer),
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("$explorerUrl/tx/${transaction.txHash}")),
                            )
                        },
                        secondary = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    wrap: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = NexVaultDimens.spacingXs)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (wrap) 3 else 1,
                overflow = if (wrap) TextOverflow.Ellipsis else TextOverflow.Visible,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = NexVaultDimens.spacingXs))
    }
}

/** Status copy for the detail screen. */
internal val TransactionStatus.labelRes: Int
    get() =
        when (this) {
            TransactionStatus.PENDING -> R.string.history_status_pending
            TransactionStatus.CONFIRMED -> R.string.history_status_confirmed
            TransactionStatus.FAILED -> R.string.history_status_failed
        }
