package com.grappim.wallosmobile.core.domain

import kotlinx.serialization.Serializable

/**
 * Everything a user needs to see before deciding to trust a certificate the device's CA store
 * rejected — a self-signed one, or a homelab CA's, which is what nearly every Wallos instance
 * presents (plan §4.5).
 *
 * Deliberately free of `java.security.cert.X509Certificate`: the trust decision is made on a
 * screen, and a screen lives in `commonMain`. `@Serializable` so `TrustedCertStorageImpl`
 * (core:storage) can JSON-encode the accepted list rather than storing a lossy `host|fingerprint`
 * string (18.1).
 */
@Serializable
data class PendingCertTrust(
    val host: String,
    val subject: String,
    val issuer: String,
    val notBefore: String,
    val notAfter: String,
    val sha256Fingerprint: String
)
