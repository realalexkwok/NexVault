package com.nexvault.wallet.feature.send

import androidx.lifecycle.SavedStateHandle
import com.nexvault.wallet.domain.model.auth.AuthResult
import com.nexvault.wallet.domain.model.chain.Chain
import com.nexvault.wallet.domain.model.chain.SupportedChains
import com.nexvault.wallet.domain.model.common.ApiKeyNotConfiguredException
import com.nexvault.wallet.domain.model.common.DataResult
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Roadmap 2.6: the send form's registered TC ids get their home here — TC-VM-003 (invalid address),
 * TC-VM-004 (amount exceeds balance), TC-VM-005 (MAX = balance − estimated gas) — plus the key-absent
 * state and the PIN-confirmed submit path.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SendViewModelTest {
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
                        tokens = listOf(ONE_ETH),
                        chartData = emptyList(),
                    ),
                ),
            )
        coEvery { estimateSendGasUseCase(any(), any(), any(), any(), any()) } returns
            DataResult.Success(
                GasEstimate(
                    slow = GasOption(BigInteger.valueOf(1_000_000_000L), null, null, 600, "~10 min", null),
                    normal = GasOption(BigInteger.TEN.pow(10), null, null, 180, "~3 min", null),
                    fast = GasOption(BigInteger.valueOf(13_000_000_000L), null, null, 30, "~30 sec", null),
                    gasLimit = BigInteger.valueOf(200_000),
                ),
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        SendViewModel(
            savedStateHandle = SavedStateHandle(),
            getSelectedChainUseCase = getSelectedChainUseCase,
            getPortfolioUseCase = getPortfolioUseCase,
            getActiveWalletUseCase = getActiveWalletUseCase,
            estimateSendGasUseCase = estimateSendGasUseCase,
            sendTransactionUseCase = sendTransactionUseCase,
            verifyPinUseCase = verifyPinUseCase,
        )

    // TC-VM-003: an invalid recipient shows the address error.
    @Test
    fun invalidRecipient_showsTheAddressError() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onToAddressChanged("not_an_address")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(R.string.send_error_invalid_address, viewModel.uiState.value.addressErrorRes)
    }

    // TC-VM-004: an amount above the balance shows the exceeds-balance error.
    @Test
    fun amountAboveBalance_showsTheExceedsBalanceError() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onToAddressChanged(RECIPIENT)
        viewModel.onAmountChanged("2.0")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(R.string.send_error_amount_exceeds_balance, viewModel.uiState.value.amountErrorRes)
    }

    // TC-VM-005: MAX fills the balance minus the estimated gas fee (0.998 for 1.0 with 0.002 gas).
    @Test
    fun max_fillsBalanceMinusTheEstimatedGasFee() = testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onToAddressChanged(RECIPIENT)
        viewModel.onAmountChanged("0.5")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onMaxClicked()

        // fee = gasLimit(200000) x normal.gasPrice(10 gwei) = 2e15 wei = 0.002 ETH
        assertEquals("0.998", viewModel.uiState.value.amount)
    }

    @Test
    fun missingRpcKey_setsTheNotConfiguredState() = testScope.runTest {
        coEvery { estimateSendGasUseCase(any(), any(), any(), any(), any()) } returns
            DataResult.Error(ApiKeyNotConfiguredException("no key"))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onToAddressChanged(RECIPIENT)
        viewModel.onAmountChanged("0.5")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isRpcNotConfigured)
        assertNull(viewModel.uiState.value.gasEstimate)
    }

    @Test
    fun confirmedPin_submitsAndRecordsTheHash() = testScope.runTest {
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

        assertEquals("0xhash", viewModel.uiState.value.submittedHash)
        coVerify(exactly = 1) {
            sendTransactionUseCase.invoke(
                withArg<SendTransactionParams> {
                    it.toAddress == RECIPIENT &&
                        it.amount == BigDecimal("0.5") &&
                        it.tokenAddress == null &&
                        it.chainId == 1
                },
                "w1",
                0,
            )
        }
    }

    @Test
    fun wrongPin_keepsTheDialogOpenWithThePinError() = testScope.runTest {
        coEvery { verifyPinUseCase.invoke("123456") } returns
            AuthResult.Failed(remainingAttempts = 4, message = "Incorrect PIN")

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

        assertTrue(viewModel.uiState.value.showPinDialog)
        assertEquals(R.string.send_pin_error, viewModel.uiState.value.pinErrorRes)
        assertNotNull(viewModel.uiState.value.pinErrorRes)
        coVerify(exactly = 0) { sendTransactionUseCase.invoke(any(), any(), any()) }
    }

    private companion object {
        val ETH: Chain = SupportedChains.ETHEREUM_MAINNET
        const val RECIPIENT = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"

        val ONE_ETH =
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
