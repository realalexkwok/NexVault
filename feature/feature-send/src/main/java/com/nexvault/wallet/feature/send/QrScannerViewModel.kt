package com.nexvault.wallet.feature.send

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ViewModel for the QR scanner (roadmap 2.7, TC-UI-006).
 *
 * The analyzer fires many times per second, so the only state worth keeping is the first decoded
 * payload: later frames are ignored, which makes the screen navigate back exactly once. Validation
 * of the payload is deliberately left to the send form, which already reports "Invalid Ethereum
 * address" and keeps a single source of truth for that rule.
 */
@HiltViewModel
class QrScannerViewModel @Inject constructor() : ViewModel() {
    data class UiState(
        val decodedPayload: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Accepts a decoded frame payload; only the first non-blank one is kept. */
    fun onPayloadDecoded(raw: String) {
        val payload = raw.trim()
        if (payload.isEmpty()) return
        _uiState.update { current ->
            if (current.decodedPayload != null) current else UiState(decodedPayload = payload)
        }
    }
}
