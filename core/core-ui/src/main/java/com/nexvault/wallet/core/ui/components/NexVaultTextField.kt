package com.nexvault.wallet.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.nexvault.wallet.core.ui.preview.ThemePreviewWrapper
import com.nexvault.wallet.core.ui.theme.NexVaultDimens
import com.nexvault.wallet.core.ui.theme.NexVaultTheme

@Composable
fun NexVaultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    error: String? = null,
    prefix: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    enabled: Boolean = true,
) {
    val colors = NexVaultTheme.colors

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(NexVaultDimens.inputHeight),
            label = label?.let { { Text(it) } },
            placeholder = placeholder?.let { { Text(it, color = colors.textMedium) } },
            prefix = prefix?.let { { Text(it, color = colors.textMedium) } },
            trailingIcon = trailingIcon,
            isError = error != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = singleLine,
            enabled = enabled,
            shape = RoundedCornerShape(NexVaultDimens.cornerRadiusMedium),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = colors.cardBorder,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = colors.textMedium,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = colors.textHigh,
                unfocusedTextColor = colors.textHigh,
                disabledTextColor = colors.textDisabled,
                disabledBorderColor = colors.textDisabled,
                disabledLabelColor = colors.textDisabled,
            ),
        )
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = NexVaultDimens.spacingMd, top = NexVaultDimens.spacingXs),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NexVaultTextFieldPreview() {
    ThemePreviewWrapper {
        NexVaultTextField(
            value = "0.5",
            onValueChange = {},
            label = "Amount",
            placeholder = "0.0",
        )
    }
}
