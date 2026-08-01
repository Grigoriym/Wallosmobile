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

    /** Removes the key only — the server URL survives a disconnect, so re-login is one field. */
    suspend fun clear()
}
