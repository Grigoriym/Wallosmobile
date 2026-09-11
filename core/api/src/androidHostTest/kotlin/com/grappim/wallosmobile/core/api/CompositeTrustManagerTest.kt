package com.grappim.wallosmobile.core.api

import com.grappim.kit.domain.CertificateHostnameMismatchException
import com.grappim.kit.domain.PendingCertTrust
import com.grappim.kit.domain.findPendingCertTrust
import com.grappim.kit.testing.FakeTrustedCertStorage
import com.grappim.kit.trustmanager.CompositeTrustManager
import com.grappim.kit.trustmanager.sha256Fingerprint
import kotlinx.coroutines.test.runTest
import java.security.cert.CertificateException
import java.security.cert.CertificateExpiredException
import java.util.Date
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
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
 * `CompositeTrustManager`'s own host-taking overload (used by its `Socket`/`SSLEngine` overrides)
 * is `internal` to `grappim-kit-trustmanager` — invisible across the module boundary, unlike when
 * this class was local. [sslEngine] drives a host through the public `SSLEngine` overload instead,
 * using a real JDK-provided engine (`SSLContext.createSSLEngine`) rather than hand-faking the
 * platform's own large `SSLEngine`/`SSLSocket` abstract classes just to carry one string.
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

        sut.checkServerTrusted(arrayOf(certificate), AUTH_TYPE, sslEngine(HOST))

        assertTrue(deviceStore.checkServerTrustedCalled)
    }

    @Test
    fun aRejectedCertificateCarriesItsDetailsUpForTheUserToDecideOn() = runTest {
        val sut = CompositeTrustManager(FakeX509TrustManager(), FakeTrustedCertStorage())

        val failure = assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(certificate), AUTH_TYPE, sslEngine(HOST))
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

        sut.checkServerTrusted(arrayOf(certificate), AUTH_TYPE, sslEngine(HOST))

        assertFalse(deviceStore.checkServerTrustedCalled)
    }

    @Test
    fun aPinDoesNotFollowTheCertificateToAnotherHost() = runTest {
        val trustedCertStorage = FakeTrustedCertStorage()
        trustedCertStorage.trust(pendingCertTrust(HOST, sha256Fingerprint(certificate)))
        val sut = CompositeTrustManager(FakeX509TrustManager(), trustedCertStorage)

        assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(certificate), AUTH_TYPE, sslEngine("other.example.com"))
        }
    }

    @Test
    fun aPinDoesNotCoverASecondCertificateFromTheSameHost() = runTest {
        val trustedCertStorage = FakeTrustedCertStorage()
        trustedCertStorage.trust(pendingCertTrust(HOST, sha256Fingerprint(certificate)))
        val sut = CompositeTrustManager(FakeX509TrustManager(), trustedCertStorage)

        assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(otherCertificate), AUTH_TYPE, sslEngine(HOST))
        }
    }

    @Test
    fun aPinnedCertificateThatHasSinceExpiredIsStillRejected() = runTest {
        val expired = FakeX509Certificate(byteArrayOf(7, 8, 9), notBefore = Date(0), notAfter = Date(1))
        val trustedCertStorage = FakeTrustedCertStorage()
        trustedCertStorage.trust(pendingCertTrust(HOST, sha256Fingerprint(expired)))
        val sut = CompositeTrustManager(FakeX509TrustManager(), trustedCertStorage)

        assertFailsWith<CertificateExpiredException> {
            sut.checkServerTrusted(arrayOf(expired), AUTH_TYPE, sslEngine(HOST))
        }
    }

    @Test
    fun withoutAHostThereIsNothingToOfferTrustFor() = runTest {
        val deviceStore = FakeX509TrustManager()
        val trustedCertStorage = FakeTrustedCertStorage()
        trustedCertStorage.trust(pendingCertTrust(HOST, sha256Fingerprint(certificate)))
        val sut = CompositeTrustManager(deviceStore, trustedCertStorage)

        val failure = assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(certificate), AUTH_TYPE)
        }

        assertTrue(deviceStore.checkServerTrustedCalled)
        assertNull(failure.findPendingCertTrust())
    }

    @Test
    fun aCertificateIssuedForAnotherNameIsNotOfferedForTrust() = runTest {
        val elsewhere = FakeX509Certificate(byteArrayOf(9, 9, 9), commonName = "other.example.com")
        val sut = CompositeTrustManager(FakeX509TrustManager(), FakeTrustedCertStorage())

        val failure = assertFailsWith<CertificateException> {
            sut.checkServerTrusted(arrayOf(elsewhere), AUTH_TYPE, sslEngine(HOST))
        }

        // Accepting it would pin something hostname verification rejects anyway.
        assertNull(failure.findPendingCertTrust())
        assertTrue(failure.cause is CertificateHostnameMismatchException)
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
            sut.checkServerTrusted(arrayOf(byIp), AUTH_TYPE, sslEngine(IP))
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
            sut.checkServerTrusted(arrayOf(byIp), AUTH_TYPE, sslEngine("192.168.0.248"))
        }

        assertNull(failure.findPendingCertTrust())
        assertTrue(failure.cause is CertificateHostnameMismatchException)
    }

    @Test
    fun anEmptyChainIsRejectedOutright() = runTest {
        val sut = CompositeTrustManager(FakeX509TrustManager(rejectsServer = false), FakeTrustedCertStorage())

        assertFailsWith<CertificateException> {
            sut.checkServerTrusted(emptyArray(), AUTH_TYPE)
        }
    }

    /**
     * A real JDK-provided engine, not a fake: [SSLEngine] carries no handshake state until one is
     * started, so its constructor-supplied [SSLEngine.getPeerHost] is enough to drive
     * [CompositeTrustManager.checkServerTrusted]'s host-taking overload without hand-implementing
     * the platform's own abstract class.
     */
    private fun sslEngine(host: String): SSLEngine =
        SSLContext.getInstance("TLS").apply { init(null, null, null) }.createSSLEngine(host, 443)

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
