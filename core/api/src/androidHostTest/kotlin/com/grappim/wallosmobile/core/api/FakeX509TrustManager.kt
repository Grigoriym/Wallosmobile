package com.grappim.wallosmobile.core.api

import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/** Stands in for the device's CA store: it either accepts the chain or it doesn't. */
internal class FakeX509TrustManager(private val rejectsServer: Boolean = true) : X509TrustManager {

    var checkServerTrustedCalled = false
        private set

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) = Unit

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
        checkServerTrustedCalled = true
        if (rejectsServer) throw CertificateException("untrusted")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
