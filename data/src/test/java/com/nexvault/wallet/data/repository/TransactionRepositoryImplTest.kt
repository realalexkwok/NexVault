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
import com.nexvault.wallet.core.network.rpc.ChainRpcClient
import com.nexvault.wallet.core.security.mnemonic.MnemonicManager
import com.nexvault.wallet.core.security.wallet.HDKeyManager
import com.nexvault.wallet.core.security.wallet.WalletStore
import com.nexvault.wallet.domain.model.common.ApiKeyNotConfiguredException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.ExplorerApiException
import com.nexvault.wallet.domain.model.common.ExplorerPlanUnsupportedException
import com.nexvault.wallet.domain.model.common.InsufficientBalanceException
import com.nexvault.wallet.domain.model.transaction.GasOption
import com.nexvault.wallet.domain.model.transaction.SendTransactionParams
import com.nexvault.wallet.domain.model.wallet.Account
import com.nexvault.wallet.domain.model.wallet.Wallet
import com.nexvault.wallet.domain.model.wallet.WalletType
import com.nexvault.wallet.domain.repository.WalletRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.web3j.crypto.Keys
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Roadmap 2.0.4: with the explorer key absent, the history refresh must fail with an explicit
 * [ApiKeyNotConfiguredException] and must never touch the network.
 *
 * Roadmap 2.0.4b: with a key, the refresh calls Etherscan **V2** with the chain id and turns the
 * explorer's rejection envelopes into a visible error — the plan gate as its own exception, the
 * legitimate "No transactions found" still as a successful empty result.
 *
 * Roadmap 2.6: the send paths (TC-REPO-003/004) and `estimateGas` are covered through the mockable
 * [ChainRpcClient] seam — the reason web3j's final classes no longer block them.
 */
class TransactionRepositoryImplTest {
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

    // TC-REPO-003: a native send signs, submits, and persists the pending row.
    @Test
    fun sendNativeTransaction_signsSubmitsAndPersistsThePendingRow() = runTest {
        stubSendableWallet()
        every { chainConfigProvider.isRpcConfigured(1) } returns true
        every { hdKeyManager.deriveEthereumKeyPair(any(), any(), any()) } returns Keys.createEcKeyPair()
        coEvery { rpcClient.getBalance(ADDRESS, 1) } returns BigInteger("1000000000000000000")
        coEvery { rpcClient.estimateGas(any(), any(), any(), any(), any()) } returns BigInteger("21000")
        coEvery { rpcClient.getTransactionCount(ADDRESS, 1) } returns BigInteger.valueOf(5)
        coEvery { rpcClient.sendRawTransaction(any(), any()) } returns "0xtxhash"

        val result =
            repository.sendNativeTransaction(
                params =
                    SendTransactionParams(
                        toAddress = RECIPIENT,
                        amount = BigDecimal("0.5"),
                        tokenAddress = null,
                        gasOption = GasOption(BigInteger.valueOf(100), null, null, 180, "~3 min", null),
                        chainId = 1,
                        data = null,
                    ),
                walletId = "w1",
                accountIndex = 0,
            )

        assertThat(result).isInstanceOf(DataResult.Success::class.java)
        assertThat((result as DataResult.Success).data).isEqualTo("0xtxhash")
        val signedSlot = slot<String>()
        coVerify(exactly = 1) { rpcClient.sendRawTransaction(capture(signedSlot), 1) }
        assertThat(signedSlot.captured).startsWith("0x")
        assertThat(signedSlot.captured.length).isGreaterThan(2)
        val entitySlot = slot<List<TransactionEntity>>()
        coVerify(exactly = 1) { transactionDao.upsertTransactions(capture(entitySlot)) }
        val persisted = entitySlot.captured.single()
        assertThat(persisted.txHash).isEqualTo("0xtxhash")
        assertThat(persisted.status).isEqualTo(0)
        assertThat(persisted.type).isEqualTo("send")
        assertThat(persisted.tokenSymbol).isNull()
    }

