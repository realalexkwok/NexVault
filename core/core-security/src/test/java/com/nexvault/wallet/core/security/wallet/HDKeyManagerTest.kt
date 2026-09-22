package com.nexvault.wallet.core.security.wallet

import com.nexvault.wallet.core.security.util.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HDKeyManagerTest {

    private val hdKeyManager = HDKeyManager()

    // BIP39 test vector mnemonic
    private val testMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"

    @Test
    fun testDeriveAddressFromKnownMnemonic() {
        // Known test vector: "abandon … about" at m/44'/60'/0'/0/0
        val seed = org.web3j.crypto.MnemonicUtils.generateSeed(testMnemonic, "")
        val keyPair = hdKeyManager.deriveEthereumKeyPair(seed, 0, 0)
        val address = hdKeyManager.deriveAddress(keyPair)

        assertEquals("0x9858EfFD232B4033E47d90003D41EC34EcaEda94", address)
    }

    @Test
    fun testDeriveMultipleAddresses() {
        val seed = org.web3j.crypto.MnemonicUtils.generateSeed(testMnemonic, "")
        val addresses = hdKeyManager.deriveAddresses(seed, 0, 5)

        assertEquals(5, addresses.size)

        // All addresses should be unique
        val addressSet = addresses.map { it.address }.toSet()
        assertEquals(5, addressSet.size)

        // All should be valid Ethereum addresses
        addresses.forEach { derived ->
            assertTrue(SecurityUtils.isValidEthereumAddress(derived.address))
        }
    }

    @Test
    fun testDifferentAccountIndices() {
        val seed = org.web3j.crypto.MnemonicUtils.generateSeed(testMnemonic, "")

        val addr1 = hdKeyManager.deriveAddress(hdKeyManager.deriveEthereumKeyPair(seed, 0, 0))
        val addr2 = hdKeyManager.deriveAddress(hdKeyManager.deriveEthereumKeyPair(seed, 1, 0))
        val addr3 = hdKeyManager.deriveAddress(hdKeyManager.deriveEthereumKeyPair(seed, 2, 0))

        assertNotEquals(addr1, addr2)
        assertNotEquals(addr2, addr3)
        assertNotEquals(addr1, addr3)
    }

    @Test
    fun testDifferentAddressIndices() {
        val seed = org.web3j.crypto.MnemonicUtils.generateSeed(testMnemonic, "")

        val addr0 = hdKeyManager.deriveAddress(hdKeyManager.deriveEthereumKeyPair(seed, 0, 0))
        val addr1 = hdKeyManager.deriveAddress(hdKeyManager.deriveEthereumKeyPair(seed, 0, 1))
        val addr2 = hdKeyManager.deriveAddress(hdKeyManager.deriveEthereumKeyPair(seed, 0, 2))

        assertNotEquals(addr0, addr1)
        assertNotEquals(addr1, addr2)
        assertNotEquals(addr0, addr2)
    }

    @Test
    fun testValidEthereumAddressFormat() {
        val seed = org.web3j.crypto.MnemonicUtils.generateSeed(testMnemonic, "")
        val keyPair = hdKeyManager.deriveEthereumKeyPair(seed, 0, 0)
        val address = hdKeyManager.deriveAddress(keyPair)

        assertTrue(address.startsWith("0x"))
        assertEquals(42, address.length)

        val hexPart = address.substring(2)
        assertTrue(hexPart.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' })
    }

    @Test
    fun testBip44Path() {
        val seed = org.web3j.crypto.MnemonicUtils.generateSeed(testMnemonic, "")
        val addresses = hdKeyManager.deriveAddresses(seed, 0, 3)

        assertEquals("m/44'/60'/0'/0/0", addresses[0].path)
        assertEquals("m/44'/60'/0'/0/1", addresses[1].path)
        assertEquals("m/44'/60'/0'/0/2", addresses[2].path)
    }

    @Test
    fun testPrivateKeyBytes() {
        val seed = org.web3j.crypto.MnemonicUtils.generateSeed(testMnemonic, "")
        val keyPair = hdKeyManager.deriveEthereumKeyPair(seed, 0, 0)
        val privateKey = hdKeyManager.getPrivateKeyBytes(keyPair)

        assertEquals(32, privateKey.size)
    }

    @Test
    fun testPublicKeyBytes() {
        val seed = org.web3j.crypto.MnemonicUtils.generateSeed(testMnemonic, "")
        val keyPair = hdKeyManager.deriveEthereumKeyPair(seed, 0, 0)
        val publicKey = hdKeyManager.getPublicKeyBytes(keyPair)

        // Ethereum public key is 64 bytes (uncompressed)
        assertEquals(64, publicKey.size)
    }

    @Test
    fun testSameSeedDerivesSameAddress() {
        val seed = org.web3j.crypto.MnemonicUtils.generateSeed(testMnemonic, "")

        val addr1 = hdKeyManager.deriveAddress(hdKeyManager.deriveEthereumKeyPair(seed, 0, 0))
        val addr2 = hdKeyManager.deriveAddress(hdKeyManager.deriveEthereumKeyPair(seed, 0, 0))

        assertEquals(addr1, addr2)
    }
}
