package com.nexvault.wallet.core.datastore.model

import kotlinx.serialization.Serializable

@Serializable
data class WalletMetadata(
    val id: String,
    val name: String,
    val createdAt: Long,
    val type: WalletType,
    val accountCount: Int,
    val isActive: Boolean,
)

@Serializable
enum class WalletType {
    HD,
    IMPORTED,
}

@Serializable
data class AccountMetadata(
    val walletId: String,
    val accountIndex: Int,
    val address: String,
    val name: String,
    val derivationPath: String,
    val isActive: Boolean,
    val addedAt: Long,
)
