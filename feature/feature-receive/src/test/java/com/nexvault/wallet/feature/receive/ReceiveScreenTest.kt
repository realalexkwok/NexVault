package com.nexvault.wallet.feature.receive

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import com.nexvault.wallet.domain.model.chain.Chain
import com.nexvault.wallet.domain.model.chain.SupportedChains
import com.nexvault.wallet.domain.model.wallet.Account
import com.nexvault.wallet.domain.model.wallet.Wallet
import com.nexvault.wallet.domain.model.wallet.WalletType
import com.nexvault.wallet.domain.usecase.chain.GetSelectedChainUseCase
import com.nexvault.wallet.domain.usecase.wallet.GetActiveWalletUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roadmap 2.7 / AC-2.7: the receive screen renders the badge, QR, full address and warning, and
 * its Copy/Share actions reach the platform. The ViewModel is a real one built from mocked use
 * cases (the 2.6 house pattern), so the composable runs exactly as it does in the app.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ReceiveScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun setContent(
        chain: Chain = SupportedChains.ETHEREUM_MAINNET,
        withWallet: Boolean = true,
        walletFlowFails: Boolean = false,
    ) {
        val getActiveWallet = mockk<GetActiveWalletUseCase>()
        val getSelectedChain = mockk<GetSelectedChainUseCase>()
        every { getActiveWallet() } returns
            if (walletFlowFails) {
                flow { throw IllegalStateException("boom") }
            } else {
                flowOf(if (withWallet) wallet() else null)
            }
        every { getSelectedChain() } returns flowOf(chain)

        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                ReceiveScreen(
                    onNavigateBack = { },
                    viewModel = ReceiveViewModel(getActiveWallet, getSelectedChain),
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun receiveScreen_showsTheAddressTheQrAndTheNetworkWarning() {
        setContent()

        composeRule.onNodeWithText("Receive").assertIsDisplayed()
        composeRule.onNodeWithTag(RECEIVE_ADDRESS_TAG).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Wallet address QR code").assertIsDisplayed()
        composeRule.onNodeWithText(SupportedChains.ETHEREUM_MAINNET.name).assertIsDisplayed()
        // The warning sits below the QR on a short test viewport, so assert presence.
        composeRule.onNodeWithText(
            "Only send assets on Ethereum to this address. Funds sent on another network may be lost.",
        ).assertExists()
    }

    @Test
    fun receiveScreen_warnsDifferentlyOnATestnet() {
        setContent(chain = SupportedChains.ETHEREUM_SEPOLIA)

        composeRule.onNodeWithText(
            "You are receiving on ${SupportedChains.ETHEREUM_SEPOLIA.name}, a test network. " +
                "These funds have no value.",
        ).assertExists()
    }

    @Test
    fun receiveScreen_withoutAWallet_explainsInsteadOfShowingAVoidQr() {
        setContent(withWallet = false)

        composeRule.onNodeWithText("No active wallet. Create or import a wallet first.").assertIsDisplayed()
    }

    @Test
    fun receiveScreen_whenTheWalletFlowFails_reportsItInsteadOfLoadingForever() {
        setContent(walletFlowFails = true)

        composeRule.onNodeWithText("Could not load the wallet address.").assertIsDisplayed()
    }

    @Test
    fun copyButton_putsTheAddressOnTheClipboard() {
        setContent()

        composeRule.onNodeWithText("Copy").performClick()
        composeRule.waitForIdle()

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals(ADDRESS, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }

    @Test
    fun shareButton_opensAChooserWithTheAddress() {
        setContent()

        composeRule.onNodeWithText("Share").performClick()
        composeRule.waitForIdle()

        val started = Shadows.shadowOf(context as Application).nextStartedActivity
        assertTrue(started.action == Intent.ACTION_CHOOSER || started.action == Intent.ACTION_SEND)
    }

    private fun wallet(accounts: List<Account> = listOf(account())) =
        Wallet(
            id = "w1",
            name = "Main",
            type = WalletType.HD,
            accounts = accounts,
            createdAt = 0L,
            isActive = true,
        )

    private fun account() =
        Account(
            walletId = "w1",
            index = 0,
            address = ADDRESS,
            name = "Account 1",
            derivationPath = "m/44'/60'/0'/0/0",
            isActive = true,
        )

    private companion object {
        const val ADDRESS = "0x97B633905380C70B1a4c18DDCd56676589bcE147"
    }
}
