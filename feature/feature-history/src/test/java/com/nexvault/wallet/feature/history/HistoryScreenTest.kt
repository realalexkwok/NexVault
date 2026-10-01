package com.nexvault.wallet.feature.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import com.nexvault.wallet.domain.model.chain.SupportedChains
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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

/**
 * Roadmap 2.8 / AC-2.8: the history tab renders date sections, filter chips, the empty state and
 * routes a row tap to the detail destination.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class HistoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var openedTxHash: String? = null

    @Test
    fun historyScreen_groupsRowsUnderDateHeaders() {
        setContent(rows = listOf(sentRow(today()), receivedRow(yesterday())))

        composeRule.onNodeWithText("History").assertIsDisplayed()
        composeRule.onNodeWithText(TODAY_LABEL).assertIsDisplayed()
        composeRule.onNodeWithText(YESTERDAY_LABEL).assertIsDisplayed()
        composeRule.onNodeWithText("All").assertIsDisplayed()
        composeRule.onNodeWithText("Failed").assertExists()
    }

    @Test
    fun historyScreen_emptyHistoryExplainsItself() {
        setContent(rows = emptyList())

        composeRule.onNodeWithText("No transactions yet").assertIsDisplayed()
    }

    @Test
    fun tappingARow_opensItsDetail() {
        setContent(rows = listOf(sentRow(today())))

        composeRule.onNodeWithContentDescription("SEND").performClick()
        composeRule.waitForIdle()

        assertThat(openedTxHash).isNotNull()
    }

    @Test
    fun aFilterChipWithNoMatches_emptiesTheList() {
        setContent(rows = listOf(sentRow(today())))

        composeRule.onNodeWithText("Received").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("No transactions yet").assertIsDisplayed()
    }

    private fun setContent(rows: List<Transaction>) {
        val getActiveWallet = mockk<GetActiveWalletUseCase>()
        val getSelectedChain = mockk<GetSelectedChainUseCase>()
        val getHistory = mockk<GetTransactionHistoryUseCase>()
        val refreshHistory = mockk<RefreshTransactionHistoryUseCase>()
        val updateStatus = mockk<UpdateTransactionStatusUseCase>()
        every { getActiveWallet() } returns flowOf(wallet())
        every { getSelectedChain() } returns flowOf(SupportedChains.ETHEREUM_MAINNET)
        coEvery { refreshHistory(any(), any(), any()) } returns DataResult.Success(Unit)
        coEvery { getHistory(any(), any(), any()) } returns DataResult.Success(rows)

        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                HistoryScreen(
                    onNavigateToDetail = { txHash, _ -> openedTxHash = txHash },
                    viewModel =
                        HistoryViewModel(
                            getActiveWallet,
                            getSelectedChain,
                            getHistory,
                            refreshHistory,
                            updateStatus,
                        ),
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun today(): Long = atHourOf(LocalDate.now(), 10)

    private fun yesterday(): Long = atHourOf(LocalDate.now().minusDays(1), 10)

    private fun atHourOf(date: LocalDate, hour: Long): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() + hour * 3600

    private fun sentRow(timestamp: Long) = row(timestamp, TransactionType.SEND, TransactionStatus.CONFIRMED)

    private fun failedRow(timestamp: Long) = row(timestamp, TransactionType.SEND, TransactionStatus.FAILED)

    private fun receivedRow(timestamp: Long) = row(timestamp, TransactionType.RECEIVE, TransactionStatus.CONFIRMED)

    private fun row(
        timestamp: Long,
        type: TransactionType,
        status: TransactionStatus,
    ) = Transaction(
        txHash = "$HASH-${type.name}-$timestamp",
        chainId = 1,
        fromAddress = ADDRESS,
        toAddress = RECIPIENT,
        value = BigDecimal("0.001"),
        gasUsed = null,
        gasPrice = null,
        tokenSymbol = "ETH",
        tokenContractAddress = null,
        tokenDecimals = 18,
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
        const val HASH = "0xabcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
        const val ADDRESS = "0x1234567890abcdef1234567890abcdef12345678"
        const val RECIPIENT = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"
    }
}
