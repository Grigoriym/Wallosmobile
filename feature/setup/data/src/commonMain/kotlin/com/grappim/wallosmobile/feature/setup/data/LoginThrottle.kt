package com.grappim.wallosmobile.feature.setup.data

import com.grappim.wallosmobile.core.logger.logcat
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The client-side rate limit plan §9 asks for. Wallos has **no login rate limiting and no lockout
 * anywhere** — not on `login.php`, not on `totp.php` — so an app that retries as fast as the user
 * can press a button is a brute-force tool pointed at the user's own server. `totp.php` is the
 * sharper case of the two: its window is `verify($code, null, 15)`, which leaves 31 codes valid at
 * any moment out of a million.
 *
 * The wait is spent *before* the next attempt, inside the call the screen is already showing a
 * spinner for — so there is nothing for the user to dismiss, but a login that has silently got
 * slower is a login that says nothing about why (5.5). Hence [awaitTurn]'s callback: the wait is
 * announced as it starts, not reported once it is over and no longer news. It only
 * ever grows from **refused credentials**; a transport failure is not a guess, and punishing a
 * flaky network would throttle the one case where retrying is the right move.
 *
 * Three attempts are free, because the first few failures are typos rather than an attack. After
 * that the wait doubles to a cap that keeps the screen usable: an eight-second ceiling is roughly
 * seven attempts a minute, which is nowhere near enough to work through a password list and is
 * still a wait a person will sit through after their fourth mistake.
 *
 * The counter lives as long as the object does, which — see [WebLoginApiImpl] — is one login
 * screen. A process restart resets it; that is the same window the web session gets, and closing
 * it would mean persisting failed-attempt counts against a server that keeps none itself.
 */
internal class LoginThrottle {

    private var refusedAttempts = 0

    /**
     * Suspends for however long the failures so far have earned, then returns. [onWait] is called
     * with that duration first, and only when there is one — an attempt that waits nothing has
     * nothing to announce.
     */
    suspend fun awaitTurn(onWait: (Duration) -> Unit) {
        val wait = waitAfter(refusedAttempts)
        if (wait > Duration.ZERO) {
            logcat { "$refusedAttempts refused attempts — holding the next one for $wait" }
            onWait(wait)
            delay(wait)
        }
    }

    fun recordRefusal() {
        refusedAttempts++
    }

    /** A completed connection says the credentials were real, so the count has nothing left to say. */
    fun reset() {
        refusedAttempts = 0
    }

    private fun waitAfter(refusals: Int): Duration {
        if (refusals <= FREE_ATTEMPTS) return Duration.ZERO
        val doublings = (refusals - FREE_ATTEMPTS - 1).coerceAtMost(MAX_DOUBLINGS)
        return BASE_WAIT * (1 shl doublings)
    }

    private companion object {
        const val FREE_ATTEMPTS = 2
        const val MAX_DOUBLINGS = 3
        val BASE_WAIT = 1.seconds
    }
}
