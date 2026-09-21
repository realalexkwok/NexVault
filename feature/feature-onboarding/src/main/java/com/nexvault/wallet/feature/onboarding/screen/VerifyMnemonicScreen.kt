package com.nexvault.wallet.feature.onboarding.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexvault.wallet.core.ui.components.NexVaultButton
import com.nexvault.wallet.core.ui.components.NexVaultTopBar
import com.nexvault.wallet.core.ui.theme.NexVaultDimens
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import com.nexvault.wallet.feature.onboarding.R
import com.nexvault.wallet.feature.onboarding.viewmodel.VerifyMnemonicViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Verify Mnemonic screen — user taps words in correct order to prove
 * they wrote down the recovery phrase.
 *
 * Wireframe reference: doc/03-UI-UX-DESIGN.md Section 2.1 — VerifyMnemonicScreen
 *
 * Layout:
 * - Top app bar with back arrow and "Step 2 of 3"
 * - Title "Verify Your Phrase"
 * - Subtitle "Tap words in the correct order"
 * - Selected words area (shows words tapped so far, numbered)
 * - Available words area (shuffled, tappable chips)
 * - Confirm button (disabled until all words selected correctly)
 *
 * On error (wrong word tapped):
 * - Selected words area shakes (horizontal shake animation)
 * - Error color briefly shown
 * - After 1.5s delay, selected words reset and user starts over
 */
@Composable
fun VerifyMnemonicScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSetPin: () -> Unit,
    viewModel: VerifyMnemonicViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest { event ->
            when (event) {
                VerifyMnemonicViewModel.NavigationEvent.NavigateToSetPin ->
                    onNavigateToSetPin()
            }
        }
    }

    VerifyMnemonicScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onWordSelected = viewModel::onWordSelected,
        onSelectedWordRemoved = viewModel::onSelectedWordRemoved,
        onConfirmClicked = viewModel::onConfirmClicked,
        onResetClicked = viewModel::onResetClicked,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VerifyMnemonicScreenContent(
    uiState: VerifyMnemonicViewModel.UiState,
    onNavigateBack: () -> Unit,
    onWordSelected: (String) -> Unit,
    onSelectedWordRemoved: (String) -> Unit,
    onConfirmClicked: () -> Unit,
    onResetClicked: () -> Unit,
) {
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(uiState.isError) {
        if (uiState.isError) {
            repeat(3) {
                shakeOffset.animateTo(
                    targetValue = 10f,
                    animationSpec = tween(durationMillis = 50),
                )
                shakeOffset.animateTo(
                    targetValue = -10f,
                    animationSpec = tween(durationMillis = 50),
                )
            }
            shakeOffset.animateTo(0f, animationSpec = tween(durationMillis = 50))
        }
    }

    val errorText =
        uiState.errorMessage
            ?: uiState.errorRes?.let { stringResource(it, *uiState.errorArgs.toTypedArray()) }

    Scaffold(
        topBar = {
            NexVaultTopBar(
                title = stringResource(R.string.verify_mnemonic_step_indicator),
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
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }

            errorText != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                    )
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
                        text = stringResource(R.string.verify_mnemonic_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(NexVaultDimens.spacingSm))

                    Text(
                        text = stringResource(R.string.verify_mnemonic_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(NexVaultDimens.spacingLg))

                    val selectedAreaColor by animateColorAsState(
                        targetValue = if (uiState.isError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        },
                        label = "selectedAreaColor",
                    )

                    Text(
                        text = stringResource(R.string.verify_mnemonic_selected_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(NexVaultDimens.spacingSm))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = NexVaultDimens.selectionAreaMinHeight)
                            .offset(x = shakeOffset.value.toInt().dp),
                        color = selectedAreaColor,
                        shape = RoundedCornerShape(NexVaultDimens.cornerRadiusMedium),
                        tonalElevation = NexVaultDimens.chipTonalElevation,
                    ) {
                        if (uiState.selectedWords.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(NexVaultDimens.spacingLg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.verify_mnemonic_empty_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                            }
                        } else {
                            FlowRow(
                                modifier = Modifier.padding(NexVaultDimens.spacing12),
                                horizontalArrangement = Arrangement.spacedBy(NexVaultDimens.spacingSm),
                                verticalArrangement = Arrangement.spacedBy(NexVaultDimens.spacingSm),
                            ) {
                                uiState.selectedWords.forEachIndexed { index, word ->
                                    WordChip(
                                        text = "${index + 1}. $word",
                                        isSelected = true,
                                        onClick = { onSelectedWordRemoved(word) },
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.isError) {
                        Spacer(modifier = Modifier.height(NexVaultDimens.spacingSm))
                        Text(
                            text = stringResource(R.string.verify_mnemonic_error),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Spacer(modifier = Modifier.height(NexVaultDimens.spacingLg))

                    Text(
                        text = stringResource(R.string.verify_mnemonic_available_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(NexVaultDimens.spacingSm))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NexVaultDimens.spacingSm),
                        verticalArrangement = Arrangement.spacedBy(NexVaultDimens.spacingSm),
                    ) {
                        uiState.availableWords.forEach { word ->
                            WordChip(
                                text = word,
                                isSelected = false,
                                onClick = { onWordSelected(word) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(NexVaultDimens.spacingLg))

                    NexVaultButton(
                        text = stringResource(R.string.verify_mnemonic_confirm),
                        onClick = onConfirmClicked,
                        enabled = uiState.isVerified,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(NexVaultDimens.spacingLg))
                }
            }
        }
    }
}

@Composable
private fun WordChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(NexVaultDimens.cornerRadiusSmall))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(NexVaultDimens.cornerRadiusSmall),
        border = androidx.compose.foundation.BorderStroke(NexVaultDimens.borderWidth, borderColor),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = NexVaultDimens.spacingMd, vertical = NexVaultDimens.spacingSm),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = textColor,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VerifyMnemonicScreenPreview() {
    NexVaultTheme {
        VerifyMnemonicScreenContent(
            uiState = VerifyMnemonicViewModel.UiState(
                originalWords = listOf(
                    "apple", "brave", "crane", "delta", "eagle", "frost",
                    "grape", "house", "ivory", "jump", "king", "lamp"
                ),
                shuffledWords = listOf(
                    "frost", "lamp", "crane", "apple", "jump", "house",
                    "brave", "ivory", "eagle", "king", "delta", "grape"
                ),
                selectedWords = listOf("apple", "brave"),
                availableWords = listOf(
                    "frost", "lamp", "crane", "jump", "house",
                    "ivory", "eagle", "king", "delta", "grape"
                ),
                isVerified = false,
                isError = false,
                isLoading = false,
                errorMessage = null,
            ),
            onNavigateBack = {},
            onWordSelected = {},
            onSelectedWordRemoved = {},
            onConfirmClicked = {},
            onResetClicked = {},
        )
    }
}
