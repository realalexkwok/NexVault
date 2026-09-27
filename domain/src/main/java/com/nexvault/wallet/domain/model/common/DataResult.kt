package com.nexvault.wallet.domain.model.common

/**
 * Generic result wrapper for data operations.
 * Used by repositories and use cases for consistent error handling.
 */
sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>

    data class Error(
        val exception: Throwable,
        val message: String? = null,
    ) : DataResult<Nothing>
}

/**
 * Extension to convert kotlin.Result to DataResult.
 */
fun <T> Result<T>.toDataResult(): DataResult<T> = fold(
    onSuccess = { DataResult.Success(it) },
    onFailure = { DataResult.Error(it, it.message) },
)

/**
 * Extension to map Success data.
 */
inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(data))
    is DataResult.Error -> this
}

/**
 * Extension to flatMap Success data.
 */
inline fun <T, R> DataResult<T>.flatMap(transform: (T) -> DataResult<R>): DataResult<R> = when (this) {
    is DataResult.Success -> transform(data)
    is DataResult.Error -> this
}

/**
 * Extension to get data or null.
 */
fun <T> DataResult<T>.getOrNull(): T? = when (this) {
    is DataResult.Success -> data
    is DataResult.Error -> null
}

/**
 * Extension to get data or throw.
 */
fun <T> DataResult<T>.getOrThrow(): T = when (this) {
    is DataResult.Success -> data
    is DataResult.Error -> throw exception
}
