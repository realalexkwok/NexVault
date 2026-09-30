package com.nexvault.wallet.core.network.adapter

import com.google.common.truth.Truth.assertThat
import com.nexvault.wallet.core.network.dto.EtherscanTokenTransferListResponse
import com.nexvault.wallet.core.network.dto.EtherscanTransactionListResponse
import com.nexvault.wallet.core.network.interceptor.CacheControlInterceptor
import com.nexvault.wallet.core.network.interceptor.CoinGeckoApiKeyInterceptor
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import java.math.BigInteger

/**
 * Roadmap 2.6 coverage: the BigInteger adapter, the envelope factory's lenient parsing, and the
 * two OkHttp interceptors.
 */
class BigIntegerAdapterTest {
    private val moshi =
        Moshi.Builder()
            .add(BigIntegerAdapter())
            .build()

    @Test
    fun bigIntegerAdapter_readsDecimalAndHexStrings() {
        val adapter = moshi.adapter(BigInteger::class.java)

        assertThat(adapter.fromJson("\"123\"")).isEqualTo(BigInteger.valueOf(123))
        assertThat(adapter.fromJson("\"0x1A\"")).isEqualTo(BigInteger.valueOf(26))
    }

    @Test
    fun bigIntegerAdapter_writesDecimalStrings() {
        val adapter = moshi.adapter(BigInteger::class.java)

        assertThat(adapter.toJson(BigInteger.valueOf(42))).isEqualTo("\"42\"")
    }
}

class EtherscanEnvelopeAdapterFactoryTest {
    private val moshi = Moshi.Builder().add(EtherscanEnvelopeAdapterFactory()).build()

    @Test
    fun rejectionEnvelope_parsesTheExplorerTextInsteadOfBlowingUp() {
        val adapter = moshi.adapter(EtherscanTransactionListResponse::class.java)

        val parsed = adapter.fromJson("""{"status":"0","message":"NOTOK","result":"Max rate limit reached"}""")

        assertThat(parsed).isNotNull()
        assertThat(parsed!!.isSuccess).isFalse()
        assertThat(parsed.resultText).isEqualTo("Max rate limit reached")
        assertThat(parsed.result).isNull()
    }

    @Test
    fun successEnvelope_parsesTheRowList() {
        val adapter = moshi.adapter(EtherscanTransactionListResponse::class.java)

        val successBody =
            """{"status":"1","message":"OK","result":[""" +
                """{"hash":"0x1","from":"0xa","to":"0xb","value":"1","gas":"21000",""" +
                """"gasPrice":"5","gasUsed":"21000","blockNumber":"1","timeStamp":"1700000000",""" +
                """"nonce":"0","isError":"0","txreceipt_status":"1","input":"0x","confirmations":"10"}]}"""
        val parsed = adapter.fromJson(successBody)

        assertThat(parsed).isNotNull()
        assertThat(parsed!!.isSuccess).isTrue()
        assertThat(parsed.result).hasSize(1)
    }

    @Test
    fun emptyTokenTransferEnvelope_parsesAsAnEmptyList() {
        val adapter = moshi.adapter(EtherscanTokenTransferListResponse::class.java)

        val parsed = adapter.fromJson("""{"status":"0","message":"No transactions found","result":[]}""")

        assertThat(parsed).isNotNull()
        assertThat(parsed!!.result).isEmpty()
    }
}

class CacheControlInterceptorTest {
    @Test
    fun interceptor_stampsTheCacheHeaderOnTheResponse() {
        val request = Request.Builder().url("https://example.com/prices").build()
        val chain = proceedChain(request) { proceedRequest ->
            assertThat(proceedRequest.url.toString()).isEqualTo("https://example.com/prices")
            response(proceedRequest)
        }

        val result = CacheControlInterceptor().intercept(chain)

        assertThat(result.code).isEqualTo(200)
        assertThat(result.header("Cache-Control")).isEqualTo("public, max-age=30")
    }
}

class CoinGeckoApiKeyInterceptorTest {
    @Test
    fun interceptor_setsTheDemoKeyHeader() {
        val request = Request.Builder().url("https://api.coingecko.com/api/v3/simple/price").build()
        val chain = proceedChain(request) { proceedRequest ->
            assertThat(proceedRequest.header("x-cg-demo-api-key")).isEqualTo("test-key")
            response(proceedRequest)
        }

        val result = CoinGeckoApiKeyInterceptor("test-key").intercept(chain)

        assertThat(result.code).isEqualTo(200)
    }

    @Test
    fun interceptor_withABlankKeyLeavesTheRequestUntouched() {
        val request = Request.Builder().url("https://api.coingecko.com/api/v3/simple/price").build()
        val chain = proceedChain(request) { proceedRequest ->
            assertThat(proceedRequest.header("x-cg-demo-api-key")).isNull()
            response(proceedRequest)
        }

        CoinGeckoApiKeyInterceptor("").intercept(chain)
    }
}

private fun proceedChain(
    request: Request,
    onProceed: (Request) -> Response,
): Interceptor.Chain =
    mockk(relaxed = true) {
        every { request() } returns request
        every { proceed(any()) } answers { onProceed(firstArg()) }
    }

private fun response(request: Request): Response =
    Response.Builder()
        .request(request)
        .protocol(okhttp3.Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body("{}".toResponseBody())
        .build()
