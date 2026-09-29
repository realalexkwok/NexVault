package com.nexvault.wallet.feature.send

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import com.nexvault.wallet.domain.model.chain.Chain
import com.nexvault.wallet.domain.model.chain.SupportedChains
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.token.Portfolio
import com.nexvault.wallet.domain.model.token.Token
import com.nexvault.wallet.domain.model.transaction.GasEstimate
import com.nexvault.wallet.domain.model.transaction.GasOption
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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Roadmap 2.6 coverage: the send form renders its asset chips, fields and fee section from a real
 * SendViewModel backed by mocked use cases.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class SendScreenCoverageTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sendScreen_showsTheFormForTheSelectedAsset() {
        val getSelectedChain = mockk<GetSelectedChainUseCase>()
        val getPortfolio = mockk<GetPortfolioUseCase>()
        val getActiveWallet = mockk<GetActiveWalletUseCase>()
        val estimateGas = mockk<EstimateSendGasUseCase>()
        val sendTransaction = mockk<SendTransactionUseCase>(relaxed = true)
        val verifyPin = mockk<VerifyPinUseCase>(relaxed = true)

        every { getSelectedChain() } returns flowOf(CHAIN)
        every { getPortfolio() } returns
            flowOf(
                DataResult.Success(
                    Portfolio(
                        totalFiatValue = 3000.0,
                        change24hPercent = 0.0,
                        tokens = listOf(ETH_TOKEN),
                        chartData = emptyList(),
                    ),
                ),
            )
        every { getActiveWallet() } returns flowOf(WALLET)
        coEvery { estimateGas(any(), any(), any(), any(), any()) } returns DataResult.Success(GAS)

        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                SendScreen(
                    onNavigateBack = { },
                    viewModel =
                        SendViewModel(
                            savedStateHandle = SavedStateHandle(),
                            getSelectedChainUseCase = getSelectedChain,
                            getPortfolioUseCase = getPortfolio,
                            getActiveWalletUseCase = getActiveWallet,
                            estimateSendGasUseCase = estimateGas,
                            sendTransactionUseCase = sendTransaction,
                            verifyPinUseCase = verifyPin,
                        ),
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Send").assertIsDisplayed()
        composeRule.onNodeWithText("ETH").assertIsDisplayed()
        composeRule.onNodeWithText("MAX").assertIsDisplayed()
    }

    private companion object {
        val CHAIN: Chain = SupportedChains.ETHEREUM_MAINNET

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

        val GAS =
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
