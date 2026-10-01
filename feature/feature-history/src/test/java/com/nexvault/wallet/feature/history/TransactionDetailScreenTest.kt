package com.nexvault.wallet.feature.history

import android.app.Application
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import com.nexvault.wallet.domain.model.chain.SupportedChains
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.transaction.Transaction
import com.nexvault.wallet.domain.model.transaction.TransactionStatus
import com.nexvault.wallet.domain.model.transaction.TransactionType
import com.nexvault.wallet.domain.usecase.chain.GetSelectedChainUseCase
import com.nexvault.wallet.domain.usecase.transaction.GetTransactionDetailUseCase
import com.nexvault.wallet.domain.usecase.transaction.UpdateTransactionStatusUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal

/**
 * Roadmap 2.8 / AC-2.8: the detail view shows the full transaction and its "View on Explorer"
 * action resolves to the chain's explorer.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class TransactionDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun detailScreen_showsTheFullTransaction() {
        setContent()

        composeRule.onNodeWithText("Transaction").assertIsDisplayed()
        composeRule.onNodeWithText("Confirmed").assertIsDisplayed()
        composeRule.onNodeWithText("0.001 ETH").assertIsDisplayed()
        composeRule.onNodeWithText(ADDRESS).assertIsDisplayed()
        composeRule.onNodeWithText(RECIPIENT).assertIsDisplayed()
        composeRule.onNodeWithText("View on Explorer").assertIsDisplayed()
    }

    @Test
    fun aPendingRow_offersTheReceiptCheck() {
        setContent(status = TransactionStatus.PENDING)

        composeRule.onNodeWithText("Check status now").assertIsDisplayed()
    }

    @Test
    fun viewOnExplorer_opensTheChainExplorer() {
        setContent()

        composeRule.onNodeWithText("View on Explorer").performClick()
        composeRule.waitForIdle()

        val started = Shadows.shadowOf(context).nextStartedActivity
        assertThat(started.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(started.data.toString()).contains("etherscan.io/tx/$HASH")
    }

    private fun setContent(status: TransactionStatus = TransactionStatus.CONFIRMED) {
        val getDetail = mockk<GetTransactionDetailUseCase>()
        val updateStatus = mockk<UpdateTransactionStatusUseCase>()
        val getSelectedChain = mockk<GetSelectedChainUseCase>()
        coEvery { getDetail(HASH, CHAIN) } returns DataResult.Success(row(status))
        every { getSelectedChain() } returns flowOf(SupportedChains.ETHEREUM_MAINNET)

        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                TransactionDetailScreen(
                    onNavigateBack = { },
                    viewModel =
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
                        ),
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun row(status: TransactionStatus) =
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
            blockNumber = 21_000_000,
            timestamp = 1_700_000_000L,
            status = status,
            type = TransactionType.SEND,
        )

    private companion object {
        const val HASH = "0xabcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
        const val CHAIN = 1
        const val ADDRESS = "0x1234567890abcdef1234567890abcdef12345678"
        const val RECIPIENT = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"
    }
}
