package com.nexvault.wallet.core.datastore.model

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Networks that can be persisted as the selected chain.
 *
 * Mirrors the four supported chains (Ethereum mainnet, Sepolia, BNB Smart Chain, Polygon) so
 * the selection round-trips through storage. Values that no longer exist — e.g. a legacy
 * GOERLI or CUSTOM entry — degrade to [MAINNET] when read.
 */
enum class NetworkType {
    MAINNET,
    SEPOLIA,
    BSC,
    POLYGON
}

enum class AutoLockTimeout(val seconds: Long) {
    IMMEDIATE(0),
    ONE_MINUTE(60),
    FIVE_MINUTES(300),
    FIFTEEN_MINUTES(900),
    THIRTY_MINUTES(1800),
    NEVER(-1);

    companion object {
        fun fromSeconds(seconds: Long): AutoLockTimeout {
            return entries.find { it.seconds == seconds } ?: FIVE_MINUTES
        }
    }
}
