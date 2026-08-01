package com.grappim.wallosmobile.utils.ui

import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.error_forbidden
import com.grappim.wallosmobile.strings.generated.resources.error_in_use
import com.grappim.wallosmobile.strings.generated.resources.error_invalid_api_key
import com.grappim.wallosmobile.strings.generated.resources.error_not_found
import com.grappim.wallosmobile.strings.generated.resources.error_not_wallos
import com.grappim.wallosmobile.strings.generated.resources.error_server
import com.grappim.wallosmobile.strings.generated.resources.error_unreachable
import com.grappim.wallosmobile.strings.generated.resources.error_validation
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The point of these rows is the *attribution* (plan §1.1): which failure layer sends the user to
 * the URL field and which sends them to the key.
 */
class GetErrorMessageTest {

    private val cases = listOf(
        WallosError.Malformed("<br /><b>Warning</b>") to RString.error_not_wallos,
        WallosError.UnsupportedEndpoint to RString.error_not_wallos,
        WallosError.Unauthenticated("Unauthorized") to RString.error_invalid_api_key,
        WallosError.Forbidden("Forbidden") to RString.error_forbidden,
        WallosError.NotFound("Unauthorized or Not Found") to RString.error_not_found,
        WallosError.Validation("Error", "name is required") to RString.error_validation,
        WallosError.InUse("Category in use") to RString.error_in_use,
        WallosError.Server("Database error") to RString.error_server
    )

    @Test
    fun `every WallosError maps to its own message`() {
        cases.forEach { (error, expected) ->
            assertEquals(NativeText.Resource(expected), getErrorMessage(error), error.toString())
        }
    }

    @Test
    fun `anything that never reached the envelope reads as a connection problem`() {
        assertEquals(
            NativeText.Resource(RString.error_unreachable),
            getErrorMessage(IllegalStateException("connection refused"))
        )
    }
}
