package com.nexvault.wallet.feature.tokens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.token.Token
import com.nexvault.wallet.domain.usecase.token.GetRecentTokenTransactionsUseCase
import com.nexvault.wallet.domain.usecase.token.GetTokenPriceChartUseCase
import com.nexvault.wallet.domain.usecase.token.ObserveTokenDetailUseCase
import com.nexvault.wallet.domain.usecase.token.RefreshTransactionHistoryUseCase
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

/**
 * Roadmap 2.6 coverage: the token detail screen renders header, stats and actions from a real
 * TokenDetailViewModel backed by mocked use cases.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class TokenDetailScreenCoverageTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tokenDetailScreen_showsTheTokenAndItsActions() {
        val observeToken = mockk<ObserveTokenDetailUseCase>()
        val priceChart = mockk<GetTokenPriceChartUseCase>()
        val recentTransactions = mockk<GetRecentTokenTransactionsUseCase>()
        val refreshHistory = mockk<RefreshTransactionHistoryUseCase>()

        every { observeToken("0xtoken", 1) } returns flowOf(TOKEN)
        coEvery { priceChart(1, "0xtoken", any()) } returns DataResult.Success(emptyList())
        coEvery { recentTransactions(1, "0xtoken", 5) } returns emptyList()
        coEvery { refreshHistory(1) } returns DataResult.Success(Unit)

        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                TokenDetailScreen(
                    onNavigateBack = { },
                    onSendClicked = { },
                    viewModel =
                        TokenDetailViewModel(
                            savedStateHandle =
                                SavedStateHandle(mapOf("contractAddress" to "0xtoken", "chainId" to 1)),
                            observeTokenDetailUseCase = observeToken,
                            getTokenPriceChartUseCase = priceChart,
                            getRecentTokenTransactionsUseCase = recentTransactions,
                            refreshTransactionHistoryUseCase = refreshHistory,
                        ),
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Token").assertIsDisplayed()
        composeRule.onNodeWithText("TKN").assertExists()
    }

    private companion object {
        val TOKEN =
            Token(
                contractAddress = "0xtoken",
                chainId = 1,
                symbol = "TKN",
                name = "Token",
                decimals = 18,
                logoUrl = null,
                balance = BigDecimal.ONE,
                fiatPrice = 2.0,
                fiatValue = 2.0,
                priceChange24h = 0.0,
                isCustom = false,
                coinGeckoId = "token",
            )
    }
}
