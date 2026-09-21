package com.nexvault.wallet.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nexvault.wallet.core.ui.preview.ThemePreviewWrapper
import com.nexvault.wallet.core.ui.theme.NexVaultDimens
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import com.nexvault.wallet.core.ui.util.formatTimestamp
import com.nexvault.wallet.core.ui.util.formatTransactionAmount
import com.nexvault.wallet.core.ui.util.truncateAddress
import com.nexvault.wallet.domain.model.transaction.Transaction
import com.nexvault.wallet.domain.model.transaction.TransactionStatus
import com.nexvault.wallet.domain.model.transaction.TransactionType
import java.math.BigDecimal
import java.math.BigInteger

/**
 * One row for a transaction in a list (token detail or history).
 *
 * @param transaction Domain transaction.
 * @param tokenSymbol Symbol shown next to the amount.
 * @param tokenDecimals Decimals hint for formatting (reserved for future precision tweaks).
 */
@Composable
fun TransactionRow(
    transaction: Transaction,
    tokenSymbol: String,
    tokenDecimals: Int,
    modifier: Modifier = Modifier,
) {
    val isSend = transaction.type == TransactionType.SEND
    val isSwap = transaction.type == TransactionType.SWAP
    val isFailed = transaction.status == TransactionStatus.FAILED
    val isPending = transaction.status == TransactionStatus.PENDING

    val icon: ImageVector = when {
        isSwap -> Icons.Default.SwapHoriz
        isSend -> Icons.Default.ArrowUpward
        else -> Icons.Default.ArrowDownward
    }

    val iconTint: Color = when {
        isFailed -> MaterialTheme.colorScheme.onSurfaceVariant
        isSwap -> NexVaultTheme.colors.info
        isSend -> MaterialTheme.colorScheme.error
        else -> NexVaultTheme.colors.positive
    }

    val amountPrefix = when {
        isSend -> "-"
        isSwap -> ""
        else -> "+"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NexVaultDimens.spacingMd, vertical = NexVaultDimens.spacing10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(NexVaultDimens.tokenIconSize)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = transaction.type.name,
                tint = iconTint,
                modifier = Modifier.size(NexVaultDimens.iconSizeSmall),
            )
        }

        Spacer(modifier = Modifier.width(NexVaultDimens.spacing12))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when {
                    isFailed -> "Failed"
                    isSwap -> "Swap"
                    isSend -> "Sent"
                    else -> "Received"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = if (isSend) {
                    "To: ${truncateAddress(transaction.toAddress)}"
                } else {
                    "From: ${truncateAddress(transaction.fromAddress)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            val displayValue = formatTransactionAmount(
                transaction.value,
                maxFractionDigits = tokenDecimals.coerceIn(2, 8),
            )
            Text(
                text = "$amountPrefix$displayValue $tokenSymbol",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isFailed) MaterialTheme.colorScheme.onSurfaceVariant else iconTint,
            )
            Text(
                text = when {
                    isPending -> "Pending"
                    isFailed -> "Failed"
                    else -> formatTimestamp(transaction.timestamp)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionRowPreview() {
    ThemePreviewWrapper {
        TransactionRow(
            transaction =
                Transaction(
                    txHash = "0x9f2c4d5e6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d",
                    chainId = 1,
                    fromAddress = "0xDc6D56BfFA21b1E9bb3C6B02F7c7cC071d351BEe",
                    toAddress = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e",
                    value = BigDecimal("1.2345"),
                    gasUsed = BigInteger.valueOf(21_000),
                    gasPrice = BigInteger.valueOf(20_000_000_000),
                    tokenSymbol = "ETH",
                    tokenContractAddress = null,
                    tokenDecimals = 18,
                    blockNumber = 20_000_000,
                    timestamp = 1_756_000_000_000,
                    status = TransactionStatus.CONFIRMED,
                    type = TransactionType.SEND,
                ),
            tokenSymbol = "ETH",
            tokenDecimals = 18,
        )
    }
}
