package com.nexvault.wallet.data.repository

import com.nexvault.wallet.core.datastore.model.AccountMetadata
import com.nexvault.wallet.core.datastore.model.WalletMetadata
import com.nexvault.wallet.core.datastore.model.WalletType as DataStoreWalletType
import com.nexvault.wallet.core.datastore.security.SecurityPreferencesDataStore
import com.nexvault.wallet.core.datastore.wallet.WalletMetadataDataStore
import com.nexvault.wallet.core.security.mnemonic.MnemonicManager
import com.nexvault.wallet.core.security.util.SecurityUtils
import com.nexvault.wallet.core.security.util.SecureUtils.secureWipe
import com.nexvault.wallet.core.security.wallet.HDKeyManager
import com.nexvault.wallet.core.security.wallet.WalletStore
import com.nexvault.wallet.data.mapper.WalletMapper
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.EncryptionException
import com.nexvault.wallet.domain.model.common.InvalidMnemonicException
import com.nexvault.wallet.domain.model.common.InvalidPrivateKeyException
import com.nexvault.wallet.domain.model.common.WalletNotFoundException
import com.nexvault.wallet.domain.model.wallet.Account
import com.nexvault.wallet.domain.model.wallet.Wallet
import com.nexvault.wallet.domain.model.wallet.WalletType
import com.nexvault.wallet.domain.model.auth.WalletCreationResult
import com.nexvault.wallet.domain.model.chain.SupportedChains
import com.nexvault.wallet.domain.repository.TokenRepository
import com.nexvault.wallet.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import java.math.BigInteger
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val mnemonicManager: MnemonicManager,
    private val hdKeyManager: HDKeyManager,
    private val walletStore: WalletStore,
    private val securityPreferences: SecurityPreferencesDataStore,
    private val walletMetadataStore: WalletMetadataDataStore,
    private val tokenRepository: TokenRepository,
) : WalletRepository {

    companion object {
        private const val DEFAULT_DERIVATION_PATH = "m/44'/60'/0'/0/0"
        private const val DERIVATION_PATH_PREFIX = "m/44'/60'/0'/0/"
        private const val DEFAULT_ACCOUNT_NAME = "Account 1"
    }

    override suspend fun createWallet(walletName: String): DataResult<WalletCreationResult> {
        return try {
            val mnemonic = mnemonicManager.generateMnemonic()
            val seed = mnemonicManager.mnemonicToSeed(mnemonic, "")

            val keyPair = hdKeyManager.deriveEthereumKeyPair(seed, 0, 0)
            val address = hdKeyManager.deriveAddress(keyPair)

            seed.secureWipe()

            val walletId = UUID.randomUUID().toString()

            walletStore.storeMnemonic(mnemonic, walletId)

            val walletMetadata = WalletMetadata(
                id = walletId,
                name = walletName,
                createdAt = System.currentTimeMillis(),
                type = DataStoreWalletType.HD,
                accountCount = 1,
                isActive = true,
            )
            walletMetadataStore.addWallet(walletMetadata)

            val accountMetadata = AccountMetadata(
                walletId = walletId,
                accountIndex = 0,
                address = address,
                name = DEFAULT_ACCOUNT_NAME,
                derivationPath = DEFAULT_DERIVATION_PATH,
                isActive = true,
                addedAt = System.currentTimeMillis(),
            )
            walletMetadataStore.addAccount(accountMetadata)

            securityPreferences.setActiveWalletId(walletId)
            securityPreferences.setActiveAccountIndex(0)
            securityPreferences.setWalletSetUp(true)

            seedDefaultTokensForSupportedChains()

            val mnemonicWords = mnemonic.split(" ")

            DataResult.Success(
                WalletCreationResult(
                    walletId = walletId,
                    address = address,
                    mnemonicWords = mnemonicWords,
                )
            )
        } catch (e: Exception) {
            DataResult.Error(EncryptionException("Failed to create wallet: ${e.message}", e))
        }
    }

    override suspend fun importFromMnemonic(
        mnemonic: String,
        walletName: String,
    ): DataResult<WalletCreationResult> {
        return try {
            val isValid = mnemonicManager.validateMnemonic(mnemonic)
            if (!isValid) {
                return DataResult.Error(InvalidMnemonicException())
            }

            val seed = mnemonicManager.mnemonicToSeed(mnemonic, "")
            val keyPair = hdKeyManager.deriveEthereumKeyPair(seed, 0, 0)
            val address = hdKeyManager.deriveAddress(keyPair)

            seed.secureWipe()

            val walletId = UUID.randomUUID().toString()

            walletStore.storeMnemonic(mnemonic, walletId)

            val walletMetadata = WalletMetadata(
                id = walletId,
                name = walletName,
                createdAt = System.currentTimeMillis(),
                type = DataStoreWalletType.HD,
                accountCount = 1,
                isActive = true,
            )
            walletMetadataStore.addWallet(walletMetadata)

            val accountMetadata = AccountMetadata(
                walletId = walletId,
                accountIndex = 0,
                address = address,
                name = DEFAULT_ACCOUNT_NAME,
                derivationPath = DEFAULT_DERIVATION_PATH,
                isActive = true,
                addedAt = System.currentTimeMillis(),
            )
            walletMetadataStore.addAccount(accountMetadata)

            securityPreferences.setActiveWalletId(walletId)
            securityPreferences.setActiveAccountIndex(0)
            securityPreferences.setWalletSetUp(true)

            seedDefaultTokensForSupportedChains()

            val mnemonicWords = mnemonic.split(" ")

            DataResult.Success(
                WalletCreationResult(
                    walletId = walletId,
                    address = address,
                    mnemonicWords = mnemonicWords,
                )
            )
        } catch (e: Exception) {
            DataResult.Error(EncryptionException("Failed to import wallet: ${e.message}", e))
        }
    }

    override suspend fun importFromPrivateKey(
        privateKey: String,
        walletName: String,
    ): DataResult<WalletCreationResult> {
        val privateKeyBytes = hexStringToByteArray(privateKey)
        return try {
            val keyPair = ECKeyPair.create(BigInteger(1, privateKeyBytes))
            val address = Keys.toChecksumAddress(Keys.getAddress(keyPair))

            val walletId = UUID.randomUUID().toString()

            // Store first, wipe afterwards: wiping before this call persisted an all-zero key,
            // so the imported wallet could never sign (CR 1.6-2). [WalletStore.storePrivateKey]
            // also zeroes the array once encryption is done; this finally is the belt-and-braces
            // wipe for every exit path (including failures before the store).
            walletStore.storePrivateKey(address, privateKeyBytes, walletId)

            val walletMetadata = WalletMetadata(
                id = walletId,
                name = walletName,
                createdAt = System.currentTimeMillis(),
                type = DataStoreWalletType.IMPORTED,
                accountCount = 1,
                isActive = true,
            )
            walletMetadataStore.addWallet(walletMetadata)

            val accountMetadata = AccountMetadata(
                walletId = walletId,
                accountIndex = 0,
                address = address,
                name = DEFAULT_ACCOUNT_NAME,
                derivationPath = "",
                isActive = true,
                addedAt = System.currentTimeMillis(),
            )
            walletMetadataStore.addAccount(accountMetadata)

            securityPreferences.setActiveWalletId(walletId)
            securityPreferences.setActiveAccountIndex(0)
            securityPreferences.setWalletSetUp(true)

            seedDefaultTokensForSupportedChains()

            DataResult.Success(
                WalletCreationResult(
                    walletId = walletId,
                    address = address,
                    mnemonicWords = emptyList(),
                )
            )
        } catch (e: Exception) {
            DataResult.Error(
                InvalidPrivateKeyException("Failed to import private key: ${e.message}"),
            )
        } finally {
            privateKeyBytes.secureWipe()
        }
    }

    private suspend fun seedDefaultTokensForSupportedChains() {
        SupportedChains.all().forEach { chain ->
            tokenRepository.seedDefaultTokens(chain.chainId)
        }
    }

    override fun getWallets(): Flow<List<Wallet>> {
        return combine(
            walletMetadataStore.wallets,
            securityPreferences.activeWalletId,
        ) { walletMetadataList, activeWalletId ->
            walletMetadataList.map { walletMeta ->
                val accounts = walletMetadataStore.accountsForWallet(walletMeta.id).first()
                WalletMapper.mapToDomain(
                    walletMetadata = walletMeta,
                    accounts = accounts,
                    activeWalletId = activeWalletId,
                )
            }
        }
    }

    override fun getActiveWallet(): Flow<Wallet?> {
        return combine(
            walletMetadataStore.wallets,
            securityPreferences.activeWalletId,
            securityPreferences.activeAccountIndex,
        ) { wallets, activeId, activeAccountIdx ->
            val activeWalletMeta = wallets.find { it.id == activeId } ?: return@combine null
            val accounts = walletMetadataStore.accountsForWallet(activeId).first()
            val wallet = WalletMapper.mapToDomain(
                walletMetadata = activeWalletMeta,
                accounts = accounts,
                activeWalletId = activeId,
            )
            wallet.copy(
                accounts = wallet.accounts.map { account ->
                    account.copy(isActive = account.index == activeAccountIdx)
                }
            )
        }
    }

    override suspend fun setActiveWallet(walletId: String): DataResult<Unit> {
        return try {
            securityPreferences.setActiveWalletId(walletId)
            securityPreferences.setActiveAccountIndex(0)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e, "Failed to set active wallet")
        }
    }

    override suspend fun addAccount(
        walletId: String,
        accountName: String,
    ): DataResult<Account> {
        return try {
            val mnemonic = walletStore.retrieveMnemonic(walletId)

            val existingAccounts = walletMetadataStore.accountsForWallet(walletId).first()
            val nextIndex = existingAccounts.size

            val seed = mnemonicManager.mnemonicToSeed(mnemonic, "")
            val keyPair = hdKeyManager.deriveEthereumKeyPair(seed, 0, nextIndex)
            val address = hdKeyManager.deriveAddress(keyPair)

            seed.secureWipe()

            val derivationPath = "$DERIVATION_PATH_PREFIX$nextIndex"

            val accountMetadata = AccountMetadata(
                walletId = walletId,
                accountIndex = nextIndex,
                address = address,
                name = accountName,
                derivationPath = derivationPath,
                isActive = false,
                addedAt = System.currentTimeMillis(),
            )
            walletMetadataStore.addAccount(accountMetadata)

            val account = Account(
                walletId = walletId,
                index = nextIndex,
                address = address,
                name = accountName,
                derivationPath = derivationPath,
                isActive = false,
            )

            DataResult.Success(account)
        } catch (e: Exception) {
            DataResult.Error(e, "Failed to add account: ${e.message}")
        }
    }

    override fun getActiveAddress(): Flow<String?> {
        return getActiveWallet().map { wallet ->
            wallet?.accounts?.find { it.isActive }?.address
                ?: wallet?.accounts?.firstOrNull()?.address
        }
    }

    override suspend fun getMnemonicForBackup(walletId: String): DataResult<List<String>> {
        return try {
            val mnemonic = walletStore.retrieveMnemonic(walletId)
            DataResult.Success(mnemonic.split(" "))
        } catch (e: IllegalStateException) {
            DataResult.Error(WalletNotFoundException())
        } catch (e: Exception) {
            DataResult.Error(EncryptionException("Failed to retrieve mnemonic: ${e.message}", e))
        }
    }

    override suspend fun deleteWallet(walletId: String): DataResult<Unit> {
        return try {
            walletStore.wipeWalletData(walletId)

            walletMetadataStore.removeWallet(walletId)

            val remainingWallets = walletMetadataStore.wallets.first()
            if (remainingWallets.isEmpty()) {
                securityPreferences.setWalletSetUp(false)
            }
            securityPreferences.setActiveWalletId("")
            securityPreferences.setActiveAccountIndex(0)

            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e, "Failed to delete wallet")
        }
    }

    override suspend fun deleteAllWallets(): DataResult<Unit> {
        return try {
            walletStore.wipeAll()

            walletMetadataStore.clearAll()

            securityPreferences.setWalletSetUp(false)
            securityPreferences.setActiveWalletId("")
            securityPreferences.setActiveAccountIndex(0)

            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e, "Failed to delete all wallets")
        }
    }

    override fun hasWallet(): Flow<Boolean> {
        return securityPreferences.isWalletSetUp
    }

    private fun hexStringToByteArray(hex: String): ByteArray {
        val cleaned = hex.removePrefix("0x").removePrefix("0X")
        return ByteArray(cleaned.length / 2) { i ->
            cleaned.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
