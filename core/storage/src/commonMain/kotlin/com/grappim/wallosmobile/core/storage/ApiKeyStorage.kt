package com.grappim.wallosmobile.core.storage

import kotlinx.coroutines.flow.Flow

/**
 * The one runtime credential: a static per-user API key. There is no token, no refresh and no
 * server-side session, so "disconnect" is just clearing this.
 */
interface ApiKeyStorage {

    /**
     * `true` while a key is stored *and* readable. A key that no longer decrypts (see
     * [SecretCipher]) reads as not connected, which sends the user back to onboarding rather
     * than into a stream of `Unauthenticated` responses.
     */
    val isConnected: Flow<Boolean>

    suspend fun getKey(): String?

    suspend fun setKey(key: String)

    /**
     * Drops the key **and everything cached under it** — the cached subscriptions belong to the
     * account whose credential is being thrown away, and the next account to log in must not
     * inherit them. The server URL survives, so re-login is one field.
     *
     * Living here rather than in a cleaner someone has to remember to call is deliberate: this is
     * the single place a key is dropped, and all three callers (disconnect, and both login paths,
     * which clear the stale key before validating a new one) need the same eviction.
     */
    suspend fun clear()
}
