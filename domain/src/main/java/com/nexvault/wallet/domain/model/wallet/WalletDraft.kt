package com.nexvault.wallet.domain.model.wallet

/**
 * A freshly generated wallet that has **not** been persisted yet.
 *
 * Exists so the onboarding UI can show the recovery phrase and obtain the user's
 * acknowledgment before anything is written to disk (roadmap 2.0.2b, option B):
 * no wallet exists until the user consents to the backup.
 *
 * @property mnemonic The full BIP-39 phrase (space separated) — only the words are displayed.
 * @property mnemonicWords The phrase split into words for the 4×3 grid.
 * @property address The first derived account address (m/44'/60'/0'/0/0).
 */
data class WalletDraft(
    val mnemonic: String,
    val mnemonicWords: List<String>,
    val address: String,
)
