package com.grappim.wallosmobile.testing

import com.grappim.wallosmobile.core.domain.PendingCertTrust
import com.grappim.wallosmobile.core.storage.cert.TrustedCertStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** An empty pin list is the honest default: a device that has trusted nothing yet. */
class FakeTrustedCertStorage : TrustedCertStorage {

    private val _pins = MutableStateFlow<List<PendingCertTrust>>(emptyList())
    val pins: StateFlow<List<PendingCertTrust>> = _pins

    override suspend fun isTrusted(host: String, sha256Fingerprint: String): Boolean =
        _pins.value.any { it.host == host && it.sha256Fingerprint == sha256Fingerprint }

    override suspend fun trust(pendingCertTrust: PendingCertTrust) {
        _pins.value = _pins.value.filterNot {
            it.host == pendingCertTrust.host && it.sha256Fingerprint == pendingCertTrust.sha256Fingerprint
        } + pendingCertTrust
    }

    override fun getAllFlow(): StateFlow<List<PendingCertTrust>> = _pins

    override suspend fun untrust(host: String, sha256Fingerprint: String) {
        _pins.value = _pins.value.filterNot { it.host == host && it.sha256Fingerprint == sha256Fingerprint }
    }
}
