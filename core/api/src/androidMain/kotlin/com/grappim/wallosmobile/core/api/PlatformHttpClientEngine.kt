package com.grappim.wallosmobile.core.api

import com.grappim.wallosmobile.core.storage.cert.TrustedCertStorage
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

actual fun createPlatformHttpClientEngine(trustedCertStorage: TrustedCertStorage): HttpClientEngine {
    val trustManager = CompositeTrustManager(systemTrustManager(), trustedCertStorage)
    val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf<TrustManager>(trustManager), null)
    }

    return OkHttp.create {
        config {
            // Hostname verification is left alone — this replaces *chain* trust, nothing else.
            sslSocketFactory(sslContext.socketFactory, trustManager)
        }
    }
}

/** A `null` KeyStore means the platform's own trust store, which is what it falls back to. */
private fun systemTrustManager(): X509TrustManager {
    val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    factory.init(null as KeyStore?)
    return factory.trustManagers.filterIsInstance<X509TrustManager>().first()
}
