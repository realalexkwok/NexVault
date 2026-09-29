package com.nexvault.wallet.core.ui.util

import com.google.common.truth.Truth.assertThat
import com.nexvault.wallet.core.ui.R
import org.junit.Test
import java.math.BigDecimal

/**
 * Roadmap 2.6 coverage: the formatting helpers the transaction and portfolio UI lean on.
 */
class FormatUtilsTest {
    @Test
    fun formatFiatValue_groupsAndFixesTwoDecimals() {
        assertThat(formatFiatValue(1234.5)).isEqualTo("\$1,234.50")
        assertThat(formatFiatValue(0.0)).isEqualTo("\$0.00")
    }

    @Test
    fun formatTokenBalance_trimsExcessDecimalsButKeepsShortScales() {
        assertThat(formatTokenBalance(BigDecimal("1.234567890"))).isEqualTo("1.234567")
        assertThat(formatTokenBalance(BigDecimal("1.5"))).isEqualTo("1.5")
        assertThat(formatTokenBalance(BigDecimal("100"))).isEqualTo("100")
    }

    @Test
    fun formatTransactionAmount_respectsTheMaxFractionDigits() {
        assertThat(formatTransactionAmount(BigDecimal("1.23456789"))).isEqualTo("1.234567")
        assertThat(formatTransactionAmount(BigDecimal("1.23456789"), 2)).isEqualTo("1.23")
        assertThat(formatTransactionAmount(BigDecimal("2.0000"))).isEqualTo("2")
    }

    @Test
    fun truncateAddress_shortensLongAddressesAndKeepsShortStrings() {
        assertThat(truncateAddress("0x1234567890abcdef1234567890abcdef12345678"))
            .isEqualTo("0x1234...5678")
        assertThat(truncateAddress("0xabc")).isEqualTo("0xabc")
    }

    @Test
    fun formatTimestamp_usesMonthDayFormat() {
        // 2021-01-15T00:00:00Z; the exact text is locale-dependent, so assert shape only.
        val formatted = formatTimestamp(1610668800L)
        assertThat(formatted).isNotEmpty()
        assertThat(formatted).doesNotContain("/")
    }

    @Test
    fun chainIconMapper_mapsKnownChainsAndDefaultsToEthereum() {
        assertThat(ChainIconMapper.getIconRes(1)).isEqualTo(R.drawable.ic_chain_ethereum)
        assertThat(ChainIconMapper.getIconRes(11155111)).isEqualTo(R.drawable.ic_chain_sepolia)
        assertThat(ChainIconMapper.getIconRes(56)).isEqualTo(R.drawable.ic_chain_bsc)
        assertThat(ChainIconMapper.getIconRes(137)).isEqualTo(R.drawable.ic_chain_polygon)
        assertThat(ChainIconMapper.getIconRes(999)).isEqualTo(R.drawable.ic_chain_ethereum)
    }
}
