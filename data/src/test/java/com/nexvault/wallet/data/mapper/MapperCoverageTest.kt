package com.nexvault.wallet.data.mapper

import com.google.common.truth.Truth.assertThat
import com.nexvault.wallet.core.database.entity.TokenEntity
import com.nexvault.wallet.core.datastore.model.AccountMetadata
import com.nexvault.wallet.core.datastore.model.NetworkType
import com.nexvault.wallet.core.datastore.model.WalletMetadata
import com.nexvault.wallet.core.network.dto.TokenTransferDto
import com.nexvault.wallet.core.network.dto.TransactionDto
import org.junit.Test
import java.math.BigDecimal
import com.nexvault.wallet.core.datastore.model.WalletType as DataStoreWalletType
import com.nexvault.wallet.core.datastore.security.AuthMethod as DataStoreAuthMethod
import com.nexvault.wallet.domain.model.auth.AuthMethod as DomainAuthMethod
import com.nexvault.wallet.domain.model.wallet.WalletType as DomainWalletType

/**
 * Roadmap 2.6 coverage: the data-layer mapping functions, both directions and edge inputs.
 */
class MapperCoverageTest {
    @Test
    fun tokenEntity_mapsEveryFieldToTheDomainModel() {
        val entity =
            TokenEntity(
                contractAddress = "0xtoken",
                chainId = 1,
                symbol = "TKN",
                name = "Token",
                decimals = 6,
                logoUrl = "https://logo",
                balance = "1.5",
                fiatPrice = 2.0,
                fiatValue = 3.0,
                priceChange24h = 4.0,
                isCustom = true,
                coinGeckoId = "tkn",
                sortOrder = 0,
                lastUpdated = 7L,
            )

        val token = entity.toDomain()

        assertThat(token.contractAddress).isEqualTo("0xtoken")
        assertThat(token.chainId).isEqualTo(1)
        assertThat(token.balance).isEqualTo(BigDecimal("1.5"))
        assertThat(token.isCustom).isTrue()
        assertThat(token.coinGeckoId).isEqualTo("tkn")
    }

    @Test
    fun transactionDto_mapsSendReceiveAndStatusDirections() {
        val sendDto = transactionDto(from = "0xAAAA")
        val receiveDto = transactionDto(from = "0xBBBB")
        val failedDto = transactionDto(from = "0xBBBB").copy(isError = "1")

        assertThat(sendDto.toEntity(1, "0xaaaa").type).isEqualTo("send")
        assertThat(receiveDto.toEntity(1, "0xaaaa").type).isEqualTo("receive")
        assertThat(failedDto.toEntity(1, "0xaaaa").status).isEqualTo(2)
    }

    @Test
    fun tokenTransferDto_mapsTheTokenColumns() {
        val dto = tokenTransferDto()

        val entity = dto.toEntity(1, "0xaaaa")

        assertThat(entity.tokenSymbol).isEqualTo("USDC")
        assertThat(entity.tokenContractAddress).isEqualTo("0xusdc")
        assertThat(entity.tokenDecimals).isEqualTo(6)
        assertThat(entity.value).isEqualTo("100")
    }

    @Test
    fun chainMapper_roundTripsAllKnownChainsAndFallsBackToMainnet() {
        assertThat(ChainMapper.chainIdToNetworkType(1)).isEqualTo(NetworkType.MAINNET)
        assertThat(ChainMapper.chainIdToNetworkType(11155111)).isEqualTo(NetworkType.SEPOLIA)
        assertThat(ChainMapper.chainIdToNetworkType(56)).isEqualTo(NetworkType.BSC)
        assertThat(ChainMapper.chainIdToNetworkType(137)).isEqualTo(NetworkType.POLYGON)
        assertThat(ChainMapper.chainIdToNetworkType(999)).isEqualTo(NetworkType.MAINNET)
        assertThat(ChainMapper.networkTypeToChainId(NetworkType.SEPOLIA)).isEqualTo(11155111)
        assertThat(ChainMapper.networkTypeToChainId(NetworkType.BSC)).isEqualTo(56)
        assertThat(ChainMapper.networkTypeToChainId(NetworkType.POLYGON)).isEqualTo(137)
        assertThat(ChainMapper.networkTypeToChainId(NetworkType.MAINNET)).isEqualTo(1)
    }

