package com.nexvault.wallet.data.repository

import com.google.common.truth.Truth.assertThat
import com.nexvault.wallet.core.database.dao.TransactionDao
import com.nexvault.wallet.core.network.api.BlockExplorerApi
import com.nexvault.wallet.core.network.api.BlockExplorerApiFactory
import com.nexvault.wallet.core.network.config.ChainConfigProvider
import com.nexvault.wallet.core.network.dto.EtherscanTokenTransferListResponse
import com.nexvault.wallet.core.network.dto.EtherscanTransactionListResponse
import com.nexvault.wallet.domain.model.common.ApiKeyNotConfiguredException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.ExplorerApiException
import com.nexvault.wallet.domain.model.common.ExplorerPlanUnsupportedException
import com.nexvault.wallet.domain.repository.WalletRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Roadmap 2.0.4: with the explorer key absent, the history refresh must fail with an explicit
 * [ApiKeyNotConfiguredException] and must never touch the network.
 *
 * Roadmap 2.0.4b: with a key, the refresh calls Etherscan **V2** with the chain id and turns the
 * explorer's rejection envelopes into a visible error — the plan gate as its own exception, the
 * legitimate "No transactions found" still as a successful empty result.
 */
class TransactionRepositoryImplTest {
    private lateinit var transactionDao: TransactionDao
    private lateinit var blockExplorerApiFactory: BlockExplorerApiFactory
    private lateinit var chainConfigProvider: ChainConfigProvider
    private lateinit var walletRepository: WalletRepository
    private lateinit var explorerApi: BlockExplorerApi
    private lateinit var repository: TransactionRepositoryImpl

    @Before
    fun setup() {
        transactionDao = mockk(relaxed = true)
        blockExplorerApiFactory = mockk(relaxed = true)
        chainConfigProvider = mockk(relaxed = true)
        walletRepository = mockk(relaxed = true)
        explorerApi = mockk()

        repository =
            TransactionRepositoryImpl(
                transactionDao,
                blockExplorerApiFactory,
                chainConfigProvider,
                walletRepository,
            )
    }

    @Test
    fun refreshTransactionHistory_withoutExplorerKey_failsWithoutCallingTheNetwork() =
        runTest {
            every { chainConfigProvider.isExplorerConfigured(1) } returns false

            val result = repository.refreshTransactionHistory(1, ADDRESS)

            assertTrue(result is DataResult.Error)
            assertTrue((result as DataResult.Error).exception is ApiKeyNotConfiguredException)
            coVerify(exactly = 0) { blockExplorerApiFactory.getApi(any()) }
            coVerify(exactly = 0) { blockExplorerApiFactory.getApiKey(any()) }
        }

    @Test
    fun refreshTransactionHistory_withExplorerKey_callsTheExplorer() =
        runTest {
            stubConfiguredExplorer(chainId = 1)

            repository.refreshTransactionHistory(1, ADDRESS)

            coVerify(exactly = 1) { blockExplorerApiFactory.getApi(1) }
        }

    @Test
    fun refreshTransactionHistory_mergesNativeAndTokenTransfersForTheRequestedChain() =
        runTest {
            stubConfiguredExplorer(chainId = 137)
            coEvery {
                explorerApi.getTransactions(chainId = 137, address = ADDRESS, apiKey = KEY, offset = OFFSET)
            } returns
                EtherscanTransactionListResponse(
                    status = "1",
                    message = "OK",
                    result = listOf(TransactionTestFixtures.transactionDto("0xnative")),
                )
            coEvery {
                explorerApi.getTokenTransfers(chainId = 137, address = ADDRESS, apiKey = KEY, offset = OFFSET)
            } returns
                EtherscanTokenTransferListResponse(
                    status = "1",
                    message = "OK",
                    result = listOf(TransactionTestFixtures.tokenTransferDto("0xtoken")),
                )

            val result = repository.refreshTransactionHistory(137, ADDRESS)

            assertThat(result).isInstanceOf(DataResult.Success::class.java)
            coVerify(exactly = 1) { transactionDao.upsertTransactions(match { it.size == 2 }) }
        }

