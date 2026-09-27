package com.nexvault.wallet.core.database.dao

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.nexvault.wallet.core.database.NexVaultDatabase
import com.nexvault.wallet.core.database.entity.TokenEntity
import com.nexvault.wallet.core.database.entity.TransactionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Roadmap 2.0.6 (TC traceability): core-database had **no test source set at all**
 * (legacy CR finding 2.2-1). TC-DB-001…003 get their mapped home here, running against an
 * in-memory [NexVaultDatabase] under Robolectric — the first use of the catalog's
 * declared-but-unused Robolectric entry.
 */
@RunWith(RobolectricTestRunner::class)
class NexVaultDaoTest {
    private lateinit var database: NexVaultDatabase
    private lateinit var tokenDao: TokenDao
    private lateinit var transactionDao: TransactionDao

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        database =
            Room
                .inMemoryDatabaseBuilder(context, NexVaultDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        tokenDao = database.tokenDao()
        transactionDao = database.transactionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // TC-DB-001: upserting the same token again updates its balance — still one row.
    @Test
    fun upsertingTheSameToken_updatesTheBalanceInsteadOfDuplicating() = runTest {
        tokenDao.upsertToken(token(chainId = 1, balance = "1.0"))
        tokenDao.upsertToken(token(chainId = 1, balance = "2.0"))

        val stored = tokenDao.getToken("native", chainId = 1)
        assertThat(stored).isNotNull()
        assertThat(stored?.balance).isEqualTo("2.0")
        assertThat(tokenDao.getTokenCountByChain(1)).isEqualTo(1)
    }

    // TC-DB-002: getTokensByChain returns only that chain's tokens, fiatValue DESC.
    @Test
    fun getTokensByChain_returnsOnlyThatChainOrderedByFiatValueDescending() = runTest {
        tokenDao.upsertTokens(
            listOf(
                token(chainId = 1, symbol = "LOW", fiatValue = 10.0, contractAddress = "0xLOW"),
                token(chainId = 1, symbol = "HIGH", fiatValue = 30.0, contractAddress = "0xHIGH"),
                token(chainId = 1, symbol = "MID", fiatValue = 20.0, contractAddress = "0xMID"),
                token(chainId = 56, symbol = "BNB", fiatValue = 99.0),
                token(chainId = 56, symbol = "CAKE", fiatValue = 1.0, contractAddress = "0xCAKE"),
            ),
        )

        val chainOne = tokenDao.getTokensByChainOnce(1)
        assertThat(chainOne).hasSize(3)
        assertThat(chainOne.map { it.symbol }).containsExactly("HIGH", "MID", "LOW").inOrder()
        assertThat(tokenDao.getTokensByChainOnce(56)).hasSize(2)
    }

    // TC-DB-003: the paginated query returns the 10 newest for the requested page.
    @Test
    fun getTransactions_returnsTheTenNewestForTheRequestedPage() = runTest {
        val address = "0xABC"
        transactionDao.upsertTransactions(
            (1..25).map { index ->
                transaction(
                    hash = "0xtx$index",
                    address = address,
                    timestamp = 1_000L + index,
                )
            },
        )

        val firstPage = transactionDao.getTransactions(chainId = 1, address = address, limit = 10, offset = 0)
        assertThat(firstPage).hasSize(10)
        assertThat(firstPage.first().txHash).isEqualTo("0xtx25")
        assertThat(firstPage.last().txHash).isEqualTo("0xtx16")

        val secondPage = transactionDao.getTransactions(chainId = 1, address = address, limit = 10, offset = 10)
        assertThat(secondPage).hasSize(10)
        assertThat(secondPage.first().txHash).isEqualTo("0xtx15")
        assertThat(secondPage.last().txHash).isEqualTo("0xtx6")
    }

    private fun token(
        chainId: Int,
        balance: String = "0.0",
        symbol: String = "ETH",
        fiatValue: Double? = null,
        contractAddress: String = "native",
    ) = TokenEntity(
        contractAddress = contractAddress,
        chainId = chainId,
        symbol = symbol,
        name = symbol,
        decimals = 18,
        logoUrl = null,
        balance = balance,
        fiatPrice = null,
        fiatValue = fiatValue,
        priceChange24h = null,
        isCustom = false,
        coinGeckoId = null,
        sortOrder = 0,
        lastUpdated = 0L,
    )

    private fun transaction(
        hash: String,
        address: String,
        timestamp: Long,
    ) = TransactionEntity(
        txHash = hash,
        chainId = 1,
        fromAddress = address,
        toAddress = address,
        value = "0",
        gasUsed = null,
        gasPrice = null,
        tokenSymbol = null,
        tokenContractAddress = null,
        tokenDecimals = null,
        blockNumber = 0L,
        timestamp = timestamp,
        status = 1,
        type = "send",
    )
}
