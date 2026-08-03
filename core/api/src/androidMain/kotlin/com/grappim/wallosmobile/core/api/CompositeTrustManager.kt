package com.grappim.wallosmobile.core.api

import com.grappim.wallosmobile.core.domain.PendingCertTrust
import com.grappim.wallosmobile.core.domain.UntrustedCertificateException
import com.grappim.wallosmobile.core.storage.cert.TrustedCertStorage
import kotlinx.coroutines.runBlocking
import java.net.Socket
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager

/**
 * The device's CA store, plus the certificates this user has accepted for a specific host
 * (trust-on-first-use). Ported from TaigaMobileNova, where the alternatives were weighed — see
 * its `docs/features/private-cert-trust/plan.md`; it matters more here, since a Wallos instance
 * is nearly always self-hosted behind a certificate its owner minted.
 *
 * Two things it deliberately does *not* do, both flaws found in the implementations that survey
 * looked at: a pin is scoped to the host it was accepted for, and validity is still checked on
 * every connection, so an accepted certificate does not become trusted forever.
 */
internal class CompositeTrustManager(
    private val defaultTrustManager: X509TrustManager,
    private val trustedCertStorage: TrustedCertStorage
) : X509ExtendedTrustManager() {

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
        defaultTrustManager.checkClientTrusted(chain, authType)
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String, socket: Socket) {
        defaultTrustManager.checkClientTrusted(chain, authType)
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String, engine: SSLEngine) {
        defaultTrustManager.checkClientTrusted(chain, authType)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
        checkServerTrusted(chain, authType, host = null)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, socket: Socket) {
        checkServerTrusted(chain, authType, (socket as? SSLSocket)?.handshakeSession?.peerHost)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, engine: SSLEngine) {
        checkServerTrusted(chain, authType, engine.peerHost)
    }

    // Takes the host explicitly, rather than only the socket/engine overrides above, so the
    // pinning decision is testable without a real handshake.
    internal fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, host: String?) {
        val leaf = chain.firstOrNull() ?: throw CertificateException("Empty certificate chain")

        // A JSSE callback has no suspension point, so the pin lookup blocks here rather than
        // making the whole handshake path suspend.
        val isPinned = host != null && runBlocking { trustedCertStorage.isTrusted(host, sha256Fingerprint(leaf)) }
        if (isPinned) {
            // Still checked on a pin hit: accepting a certificate once is not accepting it after
            // it expires.
            leaf.checkValidity()
            return
        }

        try {
            defaultTrustManager.checkServerTrusted(chain, authType)
        } catch (e: CertificateException) {
            // Nothing to offer trust for without a host, and nothing to offer it for when the
            // certificate doesn't cover this host either: hostname verification is a separate
            // step further on that a pin cannot satisfy, so a dialog there would only collect an
            // acceptance that can never produce a working connection.
            if (host == null || !hostMatchesCertificate(host, leaf)) throw e
            throw CertificateException(UntrustedCertificateException(pendingCertTrust(host, leaf)))
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = defaultTrustManager.acceptedIssuers

    private fun pendingCertTrust(host: String, certificate: X509Certificate): PendingCertTrust = PendingCertTrust(
        host = host,
        subject = certificate.subjectX500Principal.name,
        issuer = certificate.issuerX500Principal.name,
        notBefore = formatDate(certificate.notBefore),
        notAfter = formatDate(certificate.notAfter),
        sha256Fingerprint = sha256Fingerprint(certificate)
    )

    private fun formatDate(date: Date): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)

    private fun hostMatchesCertificate(host: String, certificate: X509Certificate): Boolean {
        val sanValues = runCatching { certificate.subjectAlternativeNames }.getOrNull().orEmpty()
            .mapNotNull { entry ->
                val type = entry.getOrNull(0) as? Int
                (entry.getOrNull(1) as? String).takeIf { type == SAN_DNS_NAME || type == SAN_IP_ADDRESS }
            }
        if (sanValues.isNotEmpty()) return sanValues.any { matchesHostname(host, it) }

        // No SANs at all, which is common for the hand-rolled certificates this feature exists
        // for: fall back to the subject's CN, as hostname verification itself used to.
        val commonName = certificate.subjectX500Principal.name
            .split(",")
            .map { it.trim() }
            .firstOrNull { it.startsWith("CN=", ignoreCase = true) }
            ?.substringAfter("=")
        return commonName != null && matchesHostname(host, commonName)
    }

    private fun matchesHostname(host: String, pattern: String): Boolean = when {
        pattern.equals(host, ignoreCase = true) -> true

        pattern.startsWith("*.") ->
            host.length > pattern.length - 1 && host.endsWith(pattern.substring(1), ignoreCase = true)

        else -> false
    }

    private companion object {
        private const val SAN_DNS_NAME = 2
        private const val SAN_IP_ADDRESS = 7
    }
}

internal fun sha256Fingerprint(certificate: X509Certificate): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
    return digest.joinToString(":") { byte -> "%02X".format(byte) }
}
