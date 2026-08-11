package com.grappim.wallosmobile.feature.settings.ui.trustedcerts

import app.cash.turbine.test
import com.grappim.wallosmobile.core.domain.PendingCertTrust
import com.grappim.wallosmobile.testing.FakeTrustedCertStorage
import com.grappim.wallosmobile.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrustedCertsViewModelTest {

    private val trustedCertStorage = FakeTrustedCertStorage()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun viewModel() = TrustedCertsViewModel(trustedCertStorage = trustedCertStorage)

    @Test
    fun `an empty store shows no certificates`() = runTest {
        assertTrue(viewModel().uiState.value.certs.isEmpty())
    }

    @Test
    fun `every trusted pin shows up in the list`() = runTest {
        trustedCertStorage.trust(CERT)

        assertEquals(listOf(CERT), viewModel().uiState.value.certs)
    }

    @Test
    fun `clicking revoke opens the confirm dialog for that certificate`() = runTest {
        trustedCertStorage.trust(CERT)
        val viewModel = viewModel()

        viewModel.uiState.value.onRevokeClick(CERT)

        assertEquals(CERT, viewModel.uiState.value.certPendingRevoke)
    }

    @Test
    fun `confirming revoke removes the pin from storage and the list`() = runTest {
        trustedCertStorage.trust(CERT)
        val viewModel = viewModel()
        viewModel.uiState.value.onRevokeClick(CERT)

        viewModel.uiState.value.onRevokeConfirm()

        assertFalse(trustedCertStorage.isTrusted(CERT.host, CERT.sha256Fingerprint))
        assertTrue(viewModel.uiState.value.certs.isEmpty())
        assertNull(viewModel.uiState.value.certPendingRevoke)
    }

    @Test
    fun `dismissing the dialog leaves the pin trusted`() = runTest {
        trustedCertStorage.trust(CERT)
        val viewModel = viewModel()
        viewModel.uiState.value.onRevokeClick(CERT)

        viewModel.uiState.value.onRevokeDialogDismiss()

        assertNull(viewModel.uiState.value.certPendingRevoke)
        assertTrue(trustedCertStorage.isTrusted(CERT.host, CERT.sha256Fingerprint))
    }

    // The list is not held locally: it is read back off the flow, the same one `CompositeTrustManager`
    // consults, so a revoke made anywhere else in the app is reflected here too.
    @Test
    fun `the list follows storage, not the tap`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertTrue(awaitItem().certs.isEmpty())

            trustedCertStorage.trust(CERT)
            assertEquals(listOf(CERT), awaitItem().certs)

            trustedCertStorage.untrust(CERT.host, CERT.sha256Fingerprint)
            assertTrue(awaitItem().certs.isEmpty())
        }
    }

    private companion object {
        private val CERT = PendingCertTrust(
            host = "wallos.example.com",
            subject = "CN=wallos.example.com",
            issuer = "CN=Test CA",
            notBefore = "2026-01-01",
            notAfter = "2027-01-01",
            sha256Fingerprint = "AA:BB:CC"
        )
    }
}
