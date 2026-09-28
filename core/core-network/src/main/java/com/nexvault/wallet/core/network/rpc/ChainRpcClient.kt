package com.nexvault.wallet.core.network.rpc

import java.math.BigInteger

/**
 * Thin, mockable seam over the chain JSON-RPC endpoints the send flow needs.
 *
 * Roadmap 2.6: `data` talks to this interface instead of `Web3j` directly, so the send paths are
 * unit-testable without mocking web3j's final classes — the reason 2.0.6 registered TC-REPO-003/004
 * to 4.8 in the first place. The production implementation is [Web3jChainRpcClient].
 */
interface ChainRpcClient {
    /**
     * Estimates the gas a transaction would consume.
     *
     * @param from sender address
     * @param to recipient address (contract address for token transfers)
     * @param value transfer value in wei (zero for token transfers)
     * @param data calldata, or null for a plain native transfer
     */
    suspend fun estimateGas(
        from: String,
        to: String,
        value: BigInteger,
        data: String?,
        chainId: Int,
    ): BigInteger

    /** Current gas price in wei. */
    suspend fun getGasPrice(chainId: Int): BigInteger

    /** Pending nonce for [address]. */
    suspend fun getTransactionCount(address: String, chainId: Int): BigInteger

    /** Native-coin balance in wei. */
    suspend fun getBalance(address: String, chainId: Int): BigInteger

    /** ERC-20 `balanceOf` for [contract] and [owner], in token base units. */
    suspend fun getTokenBalance(contract: String, owner: String, chainId: Int): BigInteger

    /** Submits a signed transaction; returns the transaction hash. */
    suspend fun sendRawTransaction(signedHex: String, chainId: Int): String
}
