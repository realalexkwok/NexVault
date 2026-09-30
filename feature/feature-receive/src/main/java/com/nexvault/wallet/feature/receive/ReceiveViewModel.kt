package com.nexvault.wallet.feature.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexvault.wallet.core.ui.components.ChainUi
import com.nexvault.wallet.core.ui.mapper.toChainUi
import com.nexvault.wallet.domain.usecase.chain.GetSelectedChainUseCase
import com.nexvault.wallet.domain.usecase.wallet.GetActiveWalletUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Why the screen has no address to show. The copy lives in `strings.xml`
 * (`receive_no_wallet` / `receive_load_failed`) so the ViewModel carries no user-facing text.
 */
enum class ReceiveError {
    NO_ACTIVE_WALLET,
    LOAD_FAILED,
}

/**
 * ViewModel for the receive screen (roadmap 2.7, AC-2.7).
 *
 * The screen only needs two streams: the active wallet's address and the selected chain (for the
 * badge and the network-compatibility warning). Everything the user can do — copy, share, show the
 * QR — is derived from those, so the state stays a plain snapshot.
 */
@HiltViewModel
class ReceiveViewModel @Inject constructor(
    private val getActiveWalletUseCase: GetActiveWalletUseCase,
    private val getSelectedChainUseCase: GetSelectedChainUseCase,
) : ViewModel() {
    data class UiState(
        val address: String = "",
        val chain: ChainUi? = null,
        val isLoading: Boolean = true,
        val error: ReceiveError? = null,
    ) {
        /** QR/share actions are only meaningful once an address is known. */
        val hasAddress: Boolean get() = address.isNotBlank()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        observeWalletAndChain()
    }

    private fun observeWalletAndChain() {
        viewModelScope.launch {
            combine(getActiveWalletUseCase(), getSelectedChainUseCase()) { wallet, chain ->
                val address = wallet?.accounts?.firstOrNull { it.isActive }?.address.orEmpty()
                UiState(
                    address = address,
                    chain = chain.toChainUi(),
                    isLoading = false,
                    error = if (address.isBlank()) ReceiveError.NO_ACTIVE_WALLET else null,
                )
            }
                // A repository failure used to leave the screen loading forever; surface it instead
                // (the screen offers the message, the navigation layer offers a way back).
                .catch { _ ->
                    _uiState.update { it.copy(isLoading = false, error = ReceiveError.LOAD_FAILED) }
                }
                .collect { state -> _uiState.update { state } }
        }
    }
}