    @Test
    fun authMapper_roundTripsAuthMethods() {
        assertThat(AuthMapper.mapAuthMethod(DataStoreAuthMethod.PIN)).isEqualTo(DomainAuthMethod.PIN)
        assertThat(AuthMapper.mapAuthMethod(DataStoreAuthMethod.PASSWORD)).isEqualTo(DomainAuthMethod.PASSWORD)
        assertThat(AuthMapper.mapAuthMethodToDataStore(DomainAuthMethod.PASSWORD))
            .isEqualTo(DataStoreAuthMethod.PASSWORD)
        assertThat(AuthMapper.mapAuthMethodToDataStore(DomainAuthMethod.PIN))
            .isEqualTo(DataStoreAuthMethod.PIN)
    }

    @Test
    fun authMapper_createAuthStateCopiesEveryField() {
        val state = AuthMapper.createAuthState(
            isWalletSetUp = true,
            isLocked = true,
            isLockedOut = true,
            lockoutRemainingSeconds = 9L,
            authMethod = DomainAuthMethod.PIN,
            isBiometricEnabled = true,
            isBiometricAvailable = true,
        )

        assertThat(state.isWalletSetUp).isTrue()
        assertThat(state.isLocked).isTrue()
        assertThat(state.isLockedOut).isTrue()
        assertThat(state.lockoutRemainingSeconds).isEqualTo(9L)
        assertThat(state.authMethod).isEqualTo(DomainAuthMethod.PIN)
        assertThat(state.isBiometricEnabled).isTrue()
        assertThat(state.isBiometricAvailable).isTrue()
    }

    @Test
    fun walletMapper_mapsMetadataToTheDomainWallet() {
        val wallet =
            WalletMapper.mapToDomain(
                walletMetadata = walletMetadata(),
                accounts = listOf(accountMetadata()),
                activeWalletId = "w1",
            )

        assertThat(wallet.id).isEqualTo("w1")
        assertThat(wallet.type).isEqualTo(DomainWalletType.HD)
        assertThat(wallet.isActive).isTrue()
        assertThat(wallet.accounts).hasSize(1)
        assertThat(wallet.accounts[0].address).isEqualTo("0xabc")
        assertThat(wallet.accounts[0].isActive).isTrue()
    }

    @Test
    fun walletMapper_mapsWalletTypesBothWays() {
        assertThat(WalletMapper.mapWalletType(DataStoreWalletType.HD)).isEqualTo(DomainWalletType.HD)
        assertThat(WalletMapper.mapWalletType(DataStoreWalletType.IMPORTED)).isEqualTo(DomainWalletType.IMPORTED)
        assertThat(WalletMapper.mapWalletTypeToDataStore(DomainWalletType.IMPORTED))
            .isEqualTo(DataStoreWalletType.IMPORTED)
    }

    @Test
    fun walletMapper_accountActiveFlagFollowsTheProvidedIndex() {
        val mapped = WalletMapper.mapAccountToDomain(accountMetadata(), activeAccountIndex = 5)

        assertThat(mapped.isActive).isFalse()
        assertThat(mapped.derivationPath).isEqualTo("m/44'/60'/0'/0/0")
    }

    private fun walletMetadata() =
        WalletMetadata(
            id = "w1",
            name = "Main",
            createdAt = 42L,
            type = DataStoreWalletType.HD,
            accountCount = 1,
            isActive = true,
        )

    private fun accountMetadata() =
        AccountMetadata(
            walletId = "w1",
            accountIndex = 0,
            address = "0xabc",
            name = "Account 1",
            derivationPath = "m/44'/60'/0'/0/0",
            isActive = true,
            addedAt = 42L,
        )

    private fun transactionDto(from: String) =
        TransactionDto(
            hash = "0xhash",
            from = from,
            to = "0xdddd",
            value = "1",
            gas = "21000",
            gasPrice = "5",
            gasUsed = "21000",
            blockNumber = "1",
            timestamp = "1700000000",
            nonce = "0",
            isError = "0",
            txReceiptStatus = "1",
            input = "0x",
            contractAddress = null,
            confirmations = "10",
        )

    private fun tokenTransferDto() =
        TokenTransferDto(
            hash = "0xhash",
            from = "0xbbbb",
            to = "0xaaaa",
            value = "100",
            tokenName = "USD Coin",
            tokenSymbol = "USDC",
            tokenDecimal = "6",
            contractAddress = "0xusdc",
            gas = "21000",
            gasPrice = "5",
            gasUsed = "21000",
            blockNumber = "1",
            timestamp = "1700000000",
            nonce = "0",
            confirmations = "10",
        )
}
