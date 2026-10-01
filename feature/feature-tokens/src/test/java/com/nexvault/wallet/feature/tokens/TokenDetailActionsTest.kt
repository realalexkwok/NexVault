package com.nexvault.wallet.feature.tokens

import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.token.Token
import com.nexvault.wallet.domain.model.transaction.Transaction
import com.nexvault.wallet.domain.model.transaction.TransactionStatus
import com.nexvault.wallet.domain.model.transaction.TransactionType
import com.nexvault.wallet.domain.usecase.token.GetRecentTokenTransactionsUseCase
import com.nexvault.wallet.domain.usecase.token.GetTokenPriceChartUseCase
import com.nexvault.wallet.domain.usecase.token.ObserveTokenDetailUseCase
import com.nexvault.wallet.domain.usecase.token.RefreshTransactionHistoryUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal

/**
 * Roadmap 2.8 (reviewer finding F-2.8-2): the token detail's Receive and See All controls are live —
 * Receive opens the receive screen (2.7) and See All opens the history destination (2.8).
 *
 * The controls sit below the chart in a LazyColumn, so the list is scrolled to the relevant item and
 * the labels are matched in the unmerged tree (the buttons merge their icon and label, and the
 * unmerged label carries the copy under test).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class TokenDetailActionsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var receiveClicked = false
    private var seeAllClicked = false

    @Test
    fun receiveAndSeeAll_bothNavigate() {
        setContent()

        val list = composeRule.onAllNodes(hasScrollToIndexAction()).onFirst()
        list.performScrollToIndex(ACTIONS_INDEX)
        composeRule.onNodeWithText("Receive", useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        assertTrue("Receive did not navigate", receiveClicked)

        list.performScrollToIndex(HISTORY_INDEX)
        composeRule.onNodeWithText("See All", useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        assertTrue("See All did not navigate", seeAllClicked)
    }

    private fun setContent() {
        val observeToken = mockk<ObserveTokenDetailUseCase>()
        val priceChart = mockk<GetTokenPriceChartUseCase>()
        val recentTransactions = mockk<GetRecentTokenTransactionsUseCase>()
        val refreshHistory = mockk<RefreshTransactionHistoryUseCase>()
        every { observeToken(CONTRACT, CHAIN) } returns flowOf(TOKEN)
        coEvery { priceChart(CHAIN, CONTRACT, any()) } returns DataResult.Success(emptyList())
        coEvery { recentTransactions(CHAIN, CONTRACT, 5) } returns listOf(TRANSACTION)
        coEvery { refreshHistory(any(), any(), any()) } returns DataResult.Success(Unit)

        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                TokenDetailScreen(
                    onNavigateBack = { },
                    onSendClicked = { },
                    onReceiveClicked = { receiveClicked = true },
                    onSeeAllClicked = { seeAllClicked = true },
                    viewModel =
                        TokenDetailViewModel(
                            savedStateHandle =
                                SavedStateHandle(mapOf("contractAddress" to CONTRACT, "chainId" to CHAIN)),
                            observeTokenDetailUseCase = observeToken,
                            getTokenPriceChartUseCase = priceChart,
                            getRecentTokenTransactionsUseCase = recentTransactions,
                            refreshTransactionHistoryUseCase = refreshHistory,
                        ),
                )
            }
        }
        // The screen must be past loading before the list has items to scroll through.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("TKN").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val CONTRACT = "0x1111111111111111111111111111111111111111"
        const val CHAIN = 1

        /** List indices of the action row and of the recent-transactions header. */
        const val ACTIONS_INDEX = 3
        const val HISTORY_INDEX = 4

        val TRANSACTION =
            Transaction(
                txHash = "0xabcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789",
                chainId = CHAIN,
                fromAddress = "0x1234567890abcdef1234567890abcdef12345678",
                toAddress = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd",
                value = BigDecimal("0.5"),
                gasUsed = null,
                gasPrice = null,
                tokenSymbol = "TKN",
                tokenContractAddress = CONTRACT,
                tokenDecimals = 18,
                blockNumber = 1,
                timestamp = 1_700_000_000L,
                status = TransactionStatus.CONFIRMED,
                type = TransactionType.SEND,
            )

        val TOKEN =
            Token(
                contractAddress = CONTRACT,
                chainId = CHAIN,
                symbol = "TKN",
                name = "Token",
                decimals = 18,
                logoUrl = null,
                balance = BigDecimal.ONE,
                fiatPrice = 2.0,
                fiatValue = 2.0,
                priceChange24h = 0.0,
            )
    }
}
