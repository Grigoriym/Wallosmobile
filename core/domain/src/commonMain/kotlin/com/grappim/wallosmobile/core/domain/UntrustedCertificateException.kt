package com.grappim.wallosmobile.core.domain

/**
 * The server presented a certificate the device doesn't trust and that isn't pinned for this host
 * yet. Alone among transport failures this one the user can resolve, by reading
 * [pendingCertTrust] and accepting it — so it carries the certificate rather than being another
 * "couldn't reach the server".
 *
 * It is *never* the exception a caller catches. JSSE only lets a trust manager throw
 * `CertificateException`, which the TLS stack then wraps again on its way out, so this travels
 * as a cause somewhere down that chain — [findPendingCertTrust] is how it is read back.
 */
class UntrustedCertificateException(val pendingCertTrust: PendingCertTrust) : Exception()

/**
 * Walks the cause chain for an [UntrustedCertificateException]. Matching on the caught type
 * alone would find nothing: by the time a failed handshake reaches a repository it is an
 * `SSLHandshakeException` wrapping a `CertificateException` wrapping this one, and how many
 * layers deep depends on the engine.
 */
fun Throwable.findPendingCertTrust(): PendingCertTrust? = generateSequence(this) { it.cause }
    .filterIsInstance<UntrustedCertificateException>()
    .firstOrNull()
    ?.pendingCertTrust
