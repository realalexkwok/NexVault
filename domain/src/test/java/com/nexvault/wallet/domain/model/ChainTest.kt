package com.nexvault.wallet.domain.model

import com.nexvault.wallet.domain.model.chain.SupportedChains
import org.junit.Assert.*
import org.junit.Test

class ChainTest {
    @Test
    fun testAllReturnsFourChains() {
        assertEquals(4, SupportedChains.all().size)
    }

    @Test
    fun testGetByChainIdEthereumMainnet() {
        val chain = SupportedChains.getByChainId(1)
        assertNotNull(chain)
        assertEquals("Ethereum", chain?.name)
    }

    @Test
    fun testGetByChainIdInvalid() {
        val chain = SupportedChains.getByChainId(999)
        assertNull(chain)
    }

    @Test
    fun testEachChainHasNonEmptyNameAndSymbol() {
        SupportedChains.all().forEach { chain ->
            assertTrue(chain.name.isNotEmpty())
            assertTrue(chain.symbol.isNotEmpty())
        }
    }

    @Test
    fun testTestnetFlags() {
        assertFalse(SupportedChains.ETHEREUM_MAINNET.isTestnet)
        assertTrue(SupportedChains.ETHEREUM_SEPOLIA.isTestnet)
        assertFalse(SupportedChains.BSC_MAINNET.isTestnet)
        assertFalse(SupportedChains.POLYGON_MAINNET.isTestnet)
    }
}
