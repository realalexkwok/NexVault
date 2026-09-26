package com.nexvault.wallet.core.network.api

import com.nexvault.wallet.core.network.dto.EtherscanBalanceResponse
import com.nexvault.wallet.core.network.dto.EtherscanTokenTransferListResponse
import com.nexvault.wallet.core.network.dto.EtherscanTransactionListResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Etherscan API **V2** block explorer service.
 *
 * Roadmap 2.0.4b: all supported chains are served by the single V2 host
 * `https://api.etherscan.io/v2/` (set as the base URL per chain by [BlockExplorerApiFactory]),
 * and the `chainid` query parameter is mandatory on every call. The V1 per-chain hosts
 * (Etherscan/BscScan/PolygonScan) are fully deprecated.
 *
 * Ref: doc/05-IMPLEMENTATION-PLAN-PHASE2.md Task 2.1.3
 */
interface BlockExplorerApi {

    /**
     * Fetches normal (native coin) transactions for an address.
     *
     * @param chainId Chain to query; required by V2, must match the base URL's chain set
     * @param module API module (default: "account")
     * @param action API action (default: "txlist")
     * @param address Wallet address
     * @param startBlock Starting block number (0 for all)
     * @param endBlock Ending block number (99999999 for latest)
     * @param page Page number for pagination
     * @param offset Number of results per page (max 10000)
     * @param sort Sort order ("asc" or "desc")
     * @param apiKey Etherscan API V2 key
     */
    @GET("api")
    suspend fun getTransactions(
        @Query("chainid") chainId: Int,
        @Query("module") module: String = "account",
        @Query("action") action: String = "txlist",
        @Query("address") address: String,
        @Query("startblock") startBlock: Long = 0,
        @Query("endblock") endBlock: Long = 99999999,
        @Query("page") page: Int = 1,
        @Query("offset") offset: Int = 20,
        @Query("sort") sort: String = "desc",
        @Query("apikey") apiKey: String,
    ): EtherscanTransactionListResponse

    /**
     * Fetches ERC-20 token transfer events for an address.
     *
     * @param chainId Chain to query; required by V2
     */
    @GET("api")
    suspend fun getTokenTransfers(
        @Query("chainid") chainId: Int,
        @Query("module") module: String = "account",
        @Query("action") action: String = "tokentx",
        @Query("address") address: String,
        @Query("startblock") startBlock: Long = 0,
        @Query("endblock") endBlock: Long = 99999999,
        @Query("page") page: Int = 1,
        @Query("offset") offset: Int = 20,
        @Query("sort") sort: String = "desc",
        @Query("apikey") apiKey: String,
    ): EtherscanTokenTransferListResponse

    /**
     * Fetches the native coin balance for an address.
     *
     * Useful as a lightweight alternative to Web3j ethGetBalance. Currently has no caller; it is
     * migrated to V2 (roadmap 2.0.4b) so the whole interface speaks one protocol version.
     *
     * @param chainId Chain to query; required by V2
     */
    @GET("api")
    suspend fun getBalance(
        @Query("chainid") chainId: Int,
        @Query("module") module: String = "account",
        @Query("action") action: String = "balance",
        @Query("address") address: String,
        @Query("tag") tag: String = "latest",
        @Query("apikey") apiKey: String,
    ): EtherscanBalanceResponse
}
