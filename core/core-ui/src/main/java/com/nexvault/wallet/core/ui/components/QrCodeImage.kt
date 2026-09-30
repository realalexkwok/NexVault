package com.nexvault.wallet.core.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.nexvault.wallet.core.ui.theme.NexVaultDimens
import android.graphics.Color as AndroidColor

/** Size of the generated bitmap; the QR is rendered at [DEFAULT_QR_SIZE] dp regardless. */
private const val QR_BITMAP_SIZE_PX = 512

/** Quiet zone (modules) ISO/IEC 18004 asks for around the code; strict readers need all four. */
private const val QR_MARGIN_MODULES = 4

private val DEFAULT_QR_SIZE = 240.dp

/**
 * Renders [content] as a QR code (roadmap 2.7, AC-2.7).
 *
 * The code is always drawn black-on-white regardless of the theme: scanners need the light
 * quiet zone, so a dark-theme inversion would break real-world scanning.
 *
 * Blank content renders nothing: zxing rejects an empty payload, and a caller that has no
 * address yet should not crash the screen it is on.
 *
 * @param content Payload to encode (the wallet address).
 * @param size Side length of the rendered code.
 * @param contentDescription Accessibility/semantics label, also used by tests.
 */
@Composable
fun QrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = DEFAULT_QR_SIZE,
    contentDescription: String = "Wallet address QR code",
) {
    if (content.isBlank()) return
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }.coerceAtLeast(QR_BITMAP_SIZE_PX)
    val bitmap = remember(content, sizePx) { generateQrBitmap(content, QR_BITMAP_SIZE_PX) }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier =
            modifier
                .size(size)
                .clip(RoundedCornerShape(NexVaultDimens.cornerRadiusMedium))
                .background(Color.White)
                .padding(NexVaultDimens.spacingSm)
                .semantics { this.contentDescription = contentDescription },
    )
}

/**
 * Encodes [content] into a square QR [Bitmap] with a quiet zone and medium error correction.
 *
 * Kept as a plain function (no Compose) so it is unit-testable and reusable; the returned bitmap
 * can be round-tripped through zxing's decoder in tests to prove it scans.
 */
internal fun generateQrBitmap(content: String, sizePx: Int = QR_BITMAP_SIZE_PX): Bitmap {
    require(content.isNotBlank()) { "QR content must not be blank" }
    val hints =
        mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to QR_MARGIN_MODULES,
            EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name(),
        )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val width = matrix.width
    val height = matrix.height
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        val rowOffset = y * width
        for (x in 0 until width) {
            pixels[rowOffset + x] = if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE
        }
    }
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
    }
}
