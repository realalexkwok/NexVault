package com.nexvault.wallet.data.mapper

import com.nexvault.wallet.core.datastore.model.NetworkType
import com.nexvault.wallet.domain.model.chain.SupportedChains
import org.junit.Assert.assertEquals
import org.junit.Test

class ChainMapperTest {
    @Test
    fun networkTypeRoundTripsForEverySupportedChain() {
        val supported =
            listOf(
                NetworkType.MAINNET,
                NetworkType.SEPOLIA,
                NetworkType.BSC,
                NetworkType.POLYGON,
            )

        supported.forEach { networkType ->
            val chainId = ChainMapper.networkTypeToChainId(networkType)

            assertEquals(networkType, ChainMapper.chainIdToNetworkType(chainId))
        }
    }

    @Test
    fun everySupportedChainMapsToItsOwnNetworkType() {
        assertEquals(
            NetworkType.MAINNET,
            ChainMapper.chainToNetworkType(SupportedChains.ETHEREUM_MAINNET),
        )
        assertEquals(
            NetworkType.SEPOLIA,
            ChainMapper.chainToNetworkType(SupportedChains.ETHEREUM_SEPOLIA),
        )
        assertEquals(
            NetworkType.BSC,
            ChainMapper.chainToNetworkType(SupportedChains.BSC_MAINNET),
        )
        assertEquals(
            NetworkType.POLYGON,
            ChainMapper.chainToNetworkType(SupportedChains.POLYGON_MAINNET),
        )
    }

    @Test
    fun unknownChainIdFallsBackToMainnet() {
        assertEquals(NetworkType.MAINNET, ChainMapper.chainIdToNetworkType(999_999))
    }
}
