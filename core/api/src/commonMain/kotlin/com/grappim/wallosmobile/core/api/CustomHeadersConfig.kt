package com.grappim.wallosmobile.core.api

import com.grappim.wallosmobile.core.storage.customheaders.CustomHeadersStorage

/** Configuration for [CustomHeaders]. */
class CustomHeadersConfig {
    lateinit var storage: CustomHeadersStorage

    /** The stored server URL, read per request so a change of server applies at once. */
    var serverUrl: () -> String = { "" }
}