    @Test
    fun refreshTransactionHistory_rejectionEnvelope_surfacesTheExplorerMessage() =
        runTest {
            stubConfiguredExplorer(chainId = 1)
            coEvery {
                explorerApi.getTransactions(chainId = 1, address = ADDRESS, apiKey = KEY, offset = OFFSET)
            } returns
                EtherscanTransactionListResponse(
                    status = "0",
                    message = "NOTOK",
                    result = null,
                    resultText = "You are using a deprecated V1 endpoint, switch to Etherscan API V2",
                )

            val result = repository.refreshTransactionHistory(1, ADDRESS)

            assertThat(result).isInstanceOf(DataResult.Error::class.java)
            val error = (result as DataResult.Error).exception
            assertThat(error).isInstanceOf(ExplorerApiException::class.java)
            assertThat(error.message).contains("deprecated V1 endpoint")
            coVerify(exactly = 0) { transactionDao.upsertTransactions(any()) }
        }

    @Test
    fun refreshTransactionHistory_planGateEnvelope_surfacesTheDedicatedException() =
        runTest {
            stubConfiguredExplorer(chainId = 56)
            coEvery {
                explorerApi.getTransactions(chainId = 56, address = ADDRESS, apiKey = KEY, offset = OFFSET)
            } returns
                EtherscanTransactionListResponse(
                    status = "0",
                    message = "NOTOK",
                    result = null,
                    resultText = PLAN_GATE_TEXT,
                )

            val result = repository.refreshTransactionHistory(56, ADDRESS)

            assertThat(result).isInstanceOf(DataResult.Error::class.java)
            val error = (result as DataResult.Error).exception
            assertThat(error).isInstanceOf(ExplorerPlanUnsupportedException::class.java)
            assertThat(error.message).contains("Free API access is not supported")
        }

    @Test
    fun refreshTransactionHistory_noTransactionsFound_isASuccessWithoutRows() =
        runTest {
            stubConfiguredExplorer(chainId = 1)
            coEvery {
                explorerApi.getTransactions(chainId = 1, address = ADDRESS, apiKey = KEY, offset = OFFSET)
            } returns
                EtherscanTransactionListResponse(
                    status = "0",
                    message = "No transactions found",
                    result = emptyList(),
                )
            coEvery {
                explorerApi.getTokenTransfers(chainId = 1, address = ADDRESS, apiKey = KEY, offset = OFFSET)
            } returns
                EtherscanTokenTransferListResponse(
                    status = "0",
                    message = "No transactions found",
                    result = emptyList(),
                )

            val result = repository.refreshTransactionHistory(1, ADDRESS)

            assertThat(result).isInstanceOf(DataResult.Success::class.java)
            coVerify(exactly = 0) { transactionDao.upsertTransactions(any()) }
        }

    @Test
    fun refreshTransactionHistory_tokenTransferRejection_surfacesAfterNativeSuccess() =
        runTest {
            stubConfiguredExplorer(chainId = 1)
            coEvery {
                explorerApi.getTokenTransfers(chainId = 1, address = ADDRESS, apiKey = KEY, offset = OFFSET)
            } returns
                EtherscanTokenTransferListResponse(
                    status = "0",
                    message = "NOTOK",
                    result = null,
                    resultText = "Max rate limit reached",
                )

            val result = repository.refreshTransactionHistory(1, ADDRESS)

            assertThat(result).isInstanceOf(DataResult.Error::class.java)
            val error = (result as DataResult.Error).exception
            assertThat(error).isInstanceOf(ExplorerApiException::class.java)
            assertThat(error.message).isEqualTo("Max rate limit reached")
            coVerify(exactly = 0) { transactionDao.upsertTransactions(any()) }
        }

    private fun stubConfiguredExplorer(chainId: Int) {
        every { chainConfigProvider.isExplorerConfigured(chainId) } returns true
        every { blockExplorerApiFactory.getApi(chainId) } returns explorerApi
        every { blockExplorerApiFactory.getApiKey(chainId) } returns KEY
        coEvery {
            explorerApi.getTransactions(chainId = chainId, address = ADDRESS, apiKey = KEY, offset = OFFSET)
        } returns EtherscanTransactionListResponse(status = "1", message = "OK", result = emptyList())
        coEvery {
            explorerApi.getTokenTransfers(chainId = chainId, address = ADDRESS, apiKey = KEY, offset = OFFSET)
        } returns EtherscanTokenTransferListResponse(status = "1", message = "OK", result = emptyList())
    }

    private companion object {
        const val ADDRESS = "0xabc"
        const val KEY = "test-key"
        const val OFFSET = 100

        /** The verbatim plan-gate body observed on chainid=56, 2026-09-26. */
        const val PLAN_GATE_TEXT =
            "Free API access is not supported for this chain. Please upgrade " +
                "your api plan for full chain coverage. https://etherscan.io/apis"
    }
}
