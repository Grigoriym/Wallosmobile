package com.grappim.wallosmobile.core.domain

import kotlin.coroutines.cancellation.CancellationException

/**
 * Like [runCatching], but with proper coroutines cancellation handling. Also only catches
 * [Exception] instead of [Throwable].
 *
 * Cancellation exceptions need to be rethrown. See
 * https://github.com/Kotlin/kotlinx.coroutines/issues/1814.
 */
inline fun <R> resultOf(block: () -> R): Result<R> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

/**
 * Like [runCatching], but with proper coroutines cancellation handling. Also only catches
 * [Exception] instead of [Throwable].
 *
 * Cancellation exceptions need to be rethrown. See
 * https://github.com/Kotlin/kotlinx.coroutines/issues/1814.
 */
inline fun <T, R> T.resultOf(block: T.() -> R): Result<R> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

/**
 * Like [Result.mapCatching], but uses [resultOf] instead of [runCatching], so a cancellation
 * thrown by [transform] is not swallowed.
 */
inline fun <R, T> Result<T>.mapResult(transform: (value: T) -> R): Result<R> = fold(
    onSuccess = { resultOf { transform(it) } },
    onFailure = { Result.failure(it) }
)
