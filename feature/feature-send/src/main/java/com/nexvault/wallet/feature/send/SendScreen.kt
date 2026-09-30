package com.nexvault.wallet.feature.send

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexvault.wallet.core.ui.components.ConfirmationDialog
import com.nexvault.wallet.core.ui.components.NexVaultButton
import com.nexvault.wallet.core.ui.components.NexVaultTextField
import com.nexvault.wallet.core.ui.components.PinInputField
import com.nexvault.wallet.core.ui.theme.NexVaultDimens
import com.nexvault.wallet.core.ui.theme.NexVaultTheme

/**
 * The send flow (roadmap 2.6): form -> review -> PIN-confirmed submit -> result with a
 * "View on Explorer" link. TC-UI-010's [ConfirmationDialog] hosts the PIN step.
 *
 * Roadmap 2.7 adds the QR button: [onScanClicked] opens the scanner and a returned
 * [scannedAddress] fills the recipient field exactly once ([onScannedAddressConsumed]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    onNavigateBack: () -> Unit,
    viewModel: SendViewModel = hiltViewModel(),
    onScanClicked: () -> Unit = {},
    scannedAddress: String? = null,
    onScannedAddressConsumed: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showReview by remember { mutableStateOf(false) }

    ScannedAddressEffect(
        scannedAddress = scannedAddress,
        viewModel = viewModel,
        onConsumed = onScannedAddressConsumed,
    )

    val submittedHash = uiState.submittedHash
    if (submittedHash != null) {
        SendResultContent(
            hash = submittedHash,
            explorerUrl = uiState.chain?.explorerUrl,
            onDone = {
                viewModel.onResultDone()
                onNavigateBack()
            },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.send_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.send_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = NexVaultDimens.spacingMd),
        ) {
            if (showReview) {
                ReviewSection(
                    uiState = uiState,
                    onBackToEdit = { showReview = false },
                    onConfirm = viewModel::onSubmitClicked,
                )
            } else {
                FormSection(
                    onScanClicked = onScanClicked,
                    uiState = uiState,
                    viewModel = viewModel,
                    onReview = { showReview = true },
                )
            }
            Spacer(modifier = Modifier.height(NexVaultDimens.spacingXl))
        }
    }

    if (uiState.showPinDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.send_confirm_dialog_title),
            confirmText = stringResource(R.string.send_confirm_dialog_confirm),
            dismissText = stringResource(R.string.send_confirm_dialog_cancel),
            onConfirm = { },
            onDismiss = viewModel::onPinDialogDismissed,
            confirmEnabled = false,
            body = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.send_confirm_dialog_message),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(NexVaultDimens.spacingMd))
                    PinInputField(
                        onDigitClick = viewModel::onPinDigitPressed,
                        onBackspaceClick = viewModel::onPinBackspacePressed,
                        filledCount = uiState.pin.length,
                        isError = uiState.pinErrorRes != null,
                    )
                    if (uiState.pinErrorRes != null) {
                        Text(
                            text = stringResource(uiState.pinErrorRes!!),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (uiState.isSubmitting) {
                        Text(
                            text = stringResource(R.string.send_submitting),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )
    }
}

/** Fills the recipient field once with the payload the scanner returned. */
@Composable
private fun ScannedAddressEffect(
    scannedAddress: String?,
    viewModel: SendViewModel,
    onConsumed: () -> Unit,
) {
    LaunchedEffect(scannedAddress) {
        if (!scannedAddress.isNullOrBlank()) {
            viewModel.onToAddressChanged(scannedAddress)
            onConsumed()
        }
    }
}

