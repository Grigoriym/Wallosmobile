package com.grappim.wallosmobile.core.storage

/**
 * The Wallos instance root the user pointed us at.
 *
 * [serverUrl] is deliberately **not** `suspend`: it is read from Ktor's `defaultRequest` block,
 * which is not a suspend context. The implementation has to keep the value cached rather than
 * reaching into DataStore on every request.
 */
interface ServerUrlStorage {

    val serverUrl: String

    suspend fun saveServerUrl(url: String)
}
