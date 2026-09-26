package com.nexvault.wallet.core.ui.util

import com.nexvault.wallet.core.ui.R

/**
 * Maps chain IDs to their corresponding drawable resource IDs.
 *
 * This keeps the chain icon resolution in the UI layer, avoiding
 * a dependency from the data layer on Android resources.
 */
object ChainIconMapper {
    /**
     * Returns the drawable resource ID for a chain's icon.
     *
     * @param chainId The EIP-155 chain ID
     * @return Drawable resource ID, or a default icon for unknown chains
     */
    fun getIconRes(chainId: Int): Int {
        return when (chainId) {
            1 -> R.drawable.ic_chain_ethereum
            11155111 -> R.drawable.ic_chain_sepolia
            56 -> R.drawable.ic_chain_bsc
            137 -> R.drawable.ic_chain_polygon
            else -> R.drawable.ic_chain_ethereum
        }
    }
}
