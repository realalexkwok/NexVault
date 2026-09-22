package com.nexvault.wallet.data.repository

import com.nexvault.wallet.core.datastore.model.NetworkType
import com.nexvault.wallet.core.datastore.preferences.UserPreferencesDataStore
import com.nexvault.wallet.domain.model.chain.SupportedChains
import com.nexvault.wallet.domain.model.common.DataResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChainRepositoryImplTest {

    private lateinit var userPreferences: UserPreferencesDataStore
    private lateinit var repository: ChainRepositoryImpl

    @Before
    fun setup() {
        userPreferences = mockk(relaxed = true)
        repository = ChainRepositoryImpl(userPreferences)
    }

    @Test
    fun getSupportedChains_returnsAll4Chains() = runTest {
        val chains = repository.getSupportedChains().first()
        assertEquals(4, chains.size)
        assertTrue(chains.any { it.chainId == 1 })
        assertTrue(chains.any { it.chainId == SupportedChains.ETHEREUM_SEPOLIA.chainId })
        assertTrue(chains.any { it.chainId == 56 })
        assertTrue(chains.any { it.chainId == 137 })
    }

    @Test
    fun getSelectedChain_defaultEthereum() = runTest {
        every { userPreferences.selectedNetwork } returns flowOf(NetworkType.MAINNET)

        val chain = repository.getSelectedChain().first()
        assertEquals(1, chain.chainId)
        assertEquals("Ethereum", chain.name)
    }

    @Test
    fun setSelectedChain_validChainId_callsRepository() = runTest {
        coEvery { userPreferences.setSelectedNetwork(any()) } returns Unit

        val result = repository.setSelectedChain(56)

        assertTrue(result is DataResult.Success)
        // BSC (56) must persist as its own network type — it used to collapse to MAINNET,
        // which broke AC-2.1 (selection survives restart) for BSC and Polygon.
        coVerify { userPreferences.setSelectedNetwork(NetworkType.BSC) }
    }

    @Test
    fun setSelectedChain_bscAndPolygonRoundTripThroughStorage() = runTest {
        listOf(56 to NetworkType.BSC, 137 to NetworkType.POLYGON).forEach { (chainId, networkType) ->
            val stored = slot<NetworkType>()
            coEvery { userPreferences.setSelectedNetwork(capture(stored)) } returns Unit
            every { userPreferences.selectedNetwork } returns flowOf(networkType)

            repository.setSelectedChain(chainId)
            val chain = repository.getSelectedChain().first()

            assertEquals(networkType, stored.captured)
            assertEquals(chainId, chain.chainId)
        }
    }

    @Test
    fun setSelectedChain_invalidChainId_returnsError() = runTest {
        val result = repository.setSelectedChain(999)

        assertTrue(result is DataResult.Error)
    }

    @Test
    fun getChainById_knownChain_returnsChain() {
        val chain = repository.getChainById(1)

        assertNotNull(chain)
        assertEquals("Ethereum", chain?.name)
    }

    @Test
    fun getChainById_unknownChain_returnsNull() {
        val chain = repository.getChainById(999)

        assertNull(chain)
    }

    @Test
    fun setChainVisible_hideChain_filtersFromVisibleChains() = runTest {
        repository.setChainVisible(56, false)

        val chains = repository.getVisibleChains().first()
        assertFalse(chains.any { it.chainId == 56 })
    }

    @Test
    fun setChainVisible_showChain_includesInVisibleChains() = runTest {
        // First hide BSC
        repository.setChainVisible(56, false)
        // Then show it again
        repository.setChainVisible(56, true)

        val chains = repository.getVisibleChains().first()
        assertTrue(chains.any { it.chainId == 56 })
    }
}
