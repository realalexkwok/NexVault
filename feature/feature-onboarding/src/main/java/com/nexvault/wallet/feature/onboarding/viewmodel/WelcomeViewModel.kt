package com.nexvault.wallet.feature.onboarding.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

/**
 * Simple ViewModel for the Welcome screen.
 * Emits navigation events when the user chooses an action.
 */
@HiltViewModel
class WelcomeViewModel @Inject constructor() : ViewModel() {
    sealed interface NavigationEvent {
        data object NavigateToCreateWallet : NavigationEvent

        data object NavigateToImportWallet : NavigationEvent
    }

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    fun onCreateWalletClicked() {
        _navigationEvent.tryEmit(NavigationEvent.NavigateToCreateWallet)
    }

    fun onImportWalletClicked() {
        _navigationEvent.tryEmit(NavigationEvent.NavigateToImportWallet)
    }
}
