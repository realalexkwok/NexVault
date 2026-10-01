package com.nexvault.wallet.data.repository

import com.google.common.truth.Truth.assertThat
import com.nexvault.wallet.core.database.dao.TokenDao
import com.nexvault.wallet.core.database.dao.TransactionDao
import com.nexvault.wallet.core.database.entity.TransactionEntity
import com.nexvault.wallet.core.network.api.BlockExplorerApi
import com.nexvault.wallet.core.network.api.BlockExplorerApiFactory
import com.nexvault.wallet.core.network.config.ChainConfigProvider
import com.nexvault.wallet.core.network.dto.EtherscanTokenTransferListResponse
import com.nexvault.wallet.core.network.dto.EtherscanTransactionListResponse
import com.nexvault.wallet.core.network.dto.EtherscanTxReceiptStatusResponse
import com.nexvault.wallet.core.network.dto.ReceiptStatusDto
import com.nexvault.wallet.core.network.rpc.ChainRpcClient
import com.nexvault.wallet.core.security.mnemonic.MnemonicManager
import com.nexvault.wallet.core.security.wallet.HDKeyManager
import com.nexvault.wallet.core.security.wallet.WalletStore
import com.nexvault.wallet.domain.model.common.ApiKeyNotConfiguredException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.ExplorerApiException
import com.nexvault.wallet.domain.model.transaction.TransactionStatus
import com.nexvault.wallet.domain.repository.WalletRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Roadmap 2.8 — the two shortcuts the roadmap flagged, closed with tests:
 *
 * 1. `getTransactionHistory` ignored `page`/`pageSize`; it now maps onto the DAO's limit/offset
 *    query, and the explorer refresh forwards the page it was asked for.
 * 2. `updateTransactionStatus` returned the cached row; it now asks the chain for the receipt and
 *    moves a pending row to confirmed or failed.
 */
class TransactionHistoryPagingTest {
    private lateinit var transactionDao: TransactionDao
    private lateinit var tokenDao: TokenDao
    private lateinit var blockExplorerApiFactory: BlockExplorerApiFactory
    private lateinit var chainConfigProvider: ChainConfigProvider
    private lateinit var walletRepository: WalletRepository
    private lateinit var rpcClient: ChainRpcClient
    private lateinit var walletStore: WalletStore
    private lateinit var mnemonicManager: MnemonicManager
    private lateinit var hdKeyManager: HDKeyManager
    private lateinit var explorerApi: BlockExplorerApi
    private lateinit var repository: TransactionRepositoryImpl

    @Before
    fun setup() {
        transactionDao = mockk(relaxed = true)
        tokenDao = mockk(relaxed = true)
        blockExplorerApiFactory = mockk(relaxed = true)
        chainConfigProvider = mockk(relaxed = true)
        walletRepository = mockk(relaxed = true)
        rpcClient = mockk(relaxed = true)
        walletStore = mockk(relaxed = true)
        mnemonicManager = mockk(relaxed = true)
        hdKeyManager = mockk(relaxed = true)
        explorerApi = mockk()

        repository =
            TransactionRepositoryImpl(
                transactionDao,
                tokenDao,
                blockExplorerApiFactory,
                chainConfigProvider,
                walletRepository,
                rpcClient,
                walletStore,
                mnemonicManager,
                hdKeyManager,
            )
        every { walletRepository.getActiveAddress() } returns flowOf(ADDRESS)
    }

    @Test
    fun `getTransactionHistory maps the page onto the dao limit and offset`() =
        runTest {
            val limit = slot<Int>()
            val offset = slot<Int>()
            coEvery {
                transactionDao.getTransactions(
                    chainId = CHAIN_ID,
                    address = ADDRESS,
                    limit = capture(limit),
                    offset = capture(offset),
                )
            } returns listOf(pendingEntity())

            val result = repository.getTransactionHistory(CHAIN_ID, ADDRESS, page = 3, pageSize = 20)

            assertThat(result).isInstanceOf(DataResult.Success::class.java)
            assertThat((result as DataResult.Success).data).hasSize(1)
            assertThat(limit.captured).isEqualTo(20)
            assertThat(offset.captured).isEqualTo(40)
        }