@Composable
private fun FormSection(
    uiState: SendUiState,
    viewModel: SendViewModel,
    onReview: () -> Unit,
    onScanClicked: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(NexVaultDimens.spacingMd))

        // Asset picker: the native coin plus every token on the selected chain.
        Text(
            text = stringResource(R.string.send_token_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.height(NexVaultDimens.spacingSm))
        Row(
            horizontalArrangement = Arrangement.spacedBy(NexVaultDimens.spacingSm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TokenChip(
                label = uiState.chain?.symbol ?: "",
                selected = uiState.selectedTokenAddress == null,
                onClick = { viewModel.onTokenSelected(null) },
            )
            uiState.tokens
                .filter { !it.isNative }
                .forEach { token ->
                    TokenChip(
                        label = token.symbol,
                        selected = uiState.selectedTokenAddress == token.contractAddress,
                        onClick = { viewModel.onTokenSelected(token.contractAddress) },
                    )
                }
        }

        Spacer(modifier = Modifier.height(NexVaultDimens.spacingMd))
        NexVaultTextField(
            value = uiState.toAddress,
            onValueChange = viewModel::onToAddressChanged,
            label = stringResource(R.string.send_to_label),
            placeholder = stringResource(R.string.send_to_placeholder),
            error = uiState.addressErrorRes?.let { stringResource(it) },
            trailingIcon = {
                IconButton(onClick = onScanClicked) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = stringResource(R.string.send_scan_qr),
                    )
                }
            },
        )

        Spacer(modifier = Modifier.height(NexVaultDimens.spacingSm))
        NexVaultTextField(
            value = uiState.amount,
            onValueChange = viewModel::onAmountChanged,
            label = stringResource(R.string.send_amount_label),
            placeholder = stringResource(R.string.send_amount_placeholder),
            error = uiState.amountErrorRes?.let { stringResource(it) },
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
            trailingIcon = {
                TextButton(onClick = viewModel::onMaxClicked) {
                    Text(stringResource(R.string.send_max))
                }
            },
        )

        Spacer(modifier = Modifier.height(NexVaultDimens.spacingMd))
        GasSection(uiState = uiState, onGasSelected = viewModel::onGasSelected)

        if (uiState.isRpcNotConfigured) {
            Spacer(modifier = Modifier.height(NexVaultDimens.spacingMd))
            Text(
                text = stringResource(R.string.send_not_configured),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(NexVaultDimens.spacingSm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(NexVaultDimens.spacingLg))
        NexVaultButton(
            text = stringResource(R.string.send_review_submit),
            onClick = onReview,
            enabled = uiState.isFormValid,
        )
    }
}

@Composable
private fun GasSection(
    uiState: SendUiState,
    onGasSelected: (Int) -> Unit,
) {
    val estimate = uiState.gasEstimate
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.send_gas_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.height(NexVaultDimens.spacingSm))
        if (uiState.isEstimating) {
            Text(
                text = stringResource(R.string.send_estimating),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (estimate != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(NexVaultDimens.spacingSm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf(estimate.slow, estimate.normal, estimate.fast).forEachIndexed { index, option ->
                    FilterChip(
                        selected = uiState.selectedGasIndex == index,
                        onClick = { onGasSelected(index) },
                        label = { Text(option.estimatedTimeName) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(NexVaultDimens.spacingXs))
            uiState.selectedGas?.fiatCost?.let { fiat ->
                Text(
                    text = "≈ $${"%.2f".format(fiat)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReviewSection(
    uiState: SendUiState,
    onBackToEdit: () -> Unit,
    onConfirm: () -> Unit,
) {
    val token = uiState.selectedToken
    val chainSymbol = uiState.chain?.symbol ?: ""
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(NexVaultDimens.spacingMd))
        Text(
            text = stringResource(R.string.send_review_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(NexVaultDimens.spacingMd))
        ReviewRow(label = stringResource(R.string.send_review_recipient), value = uiState.toAddress)
        ReviewRow(
            label = stringResource(R.string.send_review_amount),
            value = "${uiState.amount} ${token?.symbol ?: chainSymbol}",
        )
        ReviewRow(
            label = stringResource(R.string.send_review_asset),
            value = token?.name ?: uiState.chain?.nativeCoinName ?: chainSymbol,
        )
        uiState.selectedGas?.let { gas ->
            ReviewRow(
                label = stringResource(R.string.send_review_fee),
                value = gas.estimatedTimeName +
                    (gas.fiatCost?.let { " - ≈ $${"%.2f".format(it)}" } ?: ""),
            )
        }
        Spacer(modifier = Modifier.height(NexVaultDimens.spacingLg))
        NexVaultButton(
            text = stringResource(R.string.send_confirm_dialog_confirm),
            onClick = onConfirm,
        )
        Spacer(modifier = Modifier.height(NexVaultDimens.spacingSm))
        TextButton(onClick = onBackToEdit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.send_back))
        }
    }
}

@Composable
private fun ReviewRow(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = NexVaultDimens.spacingXs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SendResultContent(
    hash: String,
    explorerUrl: String?,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(NexVaultDimens.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.send_result_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(NexVaultDimens.spacingMd))
        Text(
            text = stringResource(R.string.send_result_pending),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(NexVaultDimens.spacingSm))
        Text(
            text = hash,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(NexVaultDimens.spacingLg))
        if (explorerUrl != null) {
            NexVaultButton(
                text = stringResource(R.string.send_result_view_on_explorer),
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$explorerUrl/tx/$hash")))
                },
                secondary = true,
            )
            Spacer(modifier = Modifier.height(NexVaultDimens.spacingSm))
        }
        NexVaultButton(
            text = stringResource(R.string.send_result_done),
            onClick = onDone,
        )
    }
}

@Composable
private fun TokenChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun SendScreenPreview() {
    NexVaultTheme(darkTheme = true) {
        SendScreen(onNavigateBack = {})
    }
}
