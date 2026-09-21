package com.nexvault.wallet.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.nexvault.wallet.core.ui.R
import com.nexvault.wallet.core.ui.preview.ThemePreviewWrapper
import com.nexvault.wallet.core.ui.theme.NexVaultDimens

/**
 * Small badge showing the current blockchain network.
 *
 * Used on the Receive screen and transaction detail to indicate
 * which network an address or transaction belongs to.
 *
 * Ref: doc/03-UI-UX-DESIGN.md Section 1.3 — ChainBadge component
 *
 * @param chainName Display name of the chain
 * @param chainIconRes Drawable resource for the chain icon
 * @param isTestnet Whether to show a testnet indicator
 * @param modifier Optional modifier
 */
@Composable
fun ChainBadge(
    chainName: String,
    chainIconRes: Int,
    isTestnet: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(NexVaultDimens.cornerRadiusLarge))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = NexVaultDimens.spacing10, vertical = NexVaultDimens.spacingXs),
    ) {
        if (chainIconRes != 0) {
            Image(
                painter = painterResource(id = chainIconRes),
                contentDescription = chainName,
                modifier = Modifier.size(NexVaultDimens.iconSizeXxs),
            )
            Spacer(modifier = Modifier.width(NexVaultDimens.spacingXs))
        }
        Text(
            text = if (isTestnet) "$chainName (Testnet)" else chainName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChainBadgePreview() {
    ThemePreviewWrapper {
        ChainBadge(chainName = "Ethereum", chainIconRes = R.drawable.ic_chain_ethereum)
    }
}
