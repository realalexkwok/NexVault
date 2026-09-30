package com.nexvault.wallet.feature.send

import com.google.common.truth.Truth.assertThat
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.junit.Test

/**
 * Roadmap 2.7 / TC-UI-006: the scanner's decoder is a pure function over a luminance plane, so the
 * whole decode path is testable without a camera — a generated QR is rendered to a Y plane and
 * must decode back to its payload.
 */
class QrDecoderTest {
    @Test
    fun `decodes a generated qr from its luminance plane`() {
        val matrix = encode(PAYLOAD)

        val decoded = decodeQrLuminance(
            luminance = luminanceOf(matrix),
            dataWidth = matrix.width,
            dataHeight = matrix.height,
            width = matrix.width,
            height = matrix.height,
        )

        assertThat(decoded).isEqualTo(PAYLOAD)
    }

    @Test
    fun `decodes when the plane is padded with a row stride`() {
        val matrix = encode(PAYLOAD)
        val stride = matrix.width + 7

        val decoded = decodeQrLuminance(
            luminance = luminanceOf(matrix, rowStride = stride),
            dataWidth = stride,
            dataHeight = matrix.height,
            width = matrix.width,
            height = matrix.height,
        )

        assertThat(decoded).isEqualTo(PAYLOAD)
    }

    @Test
    fun `returns null for a plane without a qr code`() {
        val blank = ByteArray(64 * 64) { 0xFF.toByte() }

        val decoded = decodeQrLuminance(blank, 64, 64, 64, 64)

        assertThat(decoded).isNull()
    }

    @Test
    fun `returns null for an empty frame`() {
        assertThat(decodeQrLuminance(ByteArray(0), 0, 0, 0, 0)).isNull()
    }

    private fun encode(payload: String): BitMatrix =
        QRCodeWriter().encode(
            payload,
            BarcodeFormat.QR_CODE,
            256,
            256,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1,
            ),
        )

    /** Renders the matrix into a Y plane: 0 = black module, 255 = white background. */
    private fun luminanceOf(matrix: BitMatrix, rowStride: Int = matrix.width): ByteArray {
        val plane = ByteArray(rowStride * matrix.height) { 0xFF.toByte() }
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix.get(x, y)) plane[y * rowStride + x] = 0
            }
        }
        return plane
    }

    private companion object {
        const val PAYLOAD = "0x97B633905380C70B1a4c18DDCd56676589bcE147"
    }
}
