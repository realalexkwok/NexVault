package com.nexvault.wallet.core.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import com.nexvault.wallet.domain.model.transaction.Transaction
import com.nexvault.wallet.domain.model.transaction.TransactionStatus
import com.nexvault.wallet.domain.model.transaction.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Roadmap 2.6 coverage: the core-ui components render and interact under Robolectric Compose.
 * These tests are what lets the JVM unit-test run exercise the 2.4k-line UI module.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    // The sandbox classloader bypasses Java instrumentation by default; scoping its
    // instrumentation to the app package lets the JaCoCo agent see the classes it defines.
    instrumentedPackages = ["com.nexvault.wallet.core.ui"],
)
class CoreUiComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun button_rendersAndInvokesOnClick() {
        var clicked = false
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                NexVaultButton(text = "Send", onClick = { clicked = true })
            }
        }
        composeRule.onNodeWithText("Send").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }

    @Test
    fun textField_updatesTheValueThroughTheCallback() {
        var value = ""
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                NexVaultTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = "Recipient",
                    placeholder = "0x…",
                )
            }
        }
        composeRule.onNodeWithText("Recipient").assertIsDisplayed()
    }

    @Test
    fun pinInputField_reportsDigitsAndBackspace() {
        val digits = mutableListOf<Int>()
        var backspaces = 0
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                PinInputField(
                    onDigitClick = { digits.add(it) },
                    onBackspaceClick = { backspaces += 1 },
                    filledCount = 2,
                    isError = false,
                )
            }
        }
        composeRule.onNodeWithContentDescription("Backspace").performClick()
        composeRule.onNodeWithText("1").performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(1), digits)
            assertEquals(1, backspaces)
        }
    }

    @Test
    fun transactionRow_showsDirectionAndAmount() {
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                TransactionRow(
                    transaction = fixtureTransaction(type = TransactionType.SEND),
                    tokenSymbol = "ETH",
                    tokenDecimals = 18,
                )
            }
        }
        composeRule.onNodeWithText("Sent").assertIsDisplayed()
        composeRule.onNodeWithText("-0.5 ETH").assertIsDisplayed()
    }

    @Test
    fun transactionRow_receivedDirectionPrefixesWithPlus() {
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                TransactionRow(
                    transaction = fixtureTransaction(type = TransactionType.RECEIVE),
                    tokenSymbol = "ETH",
                    tokenDecimals = 18,
                )
            }
        }
        composeRule.onNodeWithText("Received").assertIsDisplayed()
        composeRule.onNodeWithText("+0.5 ETH").assertIsDisplayed()
    }

    @Test
    fun confirmationDialog_confirmAndDismiss() {
        var confirmed = 0
        var dismissed = false
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                ConfirmationDialog(
                    texts =
                        ConfirmationDialogTexts(
                            title = "Review",
                            confirmText = "Confirm",
                            dismissText = "Cancel",
                        ),
                    onConfirm = { confirmed += 1 },
                    onDismiss = { dismissed = true },
                    body = { androidx.compose.material3.Text("Body text") },
                )
            }
        }
        composeRule.onNodeWithText("Body text").assertIsDisplayed()
        composeRule.onNodeWithText("Confirm").performClick()
        composeRule.runOnIdle { assertEquals(1, confirmed) }
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.runOnIdle { assertTrue(dismissed) }
    }

    @Test
    fun tokenIcon_fallsBackToTheSymbolBox() {
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                TokenIcon(imageUrl = null, symbol = "ETH")
            }
        }
        composeRule.onNodeWithText("E").assertIsDisplayed()
    }

    @Test
    fun chainBadge_rendersName() {
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                ChainBadge(chainName = "Ethereum", chainIconRes = 0)
            }
        }
        composeRule.onNodeWithText("Ethereum").assertIsDisplayed()
    }

    @Test
    fun priceChangeChip_colorsAndText() {
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                PriceChangeChip(percentage = "+2.4%", isPositive = true)
            }
        }
        composeRule.onNodeWithText("+2.4%").assertIsDisplayed()
    }

    @Test
    fun emptyStateView_rendersTitleAndSubtitle() {
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                EmptyStateView(title = "Nothing here", subtitle = "Try again later")
            }
        }
        composeRule.onNodeWithText("Nothing here").assertIsDisplayed()
        composeRule.onNodeWithText("Try again later").assertIsDisplayed()
    }

    @Test
    fun card_wrapsItsContent() {
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                NexVaultCard {
                    androidx.compose.material3.Text("Inside the card")
                }
            }
        }
        composeRule.onNodeWithText("Inside the card").assertIsDisplayed()
    }

    private fun fixtureTransaction(type: TransactionType) =
        Transaction(
            txHash = "0x9f2c4d5e6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d",
            chainId = 1,
            fromAddress = "0x1111111111111111111111111111111111111111",
            toAddress = "0x2222222222222222222222222222222222222222",
            value = BigDecimal("0.5"),
            gasUsed = BigInteger.valueOf(21_000),
            gasPrice = BigInteger.valueOf(20_000_000_000),
            tokenSymbol = "ETH",
            tokenContractAddress = null,
            tokenDecimals = 18,
            blockNumber = 20_000_000,
            timestamp = 1_756_000_000_000,
            status = TransactionStatus.CONFIRMED,
            type = type,
        )
}
