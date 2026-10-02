package com.nexvault.wallet

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Roadmap 2.6: TC-INT-002 — the send flow on a real device against Sepolia.
 *
 * Precondition (same recipe as CreationFlowE2ETest): a wallet created with PIN `123456`.
 * `sendForm_validatesAndDisablesSubmitWithoutFunds` runs unconditionally; the funded happy path
 * self-gates on the wallet balance and runs to completion only during the funded walk — the
 * walk's evidence is the successful run of this class with funds on the wallet.
 *
 * Compose `performTextInput` is used for the address field so the walk never depends on the
 * device IME (the adb `input text` path is IME-routed and mangles `x` on this device).
 */
@RunWith(AndroidJUnit4::class)
class SendFlowE2ETest {
    // The scanner binds a real camera, so the permission is granted before the activity starts.
    @get:Rule(order = 0)
    val cameraPermission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun unlockWithPin() {
        // After the creation flow the session may still be unlocked (Home shows directly);
        // on later runs the app opens locked. Wait for either state.
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithText("Total Balance").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Enter your PIN to unlock").fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodesWithText("Total Balance").fetchSemanticsNodes().isNotEmpty()) return
        for (digit in "123456") {
            composeRule.onNodeWithText(digit.toString()).performClick()
            composeRule.waitForIdle()
        }
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("Enter your PIN to unlock").fetchSemanticsNodes().isEmpty()
        }
    }

    private fun switchToSepolia() {
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("Total Balance").fetchSemanticsNodes().isNotEmpty()
        }
        // Home restores its scroll position, so the chain selector may sit above the fold —
        // scroll the lazy list to the top before tapping it (the first "Ethereum" match in the
        // tree is otherwise a token row, which navigates to the token detail).
        composeRule.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(0)
        composeRule.waitForIdle()
        // Idempotent: earlier runs may already have selected Sepolia (the selection persists).
        if (composeRule.onAllNodesWithText("Sepolia Testnet").fetchSemanticsNodes().isNotEmpty()) return
        composeRule.onAllNodesWithText("Ethereum").onFirst().performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Sepolia Testnet").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Sepolia Testnet").performClick()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("Total Balance").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openSendForm() {
        // Item 3 of the dashboard list is the quick-actions row that holds Send.
        composeRule.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(3)
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasContentDescription("Send")).fetchSemanticsNodes().isNotEmpty()
        }
        // The quick-action label text sits outside the clickable icon button, so target the
        // icon via its content description (same string as the label).
        composeRule.onNodeWithContentDescription("Send").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Confirm and send").fetchSemanticsNodes().isNotEmpty()
        }
    }

    // TC-INT-002 part 1: form validation on-device; submit stays disabled without funds.
    @Test
    fun sendForm_validatesAndDisablesSubmitWithoutFunds() {
        unlockWithPin()
        switchToSepolia()
        openSendForm()

        // The custom text field hides its placeholder from the semantics tree, so address the
        // fields positionally: [0] = To, [1] = Amount.
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("not_an_address")
        composeRule.onNodeWithText("Invalid Ethereum address").assertIsDisplayed()

        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput("0.0001")
        composeRule.onNodeWithText("Amount exceeds your balance").assertIsDisplayed()

        // The review button must stay disabled while the form is invalid / unfunded.
        composeRule.onNodeWithText("Confirm and send").assertIsNotEnabled()
    }

    // TC-INT-002 part 2: the funded happy path. Self-gates on a funded wallet, so the default
    // suite stays green; during the walk it runs end-to-end (submit -> hash -> explorer link).
    @Test
    fun sendFlow_submitsAndShowsTheHash() {
        unlockWithPin()
        switchToSepolia()
        openSendForm()

        composeRule.onAllNodes(hasSetTextAction())[0]
            .performTextInput("0x0000000000000000000000000000000000000001")
        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput("0.0001")

        // Without funds the "amount exceeds balance" error persists and the review button never
        // enables; the funded walk is the only time this path proceeds.
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithText("Amount exceeds your balance").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasText("Confirm and send") and isEnabled())
                    .fetchSemanticsNodes().isNotEmpty()
        }
        val stillUnfunded =
            composeRule.onAllNodesWithText("Amount exceeds your balance").fetchSemanticsNodes().isNotEmpty()
        if (stillUnfunded) {
            // Unfunded: the review button stays disabled — the walk re-runs this test once the
            // faucet has funded the wallet.
            composeRule.onNodeWithText("Confirm and send").assertIsNotEnabled()
            return
        }

        composeRule.onNodeWithText("Confirm and send").performClick()
        // AC-2.6: the next step is the review screen ("Confirm" shows the transaction details
        // again); the PIN dialog opens only after that Confirm — first funded run (2.10) exposed
        // that the old wait skipped this step and timed out on the review screen.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Confirm").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Confirm").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Enter your PIN to sign and submit.").fetchSemanticsNodes().isNotEmpty()
        }
        for (digit in "123456") {
            composeRule.onNodeWithText(digit.toString()).performClick()
            composeRule.waitForIdle()
        }

        composeRule.waitUntil(timeoutMillis = 90_000) {
            composeRule.onAllNodesWithText("Transaction submitted").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("View on Explorer").assertIsDisplayed()

        // The hash is a 0x-prefixed 64-hex string rendered on the result screen.
        val hashNodes = composeRule.onAllNodes(hasText("0x", substring = true)).fetchSemanticsNodes()
        assertTrue("Result hash not found", hashNodes.isNotEmpty())
    }

    // TC-UI-006: the send form's QR button opens the scanner screen.
    @Test
    fun sendForm_qrButtonNavigatesToTheScanner() {
        unlockWithPin()
        switchToSepolia()
        openSendForm()

        composeRule.onNodeWithContentDescription("Scan QR code").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Scan QR code").fetchSemanticsNodes().isNotEmpty()
        }

        // Back to the form: the scanner is a pushed route, not a tab replacement.
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Confirm and send").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
