package com.nexvault.wallet.core.network.rpc

import com.nexvault.wallet.core.network.web3.Web3jProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production [ChainRpcClient] over web3j (roadmap 2.6).
 *
 * Every call runs on [Dispatchers.IO]; web3j failures propagate as-is for the repository to map.
 */
@Singleton
class Web3jChainRpcClient @Inject constructor(
    private val web3jProvider: Web3jProvider,
) : ChainRpcClient {
    override suspend fun estimateGas(
        from: String,
        to: String,
        value: BigInteger,
        data: String?,
        chainId: Int,
    ): BigInteger = withContext(Dispatchers.IO) {
        val transaction =
            Transaction.createFunctionCallTransaction(
                from,
                null,
                null,
                null,
                to,
                value,
                data,
            )
        web3jProvider.getWeb3j(chainId).ethEstimateGas(transaction).send().amountUsed
    }

    override suspend fun getGasPrice(chainId: Int): BigInteger = withContext(Dispatchers.IO) {
        web3jProvider.getWeb3j(chainId).ethGasPrice().send().gasPrice
    }

    override suspend fun getTransactionCount(address: String, chainId: Int): BigInteger =
        withContext(Dispatchers.IO) {
            web3jProvider
                .getWeb3j(chainId)
                .ethGetTransactionCount(address, DefaultBlockParameterName.PENDING)
                .send()
                .transactionCount
        }

    override suspend fun getBalance(address: String, chainId: Int): BigInteger =
        withContext(Dispatchers.IO) {
            web3jProvider
                .getWeb3j(chainId)
                .ethGetBalance(address, DefaultBlockParameterName.LATEST)
                .send()
                .balance
        }

    override suspend fun getTokenBalance(
        contract: String,
        owner: String,
        chainId: Int,
    ): BigInteger = withContext(Dispatchers.IO) {
        val function =
            Function(
                "balanceOf",
                listOf(Address(owner)),
                listOf(object : TypeReference<Uint256>() {}),
            )
        val response =
            web3jProvider
                .getWeb3j(chainId)
                .ethCall(
                    Transaction.createEthCallTransaction(ZERO_ADDRESS, contract, FunctionEncoder.encode(function)),
                    DefaultBlockParameterName.LATEST,
                ).send()
        val hex = response.value ?: return@withContext BigInteger.ZERO
        if (hex == "0x" || hex.isEmpty()) return@withContext BigInteger.ZERO
        val decoded = FunctionReturnDecoder.decode(hex, function.outputParameters)
        if (decoded.isEmpty()) return@withContext BigInteger.ZERO
        (decoded[0] as Uint256).value
    }

    override suspend fun sendRawTransaction(signedHex: String, chainId: Int): String =
        withContext(Dispatchers.IO) {
            web3jProvider.getWeb3j(chainId).ethSendRawTransaction(signedHex).send().transactionHash
        }

    private companion object {
        const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
    }
}
