package com.nexvault.wallet.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexvault.wallet.domain.model.chain.Chain
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.transaction.Transaction
import com.nexvault.wallet.domain.model.transaction.TransactionStatus
import com.nexvault.wallet.domain.usecase.chain.GetSelectedChainUseCase
import com.nexvault.wallet.domain.usecase.transaction.GetTransactionDetailUseCase
import com.nexvault.wallet.domain.usecase.transaction.UpdateTransactionStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for a single transaction (roadmap 2.8, AC-2.8: "tapping a transaction opens a detail
 * view with full info").
 *
 * Pending rows can be re-checked against the chain from here too, so the user does not have to go
 * back and pull-to-refresh the list.
 */
@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTransactionDetailUseCase: GetTransactionDetailUseCase,
    private val updateTransactionStatusUseCase: UpdateTransactionStatusUseCase,
    private val getSelectedChainUseCase: GetSelectedChainUseCase,
) : ViewModel() {
    data class UiState(
        val transaction: Transaction? = null,
        val chain: Chain? = null,
        val isLoading: Boolean = true,
        val isCheckingReceipt: Boolean = false,
        val errorMessage: String? = null,
    ) {
        val canCheckReceipt: Boolean
            get() = transaction?.status == TransactionStatus.PENDING && !isCheckingReceipt
    }

    private val txHash: String = checkNotNull(savedStateHandle.get<String>(ARG_TX_HASH))
    private val chainId: Int = checkNotNull(savedStateHandle.get<Int>(ARG_CHAIN_ID))

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getSelectedChainUseCase().collect { chain ->
                _uiState.update { it.copy(chain = chain) }
            }
        }
        load()
    }

    /** Re-reads the stored row; also used after a receipt check. */
    fun load() {
        viewModelScope.launch {
            when (val result = getTransactionDetailUseCase(txHash, chainId)) {
                is DataResult.Success ->
                    _uiState.update {
                        it.copy(transaction = result.data, isLoading = false, errorMessage = null)
                    }

                is DataResult.Error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
            }
        }
    }

    /** Asks the chain whether a pending transaction has been mined (roadmap 2.8 shortcut fix). */
    fun onCheckReceipt() {
        if (!_uiState.value.canCheckReceipt) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingReceipt = true) }
            when (val result = updateTransactionStatusUseCase(txHash, chainId)) {
                is DataResult.Success ->
                    _uiState.update {
                        it.copy(
                            transaction = result.data,
                            isCheckingReceipt = false,
                            errorMessage = null,
                        )
                    }

                is DataResult.Error ->
                    _uiState.update {
                        it.copy(isCheckingReceipt = false, errorMessage = result.message)
                    }
            }
        }
    }

    companion object {
        const val ARG_TX_HASH = "txHash"
        const val ARG_CHAIN_ID = "chainId"
    }
}
