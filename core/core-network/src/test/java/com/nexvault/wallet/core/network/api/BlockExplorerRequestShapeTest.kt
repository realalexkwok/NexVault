package com.nexvault.wallet.core.network.api

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.nexvault.wallet.core.network.adapter.BigDecimalAdapter
import com.nexvault.wallet.core.network.adapter.BigIntegerAdapter
import com.nexvault.wallet.core.network.adapter.EtherscanEnvelopeAdapterFactory
import com.nexvault.wallet.core.network.config.ChainConfigProvider
import com.squareup.moshi.Moshi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Roadmap 2.0.4b, AC-1: the assertions run against the request Retrofit actually **built** — not
 * against a constant — so a missing or wrong `chainid` cannot slip through.
 *
 * MockWebServer is not in `specs/tech-stack.md`, so a capturing `okhttp3.Interceptor` returns a
 * canned envelope and records the outgoing request instead.
 */
class BlockExplorerRequestShapeTest {
    private val capturedRequests = mutableListOf<Request>()

    private val moshi: Moshi =
        Moshi
            .Builder()
            .add(BigDecimalAdapter())
            .add(BigIntegerAdapter())
            .add(EtherscanEnvelopeAdapterFactory())
            .build()

    private val configProvider =
        ChainConfigProvider(
            infuraApiKey = "infura-key",
            alchemyApiKey = "alchemy-key",
            etherscanApiKey = SHAPE_TEST_KEY,
        )

    @Test
    fun everyEndpointUsesTheV2RouteWithItsOwnChainId() =
        runTest {
            SUPPORTED_CHAIN_IDS.forEach { chainId ->
                val api = apiFor(chainId)
                api.getTransactions(chainId = chainId, address = ADDRESS, apiKey = SHAPE_TEST_KEY)
                api.getTokenTransfers(chainId = chainId, address = ADDRESS, apiKey = SHAPE_TEST_KEY)
                api.getBalance(chainId = chainId, address = ADDRESS, apiKey = SHAPE_TEST_KEY)
            }

            assertThat(capturedRequests).hasSize(SUPPORTED_CHAIN_IDS.size * 3)
            capturedRequests.forEachIndexed { index, request ->
                val expectedChainId = SUPPORTED_CHAIN_IDS[index / 3]
                val message = "built request: ${request.url}"

                assertWithMessage(message).that(request.url.scheme).isEqualTo("https")
                assertWithMessage(message).that(request.url.host).isEqualTo("api.etherscan.io")
                assertWithMessage(message)
                    .that(request.url.pathSegments)
                    .containsExactly("v2", "api")
                    .inOrder()
                assertWithMessage(message)
                    .that(request.url.queryParameter("chainid"))
                    .isEqualTo(expectedChainId.toString())
                assertWithMessage(message).that(request.url.queryParameter("module")).isEqualTo("account")
                assertWithMessage(message).that(request.url.queryParameter("address")).isEqualTo(ADDRESS)
                assertWithMessage(message)
                    .that(request.url.queryParameter("apikey"))
                    .isEqualTo(SHAPE_TEST_KEY)

                when (request.url.queryParameter("action")) {
                    "txlist", "tokentx" -> {
                        assertWithMessage(message)
                            .that(request.url.queryParameter("startblock"))
                            .isEqualTo("0")
                        assertWithMessage(message)
                            .that(request.url.queryParameter("endblock"))
                            .isEqualTo("99999999")
                        assertWithMessage(message)
                            .that(request.url.queryParameter("offset"))
                            .isEqualTo("20")
                        assertWithMessage(message).that(request.url.queryParameter("sort")).isEqualTo("desc")
                    }

                    "balance" -> {
                        assertWithMessage(message)
                            .that(request.url.queryParameter("tag"))
                            .isEqualTo("latest")
                        assertWithMessage(message)
                            .that(request.url.queryParameter("startblock"))
                            .isNull()
                    }

                    else -> throw AssertionError("Unexpected action in ${request.url}")
                }
            }
        }

    @Test
    fun eachEndpointKeepsItsV2Action() =
        runTest {
            val api = apiFor(1)

            api.getTransactions(chainId = 1, address = ADDRESS, apiKey = SHAPE_TEST_KEY)
            api.getTokenTransfers(chainId = 1, address = ADDRESS, apiKey = SHAPE_TEST_KEY)
            api.getBalance(chainId = 1, address = ADDRESS, apiKey = SHAPE_TEST_KEY)

            assertThat(capturedRequests.map { it.url.queryParameter("action") })
                .containsExactly("txlist", "tokentx", "balance")
                .inOrder()
        }

    private fun apiFor(chainId: Int): BlockExplorerApi {
        val capturingClient =
            OkHttpClient
                .Builder()
                .addInterceptor { chain ->
                    capturedRequests += chain.request()
                    Response
                        .Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(CANNED_ENVELOPE.toResponseBody("application/json".toMediaType()))
                        .build()
                }.build()

        val baseUrl = checkNotNull(configProvider.getConfig(chainId)).explorerApiBaseUrl

        return Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(capturingClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BlockExplorerApi::class.java)
    }

    private companion object {
        const val ADDRESS = "0x1234567890abcdef1234567890abcdef12345678"
        const val SHAPE_TEST_KEY = "shape-test-key"

        val SUPPORTED_CHAIN_IDS = listOf(1, 11155111, 56, 137)

        /** Parses for all three endpoints; its values are irrelevant to the request shape. */
        const val CANNED_ENVELOPE = """{"status":"1","message":"OK","result":""}"""
    }
}
