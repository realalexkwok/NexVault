package com.nexvault.wallet.feature.onboarding.viewmodel

import com.nexvault.wallet.domain.model.auth.WalletCreationResult
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.usecase.wallet.ImportFromMnemonicUseCase
import com.nexvault.wallet.domain.usecase.wallet.ImportFromPrivateKeyUseCase
import com.nexvault.wallet.feature.onboarding.R
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImportWalletViewModelTest {

    private lateinit var importFromMnemonicUseCase: ImportFromMnemonicUseCase
    private lateinit var importFromPrivateKeyUseCase: ImportFromPrivateKeyUseCase
    private lateinit var viewModel: ImportWalletViewModel

    private val validMnemonic12 = "apple brave crane delta eagle frost grape house ivory jump king lamp"
    private val validMnemonic24 = "apple brave crane delta eagle frost grape house ivory jump king lamp moon peace quiet brave delta eagle float glad gift knee river peace"

    @Before
    fun setup() {
        importFromMnemonicUseCase = mockk()
        importFromPrivateKeyUseCase = mockk()
        viewModel = ImportWalletViewModel(importFromMnemonicUseCase, importFromPrivateKeyUseCase)
    }

    @Test
    fun initialStateIsCorrect() {
        val state = viewModel.uiState.value
        assertEquals(ImportWalletViewModel.ImportMode.MNEMONIC, state.importMode)
        assertEquals("", state.mnemonicInput)
        assertEquals("", state.privateKeyInput)
        assertNull(state.mnemonicErrorRes)
        assertNull(state.privateKeyErrorRes)
        assertFalse(state.isLoading)
        assertNull(state.generalErrorRes)
        assertNull(state.generalErrorMessage)
        assertFalse(state.isImportEnabled)
    }

    @Test
    fun modeSwitchClearsErrors() {
        viewModel.onMnemonicInputChanged("word1 word2 123number word4 word5 word6 word7 word8 word9 word10 word11 word12")
        assertTrue(viewModel.uiState.value.mnemonicErrorRes != null)

        viewModel.onImportModeChanged(ImportWalletViewModel.ImportMode.PRIVATE_KEY)

        val state = viewModel.uiState.value
        assertEquals(ImportWalletViewModel.ImportMode.PRIVATE_KEY, state.importMode)
        assertNull(state.mnemonicErrorRes)
        assertNull(state.privateKeyErrorRes)
        assertNull(state.generalErrorRes)
        assertNull(state.generalErrorMessage)
    }

    @Test
    fun valid12WordMnemonicEnablesImport() {
        viewModel.onMnemonicInputChanged(validMnemonic12)

        val state = viewModel.uiState.value
        assertTrue(state.isImportEnabled)
        assertNull(state.mnemonicErrorRes)
    }

    @Test
    fun valid24WordMnemonicEnablesImport() {
        viewModel.onMnemonicInputChanged(validMnemonic24)

        val state = viewModel.uiState.value
        assertTrue(state.isImportEnabled)
        assertNull(state.mnemonicErrorRes)
    }

    @Test
    fun incompleteMnemonicDisablesImport() {
        val incomplete = "apple brave crane delta eagle frost grape house ivory jump king"
        viewModel.onMnemonicInputChanged(incomplete)

        val state = viewModel.uiState.value
        assertFalse(state.isImportEnabled)
    }

    @Test
    fun mnemonicWithInvalidCharsShowsError() {
        viewModel.onMnemonicInputChanged("word1 word2 123number word4 word5 word6 word7 word8 word9 word10 word11 word12")

        val state = viewModel.uiState.value
        assertEquals(R.string.import_wallet_mnemonic_invalid_chars, state.mnemonicErrorRes)
        assertFalse(state.isImportEnabled)
    }

    @Test
    fun mnemonicWithTooManyWordsShowsError() {
        val tooMany = "apple brave crane delta eagle frost grape house ivory jump king lamp moon peace quiet brave delta eagle float glad gift knee river peace quiet extra"
        viewModel.onMnemonicInputChanged(tooMany)

        val state = viewModel.uiState.value
        assertEquals(R.string.import_wallet_mnemonic_too_many_words, state.mnemonicErrorRes)
    }

    @Test
    fun validPrivateKeyEnablesImport() {
        val validKey = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2"

        viewModel.onImportModeChanged(ImportWalletViewModel.ImportMode.PRIVATE_KEY)
        viewModel.onPrivateKeyInputChanged(validKey)

        val state = viewModel.uiState.value
        assertTrue(state.isImportEnabled)
        assertNull(state.privateKeyErrorRes)
    }

    @Test
    fun privateKeyWith0xPrefixWorks() {
        val validKeyWithPrefix = "0xa1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2"

        viewModel.onImportModeChanged(ImportWalletViewModel.ImportMode.PRIVATE_KEY)
        viewModel.onPrivateKeyInputChanged(validKeyWithPrefix)

        val state = viewModel.uiState.value
        assertTrue(state.isImportEnabled)
    }

    @Test
    fun shortPrivateKeyDisablesImport() {
        val shortKey = "a1b2c3d4e5f6a1b2c3d4e5f6"

        viewModel.onImportModeChanged(ImportWalletViewModel.ImportMode.PRIVATE_KEY)
        viewModel.onPrivateKeyInputChanged(shortKey)

        val state = viewModel.uiState.value
        assertFalse(state.isImportEnabled)
    }
}
