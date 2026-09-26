package com.nexvault.wallet.core.ui.components

/**
 * UI model for chain display in the selector.
 *
 * This is a presentation-layer model to avoid leaking domain models
 * into the composable. Map from domain [Chain] to [ChainUi] in the ViewModel.
 *
 * @property chainId The EIP-155 chain ID
 * @property name Human-readable chain name
 * @property symbol Native coin ticker symbol
 * @property iconRes Drawable resource ID for the chain icon
 * @property isTestnet Whether this is a testnet
 */
data class ChainUi(
    val chainId: Int,
    val name: String,
    val symbol: String,
    val iconRes: Int,
    val isTestnet: Boolean,
)