    // TC-REPO-004: the repository validates the balance before submitting anything.
    @Test
    fun sendNativeTransaction_insufficientBalance_failsWithoutSubmitting() = runTest {
        stubSendableWallet()
        every { chainConfigProvider.isRpcConfigured(1) } returns true
        coEvery { rpcClient.getBalance(ADDRESS, 1) } returns BigInteger("100000000000000000")
        coEvery { rpcClient.estimateGas(any(), any(), any(), any(), any()) } returns BigInteger("21000")

        val result =
            repository.sendNativeTransaction(
                params =
                    SendTransactionParams(
                        toAddress = RECIPIENT,
                        amount = BigDecimal("1.0"),
                        tokenAddress = null,
                        gasOption = GasOption(BigInteger.valueOf(100), null, null, 180, "~3 min", null),
                        chainId = 1,
                        data = null,
                    ),
                walletId = "w1",
                accountIndex = 0,
            )

        assertThat(result).isInstanceOf(DataResult.Error::class.java)
        assertThat((result as DataResult.Error).exception).isInstanceOf(InsufficientBalanceException::class.java)
        coVerify(exactly = 0) { rpcClient.sendRawTransaction(any(), any()) }
        coVerify(exactly = 0) { transactionDao.upsertTransactions(any()) }
    }

    @Test
    fun sendTokenTransaction_encodesTheTransferAndPersistsTheTokenRow() = runTest {
        stubSendableWallet()
        every { chainConfigProvider.isRpcConfigured(1) } returns true
        every { hdKeyManager.deriveEthereumKeyPair(any(), any(), any()) } returns Keys.createEcKeyPair()
        coEvery { tokenDao.getToken(TOKEN_ADDRESS, 1) } returns
            TransactionTestFixtures.tokenEntity(contract = TOKEN_ADDRESS, decimals = 6, symbol = "USDC")
        coEvery { rpcClient.getTokenBalance(TOKEN_ADDRESS, ADDRESS, 1) } returns BigInteger("5000000")
        coEvery { rpcClient.getBalance(ADDRESS, 1) } returns BigInteger("1000000000000000000")
        coEvery { rpcClient.estimateGas(any(), any(), any(), any(), any()) } returns BigInteger("60000")
        coEvery { rpcClient.getTransactionCount(ADDRESS, 1) } returns BigInteger.valueOf(5)
        coEvery { rpcClient.sendRawTransaction(any(), any()) } returns "0xtokenhash"

        val result =
            repository.sendTokenTransaction(
                params =
                    SendTransactionParams(
                        toAddress = RECIPIENT,
                        amount = BigDecimal("2.5"),
                        tokenAddress = TOKEN_ADDRESS,
                        gasOption = GasOption(BigInteger.valueOf(100), null, null, 180, "~3 min", null),
                        chainId = 1,
                        data = null,
                    ),
                walletId = "w1",
                accountIndex = 0,
            )

        assertThat(result).isInstanceOf(DataResult.Success::class.java)
        val entitySlot = slot<List<TransactionEntity>>()
        coVerify(exactly = 1) { transactionDao.upsertTransactions(capture(entitySlot)) }
        val persisted = entitySlot.captured.single()
        assertThat(persisted.txHash).isEqualTo("0xtokenhash")
        assertThat(persisted.tokenSymbol).isEqualTo("USDC")
        assertThat(persisted.tokenContractAddress).isEqualTo(TOKEN_ADDRESS)
    }

    @Test
    fun estimateSendGas_returnsSlowNormalAndFastTiersWithFiatCost() = runTest {
        every { chainConfigProvider.isRpcConfigured(1) } returns true
        coEvery { rpcClient.getGasPrice(1) } returns BigInteger.valueOf(100)
        coEvery { rpcClient.estimateGas(any(), any(), any(), any(), any()) } returns BigInteger.valueOf(21000)
        coEvery { tokenDao.getToken("native", 1) } returns
            TransactionTestFixtures.tokenEntity(contract = "native", decimals = 18, symbol = "ETH", fiatPrice = 3000.0)

        val result = repository.estimateSendGas(ADDRESS, RECIPIENT, BigDecimal.ONE, null, 1)

        assertThat(result).isInstanceOf(DataResult.Success::class.java)
        val estimate = (result as DataResult.Success).data
        assertThat(estimate.slow.gasPrice).isEqualTo(BigInteger.valueOf(80))
        assertThat(estimate.normal.gasPrice).isEqualTo(BigInteger.valueOf(100))
        assertThat(estimate.fast.gasPrice).isEqualTo(BigInteger.valueOf(130))
        assertThat(estimate.normal.fiatCost).isNotNull()
        assertThat(estimate.fast.estimatedTimeName).isEqualTo("~30 sec")
    }

