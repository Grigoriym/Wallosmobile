package com.grappim.wallosmobile.core.storage.cert

import com.grappim.wallosmobile.core.storage.FakePreferencesDataStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
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

        storage.trust(HOST, FINGERPRINT)

        assertTrue(storage.isTrusted(HOST, FINGERPRINT))
    }

    @Test
    fun `a pin does not follow the certificate to another host`() = runTest {
        val storage = TrustedCertStorageImpl(FakePreferencesDataStore())

        storage.trust(HOST, FINGERPRINT)

        assertFalse(storage.isTrusted("other.example.com", FINGERPRINT))
    }

    @Test
    fun `a pin does not cover a second certificate on the same host`() = runTest {
        val storage = TrustedCertStorageImpl(FakePreferencesDataStore())

        storage.trust(HOST, FINGERPRINT)

        assertFalse(storage.isTrusted(HOST, "99:88:77"))
    }

    @Test
    fun `trusting a second host keeps the first one`() = runTest {
        val storage = TrustedCertStorageImpl(FakePreferencesDataStore())

        storage.trust(HOST, FINGERPRINT)
        storage.trust("other.example.com", "99:88:77")

        assertTrue(storage.isTrusted(HOST, FINGERPRINT))
        assertTrue(storage.isTrusted("other.example.com", "99:88:77"))
    }

    @Test
    fun `accepting the same certificate twice stores one pin`() = runTest {
        val storage = TrustedCertStorageImpl(FakePreferencesDataStore())

        storage.trust(HOST, FINGERPRINT)
        storage.trust(HOST, FINGERPRINT)

        assertTrue(storage.isTrusted(HOST, FINGERPRINT))
    }

    private companion object {
        private const val HOST = "wallos.example.com"
        private const val FINGERPRINT = "AA:BB:CC"
    }
}
