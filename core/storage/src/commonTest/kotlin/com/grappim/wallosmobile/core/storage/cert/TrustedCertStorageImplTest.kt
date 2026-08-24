package com.grappim.wallosmobile.core.storage.cert

import com.grappim.wallosmobile.core.domain.PendingCertTrust
import com.grappim.wallosmobile.core.storage.FakePreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrustedCertStorageImplTest {

    @Test
    fun `nothing is trusted until it is accepted`() = runTest {
        val storage = TrustedCertStorageImpl(FakePreferencesDataStore())

        assertFalse(storage.isTrusted(HOST, FINGERPRINT))
    }

    @Test
    fun `an accepted certificate is trusted for its host`() = runTest {
        val storage = TrustedCertStorageImpl(FakePreferencesDataStore())

        storage.trust(pendingCertTrust(HOST, FINGERPRINT))

        assertTrue(storage.isTrusted(HOST, FINGERPRINT))
    }

    @Test
    fun `a pin does not follow the certificate to another host`() = runTest {
        val storage = TrustedCertStorageImpl(FakePreferencesDataStore())

        storage.trust(pendingCertTrust(HOST, FINGERPRINT))

        assertFalse(storage.isTrusted("other.example.com", FINGERPRINT))
    }

    @Test
    fun `a pin does not cover a second certificate on the same host`() = runTest {
        val storage = TrustedCertStorageImpl(FakePreferencesDataStore())

        storage.trust(pendingCertTrust(HOST, FINGERPRINT))

        assertFalse(storage.isTrusted(HOST, "99:88:77"))
    }

    @Test
    fun `trusting a second host keeps the first one`() = runTest {
        val storage = TrustedCertStorageImpl(FakePreferencesDataStore())

        storage.trust(pendingCertTrust(HOST, FINGERPRINT))
        storage.trust(pendingCertTrust("other.example.com", "99:88:77"))

        assertTrue(storage.isTrusted(HOST, FINGERPRINT))
        assertTrue(storage.isTrusted("other.example.com", "99:88:77"))
    }

    @Test
    fun `accepting the same certificate twice stores one pin`() = runTest {
        val storage = TrustedCertStorageImpl(FakePreferencesDataStore())

        storage.trust(pendingCertTrust(HOST, FINGERPRINT))
        storage.trust(pendingCertTrust(HOST, FINGERPRINT))

        assertTrue(storage.isTrusted(HOST, FINGERPRINT))
        assertEquals(1, storage.getAllFlow().first().size)
    }

    @Test
    fun `getAllFlow reflects every trusted pin with its full detail`() = runTest {
        val storage = TrustedCertStorageImpl(FakePreferencesDataStore())
        val entry = pendingCertTrust(HOST, FINGERPRINT)

        storage.trust(entry)

        assertEquals(listOf(entry), storage.getAllFlow().first())
    }

    @Test
    fun `untrust removes the pin`() = runTest {
        val storage = TrustedCertStorageImpl(FakePreferencesDataStore())
        storage.trust(pendingCertTrust(HOST, FINGERPRINT))

        storage.untrust(HOST, FINGERPRINT)

        assertFalse(storage.isTrusted(HOST, FINGERPRINT))
        assertTrue(storage.getAllFlow().first().isEmpty())
    }

    @Test
    fun `untrust leaves other pins alone`() = runTest {
        val storage = TrustedCertStorageImpl(FakePreferencesDataStore())
        storage.trust(pendingCertTrust(HOST, FINGERPRINT))
        storage.trust(pendingCertTrust("other.example.com", "99:88:77"))

        storage.untrust(HOST, FINGERPRINT)

        assertFalse(storage.isTrusted(HOST, FINGERPRINT))
        assertTrue(storage.isTrusted("other.example.com", "99:88:77"))
    }

    @Test
    fun `untrust on an unknown pin is a no-op`() = runTest {
        val storage = TrustedCertStorageImpl(FakePreferencesDataStore())

        storage.untrust(HOST, FINGERPRINT)

        assertTrue(storage.getAllFlow().first().isEmpty())
    }

    private fun pendingCertTrust(host: String, sha256Fingerprint: String) = PendingCertTrust(
        host = host,
        subject = "CN=$host",
        issuer = "CN=Test CA",
        notBefore = "2026-01-01",
        notAfter = "2027-01-01",
        sha256Fingerprint = sha256Fingerprint
    )

    private companion object {
        private const val HOST = "wallos.example.com"
        private const val FINGERPRINT = "AA:BB:CC"
    }
}
