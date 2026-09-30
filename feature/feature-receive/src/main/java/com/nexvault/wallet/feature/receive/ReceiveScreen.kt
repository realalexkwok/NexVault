package com.nexvault.wallet.feature.receive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexvault.wallet.core.ui.components.ChainBadge
import com.nexvault.wallet.core.ui.components.NexVaultButton
import com.nexvault.wallet.core.ui.components.NexVaultTopBar
import com.nexvault.wallet.core.ui.components.QrCodeImage
import com.nexvault.wallet.core.ui.theme.NexVaultDimens
import kotlinx.coroutines.launch

/** Test tag for the address text, so UI tests do not depend on the address value. */
const val RECEIVE_ADDRESS_TAG = "receive_address"

/**
 * Receive screen (roadmap 2.7, AC-2.7): chain badge, the wallet address as a QR code, the full
 * copyable address, copy/share actions and the network-compatibility warning.
 */
@Composable
fun ReceiveScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReceiveViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.receive_copied)

    Scaffold(
        topBar = {
            NexVaultTopBar(
                title = stringResource(R.string.receive_title),
                showBackButton = true,
                onBackClick = onNavigateBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = NexVaultDimens.spacingLg)
                    .verticalScroll(rememberScrollState()),
        ) {
            uiState.chain?.let { chain ->
                ChainBadge(
                    chainName = chain.name,
                    chainIconRes = chain.iconRes,
                    isTestnet = chain.isTestnet,
                    modifier = Modifier.padding(vertical = NexVaultDimens.spacingMd),
                )
            }

            if (uiState.hasAddress) {
                QrCodeImage(
                    content = uiState.address,
                    size = 240.dp,
                    modifier = Modifier.padding(vertical = NexVaultDimens.spacingMd),
                )

                Text(
                    text = uiState.address,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag(RECEIVE_ADDRESS_TAG),
                )

                Spacer(modifier = Modifier.height(NexVaultDimens.spacingLg))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(NexVaultDimens.spacingMd),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    NexVaultButton(
                        text = stringResource(R.string.receive_copy),
                        secondary = true,
                        onClick = {
                            copyToClipboard(context, uiState.address)
                            scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    NexVaultButton(
                        text = stringResource(R.string.receive_share),
                        onClick = { shareAddress(context, uiState.address) },
                        modifier = Modifier.weight(1f),
                    )
                }
            } else if (!uiState.isLoading) {
                Text(
                    text =
                        when (uiState.error) {
                            ReceiveError.LOAD_FAILED -> stringResource(R.string.receive_load_failed)
                            ReceiveError.NO_ACTIVE_WALLET, null -> stringResource(R.string.receive_no_wallet)
                        },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = NexVaultDimens.spacingXl),
                )
            }

            NetworkCompatibilityWarning(
                isTestnet = uiState.chain?.isTestnet == true,
                chainName = uiState.chain?.name.orEmpty(),
                modifier = Modifier.padding(vertical = NexVaultDimens.spacingMd),
            )
        }
    }
}

/**
 * AC-2.7's network warning: testnets differ, and the same address string is not portable across
 * chains the app does not support.
 */
@Composable
private fun NetworkCompatibilityWarning(
    isTestnet: Boolean,
    chainName: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(NexVaultDimens.spacingSm),
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(NexVaultDimens.iconSizeSmall),
        )
        Text(
            text =
                if (isTestnet) {
                    stringResource(R.string.receive_warning_testnet, chainName)
                } else {
                    stringResource(R.string.receive_warning_network, chainName)
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun copyToClipboard(context: Context, address: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Wallet address", address))
}

private fun shareAddress(context: Context, address: String) {
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, address)
        }
    context.startActivity(Intent.createChooser(intent, null))
}
