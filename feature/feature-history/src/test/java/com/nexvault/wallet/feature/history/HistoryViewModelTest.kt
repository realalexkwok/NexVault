package com.nexvault.wallet.feature.history

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.nexvault.wallet.domain.model.chain.Chain
import com.nexvault.wallet.domain.model.chain.SupportedChains
import com.nexvault.wallet.domain.model.common.ApiKeyNotConfiguredException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.transaction.Transaction
import com.nexvault.wallet.domain.model.transaction.TransactionStatus
import com.nexvault.wallet.domain.model.transaction.TransactionType
import com.nexvault.wallet.domain.model.wallet.Account
import com.nexvault.wallet.domain.model.wallet.Wallet
import com.nexvault.wallet.domain.model.wallet.WalletType
import com.nexvault.wallet.domain.usecase.chain.GetSelectedChainUseCase
import com.nexvault.wallet.domain.usecase.token.RefreshTransactionHistoryUseCase
import com.nexvault.wallet.domain.usecase.transaction.GetTransactionHistoryUseCase
import com.nexvault.wallet.domain.usecase.transaction.UpdateTransactionStatusUseCase
import com.nexvault.wallet.domain.usecase.wallet.GetActiveWalletUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

/**
 * Roadmap 2.8: the history list groups by date, filters by chip, pages through the explorer and
 * surfaces the explorer's standing conditions.
 */
class HistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val getActiveWallet = mockk<GetActiveWalletUseCase>()
    private val getSelectedChain = mockk<GetSelectedChainUseCase>()
    private val getHistory = mockk<GetTransactionHistoryUseCase>()
    private val refreshHistory = mockk<RefreshTransactionHistoryUseCase>()
    private val updateStatus = mockk<UpdateTransactionStatusUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `first load refreshes the explorer and groups the stored rows by date`() =
        runTest(dispatcher) {
            stubWalletAndChain()
            coEvery { refreshHistory(CHAIN, page = 1, pageSize = 20) } returns DataResult.Success(Unit)
            coEvery { getHistory(CHAIN, page = 1, pageSize = 20) } returns
                DataResult.Success(listOf(receivedRow(date(0)), sentRow(date(1))))

            val viewModel = viewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.groups).hasSize(2)
            assertThat(state.groups[0].label).isEqualTo(TODAY_LABEL)
            assertThat(state.groups[1].label).isEqualTo(YESTERDAY_LABEL)
            assertThat(state.isLoading).isFalse()
        }

    @Test
    fun `a short first page means there is nothing more to load`() =
        runTest(dispatcher) {
            stubWalletAndChain()
            coEvery { refreshHistory(CHAIN, page = 1, pageSize = 20) } returns DataResult.Success(Unit)
            coEvery { getHistory(CHAIN, page = 1, pageSize = 20) } returns DataResult.Success(listOf(sentRow(date(0))))

            val viewModel = viewModel()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.hasMore).isFalse()
        }

    @Test
    fun `loadMore fetches the next explorer page and appends it`() =
        runTest(dispatcher) {
            stubWalletAndChain()
            coEvery { refreshHistory(CHAIN, page = any(), pageSize = any()) } returns DataResult.Success(Unit)
            coEvery { getHistory(CHAIN, page = 1, pageSize = 20) } returns DataResult.Success(fullPage())
            coEvery { getHistory(CHAIN, page = 2, pageSize = 20) } returns DataResult.Success(listOf(sentRow(date(2))))

            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.loadMore()
            advanceUntilIdle()

            coVerify { refreshHistory(CHAIN, page = 2, pageSize = 20) }
            assertThat(viewModel.uiState.value.groups.flatMap { it.transactions }).hasSize(21)
        }

    @Test
    fun `chip filters narrow the visible groups`() =
        runTest(dispatcher) {
            stubWalletAndChain()
            coEvery { refreshHistory(CHAIN, page = any(), pageSize = any()) } returns DataResult.Success(Unit)
            coEvery { getHistory(CHAIN, page = 1, pageSize = 20) } returns
                DataResult.Success(
                    listOf(
                        sentRow(date(0)),
                        receivedRow(date(0)),
                        failedRow(date(1)),
                    ),
                )

            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onFilterSelected(HistoryFilter.FAILED)
            assertThat(viewModel.uiState.value.groups.single().transactions.single().status)
                .isEqualTo(TransactionStatus.FAILED)

            viewModel.onFilterSelected(HistoryFilter.RECEIVED)
            assertThat(viewModel.uiState.value.groups.single().transactions.single().type)
                .isEqualTo(TransactionType.RECEIVE)

            viewModel.onFilterSelected(HistoryFilter.ALL)
            assertThat(viewModel.uiState.value.groups.flatMap { it.transactions }).hasSize(3)
        }

    @Test
    fun `a pending row is re-checked against the chain after the refresh`() =
        runTest(dispatcher) {
            stubWalletAndChain()
            val pending = pendingRow(date(0))
            coEvery { refreshHistory(CHAIN, page = any(), pageSize = any()) } returns DataResult.Success(Unit)
            coEvery { getHistory(CHAIN, page = 1, pageSize = 20) } returns DataResult.Success(listOf(pending))
            coEvery { updateStatus(PENDING_HASH, CHAIN) } returns
                DataResult.Success(pending.copy(status = TransactionStatus.CONFIRMED))

            val viewModel = viewModel()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.groups.single().transactions.single().status)
                .isEqualTo(TransactionStatus.CONFIRMED)
        }

    @Test
    fun `a missing explorer key becomes a standing notice instead of an error`() =
        runTest(dispatcher) {
            stubWalletAndChain()
            coEvery { refreshHistory(CHAIN, page = any(), pageSize = any()) } returns
                DataResult.Error(ApiKeyNotConfiguredException("Block explorer API is not configured"))
            coEvery { getHistory(CHAIN, page = 1, pageSize = 20) } returns DataResult.Success(emptyList())

            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.isExplorerConfigured).isFalse()
                assertThat(state.isEmpty).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `grouping labels today and yesterday and formats older dates`() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 9, 30)
        val rows =
            listOf(
                sentRow(today.atStartOfDay(zone).toEpochSecond() + 60),
                sentRow(today.minusDays(1).atStartOfDay(zone).toEpochSecond() + 60),
                sentRow(today.minusDays(5).atStartOfDay(zone).toEpochSecond() + 60),
            )

        val groups = groupByLocalDate(rows, zoneId = zone, today = today)

        assertThat(groups.map { it.label })
            .containsExactly(TODAY_LABEL, YESTERDAY_LABEL, "Sep 25, 2026")
            .inOrder()
    }

    private fun viewModel() =
        HistoryViewModel(getActiveWallet, getSelectedChain, getHistory, refreshHistory, updateStatus)

    private fun stubWalletAndChain() {
        every { getActiveWallet() } returns flowOf(wallet())
        every { getSelectedChain() } returns flowOf(CHAIN_OBJECT)
    }

    private fun date(daysAgo: Long): Long =
        LocalDate.now().minusDays(daysAgo).atStartOfDay(ZoneId.systemDefault()).toEpochSecond() + 3600

    private fun fullPage(): List<Transaction> = (1..20).map { sentRow(date(it.toLong())) }

    private fun sentRow(timestamp: Long) =
        row(timestamp, TransactionType.SEND, TransactionStatus.CONFIRMED, "0xsend$timestamp")

    private fun receivedRow(timestamp: Long) =
        row(timestamp, TransactionType.RECEIVE, TransactionStatus.CONFIRMED, "0xrecv$timestamp")

    private fun failedRow(timestamp: Long) =
        row(timestamp, TransactionType.SEND, TransactionStatus.FAILED, "0xfail$timestamp")

    private fun pendingRow(timestamp: Long) =
        row(timestamp, TransactionType.SEND, TransactionStatus.PENDING, PENDING_HASH)

    private fun row(
        timestamp: Long,
        type: TransactionType,
        status: TransactionStatus,
        hash: String,
    ) = Transaction(
        txHash = hash,
        chainId = CHAIN,
        fromAddress = ADDRESS,
        toAddress = RECIPIENT,
        value = BigDecimal("0.001"),
        gasUsed = null,
        gasPrice = null,
        tokenSymbol = null,
        tokenContractAddress = null,
        tokenDecimals = null,
        blockNumber = 1,
        timestamp = timestamp,
        status = status,
        type = type,
    )

    private fun wallet() =
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

    private companion object {
        const val CHAIN = 1
        const val ADDRESS = "0x1234567890abcdef1234567890abcdef12345678"
        const val RECIPIENT = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"
        const val PENDING_HASH = "0xpending"
        val CHAIN_OBJECT: Chain = SupportedChains.ETHEREUM_MAINNET
    }
}
