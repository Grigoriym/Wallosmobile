package com.grappim.wallosmobile.core.api

import com.grappim.wallosmobile.core.domain.PendingCertTrust
import com.grappim.wallosmobile.core.domain.findPendingCertTrust
import com.grappim.wallosmobile.testing.FakeTrustedCertStorage
import kotlinx.coroutines.test.runTest
import java.security.cert.CertificateException
import java.security.cert.CertificateExpiredException
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A host test, not an instrumented one: `javax.net.ssl` is the JDK's, not Android's, and nothing
 * here opens a socket.
 *
 * Test names are camelCase because detekt's `FunctionNaming` exclusions cover `commonTest` and
 * not `androidHostTest` — the same reason `WallosDBTest` (3.3) reads that way.
 */
class CompositeTrustManagerTest {

    private val certificate = FakeX509Certificate(byteArrayOf(1, 2, 3))
    private val otherCertificate = FakeX509Certificate(byteArrayOf(4, 5, 6))

    @Test
    fun unpinnedCertificateIsLeftToTheDeviceTrustStore() = runTest {
        val deviceStore = FakeX509TrustManager(rejectsServer = false)
        val sut = CompositeTrustManager(deviceStore, FakeTrustedCertStorage())

        sut.checkServerTrusted(arrayOf(certificate), AUTH_TYPE, HOST)

        assertTrue(deviceStore.checkServerTrustedCalled)
    }

    @Test
    fun aRejectedCertificateCarriesItsDetailsUpForTheUserToDecideOn() = runTest {
        val sut = CompositeTrustManager(FakeX509TrustManager(), FakeTrustedCertStorage())

        val failure = assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(certificate), AUTH_TYPE, HOST)
        }

        val pendingCertTrust = assertNotNull(failure.findPendingCertTrust())
        assertEquals(HOST, pendingCertTrust.host)
        assertEquals(sha256Fingerprint(certificate), pendingCertTrust.sha256Fingerprint)
        assertEquals("CN=$HOST", pendingCertTrust.subject)
        assertEquals("CN=Fake Issuer", pendingCertTrust.issuer)
    }

    @Test
    fun aPinnedCertificateIsAcceptedWithoutConsultingTheDeviceTrustStore() = runTest {
        val deviceStore = FakeX509TrustManager()
        val trustedCertStorage = FakeTrustedCertStorage()
        trustedCertStorage.trust(pendingCertTrust(HOST, sha256Fingerprint(certificate)))
        val sut = CompositeTrustManager(deviceStore, trustedCertStorage)

        sut.checkServerTrusted(arrayOf(certificate), AUTH_TYPE, HOST)

        assertFalse(deviceStore.checkServerTrustedCalled)
    }

    @Test
    fun aPinDoesNotFollowTheCertificateToAnotherHost() = runTest {
        val trustedCertStorage = FakeTrustedCertStorage()
        trustedCertStorage.trust(pendingCertTrust(HOST, sha256Fingerprint(certificate)))
        val sut = CompositeTrustManager(FakeX509TrustManager(), trustedCertStorage)

        assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(certificate), AUTH_TYPE, "other.example.com")
        }
    }

    @Test
    fun aPinDoesNotCoverASecondCertificateFromTheSameHost() = runTest {
        val trustedCertStorage = FakeTrustedCertStorage()
        trustedCertStorage.trust(pendingCertTrust(HOST, sha256Fingerprint(certificate)))
        val sut = CompositeTrustManager(FakeX509TrustManager(), trustedCertStorage)

        assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(otherCertificate), AUTH_TYPE, HOST)
        }
    }

    @Test
    fun aPinnedCertificateThatHasSinceExpiredIsStillRejected() = runTest {
        val expired = FakeX509Certificate(byteArrayOf(7, 8, 9), notBefore = Date(0), notAfter = Date(1))
        val trustedCertStorage = FakeTrustedCertStorage()
        trustedCertStorage.trust(pendingCertTrust(HOST, sha256Fingerprint(expired)))
        val sut = CompositeTrustManager(FakeX509TrustManager(), trustedCertStorage)

        assertFailsWith<CertificateExpiredException> {
            sut.checkServerTrusted(arrayOf(expired), AUTH_TYPE, HOST)
        }
    }

    @Test
    fun withoutAHostThereIsNothingToOfferTrustFor() = runTest {
        val deviceStore = FakeX509TrustManager()
        val trustedCertStorage = FakeTrustedCertStorage()
        trustedCertStorage.trust(pendingCertTrust(HOST, sha256Fingerprint(certificate)))
        val sut = CompositeTrustManager(deviceStore, trustedCertStorage)

        val failure = assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(certificate), AUTH_TYPE, host = null)
        }

        assertTrue(deviceStore.checkServerTrustedCalled)
        assertNull(failure.findPendingCertTrust())
    }

    @Test
    fun aCertificateIssuedForAnotherNameIsNotOfferedForTrust() = runTest {
        val elsewhere = FakeX509Certificate(byteArrayOf(9, 9, 9), commonName = "other.example.com")
        val sut = CompositeTrustManager(FakeX509TrustManager(), FakeTrustedCertStorage())

        val failure = assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(elsewhere), AUTH_TYPE, HOST)
        }

        // Accepting it would pin something hostname verification rejects anyway.
        assertNull(failure.findPendingCertTrust())
    }

    @Test
    fun aCertificateWhoseSanCoversTheAddressIsOfferedForTrust() = runTest {
        val byIp = FakeX509Certificate(
            byteArrayOf(9, 9, 9),
            commonName = "not-this-one",
            subjectAlternativeNames = listOf(listOf(SAN_IP_ADDRESS, IP))
        )
        val sut = CompositeTrustManager(FakeX509TrustManager(), FakeTrustedCertStorage())

        val failure = assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(byIp), AUTH_TYPE, IP)
        }

        assertNotNull(failure.findPendingCertTrust())
    }

    @Test
    fun aCertificateWhoseSanCoversADifferentAddressIsNotOfferedForTrust() = runTest {
        val byIp = FakeX509Certificate(
            byteArrayOf(9, 9, 9),
            subjectAlternativeNames = listOf(listOf(SAN_IP_ADDRESS, IP))
        )
        val sut = CompositeTrustManager(FakeX509TrustManager(), FakeTrustedCertStorage())

        val failure = assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(byIp), AUTH_TYPE, "192.168.0.248")
        }

        assertNull(failure.findPendingCertTrust())
    }

    @Test
    fun anEmptyChainIsRejectedOutright() = runTest {
        val sut = CompositeTrustManager(FakeX509TrustManager(rejectsServer = false), FakeTrustedCertStorage())

        assertFailsWith<CertificateException> {
            sut.checkServerTrusted(emptyArray(), AUTH_TYPE, HOST)
        }
    }

    private fun pendingCertTrust(host: String, sha256Fingerprint: String) = PendingCertTrust(
        host = host,
        subject = "CN=$host",
        issuer = "CN=Fake Issuer",
        notBefore = "2026-01-01",
        notAfter = "2027-01-01",
        sha256Fingerprint = sha256Fingerprint
    )

    private companion object {
        private const val HOST = "wallos.example.com"
        private const val IP = "192.168.0.241"
        private const val AUTH_TYPE = "RSA"
        private const val SAN_IP_ADDRESS = 7
    }
}
