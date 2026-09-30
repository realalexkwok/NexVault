package com.nexvault.wallet.feature.send

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roadmap 2.7 / TC-UI-006: the scanner screen's permission-denied path. Robolectric grants no
 * camera permission, which is exactly the branch under test — the camera itself is never bound.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class QrScannerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun scannerScreen_withoutCameraPermission_explainsAndOffersRetry() {
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                QrScannerScreen(
                    onQrDetected = { },
                    onNavigateBack = { },
                    viewModel = QrScannerViewModel(),
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Scan QR code").assertIsDisplayed()
        composeRule.onNodeWithText("Camera access is needed to scan a QR code.").assertIsDisplayed()
        composeRule.onNodeWithText("Allow camera").assertIsDisplayed()
    }
}
