package com.grappim.wallosmobile.testing

import com.grappim.wallosmobile.core.storage.cert.TrustedCertStorage

/** An empty pin set is the honest default: a device that has trusted nothing yet. */
class FakeTrustedCertStorage : TrustedCertStorage {

    private val pins = mutableSetOf<Pair<String, String>>()

    override suspend fun isTrusted(host: String, sha256Fingerprint: String): Boolean =
        (host to sha256Fingerprint) in pins

    override suspend fun trust(host: String, sha256Fingerprint: String) {
        pins += host to sha256Fingerprint
    }
}
