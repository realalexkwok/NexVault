package com.nexvault.wallet.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.TextButton
import com.nexvault.wallet.core.ui.R
import com.nexvault.wallet.core.ui.preview.ThemePreviewWrapper
import com.nexvault.wallet.core.ui.theme.NexVaultDimens

/**
 * Dropdown selector for switching between blockchain networks.
 *
 * Displays the currently selected chain with its icon and name.
 * Tapping opens a dropdown menu showing all supported chains.
 *
 * Ref: doc/03-UI-UX-DESIGN.md Section 2.3 — ChainSelector in Home Screen top bar
 * Ref: doc/05-IMPLEMENTATION-PLAN-PHASE2.md Task 2.3.3
 *
 * @param selectedChain The currently selected chain
 * @param supportedChains List of all available chains
 * @param onChainSelected Callback when user selects a different chain
 * @param modifier Optional modifier
 */
@Composable
fun ChainSelectorDropdown(
    selectedChain: ChainUi,
    supportedChains: List<ChainUi>,
    onChainSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.padding(horizontal = NexVaultDimens.spacing12, vertical = NexVaultDimens.spacingSm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(id = selectedChain.iconRes),
                    contentDescription = selectedChain.name,
                    modifier = Modifier.size(NexVaultDimens.iconSizeMedium),
                )
                Spacer(modifier = Modifier.width(NexVaultDimens.spacingSm))
                Text(
                    text = selectedChain.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(NexVaultDimens.spacingXs))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select network",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            supportedChains.forEach { chain ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = chain.iconRes),
                                contentDescription = chain.name,
                                modifier = Modifier.size(NexVaultDimens.iconSizeSmall),
                            )
                            Spacer(modifier = Modifier.width(NexVaultDimens.spacingSm))
                            Column {
                                Text(
                                    text = chain.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (chain.isTestnet) {
                                    Text(
                                        text = "Testnet",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onChainSelected(chain.chainId)
                    },
                    trailingIcon = if (chain.chainId == selectedChain.chainId) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else null,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChainSelectorDropdownPreview() {
    ThemePreviewWrapper {
        ChainSelectorDropdown(
            selectedChain =
                ChainUi(
                    chainId = 1,
                    name = "Ethereum",
                    symbol = "ETH",
                    iconRes = R.drawable.ic_chain_ethereum,
                    isTestnet = false,
                ),
            supportedChains =
                listOf(
                    ChainUi(
                        chainId = 1,
                        name = "Ethereum",
                        symbol = "ETH",
                        iconRes = R.drawable.ic_chain_ethereum,
                        isTestnet = false,
                    ),
                    ChainUi(
                        chainId = 137,
                        name = "Polygon",
                        symbol = "MATIC",
                        iconRes = R.drawable.ic_chain_polygon,
                        isTestnet = false,
                    ),
                    ChainUi(
                        chainId = 11155111,
                        name = "Sepolia",
                        symbol = "ETH",
                        iconRes = R.drawable.ic_chain_sepolia,
                        isTestnet = true,
                    ),
                ),
            onChainSelected = {},
        )
    }
}
