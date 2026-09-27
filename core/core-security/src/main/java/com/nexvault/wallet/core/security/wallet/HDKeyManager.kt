package com.nexvault.wallet.core.security.wallet

import com.nexvault.wallet.core.security.util.SecureUtils.secureWipe
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HDKeyManager @Inject constructor() {
    companion object {
        const val BIP44_ETHEREUM_PATH = "m/44'/60'/0'/0/0"
        private const val HARDENED_BIT = 0x80000000.toInt()
    }

    fun deriveEthereumKeyPair(
        seed: ByteArray,
        accountIndex: Int = 0,
        addressIndex: Int = 0,
    ): ECKeyPair {
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)

        // BIP44 path: m/44'/60'/account'/0/address
        val path = intArrayOf(
            (44 or HARDENED_BIT).toInt(), // purpose - BIP44
            (60 or HARDENED_BIT).toInt(), // coin_type - Ethereum
            (accountIndex or HARDENED_BIT).toInt(), // account
            0, // change (external)
            addressIndex, // address_index
        )

        val bip44KeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path)

        // Create a new ECKeyPair with the derived keys
        val privateKey = bip44KeyPair.privateKey
        val keyPair = ECKeyPair.create(privateKey)

        // Secure wipe the sensitive key bytes
        privateKey.toByteArray().secureWipe()

        return keyPair
    }

    fun deriveAddress(keyPair: ECKeyPair): String {
        return Keys.toChecksumAddress(Keys.getAddress(keyPair))
    }

    fun deriveAddresses(
        seed: ByteArray,
        accountIndex: Int = 0,
        count: Int = 5,
    ): List<DerivedAddress> {
        return (0 until count).map { index ->
            val keyPair = deriveEthereumKeyPair(seed, accountIndex, index)
            val address = deriveAddress(keyPair)
            val path = "m/44'/60'/$accountIndex'/0/$index"

            DerivedAddress(address, path, index)
        }
    }

    fun getPrivateKeyBytes(keyPair: ECKeyPair): ByteArray {
        return keyPair.privateKey.toByteArray()
    }

    fun getPublicKeyBytes(keyPair: ECKeyPair): ByteArray {
        return keyPair.publicKey.toByteArray()
    }
}

data class DerivedAddress(
    val address: String,
    val path: String,
    val index: Int,
)
