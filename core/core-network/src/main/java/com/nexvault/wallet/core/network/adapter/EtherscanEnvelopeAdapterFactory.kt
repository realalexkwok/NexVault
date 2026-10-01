package com.nexvault.wallet.core.network.adapter

import com.nexvault.wallet.core.network.dto.EtherscanTokenTransferListResponse
import com.nexvault.wallet.core.network.dto.EtherscanTransactionListResponse
import com.nexvault.wallet.core.network.dto.EtherscanTxReceiptStatusResponse
import com.nexvault.wallet.core.network.dto.ReceiptStatusDto
import com.nexvault.wallet.core.network.dto.TokenTransferDto
import com.nexvault.wallet.core.network.dto.TransactionDto
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

/**
 * Moshi factory binding the two Etherscan V2 list envelopes (roadmap 2.0.4b).
 *
 * The envelopes put a JSON **array** in `result` when the call succeeds and a JSON **String** when
 * the explorer rejects it (deprecated endpoint, invalid key, plan gate). Moshi codegen cannot
 * express that union — with a non-null `List` field the rejection body did not parse at all and the
 * explorer's explanation was lost — so these adapters read the envelope by hand and keep the
 * rejection text in `resultText`.
 *
 * Registered in `NetworkModule.provideMoshi()`; the envelopes carry no
 * `@JsonClass(generateAdapter = true)` any more.
 */
class EtherscanEnvelopeAdapterFactory : JsonAdapter.Factory {
    override fun create(
        type: Type,
        annotations: Set<Annotation>,
        moshi: Moshi,
    ): JsonAdapter<*>? =
        when (type) {
            EtherscanTransactionListResponse::class.java ->
                LenientListEnvelopeAdapter(
                    moshi = moshi,
                    itemType = TransactionDto::class.java,
                    build = { parts ->
                        EtherscanTransactionListResponse(
                            status = parts.status,
                            message = parts.message,
                            result = parts.result,
                            resultText = parts.resultText,
                        )
                    },
                    parts = { value ->
                        EnvelopeParts(
                            status = value.status,
                            message = value.message,
                            result = value.result,
                            resultText = value.resultText,
                        )
                    },
                )

            EtherscanTokenTransferListResponse::class.java ->
                LenientListEnvelopeAdapter(
                    moshi = moshi,
                    itemType = TokenTransferDto::class.java,
                    build = { parts ->
                        EtherscanTokenTransferListResponse(
                            status = parts.status,
                            message = parts.message,
                            result = parts.result,
                            resultText = parts.resultText,
                        )
                    },
                    parts = { value ->
                        EnvelopeParts(
                            status = value.status,
                            message = value.message,
                            result = value.result,
                            resultText = value.resultText,
                        )
                    },
                )

            EtherscanTxReceiptStatusResponse::class.java ->
                LenientObjectEnvelopeAdapter(
                    moshi = moshi,
                    itemType = ReceiptStatusDto::class.java,
                    build = { parts ->
                        EtherscanTxReceiptStatusResponse(
                            status = parts.status,
                            message = parts.message,
                            result = parts.result,
                            resultText = parts.resultText,
                        )
                    },
                    parts = { value ->
                        ObjectEnvelopeParts(
                            status = value.status,
                            message = value.message,
                            result = value.result,
                            resultText = value.resultText,
                        )
                    },
                )

            else -> null
        }
}

/** The envelope fields that do not depend on the payload type. */
internal data class EnvelopeParts<T>(
    val status: String,
    val message: String,
    val result: List<T>? = null,
    val resultText: String? = null,
)

/** Parts of an envelope whose `result` is a single object (receipt status). */
internal data class ObjectEnvelopeParts<T>(
    val status: String,
    val message: String,
    val result: T?,
    val resultText: String?,
)

/**
 * Reads an Etherscan list envelope whose `result` is either an array of [T] or a rejection String.
 *
 * @param moshi used to resolve the row-list adapter lazily, once Moshi is fully built
 * @param itemType row type (`TransactionDto` or `TokenTransferDto`)
 * @param build wraps parsed parts into the concrete response type
 * @param parts unwraps a response for the symmetric write path
 */
