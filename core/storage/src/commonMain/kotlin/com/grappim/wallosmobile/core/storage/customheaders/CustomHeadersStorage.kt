package com.grappim.wallosmobile.core.storage.customheaders

import kotlinx.coroutines.flow.Flow

/**
 * The headers a reverse proxy in front of Wallos asks for (plan §4.5b). They belong to the
 * *server*, like the URL, so `ApiKeyStorage.clear()` leaves them alone.
 *
 * [currentHeaders] is deliberately **not** `suspend`: it is read per request from a Ktor plugin,
 * which can't suspend there, so the implementation keeps the value cached.
 */
interface CustomHeadersStorage {

    /** Emits the stored list, empty until something is saved, and again on every change. */
    val headers: Flow<List<CustomHeader>>

    val currentHeaders: List<CustomHeader>

    suspend fun setHeaders(headers: List<CustomHeader>)
}
