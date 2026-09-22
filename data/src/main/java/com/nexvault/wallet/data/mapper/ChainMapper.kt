package com.nexvault.wallet.data.mapper

import com.nexvault.wallet.core.datastore.model.NetworkType
import com.nexvault.wallet.domain.model.chain.Chain
import com.nexvault.wallet.domain.model.chain.SupportedChains

/**
 * Maps between datastore NetworkType and domain chain ID.
 */
object ChainMapper {

    fun networkTypeToChainId(type: NetworkType): Int {
        return when (type) {
            NetworkType.MAINNET -> SupportedChains.ETHEREUM_MAINNET.chainId
            NetworkType.SEPOLIA -> SupportedChains.ETHEREUM_SEPOLIA.chainId
            NetworkType.BSC -> SupportedChains.BSC_MAINNET.chainId
            NetworkType.POLYGON -> SupportedChains.POLYGON_MAINNET.chainId
        }
    }

    fun chainIdToNetworkType(chainId: Int): NetworkType {
        return when (chainId) {
            SupportedChains.ETHEREUM_MAINNET.chainId -> NetworkType.MAINNET
            SupportedChains.ETHEREUM_SEPOLIA.chainId -> NetworkType.SEPOLIA
            SupportedChains.BSC_MAINNET.chainId -> NetworkType.BSC
            SupportedChains.POLYGON_MAINNET.chainId -> NetworkType.POLYGON
            else -> NetworkType.MAINNET // Unsupported chains fall back to Ethereum mainnet
        }
    }

    fun chainToNetworkType(chain: Chain): NetworkType {
        return chainIdToNetworkType(chain.chainId)
    }
}
