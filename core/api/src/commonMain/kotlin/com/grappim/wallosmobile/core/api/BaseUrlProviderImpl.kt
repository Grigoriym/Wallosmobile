package com.grappim.wallosmobile.core.api

import com.grappim.wallosmobile.core.storage.ServerUrlStorage
import org.koin.core.annotation.Factory

@Factory(binds = [BaseUrlProvider::class])
class BaseUrlProviderImpl(private val serverUrlStorage: ServerUrlStorage) : BaseUrlProvider {

    /**
     * The trailing slash is load-bearing. Ktor's `DefaultRequest` only appends a relative request
     * path (`api/status/version.php`) to the default URL's path when that path ends in `/`;
     * without it, `https://host/wallos` + `api/…` resolves to `https://host/api/…` and every
     * subpath install breaks.
     */
    override fun getBaseUrl(): String {
        val url = serverUrlStorage.serverUrl.trim()
        return if (url.isEmpty()) "" else url.trimEnd('/') + "/"
    }
}