    @Test
    fun estimateSendGas_forAToken_encodesTheTransferCalldata() = runTest {
        every { chainConfigProvider.isRpcConfigured(1) } returns true
        coEvery { rpcClient.getGasPrice(1) } returns BigInteger.valueOf(100)
        coEvery { rpcClient.estimateGas(any(), any(), any(), any(), any()) } returns BigInteger.valueOf(60000)
        coEvery { tokenDao.getToken(TOKEN_ADDRESS, 1) } returns
            TransactionTestFixtures.tokenEntity(contract = TOKEN_ADDRESS, decimals = 6, symbol = "USDC")
        coEvery { tokenDao.getToken("native", 1) } returns null

        val result = repository.estimateSendGas(ADDRESS, RECIPIENT, BigDecimal("2.5"), TOKEN_ADDRESS, 1)

        assertThat(result).isInstanceOf(DataResult.Success::class.java)
        val dataSlot = slot<String>()
        coVerify { rpcClient.estimateGas(ADDRESS, TOKEN_ADDRESS, BigInteger.ZERO, capture(dataSlot), 1) }
        assertThat(dataSlot.captured).startsWith("0x")
        assertThat(dataSlot.captured.length).isGreaterThan(2)
    }

    @Test
    fun estimateSendGas_withoutRpcKey_failsWithoutCallingTheRpc() = runTest {
        every { chainConfigProvider.isRpcConfigured(1) } returns false

        val result = repository.estimateSendGas(ADDRESS, RECIPIENT, BigDecimal.ONE, null, 1)

        assertThat(result).isInstanceOf(DataResult.Error::class.java)
        assertThat((result as DataResult.Error).exception).isInstanceOf(ApiKeyNotConfiguredException::class.java)
        coVerify(exactly = 0) { rpcClient.getGasPrice(any()) }
        coVerify(exactly = 0) { rpcClient.estimateGas(any(), any(), any(), any(), any()) }
    }

    private fun stubSendableWallet() {
        val wallet =
            Wallet(
                id = "w1",
                name = "Main",
                type = WalletType.HD,
                accounts =
                    listOf(
                        Account(
                            walletId = "w1",
                            index = 0,
                            address = ADDRESS,
                            name = "Account 1",
                            derivationPath = "m/44'/60'/0'/0/0",
                            isActive = true,
                        ),
                    ),
                createdAt = 0L,
                isActive = true,
            )
        every { walletRepository.getWallets() } returns flowOf(listOf(wallet))
        coEvery { walletRepository.getMnemonicForBackup("w1") } returns
            DataResult.Success(MNEMONIC_WORDS)
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
        const val ADDRESS = "0x1234567890abcdef1234567890abcdef12345678"
        const val RECIPIENT = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"
        const val TOKEN_ADDRESS = "0x1111111111111111111111111111111111111111"
        const val KEY = "test-key"
        const val OFFSET = 100

        /** The canonical public BIP-39 test vector (never a wallet's real phrase). */
        val MNEMONIC_WORDS =
            listOf(
                "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
                "abandon", "abandon", "abandon", "abandon", "abandon", "about",
            )

        /** The verbatim plan-gate body observed on chainid=56, 2026-09-26. */
        const val PLAN_GATE_TEXT =
            "Free API access is not supported for this chain. Please upgrade " +
                "your api plan for full chain coverage. https://etherscan.io/apis"
    }
}
