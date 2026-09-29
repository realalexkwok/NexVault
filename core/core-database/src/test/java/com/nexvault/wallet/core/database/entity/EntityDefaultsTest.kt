package com.nexvault.wallet.core.database.entity

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Roadmap 2.6 coverage: the Room entity shapes and copy semantics, exercised on the JVM without
 * Robolectric. The DAO behavior itself is covered by the existing Robolectric DAO tests.
 */
class EntityDefaultsTest {
    @Test
    fun tokenEntity_roundTripsItsFields() {
        val entity =
            TokenEntity(
                contractAddress = "0xtoken",
                chainId = 1,
                symbol = "TKN",
                name = "Token",
                decimals = 18,
                logoUrl = null,
                balance = "0",
                fiatPrice = null,
                fiatValue = null,
                priceChange24h = null,
                isCustom = false,
                coinGeckoId = null,
                sortOrder = 0,
                lastUpdated = 42L,
            )

        assertThat(entity.balance).isEqualTo("0")
        assertThat(entity.fiatPrice).isNull()
        assertThat(entity.isCustom).isFalse()
        assertThat(entity.sortOrder).isEqualTo(0)
        assertThat(entity.logoUrl).isNull()
        assertThat(entity.copy(symbol = "TKN2").contractAddress).isEqualTo("0xtoken")
    }

    @Test
    fun transactionEntity_roundTripsItsFields() {
        val entity =
            TransactionEntity(
                txHash = "0xhash",
                chainId = 1,
                fromAddress = "0xfrom",
                toAddress = "0xto",
                value = "1.5",
                gasUsed = null,
                gasPrice = "5",
                tokenSymbol = null,
                tokenContractAddress = null,
                tokenDecimals = null,
                blockNumber = 0L,
                timestamp = 1_700_000_000L,
                status = 0,
                type = "send",
            )

        assertThat(entity.status).isEqualTo(0)
        assertThat(entity.blockNumber).isEqualTo(0L)
        assertThat(entity.type).isEqualTo("send")
        assertThat(entity.copy(status = 1).status).isEqualTo(1)
    }

    @Test
    fun nftEntity_roundTripsItsFields() {
        val entity =
            NftEntity(
                contractAddress = "0xnft",
                tokenId = "1",
                chainId = 1,
                name = "Nft",
                description = null,
                imageUrl = null,
                animationUrl = null,
                collectionName = null,
                standard = "ERC-721",
                attributes = null,
                lastUpdated = 0L,
            )

        assertThat(entity.name).isEqualTo("Nft")
        assertThat(entity.chainId).isEqualTo(1)
        assertThat(entity.standard).isEqualTo("ERC-721")
    }

    @Test
    fun addressBookEntity_copyPreservesTheAddress() {
        val entity =
            AddressBookEntity(
                address = "0xabc",
                name = "Alice",
                chainId = 1,
                createdAt = 42L,
            )

        assertThat(entity.copy(name = "Bob").address).isEqualTo("0xabc")
        assertThat(entity.createdAt).isEqualTo(42L)
    }
}
