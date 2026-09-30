package com.nexvault.wallet.core.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roadmap 2.7 / AC-2.7: the generated QR encodes the address and decodes back with zxing's own
 * reader — the programmatic half of "scans correctly with any QR reader".
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class QrCodeImageTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun generatedQr_decodesBackToTheAddress() {
        val bitmap = generateQrBitmap(ADDRESS)

        assertEquals(ADDRESS, decode(bitmap))
    }

    @Test
    fun generatedQr_hasAQuietZone() {
        val bitmap = generateQrBitmap(ADDRESS)

        // The spec's quiet zone means the very first pixel is background, not a module.
        assertEquals(bitmap.getPixel(0, 0), bitmap.getPixel(bitmap.width - 1, 0))
        assertEquals(-0x1, bitmap.getPixel(0, 0))
    }

    @Test
    fun qrCodeImage_rendersWithItsAccessibilityLabel() {
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                QrCodeImage(content = ADDRESS)
            }
        }

        composeRule.onNodeWithContentDescription("Wallet address QR code").assertIsDisplayed()
    }

    @Test
    fun qrCodeImage_honoursACustomContentDescription() {
        composeRule.setContent {
            NexVaultTheme(darkTheme = false) {
                QrCodeImage(content = ADDRESS, contentDescription = "Sepolia address code")
            }
        }

        composeRule.onNodeWithContentDescription("Sepolia address code").assertIsDisplayed()
    }

    @Test
    fun blankContent_rendersNothingInsteadOfCrashing() {
        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                QrCodeImage(content = "")
            }
        }

        composeRule.onNodeWithContentDescription("Wallet address QR code").assertDoesNotExist()
    }

    @Test(expected = IllegalArgumentException::class)
    fun generateQrBitmap_rejectsBlankContent() {
        generateQrBitmap("  ")
    }

    private fun decode(bitmap: android.graphics.Bitmap): String {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val binary = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels)))
        return QRCodeReader().decode(binary).text
    }

    private companion object {
        const val ADDRESS = "0x97B633905380C70B1a4c18DDCd56676589bcE147"
    }
}
