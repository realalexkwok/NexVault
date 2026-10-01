package com.nexvault.wallet.feature.history

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.nexvault.wallet.domain.model.chain.SupportedChains
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.transaction.Transaction
import com.nexvault.wallet.domain.model.transaction.TransactionStatus
import com.nexvault.wallet.domain.model.transaction.TransactionType
import com.nexvault.wallet.domain.usecase.chain.GetSelectedChainUseCase
import com.nexvault.wallet.domain.usecase.transaction.GetTransactionDetailUseCase
import com.nexvault.wallet.domain.usecase.transaction.UpdateTransactionStatusUseCase
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

/** Roadmap 2.8: the transaction detail screen loads the row and can re-check a pending receipt. */
class TransactionDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val getDetail = mockk<GetTransactionDetailUseCase>()
    private val updateStatus = mockk<UpdateTransactionStatusUseCase>()
    private val getSelectedChain = mockk<GetSelectedChainUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { getSelectedChain() } returns flowOf(SupportedChains.ETHEREUM_MAINNET)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads the stored transaction with its chain`() =
        runTest(dispatcher) {
            coEvery { getDetail(HASH, CHAIN) } returns DataResult.Success(pendingRow())

            val viewModel = viewModel()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.transaction?.txHash).isEqualTo(HASH)
            assertThat(viewModel.uiState.value.chain?.explorerUrl).contains("etherscan.io")
            assertThat(viewModel.uiState.value.isLoading).isFalse()
        }

    @Test
    fun `the receipt check moves a pending row to confirmed`() =
        runTest(dispatcher) {
            coEvery { getDetail(HASH, CHAIN) } returns DataResult.Success(pendingRow())
            coEvery { updateStatus(HASH, CHAIN) } returns
                DataResult.Success(pendingRow().copy(status = TransactionStatus.CONFIRMED))

            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onCheckReceipt()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.transaction?.status).isEqualTo(TransactionStatus.CONFIRMED)
            assertThat(viewModel.uiState.value.canCheckReceipt).isFalse()
        }

    @Test
    fun `a settled row offers no receipt check`() =
        runTest(dispatcher) {
            coEvery { getDetail(HASH, CHAIN) } returns
                DataResult.Success(pendingRow().copy(status = TransactionStatus.CONFIRMED))

            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onCheckReceipt()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.canCheckReceipt).isFalse()
            coVerify(exactly = 0) { updateStatus(any(), any()) }
        }

    @Test
    fun `a failing load becomes a visible error`() =
        runTest(dispatcher) {
            coEvery { getDetail(HASH, CHAIN) } returns
                DataResult.Error(IllegalArgumentException("Transaction not found"), "Transaction not found")

            val viewModel = viewModel()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.transaction).isNull()
            assertThat(viewModel.uiState.value.errorMessage).isEqualTo("Transaction not found")
        }

    private fun viewModel() =
        TransactionDetailViewModel(
            savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        TransactionDetailViewModel.ARG_TX_HASH to HASH,
                        TransactionDetailViewModel.ARG_CHAIN_ID to CHAIN,
                    ),
                ),
            getTransactionDetailUseCase = getDetail,
            updateTransactionStatusUseCase = updateStatus,
            getSelectedChainUseCase = getSelectedChain,
        )

    private fun pendingRow() =
        Transaction(
            txHash = HASH,
            chainId = CHAIN,
            fromAddress = ADDRESS,
            toAddress = RECIPIENT,
            value = BigDecimal("0.001"),
            gasUsed = null,
            gasPrice = null,
            tokenSymbol = "ETH",
            tokenContractAddress = null,
            tokenDecimals = 18,
            blockNumber = 0,
            timestamp = 1_700_000_000L,
            status = TransactionStatus.PENDING,
            type = TransactionType.SEND,
        )

    private companion object {
        const val HASH = "0xabcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
        const val CHAIN = 1
        const val ADDRESS = "0x1234567890abcdef1234567890abcdef12345678"
        const val RECIPIENT = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"
    }
}
