package com.nexvault.wallet.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nexvault.wallet.core.ui.theme.NexVaultDimens
import com.nexvault.wallet.feature.home.R

/**
 * Dialog to add a custom ERC-20 token by contract address.
 */
@Composable
fun AddTokenDialog(
    isLoading: Boolean,
    @StringRes errorRes: Int?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var contractAddress by remember { mutableStateOf("") }
    val errorText = errorRes?.let { stringResource(it) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(stringResource(R.string.home_add_token_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.home_add_token_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(NexVaultDimens.spacing12))
                OutlinedTextField(
                    value = contractAddress,
                    onValueChange = { contractAddress = it.trim() },
                    label = { Text(stringResource(R.string.home_add_token_contract_label)) },
                    placeholder = { Text(stringResource(R.string.home_add_token_contract_placeholder)) },
                    singleLine = true,
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.height(NexVaultDimens.spacingSm))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(contractAddress) },
                enabled = contractAddress.length >= 42 && contractAddress.startsWith("0x") && !isLoading,
            ) {
                Text(stringResource(R.string.home_add_token_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
            ) {
                Text(stringResource(R.string.home_add_token_cancel))
            }
        },
    )
}
