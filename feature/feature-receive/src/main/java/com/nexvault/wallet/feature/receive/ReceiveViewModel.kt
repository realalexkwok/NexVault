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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Message shown when the app has no active wallet to receive into. */
internal const val NO_ACTIVE_WALLET_MESSAGE = "No active wallet. Create or import a wallet first."

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
        val errorMessage: String? = null,
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
                    errorMessage = if (address.isBlank()) NO_ACTIVE_WALLET_MESSAGE else null,
                )
            }.collect { state -> _uiState.update { state } }
        }
    }
}
