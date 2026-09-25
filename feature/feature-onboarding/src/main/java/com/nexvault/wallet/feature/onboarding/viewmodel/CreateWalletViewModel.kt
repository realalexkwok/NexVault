package com.nexvault.wallet.feature.onboarding.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.wallet.WalletDraft
import com.nexvault.wallet.domain.usecase.wallet.CreateWalletUseCase
import com.nexvault.wallet.domain.usecase.wallet.GenerateWalletDraftUseCase
import com.nexvault.wallet.feature.onboarding.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Create Wallet screen.
 *
 * Two-step flow (roadmap 2.0.2b, option B):
 * 1. On construction a wallet **draft** is generated (mnemonic + first address) and shown;
 *    nothing is written to disk yet — no wallet exists before the user consents to the backup.
 * 2. Tapping Continue (enabled only after the acknowledgment checkbox) persists the draft and
 *    navigates to the Verify Mnemonic screen.
 *
 * `is_wallet_set_up` is deliberately not written here: onboarding completes only once the PIN
 * has been set (`SetPinViewModel` → `CompleteOnboardingUseCase`), so the root router cannot
 * swap graphs mid-flow and strand the user on the Unlock screen.
 */
@HiltViewModel
class CreateWalletViewModel @Inject constructor(
    internal val generateWalletDraftUseCase: GenerateWalletDraftUseCase,
    internal val createWalletUseCase: CreateWalletUseCase,
) : ViewModel() {

    data class UiState(
        val mnemonicWords: List<String> = emptyList(),
        val walletId: String = "",
        val address: String = "",
        val isAcknowledged: Boolean = false,
        val isLoading: Boolean = true,
        @StringRes val errorRes: Int? = null,
        val errorArgs: List<Any> = emptyList(),
        val errorMessage: String? = null,
    )

    sealed interface NavigationEvent {
        data class NavigateToVerifyMnemonic(val walletId: String) : NavigationEvent
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    /** The generated, not-yet-persisted wallet. */
    private var draft: WalletDraft? = null

    init {
        generateDraft()
    }

    private fun generateDraft() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorRes = null, errorArgs = emptyList(), errorMessage = null)
            }

            when (val result = generateWalletDraftUseCase()) {
                is DataResult.Success -> {
                    draft = result.data
                    _uiState.update {
                        it.copy(
                            mnemonicWords = result.data.mnemonicWords,
                            address = result.data.address,
                            isLoading = false,
                            errorRes = null,
                            errorMessage = null,
                        )
                    }
                }
                is DataResult.Error -> {
                    draft = null
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorRes = R.string.create_wallet_failed,
                            errorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    private fun persistDraft() {
        val currentDraft = draft ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorRes = null, errorArgs = emptyList(), errorMessage = null)
            }

            when (val result = createWalletUseCase(WALLET_NAME, currentDraft)) {
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(
                            walletId = result.data.walletId,
                            address = result.data.address,
                            isLoading = false,
                            errorRes = null,
                            errorMessage = null,
                        )
                    }
                    _navigationEvent.tryEmit(
                        NavigationEvent.NavigateToVerifyMnemonic(result.data.walletId)
                    )
                }
                is DataResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorRes = R.string.create_wallet_failed,
                            errorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun onAcknowledgeToggled(acknowledged: Boolean) {
        _uiState.update { it.copy(isAcknowledged = acknowledged) }
    }

    /**
     * Persists the draft and navigates to the Verify Mnemonic screen.
     *
     * Guarded against double taps by [UiState.isLoading]; the acknowledge checkbox is also
     * enforced here because the button's `enabled` state is a UI convenience, not a contract.
     */
    fun onContinueClicked() {
        val state = _uiState.value
        if (state.isLoading || draft == null) return
        if (!state.isAcknowledged) return
        persistDraft()
    }

    /**
     * Retry after a failure: a generation failure regenerates a fresh draft, a persist failure
     * retries the **same** draft so the mnemonic already shown to the user is never replaced
     * silently under their eyes.
     */
    fun onRetryClicked() {
        if (draft == null) {
            generateDraft()
        } else {
            persistDraft()
        }
    }

    private companion object {
        const val WALLET_NAME = "Main Wallet"
    }
}
