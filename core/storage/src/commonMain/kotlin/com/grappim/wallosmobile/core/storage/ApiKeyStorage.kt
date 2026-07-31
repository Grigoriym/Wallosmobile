package com.grappim.wallosmobile.core.storage

/**
 * The one runtime credential: a static per-user API key. There is no token, no refresh and no
 * server-side session, so "disconnect" is just clearing this.
 *
 * Only the read is declared here — it is what `core:api` needs to sign a request. The writes
 * and the connected-state flow arrive with the DataStore implementation.
 */
interface ApiKeyStorage {
    suspend fun getKey(): String?
}
