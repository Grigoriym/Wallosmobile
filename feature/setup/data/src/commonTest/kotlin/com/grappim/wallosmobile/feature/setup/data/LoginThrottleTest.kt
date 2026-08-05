package com.grappim.wallosmobile.feature.setup.data

import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Virtual time, so the schedule is asserted rather than waited for: `runTest` skips a `delay` and
 * records it in [kotlinx.coroutines.test.TestScope.currentTime].
 */
class LoginThrottleTest {

    /** A password typo is not an attack — the first few retries have to stay instant. */
    @Test
    fun `the first three attempts are not made to wait`() = runTest {
        val throttle = LoginThrottle()

        repeat(3) {
            throttle.awaitTurn { }
            throttle.recordRefusal()
        }

        assertEquals(0, currentTime)
    }

    /**
     * The wait is spent inside the call the screen is already showing a spinner for, so the only
     * moment it is worth announcing is before it starts — afterwards it is over and not news.
     */
    @Test
    fun `the wait is announced before it is spent`() = runTest {
        val throttle = LoginThrottle()
        repeat(3) { throttle.recordRefusal() }

        var announced: Duration? = null
        var announcedAt = -1L
        throttle.awaitTurn {
            announced = it
            announcedAt = currentTime
        }

        assertEquals(1.seconds, announced)
        assertEquals(0, announcedAt)
        assertEquals(1.seconds.inWholeMilliseconds, currentTime)
    }

    /** An attempt that is not held back has nothing to say, and saying it anyway would be noise. */
    @Test
    fun `an attempt that waits nothing announces nothing`() = runTest {
        val throttle = LoginThrottle()
        repeat(2) { throttle.recordRefusal() }

        var announced: Duration? = null
        throttle.awaitTurn { announced = it }

        assertNull(announced)
    }

    @Test
    fun `the wait doubles once the free attempts are spent`() = runTest {
        val throttle = LoginThrottle()
        val waits = mutableListOf<Long>()

        repeat(7) {
            val before = currentTime
            throttle.awaitTurn { }
            waits += currentTime - before
            throttle.recordRefusal()
        }

        assertEquals(listOf(0L, 0L, 0L, 1000L, 2000L, 4000L, 8000L), waits)
    }

    /** Capped, or the screen becomes unusable long before the throttle stops mattering. */
    @Test
    fun `the wait stops growing at eight seconds`() = runTest {
        val throttle = LoginThrottle()
        repeat(12) { throttle.recordRefusal() }

        val before = currentTime
        throttle.awaitTurn { }

        assertEquals(8.seconds.inWholeMilliseconds, currentTime - before)
    }

    /** The credentials turned out to be real, so the failures behind them said nothing. */
    @Test
    fun `a connection clears what the failures earned`() = runTest {
        val throttle = LoginThrottle()
        repeat(6) { throttle.recordRefusal() }

        throttle.reset()
        val before = currentTime
        throttle.awaitTurn { }

        assertEquals(0, currentTime - before)
    }
}
