package com.nexvault.wallet.feature.send

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Roadmap 2.7 / TC-UI-006: the scanner keeps only the first payload — the analyzer fires dozens of
 * frames per second and the screen must navigate back exactly once.
 */
class QrScannerViewModelTest {
    @Test
    fun `keeps the first decoded payload and ignores later frames`() =
        runTest {
            val viewModel = QrScannerViewModel()

            viewModel.uiState.test {
                assertThat(awaitItem().decodedPayload).isNull()

                viewModel.onPayloadDecoded("0x1111111111111111111111111111111111111111")
                assertThat(awaitItem().decodedPayload)
                    .isEqualTo("0x1111111111111111111111111111111111111111")

                viewModel.onPayloadDecoded("0x2222222222222222222222222222222222222222")
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `trims the payload and ignores blank frames`() =
        runTest {
            val viewModel = QrScannerViewModel()

            viewModel.uiState.test {
                assertThat(awaitItem().decodedPayload).isNull()

                viewModel.onPayloadDecoded("   ")
                expectNoEvents()

                viewModel.onPayloadDecoded("  0x3333333333333333333333333333333333333333  ")
                assertThat(awaitItem().decodedPayload)
                    .isEqualTo("0x3333333333333333333333333333333333333333")
                cancelAndIgnoreRemainingEvents()
            }
        }
}
