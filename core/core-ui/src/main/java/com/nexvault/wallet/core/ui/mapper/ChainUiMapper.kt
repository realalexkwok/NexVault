package com.nexvault.wallet.core.ui.mapper

import com.nexvault.wallet.core.ui.components.ChainUi
import com.nexvault.wallet.core.ui.util.ChainIconMapper
import com.nexvault.wallet.domain.model.chain.Chain

/**
 * Maps a domain [Chain] to a [ChainUi] presentation model.
 *
 * Resolves the chain icon drawable resource using [ChainIconMapper].
 */
fun Chain.toChainUi(): ChainUi {
    return ChainUi(
        chainId = chainId,
        name = name,
        symbol = symbol,
        iconRes = ChainIconMapper.getIconRes(chainId),
        isTestnet = isTestnet,
    )
}

/**
 * Maps a list of domain [Chain] objects to [ChainUi] list.
 */
fun List<Chain>.toChainUiList(): List<ChainUi> {
    return map { it.toChainUi() }
}
