package com.grappim.wallosmobile.core.api

import com.grappim.wallosmobile.core.storage.cert.TrustedCertStorage
import io.ktor.client.engine.HttpClientEngine

/**
 * The engine every client here is built on. It exists only because the engine is where TLS trust
 * is configured (plan §4.5) — Ktor would otherwise autodiscover the same one, and did until 3.7.
 */
expect fun createPlatformHttpClientEngine(trustedCertStorage: TrustedCertStorage): HttpClientEngine
