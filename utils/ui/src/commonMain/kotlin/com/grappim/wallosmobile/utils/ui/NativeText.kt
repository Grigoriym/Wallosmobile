package com.grappim.wallosmobile.utils.ui

import androidx.compose.runtime.Composable
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.core.domain.findPendingCertTrust
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.error_certificate_changed
import com.grappim.wallosmobile.strings.generated.resources.error_forbidden
import com.grappim.wallosmobile.strings.generated.resources.error_in_use
import com.grappim.wallosmobile.strings.generated.resources.error_invalid_api_key
import com.grappim.wallosmobile.strings.generated.resources.error_not_found
import com.grappim.wallosmobile.strings.generated.resources.error_not_wallos
import com.grappim.wallosmobile.strings.generated.resources.error_server
import com.grappim.wallosmobile.strings.generated.resources.error_unreachable
import com.grappim.wallosmobile.strings.generated.resources.error_validation
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A string a non-UI layer can produce without touching a resource loader: the ViewModel picks the
 * variant, the Composable resolves it with [asString].
 */
sealed class NativeText {
    data object Empty : NativeText()

    data class Simple(val text: String) : NativeText()

    data class Resource(val stringResource: StringResource) : NativeText()

    fun isEmpty(): Boolean = this is Empty

    fun isNotEmpty(): Boolean = this !is Empty
}

@Composable
fun NativeText.asString(): String = when (this) {
    is NativeText.Empty -> ""
    is NativeText.Simple -> text
    is NativeText.Resource -> stringResource(stringResource)
}

/**
 * The one place a [Throwable] becomes something a user reads.
 *
 * The wording follows the *failure layer* the error came from (plan §1.1, API doc §5.1), because
 * that is what tells the user which field to fix: a body that never looked like Wallos, or an
 * endpoint the instance doesn't have, means the **URL** is wrong; a well-formed refusal means the
 * URL is right and the **key** is. Anything that isn't a [WallosError] never reached the envelope
 * at all — it is transport, so it points at the URL too, unless the transport carries a
 * certificate the user is being asked to look at.
 */
fun getErrorMessage(throwable: Throwable): NativeText = NativeText.Resource(
    if (throwable is WallosError) {
        // Exhaustive on purpose: a new `WallosError` must fail the build here rather than fall
        // through to a message about the connection.
        when (throwable) {
            is WallosError.Malformed, WallosError.UnsupportedEndpoint -> RString.error_not_wallos
            is WallosError.Unauthenticated -> RString.error_invalid_api_key
            is WallosError.Forbidden -> RString.error_forbidden
            is WallosError.NotFound -> RString.error_not_found
            is WallosError.Validation -> RString.error_validation
            is WallosError.InUse -> RString.error_in_use
            is WallosError.Server -> RString.error_server
        }
    } else if (throwable.findPendingCertTrust() != null) {
        // The one transport failure the user can act on, and the one `error_unreachable` argues
        // *against* acting on: the connection worked until the certificate changed (plan §4.5).
        RString.error_certificate_changed
    } else {
        RString.error_unreachable
    }
)
