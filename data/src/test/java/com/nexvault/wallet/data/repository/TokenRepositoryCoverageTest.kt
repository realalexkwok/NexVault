package com.nexvault.wallet.data.repository

import com.google.common.truth.Truth.assertThat
import com.nexvault.wallet.core.database.dao.TokenDao
import com.nexvault.wallet.core.database.entity.TokenEntity
import com.nexvault.wallet.core.network.api.CoinGeckoApi
import com.nexvault.wallet.core.network.config.ChainConfigProvider
import com.nexvault.wallet.core.network.dto.PriceHistoryResponse
import com.nexvault.wallet.core.network.web3.Web3jProvider
import com.nexvault.wallet.domain.model.common.ApiKeyNotConfiguredException
import com.nexvault.wallet.domain.model.chain.SupportedChains
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.repository.ChainRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Roadmap 2.6 coverage: the token repository paths beyond the key-gate tests — removal, seeding,
 * portfolio chart, token detail, and price-chart lookups.
 */
class TokenRepositoryCoverageTest {
    private lateinit var tokenDao: TokenDao
    private lateinit var coinGeckoApi: CoinGeckoApi
    private lateinit var chainConfigProvider: ChainConfigProvider
    private lateinit var chainRepository: ChainRepository
    private lateinit var web3jProvider: Web3jProvider
    private lateinit var repository: TokenRepositoryImpl

    @Before
    fun setup() {
        tokenDao = mockk(relaxed = true)
        coinGeckoApi = mockk(relaxed = true)
        chainConfigProvider = mockk(relaxed = true)
        chainRepository = mockk(relaxed = true)
        web3jProvider = mockk(relaxed = true)
        repository =
            TokenRepositoryImpl(
                tokenDao = tokenDao,
                coinGeckoApi = coinGeckoApi,
                chainConfigProvider = chainConfigProvider,
                chainRepository = chainRepository,
                web3jProvider = web3jProvider,
            )
        coEvery { chainRepository.getChainById(1) } returns SupportedChains.ETHEREUM_MAINNET
    }

    @Test
    fun removeToken_deletesTheRow() = runTest {
        val result = repository.removeToken(1, "0xtoken")

        assertTrue(result is DataResult.Success)
        io.mockk.coVerify { tokenDao.deleteToken("0xtoken", 1) }
    }

    @Test
    fun getTotalFiatValue_defaultsToZeroWhenAbsent() = runTest {
        every { tokenDao.getTotalFiatValue(1) } returns kotlinx.coroutines.flow.flowOf(null)

        val value = repository.getTotalFiatValue(1, "0xaddr").first()

        assertThat(value).isEqualTo(0.0)
    }

    @Test
    fun getPortfolioChartData_mapsThePriceRows() = runTest {
        coEvery {
            coinGeckoApi.getPriceHistory(
                coinId = "ethereum",
                vsCurrency = "usd",
                days = "7",
            )
        } returns PriceHistoryResponse(prices = listOf(listOf(1700000000.0, 2000.0)))

        val result = repository.getPortfolioChartData(1, 7)

        assertTrue(result is DataResult.Success)
        val points = (result as DataResult.Success).data
        assertThat(points).hasSize(1)
        assertThat(points[0].timestamp).isEqualTo(1700000000L)
        assertThat(points[0].value).isEqualTo(2000.0)
    }

    @Test
    fun getPortfolioChartData_unknownChainErrors() = runTest {
        coEvery { chainRepository.getChainById(999) } returns null

        val result = repository.getPortfolioChartData(999, 7)

        assertTrue(result is DataResult.Error)
    }

    @Test
    fun getTokenDetail_missingRowErrors() = runTest {
        coEvery { tokenDao.getToken("0xtoken", 1) } returns null

        val result = repository.getTokenDetail(1, "0xtoken", "0xaddr")

        assertTrue(result is DataResult.Error)
    }

    @Test
    fun getTokenDetail_returnsTheDomainToken() = runTest {
        coEvery { tokenDao.getToken("0xtoken", 1) } returns TransactionTestFixtures.tokenEntity("0xtoken", 18, "TKN")

        val result = repository.getTokenDetail(1, "0xtoken", "0xaddr")

        assertTrue(result is DataResult.Success)
        assertThat((result as DataResult.Success).data.symbol).isEqualTo("TKN")
    }

    @Test
    fun getTokenPriceChart_fallsBackToTheNativeCoinId() = runTest {
        coEvery { tokenDao.getToken("0xtoken", 1) } returns
            TransactionTestFixtures.tokenEntity("0xtoken", 18, "TKN").copy(coinGeckoId = null)
        coEvery {
            coinGeckoApi.getPriceHistory(
                coinId = "ethereum",
                vsCurrency = "usd",
                days = "30",
            )
        } returns PriceHistoryResponse(prices = emptyList())

        val result = repository.getTokenPriceChart(1, "0xtoken", 30)

        assertTrue(result is DataResult.Success)
        assertThat((result as DataResult.Success).data).isEmpty()
    }

    @Test
    fun seedDefaultTokens_skipsWhenRowsExist() = runTest {
        coEvery { tokenDao.getTokenCountByChain(1) } returns 3

        val result = repository.seedDefaultTokens(1)

        assertTrue(result is DataResult.Success)
        io.mockk.coVerify(exactly = 0) { tokenDao.upsertTokens(any()) }
    }

    @Test
    fun seedDefaultTokens_upsertsWhenEmpty() = runTest {
        coEvery { tokenDao.getTokenCountByChain(1) } returns 0

        val result = repository.seedDefaultTokens(1)

        assertTrue(result is DataResult.Success)
        io.mockk.coVerify(exactly = 1) { tokenDao.upsertTokens(any()) }
    }

    @Test
    fun refreshBalances_keyGateOnKeyedChains() = runTest {
        every { chainConfigProvider.isRpcConfigured(1) } returns false

        val result = repository.refreshBalances(1, "0xaddr")

        assertTrue(result is DataResult.Error)
        assertTrue((result as DataResult.Error).exception is ApiKeyNotConfiguredException)
    }
}
