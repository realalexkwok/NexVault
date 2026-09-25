package com.nexvault.wallet.feature.onboarding.screen

import android.app.Activity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexvault.wallet.core.ui.components.NexVaultButton
import com.nexvault.wallet.core.ui.components.NexVaultTopBar
import com.nexvault.wallet.core.ui.theme.NexVaultDimens
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import com.nexvault.wallet.feature.onboarding.R
import com.nexvault.wallet.feature.onboarding.viewmodel.CreateWalletViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Create Wallet screen — displays the generated 12-word mnemonic phrase.
 * User must write down the words and check the acknowledgment checkbox
 * before the Continue button is enabled.
 *
 * Wireframe reference: doc/03-UI-UX-DESIGN.md Section 2.1 — CreateWalletScreen
 *
 * Layout:
 * - Top app bar with back arrow and "Step 1 of 3"
 * - Title "Your Recovery Phrase"
 * - Subtitle warning about writing words down
 * - 4×3 grid of numbered mnemonic words
 * - Checkbox "I have written it down"
 * - Continue button (disabled until checkbox checked)
 *
 * FLAG_SECURE is set on this screen to prevent screenshots.
 */
@Composable
fun CreateWalletScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVerifyMnemonic: (String) -> Unit,
    viewModel: CreateWalletViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is CreateWalletViewModel.NavigationEvent.NavigateToVerifyMnemonic ->
                    onNavigateToVerifyMnemonic(event.walletId)
            }
        }
    }

    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        window?.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
        )
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    CreateWalletScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onAcknowledgeToggled = viewModel::onAcknowledgeToggled,
        onContinueClicked = viewModel::onContinueClicked,
        onRetryClicked = viewModel::onRetryClicked,
    )
}

@Composable
private fun CreateWalletScreenContent(
    uiState: CreateWalletViewModel.UiState,
    onNavigateBack: () -> Unit,
    onAcknowledgeToggled: (Boolean) -> Unit,
    onContinueClicked: () -> Unit,
    onRetryClicked: () -> Unit,
) {
    val errorText =
        uiState.errorMessage
            ?: uiState.errorRes?.let { stringResource(it, *uiState.errorArgs.toTypedArray()) }

    Scaffold(
        topBar = {
            NexVaultTopBar(
                title = stringResource(R.string.create_wallet_step_indicator),
                showBackButton = true,
                onBackClick = onNavigateBack,
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(NexVaultDimens.spacingMd))
                        Text(
                            text = stringResource(R.string.create_wallet_creating),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            errorText != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(NexVaultDimens.spacingMd))
                        NexVaultButton(
                            text = stringResource(R.string.create_wallet_retry),
                            onClick = onRetryClicked,
                        )
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = NexVaultDimens.spacingLg)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(modifier = Modifier.height(NexVaultDimens.spacingMd))

                    Text(
                        text = stringResource(R.string.create_wallet_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(NexVaultDimens.spacingSm))

                    Text(
                        text = stringResource(R.string.create_wallet_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(NexVaultDimens.spacingLg))

                    MnemonicGrid(
                        words = uiState.mnemonicWords,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(NexVaultDimens.spacingLg))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = NexVaultDimens.spacingSm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = uiState.isAcknowledged,
                            onCheckedChange = onAcknowledgeToggled,
                        )
                        Spacer(modifier = Modifier.width(NexVaultDimens.spacingSm))
                        Text(
                            text = stringResource(R.string.create_wallet_acknowledge),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(NexVaultDimens.spacingSm))

                    NexVaultButton(
                        text = stringResource(R.string.create_wallet_continue),
                        onClick = onContinueClicked,
                        enabled = uiState.isAcknowledged,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(NexVaultDimens.spacingLg))
                }
            }
        }
    }
}

/**
 * Displays mnemonic words in a 4×3 grid (3 rows, 4 columns).
 * Each cell shows the word number and the word.
 *
 * Plain [Row]s rather than a `LazyVerticalGrid`: this grid sits inside the screen's
 * `verticalScroll` column, and a lazy grid there is measured with an infinite maximum height
 * — Compose throws `IllegalStateException` and the screen crashes (found by the 2.0.2b device
 * walk). The phrase is at most 24 items, so laziness buys nothing anyway.
 */
@Composable
fun MnemonicGrid(
    words: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(NexVaultDimens.spacingSm),
    ) {
        words.chunked(MNEMONIC_COLUMNS).forEachIndexed { rowIndex, rowWords ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NexVaultDimens.spacingSm),
            ) {
                rowWords.forEachIndexed { columnIndex, word ->
                    MnemonicWordCell(
                        number = rowIndex * MNEMONIC_COLUMNS + columnIndex + 1,
                        word = word,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keep a short last row aligned with the 4-column grid.
                repeat(MNEMONIC_COLUMNS - rowWords.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private const val MNEMONIC_COLUMNS = 4

@Composable
private fun MnemonicWordCell(
    number: Int,
    word: String,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NexVaultDimens.cornerRadiusSmall))
            .border(
                width = NexVaultDimens.borderWidth,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(NexVaultDimens.cornerRadiusSmall),
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(NexVaultDimens.cornerRadiusSmall),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NexVaultDimens.spacing12, vertical = NexVaultDimens.spacing10),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$number.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(NexVaultDimens.spacingXs))
            Text(
                text = word,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateWalletScreenPreview() {
    NexVaultTheme {
        CreateWalletScreenContent(
            uiState = CreateWalletViewModel.UiState(
                mnemonicWords = listOf(
                    "apple", "brave", "crane", "delta", "eagle", "frost",
                    "grape", "house", "ivory", "jump", "king", "lamp"
                ),
                walletId = "sample-id",
                address = "0x1234...abcd",
                isAcknowledged = false,
                isLoading = false,
                errorRes = null,
            ),
            onNavigateBack = {},
            onAcknowledgeToggled = {},
            onContinueClicked = {},
            onRetryClicked = {},
        )
    }
}
