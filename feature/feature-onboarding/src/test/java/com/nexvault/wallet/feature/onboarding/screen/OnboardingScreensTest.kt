package com.nexvault.wallet.feature.onboarding.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.wallet.WalletDraft
import com.nexvault.wallet.domain.usecase.wallet.CreateWalletUseCase
import com.nexvault.wallet.domain.usecase.wallet.GenerateWalletDraftUseCase
import com.nexvault.wallet.domain.usecase.wallet.GetMnemonicForBackupUseCase
import com.nexvault.wallet.feature.onboarding.viewmodel.CreateWalletViewModel
import com.nexvault.wallet.feature.onboarding.viewmodel.VerifyMnemonicViewModel
import com.nexvault.wallet.feature.onboarding.viewmodel.WelcomeViewModel
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roadmap 2.6 coverage: the onboarding screens render and react on the JVM under Robolectric,
 * with real ViewModels built from mocked use cases.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class OnboardingScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun welcomeScreen_offersBothEntryPoints() {
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                WelcomeScreen(
                    onNavigateToCreateWallet = { },
                    onNavigateToImportWallet = { },
                    viewModel = WelcomeViewModel(),
                )
            }
        }

        composeRule.onNodeWithText("Create New Wallet").assertIsDisplayed()
        composeRule.onNodeWithText("Import Wallet").assertIsDisplayed()
    }

    @Test
    fun createWalletScreen_showsTheGeneratedMnemonicWords() {
        val draft =
            WalletDraft(
                mnemonic = WORDS.joinToString(" "),
                mnemonicWords = WORDS,
                address = "0x1234567890abcdef1234567890abcdef12345678",
            )
        val generateDraft = mockk<GenerateWalletDraftUseCase>()
        coEvery { generateDraft() } returns DataResult.Success(draft)
        val createWallet = mockk<CreateWalletUseCase>(relaxed = true)
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                CreateWalletScreen(
                    onNavigateBack = { },
                    onNavigateToVerifyMnemonic = { _ -> },
                    viewModel = CreateWalletViewModel(generateDraft, createWallet),
                )
            }
        }
        composeRule.waitForIdle()

        // "abandon" repeats eleven times in the canonical vector, so assert the unique word.
        composeRule.onNodeWithText("about").assertIsDisplayed()
        composeRule.onNodeWithText("I have written down my recovery phrase").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Continue").assertExists()
    }

    @Test
    fun verifyMnemonicScreen_listsTheWordsToTap() {
        val backup = mockk<GetMnemonicForBackupUseCase>()
        coEvery { backup("w1") } returns DataResult.Success(WORDS)

        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                VerifyMnemonicScreen(
                    onNavigateBack = { },
                    onNavigateToSetPin = { },
                    viewModel =
                        VerifyMnemonicViewModel(
                            savedStateHandle = SavedStateHandle(mapOf("walletId" to "w1")),
                            getMnemonicForBackupUseCase = backup,
                        ),
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("about").assertExists()
    }

    private companion object {
        // Canonical public BIP-39 test vector words (never real wallet material).
        val WORDS =
            listOf(
                "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
                "abandon", "abandon", "abandon", "abandon", "abandon", "about",
            )
    }
}