    @Test
    fun `getTransactionHistory clamps a nonsense page to the first page`() =
        runTest {
            val offset = slot<Int>()
            coEvery {
                transactionDao.getTransactions(
                    chainId = CHAIN_ID,
                    address = ADDRESS,
                    limit = any(),
                    offset = capture(offset),
                )
            } returns emptyList()

            repository.getTransactionHistory(CHAIN_ID, ADDRESS, page = 0, pageSize = 20)

            assertThat(offset.captured).isEqualTo(0)
        }

    @Test
    fun `refreshTransactionHistory forwards the requested page to the explorer`() =
        runTest {
            stubConfiguredExplorer()
            val page = slot<Int>()
            val offset = slot<Int>()
            coEvery {
                explorerApi.getTransactions(
                    chainId = CHAIN_ID,
                    address = ADDRESS,
                    apiKey = KEY,
                    page = capture(page),
                    offset = capture(offset),
                )
            } returns EtherscanTransactionListResponse(status = "1", message = "OK", result = emptyList())
            coEvery {
                explorerApi.getTokenTransfers(
                    chainId = CHAIN_ID,
                    address = ADDRESS,
                    apiKey = KEY,
                    page = any(),
                    offset = any(),
                )
            } returns EtherscanTokenTransferListResponse(status = "1", message = "OK", result = emptyList())

            val result = repository.refreshTransactionHistory(CHAIN_ID, ADDRESS, page = 2, pageSize = 25)

            assertThat(result).isInstanceOf(DataResult.Success::class.java)
            assertThat(page.captured).isEqualTo(2)
            assertThat(offset.captured).isEqualTo(25)
        }

    @Test
    fun `a pending row is confirmed when the chain reports a successful receipt`() =
        runTest {
            stubConfiguredExplorer()
            // The repository reads the row back after the UPDATE, so the DAO answers with the
            // settled row the second time — exactly what Room does.
            coEvery { transactionDao.getTransaction(TX_HASH, CHAIN_ID) } returns
                pendingEntity() andThen pendingEntity().copy(status = 1)
            coEvery { explorerApi.getTxReceiptStatus(chainId = CHAIN_ID, txHash = TX_HASH, apiKey = KEY) } returns
                receipt(succeeded = true)

            val result = repository.updateTransactionStatus(TX_HASH, CHAIN_ID)

            val transaction = (result as DataResult.Success).data
            assertThat(transaction.status).isEqualTo(TransactionStatus.CONFIRMED)
            coVerify { transactionDao.updateTransactionStatus(TX_HASH, CHAIN_ID, 1, any()) }
        }

    @Test
    fun `a pending row fails when the chain reports a reverted receipt`() =
        runTest {
            stubConfiguredExplorer()
            coEvery { transactionDao.getTransaction(TX_HASH, CHAIN_ID) } returns
                pendingEntity() andThen pendingEntity().copy(status = 2)
            coEvery { explorerApi.getTxReceiptStatus(chainId = CHAIN_ID, txHash = TX_HASH, apiKey = KEY) } returns
                receipt(succeeded = false)

            val result = repository.updateTransactionStatus(TX_HASH, CHAIN_ID)

            assertThat((result as DataResult.Success).data.status).isEqualTo(TransactionStatus.FAILED)
            coVerify { transactionDao.updateTransactionStatus(TX_HASH, CHAIN_ID, 2, any()) }
        }

    @Test
    fun `a pending row stays pending on the live no-receipt envelope`() =
        runTest {
            stubConfiguredExplorer()
            coEvery { transactionDao.getTransaction(TX_HASH, CHAIN_ID) } returns pendingEntity()
            // Verbatim live probe of an unknown hash (2026-09-30): the call succeeds and the inner
            // status is empty. Treating "" as a revert would persist FAILED for a pending row.
            coEvery { explorerApi.getTxReceiptStatus(chainId = CHAIN_ID, txHash = TX_HASH, apiKey = KEY) } returns
                EtherscanTxReceiptStatusResponse(status = "1", message = "OK", result = ReceiptStatusDto(status = ""))

            val result = repository.updateTransactionStatus(TX_HASH, CHAIN_ID)

            assertThat((result as DataResult.Success).data.status).isEqualTo(TransactionStatus.PENDING)
            coVerify(exactly = 0) { transactionDao.updateTransactionStatus(any(), any(), any(), any()) }
        }