internal class LenientListEnvelopeAdapter<T, R : Any>(
    private val moshi: Moshi,
    private val itemType: Class<T>,
    private val build: (EnvelopeParts<T>) -> R,
    private val parts: (R) -> EnvelopeParts<T>,
) : JsonAdapter<R>() {
    private val resultAdapter: JsonAdapter<List<T>> by lazy {
        moshi.adapter(Types.newParameterizedType(List::class.java, itemType))
    }

    override fun fromJson(reader: JsonReader): R {
        var status = "0"
        var message = ""
        var result: List<T>? = null
        var resultText: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "status" -> status = reader.nextStringOrEmpty()
                "message" -> message = reader.nextStringOrEmpty()
                "result" ->
                    when (reader.peek()) {
                        JsonReader.Token.BEGIN_ARRAY -> result = resultAdapter.fromJson(reader)
                        JsonReader.Token.STRING -> resultText = reader.nextString()
                        JsonReader.Token.NULL -> reader.nextNull<Unit>()
                        else -> reader.skipValue()
                    }

                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return build(
            EnvelopeParts(status = status, message = message, result = result, resultText = resultText),
        )
    }

    override fun toJson(
        writer: JsonWriter,
        value: R?,
    ) {
        if (value == null) {
            writer.nullValue()
            return
        }
        val parts = parts(value)
        writer.beginObject()
        writer.name("status").value(parts.status)
        writer.name("message").value(parts.message)
        writer.name("result")
        when {
            parts.result != null -> resultAdapter.toJson(writer, parts.result)
            parts.resultText != null -> writer.value(parts.resultText)
            else -> writer.nullValue()
        }
        writer.endObject()
    }

    private fun JsonReader.nextStringOrEmpty(): String =
        if (peek() == JsonReader.Token.STRING) {
            nextString()
        } else {
            skipValue()
            ""
        }
}

/**
 * Reads an Etherscan envelope whose `result` is either a single object of [T] or a rejection String
 * (roadmap 2.8, `transaction/gettxreceiptstatus`).
 *
 * @param moshi used to resolve the payload adapter lazily, once Moshi is fully built
 * @param itemType payload type (`ReceiptStatusDto`)
 * @param build wraps parsed parts into the concrete response type
 * @param parts unwraps a response for the symmetric write path
 */
internal class LenientObjectEnvelopeAdapter<T : Any, R : Any>(
    private val moshi: Moshi,
    private val itemType: Class<T>,
    private val build: (ObjectEnvelopeParts<T>) -> R,
    private val parts: (R) -> ObjectEnvelopeParts<T>,
) : JsonAdapter<R>() {
    private val resultAdapter: JsonAdapter<T> by lazy { moshi.adapter(itemType) }

    override fun fromJson(reader: JsonReader): R {
        var status = "0"
        var message = ""
        var result: T? = null
        var resultText: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "status" -> status = reader.nextStringOrEmpty()
                "message" -> message = reader.nextStringOrEmpty()
                "result" ->
                    when (reader.peek()) {
                        JsonReader.Token.BEGIN_OBJECT -> result = resultAdapter.fromJson(reader)
                        JsonReader.Token.STRING -> resultText = reader.nextString()
                        JsonReader.Token.NULL -> reader.nextNull<Unit>()
                        else -> reader.skipValue()
                    }

                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return build(
            ObjectEnvelopeParts(status = status, message = message, result = result, resultText = resultText),
        )
    }

    override fun toJson(
        writer: JsonWriter,
        value: R?,
    ) {
        if (value == null) {
            writer.nullValue()
            return
        }
        val parsed = parts(value)
        writer.beginObject()
        writer.name("status").value(parsed.status)
        writer.name("message").value(parsed.message)
        writer.name("result")
        when {
            parsed.result != null -> resultAdapter.toJson(writer, parsed.result)
            parsed.resultText != null -> writer.value(parsed.resultText)
            else -> writer.nullValue()
        }
        writer.endObject()
    }

    private fun JsonReader.nextStringOrEmpty(): String =
        if (peek() == JsonReader.Token.STRING) {
            nextString()
        } else {
            skipValue()
            ""
        }
}
