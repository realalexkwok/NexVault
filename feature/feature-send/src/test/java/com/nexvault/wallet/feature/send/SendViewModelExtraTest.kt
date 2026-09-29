package com.nexvault.wallet.feature.send

import androidx.lifecycle.SavedStateHandle
import com.nexvault.wallet.domain.model.auth.AuthResult
import com.nexvault.wallet.domain.model.chain.Chain
import com.nexvault.wallet.domain.model.chain.SupportedChains
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.InsufficientBalanceException
import com.nexvault.wallet.domain.model.token.Portfolio
import com.nexvault.wallet.domain.model.token.Token
import com.nexvault.wallet.domain.model.transaction.GasEstimate
import com.nexvault.wallet.domain.model.transaction.GasOption
import com.nexvault.wallet.domain.model.transaction.SendTransactionParams
import com.nexvault.wallet.domain.model.wallet.Account
import com.nexvault.wallet.domain.model.wallet.Wallet
import com.nexvault.wallet.domain.model.wallet.WalletType
import com.nexvault.wallet.domain.usecase.auth.VerifyPinUseCase
import com.nexvault.wallet.domain.usecase.chain.GetSelectedChainUseCase
import com.nexvault.wallet.domain.usecase.token.GetPortfolioUseCase
import com.nexvault.wallet.domain.usecase.transaction.EstimateSendGasUseCase
import com.nexvault.wallet.domain.usecase.transaction.SendTransactionUseCase
import com.nexvault.wallet.domain.usecase.wallet.GetActiveWalletUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Roadmap 2.6 coverage: the remaining send-form paths beyond the registered TCs — token selection,
 * ERC-20 MAX, gas tier switching, PIN editing, dialog dismissal and the submit error mapping.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SendViewModelExtraTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var getSelectedChainUseCase: GetSelectedChainUseCase
    private lateinit var getPortfolioUseCase: GetPortfolioUseCase
    private lateinit var getActiveWalletUseCase: GetActiveWalletUseCase
    private lateinit var estimateSendGasUseCase: EstimateSendGasUseCase
    private lateinit var sendTransactionUseCase: SendTransactionUseCase
    private lateinit var verifyPinUseCase: VerifyPinUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getSelectedChainUseCase = mockk()
        getPortfolioUseCase = mockk()
        getActiveWalletUseCase = mockk()
        estimateSendGasUseCase = mockk()
        sendTransactionUseCase = mockk()
        verifyPinUseCase = mockk()

        every { getSelectedChainUseCase() } returns flowOf(ETH)
        every { getActiveWalletUseCase() } returns flowOf(WALLET)
        every { getPortfolioUseCase() } returns
            flowOf(
                DataResult.Success(
                    Portfolio(
                        totalFiatValue = 0.0,
                        change24hPercent = 0.0,
                        tokens = listOf(ETH_TOKEN, USDC_TOKEN),
                        chartData = emptyList(),
                    ),
                ),
            )
        coEvery { estimateSendGasUseCase(any(), any(), any(), any(), any()) } returns
            DataResult.Success(GAS_ESTIMATE)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        SendViewModel(
            savedStateHandle = savedStateHandle,
            getSelectedChainUseCase = getSelectedChainUseCase,
            getPortfolioUseCase = getPortfolioUseCase,
            getActiveWalletUseCase = getActiveWalletUseCase,
            estimateSendGasUseCase = estimateSendGasUseCase,
            sendTransactionUseCase = sendTransactionUseCase,
            verifyPinUseCase = verifyPinUseCase,
        )

    @Test
    fun tokenPicker_switchesToErc20AndClearsTheAmount() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onAmountChanged("0.5")

        viewModel.onTokenSelected(USDC_TOKEN.contractAddress)

        assertEquals(USDC_TOKEN.contractAddress, viewModel.uiState.value.selectedTokenAddress)
        assertEquals("", viewModel.uiState.value.amount)
    }

    @Test
    fun max_forErc20_fillsTheFullTokenBalance() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onTokenSelected(USDC_TOKEN.contractAddress)

        viewModel.onMaxClicked()

        // The gas fee is paid in the native coin, so MAX for a token is its whole balance.
        assertEquals("2500.5", viewModel.uiState.value.amount)
    }

    @Test
    fun gasTierSelection_movesBetweenSlowNormalFast() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onToAddressChanged(RECIPIENT)
        viewModel.onAmountChanged("0.5")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onGasSelected(2)

        assertEquals(2, viewModel.uiState.value.selectedGasIndex)
        assertEquals(GAS_ESTIMATE.fast, viewModel.uiState.value.selectedGas)
    }

    @Test
    fun pinBackspace_removesTheLastDigit() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onToAddressChanged(RECIPIENT)
        viewModel.onAmountChanged("0.5")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onSubmitClicked()
        viewModel.onPinDigitPressed(1)
        viewModel.onPinDigitPressed(2)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onPinBackspacePressed()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("1", viewModel.uiState.value.pin)
        assertTrue(viewModel.uiState.value.showPinDialog)
    }

    @Test
    fun dialogDismiss_clearsThePin() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onToAddressChanged(RECIPIENT)
        viewModel.onAmountChanged("0.5")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onSubmitClicked()
        viewModel.onPinDigitPressed(1)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onPinDialogDismissed()

        assertFalse(viewModel.uiState.value.showPinDialog)
        assertEquals("", viewModel.uiState.value.pin)
    }

    @Test
    fun submitFailure_insufficientBalance_setsTheAmountError() = testScope.runTest {
        coEvery { verifyPinUseCase.invoke("123456") } returns AuthResult.Success
        coEvery { sendTransactionUseCase.invoke(any(), "w1", 0) } returns
            DataResult.Error(
                InsufficientBalanceException("1", "2"),
                "Insufficient balance: available=1, required=2",
            )

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onToAddressChanged(RECIPIENT)
        viewModel.onAmountChanged("0.5")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onSubmitClicked()
        for (digit in "123456") {
            viewModel.onPinDigitPressed(digit.toString().toInt())
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(R.string.send_error_insufficient_balance, viewModel.uiState.value.amountErrorRes)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.showPinDialog)
    }

    @Test
    fun submitSuccess_forErc20_passesTheTokenAddress() = testScope.runTest {
        coEvery { verifyPinUseCase.invoke("123456") } returns AuthResult.Success
        coEvery { sendTransactionUseCase.invoke(any(), "w1", 0) } returns DataResult.Success("0xhash")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onTokenSelected(USDC_TOKEN.contractAddress)
        viewModel.onToAddressChanged(RECIPIENT)
        viewModel.onAmountChanged("2.5")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onSubmitClicked()
        for (digit in "123456") {
            viewModel.onPinDigitPressed(digit.toString().toInt())
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("0xhash", viewModel.uiState.value.submittedHash)
        coVerify(exactly = 1) {
            sendTransactionUseCase.invoke(
                withArg<SendTransactionParams> { it.tokenAddress == USDC_TOKEN.contractAddress },
                "w1",
                0,
            )
        }
    }

    @Test
    fun resultDone_clearsTheHash() = testScope.runTest {
        coEvery { verifyPinUseCase.invoke("123456") } returns AuthResult.Success
        coEvery { sendTransactionUseCase.invoke(any(), "w1", 0) } returns DataResult.Success("0xhash")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onToAddressChanged(RECIPIENT)
        viewModel.onAmountChanged("0.5")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onSubmitClicked()
        for (digit in "123456") {
            viewModel.onPinDigitPressed(digit.toString().toInt())
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onResultDone()

        assertNull(viewModel.uiState.value.submittedHash)
    }

    private companion object {
        val ETH: Chain = SupportedChains.ETHEREUM_MAINNET
        const val RECIPIENT = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"

        val ETH_TOKEN =
            Token(
                contractAddress = "native",
                chainId = 1,
                symbol = "ETH",
                name = "Ethereum",
                decimals = 18,
                logoUrl = null,
                balance = BigDecimal.ONE,
                fiatPrice = 3000.0,
                fiatValue = 3000.0,
                priceChange24h = 0.0,
            )

        val USDC_TOKEN =
            Token(
                contractAddress = "0xusdc",
                chainId = 1,
                symbol = "USDC",
                name = "USD Coin",
                decimals = 6,
                logoUrl = null,
                balance = BigDecimal("2500.5"),
                fiatPrice = 1.0,
                fiatValue = 2500.5,
                priceChange24h = 0.0,
            )

        val GAS_ESTIMATE =
            GasEstimate(
                slow = GasOption(BigInteger.valueOf(8_000_000_000L), null, null, 600, "~10 min", null),
                normal = GasOption(BigInteger.TEN.pow(10), null, null, 180, "~3 min", null),
                fast = GasOption(BigInteger.valueOf(13_000_000_000L), null, null, 30, "~30 sec", null),
                gasLimit = BigInteger.valueOf(200_000),
            )

        val WALLET =
            Wallet(
                id = "w1",
                name = "Main",
                type = WalletType.HD,
                accounts =
                    listOf(
                        Account(
                            walletId = "w1",
                            index = 0,
                            address = "0x1234567890abcdef1234567890abcdef12345678",
                            name = "Account 1",
                            derivationPath = "m/44'/60'/0'/0/0",
                            isActive = true,
                        ),
                    ),
                createdAt = 0L,
                isActive = true,
            )
    }
}
