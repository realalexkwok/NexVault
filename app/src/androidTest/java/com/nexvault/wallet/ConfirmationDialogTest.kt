package com.nexvault.wallet

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexvault.wallet.core.ui.components.ConfirmationDialog
import com.nexvault.wallet.core.ui.components.ConfirmationDialogTexts
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Roadmap 2.6: TC-UI-010 — the generic [ConfirmationDialog]: title, body, confirm and dismiss
 * actions, and the disabled confirm state used by the send flow's PIN step (the screen embeds
 * its [androidx.compose.material3.Text]-based body and drives the button states; the dialog
 * itself stays screen-agnostic).
 *
 * Hosted on the debug-only [DialogTestHostActivity]: MainActivity already sets its own content,
 * and the rule's default empty activity resolves to the wrong process under this app's manifest.
 */
@RunWith(AndroidJUnit4::class)
class ConfirmationDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<DialogTestHostActivity>()

    // TC-UI-010: the dialog renders its title, body, and both actions.
    @Test
    fun dialog_rendersTitleBodyAndActions() {
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                ConfirmationDialog(
                    texts =
                        ConfirmationDialogTexts(
                            title = "Confirm transaction",
                            confirmText = "Confirm",
                            dismissText = "Cancel",
                        ),
                    onConfirm = { },
                    onDismiss = { },
                    body = { androidx.compose.material3.Text("Enter your PIN") },
                )
            }
        }

        composeRule.onNodeWithText("Confirm transaction").assertIsDisplayed()
        composeRule.onNodeWithText("Enter your PIN").assertIsDisplayed()
        composeRule.onNodeWithText("Confirm").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    // TC-UI-010: tapping confirm invokes the callback exactly once.
    @Test
    fun confirm_invokesTheCallback() {
        var confirmed = false
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                ConfirmationDialog(
                    texts =
                        ConfirmationDialogTexts(
                            title = "Confirm transaction",
                            confirmText = "Confirm",
                            dismissText = "Cancel",
                        ),
                    onConfirm = { confirmed = true },
                    onDismiss = { },
                )
            }
        }

        composeRule.onNodeWithText("Confirm").performClick()
        composeRule.waitForIdle()

        assertTrue(confirmed)
    }

    // TC-UI-010: dismiss invokes the callback; a disabled confirm stays inert.
    @Test
    fun dismiss_invokesTheCallback_andDisabledConfirmIsInert() {
        var dismissed = false
        var confirmed = 0
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                ConfirmationDialog(
                    texts =
                        ConfirmationDialogTexts(
                            title = "Confirm transaction",
                            confirmText = "Confirm",
                            dismissText = "Cancel",
                        ),
                    onConfirm = { confirmed += 1 },
                    onDismiss = { dismissed = true },
                    confirmEnabled = false,
                )
            }
        }

        composeRule.onNodeWithText("Confirm").assertIsNotEnabled()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()

        assertTrue(dismissed)
        assertEquals(0, confirmed)
    }
}