    @Test
    fun `a pending row also stays pending when the envelope carries no payload at all`() =
        runTest {
            stubConfiguredExplorer()
            coEvery { transactionDao.getTransaction(TX_HASH, CHAIN_ID) } returns pendingEntity()
            coEvery { explorerApi.getTxReceiptStatus(chainId = CHAIN_ID, txHash = TX_HASH, apiKey = KEY) } returns
                EtherscanTxReceiptStatusResponse(status = "1", message = "OK", result = null)

            val result = repository.updateTransactionStatus(TX_HASH, CHAIN_ID)

            assertThat((result as DataResult.Success).data.status).isEqualTo(TransactionStatus.PENDING)
            coVerify(exactly = 0) { transactionDao.updateTransactionStatus(any(), any(), any(), any()) }
        }

    @Test
    fun `an absurd page cannot overflow the offset`() =
        runTest {
            val offset = slot<Int>()
            coEvery {
                transactionDao.getTransactions(
                    chainId = CHAIN_ID,
                    address = ADDRESS,
                    limit = any(),
                    offset = capture(offset),
                )
            } returns emptyList()

            repository.getTransactionHistory(CHAIN_ID, ADDRESS, page = Int.MAX_VALUE, pageSize = 20)

            assertThat(offset.captured).isAtLeast(0)
        }

    @Test
    fun `a settled row never asks the chain again`() =
        runTest {
            stubConfiguredExplorer()
            coEvery { transactionDao.getTransaction(TX_HASH, CHAIN_ID) } returns
                pendingEntity().copy(status = 1)

            val result = repository.updateTransactionStatus(TX_HASH, CHAIN_ID)

            assertThat((result as DataResult.Success).data.status).isEqualTo(TransactionStatus.CONFIRMED)
            coVerify(exactly = 0) {
                explorerApi.getTxReceiptStatus(chainId = any(), txHash = any(), apiKey = any())
            }
        }

    @Test
    fun `a rejected receipt envelope surfaces the explorer error`() =
        runTest {
            stubConfiguredExplorer()
            coEvery { transactionDao.getTransaction(TX_HASH, CHAIN_ID) } returns pendingEntity()
            coEvery { explorerApi.getTxReceiptStatus(chainId = CHAIN_ID, txHash = TX_HASH, apiKey = KEY) } returns
                EtherscanTxReceiptStatusResponse(
                    status = "0",
                    message = "NOTOK",
                    resultText = "Invalid API Key",
                )

            val result = repository.updateTransactionStatus(TX_HASH, CHAIN_ID)

            assertThat(result).isInstanceOf(DataResult.Error::class.java)
            assertThat((result as DataResult.Error).exception).isInstanceOf(ExplorerApiException::class.java)
        }

    @Test
    fun `without an explorer key the receipt check fails instead of guessing`() =
        runTest {
            every { chainConfigProvider.isExplorerConfigured(CHAIN_ID) } returns false
            coEvery { transactionDao.getTransaction(TX_HASH, CHAIN_ID) } returns pendingEntity()

            val result = repository.updateTransactionStatus(TX_HASH, CHAIN_ID)

            assertThat(result).isInstanceOf(DataResult.Error::class.java)
            assertThat((result as DataResult.Error).exception)
                .isInstanceOf(ApiKeyNotConfiguredException::class.java)
        }

    private fun stubConfiguredExplorer() {
        every { chainConfigProvider.isExplorerConfigured(CHAIN_ID) } returns true
        every { blockExplorerApiFactory.getApi(CHAIN_ID) } returns explorerApi
        every { blockExplorerApiFactory.getApiKey(CHAIN_ID) } returns KEY
    }

    private fun receipt(succeeded: Boolean) =
        EtherscanTxReceiptStatusResponse(
            status = "1",
            message = "OK",
            result = ReceiptStatusDto(status = if (succeeded) "1" else "0"),
        )

    private fun pendingEntity() =
        TransactionEntity(
            txHash = TX_HASH,
            chainId = CHAIN_ID,
            fromAddress = ADDRESS,
            toAddress = RECIPIENT,
            value = "1000000000000000",
            gasUsed = null,
            gasPrice = "1000000000",
            tokenSymbol = null,
            tokenContractAddress = null,
            tokenDecimals = null,
            blockNumber = 0,
            timestamp = 1_700_000_000L,
            status = 0,
            type = "send",
        )

    private companion object {
        const val ADDRESS = "0x1234567890abcdef1234567890abcdef12345678"
        const val RECIPIENT = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"
        const val TX_HASH = "0xabcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
        const val CHAIN_ID = 1
        const val KEY = "test-key"
    }
}
