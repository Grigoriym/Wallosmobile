package com.grappim.wallosmobile.core.api

import io.ktor.http.Parameters

internal const val API_KEY_PARAM = "api_key"

/**
 * A `null` key means nothing has been stored yet. Omitting the parameter is the right move: the
 * server answers `Missing API key`, which maps to `Unauthenticated` and sends the user to setup —
 * exactly where a caller with no key belongs.
 */
internal fun FormParams.withApiKey(apiKey: String?): FormParams = apply {
    if (apiKey != null) put(API_KEY_PARAM, apiKey)
}

internal fun FormParams.build(): Parameters = Parameters.build {
    asMap().forEach { (key, value) -> append(key, value) }
}
