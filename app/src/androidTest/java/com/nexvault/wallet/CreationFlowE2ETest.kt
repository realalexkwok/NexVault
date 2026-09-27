package com.nexvault.wallet

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Roadmap 2.0.6: TC-INT-001 — the full wallet creation flow, end to end:
 * create wallet → verify mnemonic → set PIN → reach Home.
 *
 * This automates exactly the flow the owner walks manually for every item's manual half.
 *
 * **Preconditions (the run recipe, see 2.0.6 `validation.md` §A5):** the app must be in a
 * fresh-install state — run `adb shell pm clear com.nexvault.wallet.debug` (the debug variant's
 * package id) before `:app:connectedDebugAndroidTest`. Consequence, deliberately: **any wallet
 * on the device is wiped**, which is why this is a documented step, not something the test
 * sneaks in.
 *
 * Determinism choices: the mnemonic is read off the create screen and tapped back on the
 * verify screen (no fixture key material); the PIN is the walk's standard `123456`;
 * the test asserts **rendering**, never live prices, so no network key is required; the
 * only assertion at the end is that Home renders, which the walk's screenshot already
 * corroborates for this build lineage.
 */
@RunWith(AndroidJUnit4::class)
class CreationFlowE2ETest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    // TC-INT-001: full wallet creation flow — create → verify mnemonic → set PIN → Home.
    @Test
    fun createVerifyPin_landsOnHome() {
        // 1. Fresh install → the Welcome screen greets us.
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Create New Wallet").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Create New Wallet").assertIsDisplayed().performClick()

        // 2. Read the 12 mnemonic words off the create screen, in grid order. The word cells
        // are the only lowercase single-token texts on the screen (the step indicator,
        // title, warning and buttons all contain capitals, spaces or digits).
        val wordMatcher =
            SemanticsMatcher("single lowercase word") { node ->
                node.config.contains(SemanticsProperties.Text) &&
                    node.config[SemanticsProperties.Text].any { it.text.matches(Regex("^[a-z]+$")) }
            }
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(wordMatcher).fetchSemanticsNodes().size == 12
        }
        val words =
            composeRule.onAllNodes(wordMatcher).fetchSemanticsNodes().map { node ->
                node.config[SemanticsProperties.Text].first().text
            }
        check(words.size == 12) { "expected 12 mnemonic words, got ${words.size}: $words" }

        // 3. Acknowledge (the only toggleable on the screen) and continue.
        composeRule.onNode(isToggleable()).performClick()
        composeRule.onNodeWithText("Continue").assertIsDisplayed().performClick()

        // 4. Verify: tap the words back in the captured order. Selected chips render as
        // "N. word", so an exact-text match always hits an available chip — even when the
        // mnemonic contains duplicate words (onFirst() takes the first of the remaining).
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Confirm").fetchSemanticsNodes().isNotEmpty()
        }
        for (word in words) {
            composeRule.onAllNodesWithText(word, substring = false).onFirst().performClick()
        }
        composeRule.onNodeWithText("Confirm").assertIsDisplayed().performClick()

        // 5. Set PIN, then confirm it. Digits are matched exactly so the "6" in the subtitle
        // ("Choose a 6-digit PIN…") can never collide with the keypad.
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Set Your PIN").fetchSemanticsNodes().isNotEmpty()
        }
        enterPin()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Confirm Your PIN").fetchSemanticsNodes().isNotEmpty()
        }
        enterPin()

        // 6. Onboarding completes → the main graph's Home screen renders.
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithText("Total Balance").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Total Balance").assertIsDisplayed()
    }

    private fun enterPin() {
        for (digit in "123456") {
            composeRule.onNodeWithText(digit.toString(), substring = false).performClick()
        }
    }
}
