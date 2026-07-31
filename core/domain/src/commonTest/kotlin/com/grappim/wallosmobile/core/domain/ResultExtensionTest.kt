package com.grappim.wallosmobile.core.domain

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ResultExtensionTest {

    @Test
    fun `resultOf wraps the value on success`() {
        val result = resultOf { 42 }

        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `resultOf captures an exception as a failure`() {
        val expected = IllegalStateException("boom")

        val result = resultOf { throw expected }

        assertSame(expected, result.exceptionOrNull())
    }

    @Test
    fun `resultOf rethrows cancellation`() {
        assertFailsWith<CancellationException> {
            resultOf { throw CancellationException("cancelled") }
        }
    }

    // TimeoutCancellationException is a CancellationException, so the single catch above covers
    // it — the reference project's separate clause for it is unreachable.
    @Test
    fun `resultOf rethrows a timeout cancellation`() = runTest {
        assertFailsWith<TimeoutCancellationException> {
            resultOf {
                withTimeout(timeMillis = 10) { delay(timeMillis = 100) }
            }
        }
    }

    @Test
    fun `receiver resultOf runs the block on the receiver`() {
        val result = "wallos".resultOf { length }

        assertEquals(6, result.getOrNull())
    }

    @Test
    fun `receiver resultOf captures an exception as a failure`() {
        val expected = IllegalStateException("boom")

        val result = "wallos".resultOf { throw expected }

        assertSame(expected, result.exceptionOrNull())
    }

    @Test
    fun `mapResult transforms a success`() {
        val result = resultOf { 21 }.mapResult { it * 2 }

        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `mapResult keeps a null success value`() {
        val result = Result.success<String?>(null).mapResult { it?.length }

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `mapResult propagates the original failure without running the transform`() {
        val expected = IllegalStateException("boom")
        var transformed = false

        val result = Result.failure<Int>(expected).mapResult {
            transformed = true
            it
        }

        assertSame(expected, result.exceptionOrNull())
        assertFalse(transformed)
    }

    @Test
    fun `mapResult captures an exception thrown by the transform`() {
        val expected = IllegalStateException("boom")

        val result = resultOf { 21 }.mapResult<Int, Int> { throw expected }

        assertSame(expected, result.exceptionOrNull())
    }

    @Test
    fun `mapResult rethrows cancellation from the transform`() {
        assertFailsWith<CancellationException> {
            resultOf { 21 }.mapResult<Int, Int> { throw CancellationException("cancelled") }
        }
    }
}
