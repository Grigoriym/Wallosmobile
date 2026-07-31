package com.grappim.wallosmobile.core.api

import com.grappim.wallosmobile.core.domain.WallosError
import kotlin.test.Test
import kotlin.test.assertEquals

class WallosErrorMapperTest {

    /**
     * `docs/WALLOS_API.md` §5.3, verbatim. Eleven rows of endpoint-specific inconsistency for
     * the one decision that must never regress: does this failure clear the stored API key?
     */
    private data class AuthCase(val situation: String, val title: String, val detail: String, val expected: WallosError)

    private val authFailures = listOf(
        // `Missing parameters` is overloaded — get_monthly_cost also returns it for a missing
        // month/year, with nothing to distinguish them. It stays Validation: our client always
        // sends the key, so a missing one is a client bug, not a reason to log the user out.
        AuthCase(
            situation = "key absent, all get_*",
            title = "Missing parameters",
            detail = "Missing parameters",
            expected = WallosError.Validation("Missing parameters", "Missing parameters")
        ),
        AuthCase(
            situation = "key absent, all set_*",
            title = "Missing API key",
            detail = "API key is required.",
            expected = WallosError.Unauthenticated("Missing API key")
        ),
        AuthCase(
            situation = "key absent, get_subscription.php",
            title = "Missing parameters",
            detail = "Both API key and subscription ID are required.",
            expected = WallosError.Validation(
                "Missing parameters",
                "Both API key and subscription ID are required."
            )
        ),
        AuthCase(
            situation = "key invalid, all get_*",
            title = "Invalid API key",
            detail = "Invalid API key",
            expected = WallosError.Unauthenticated("Invalid API key")
        ),
        AuthCase(
            situation = "key invalid, get_subscription.php",
            title = "Invalid API key",
            detail = "Unauthorized access.",
            expected = WallosError.Unauthenticated("Invalid API key")
        ),
        AuthCase(
            situation = "key invalid, get_monthly_cost.php (detail arrives in notes)",
            title = "Invalid API key",
            detail = "User not found or API key invalid.",
            expected = WallosError.Unauthenticated("Invalid API key")
        ),
        AuthCase(
            situation = "key invalid, all set_*",
            title = "Unauthorized",
            detail = "Invalid API key.",
            expected = WallosError.Unauthenticated("Unauthorized")
        ),
        AuthCase(
            situation = "key invalid, set_disable_password_login.php",
            title = "Unauthorized",
            detail = "Invalid API key or insufficient privileges.",
            expected = WallosError.Unauthenticated("Unauthorized")
        ),
        AuthCase(
            situation = "valid key, not admin, admin get_*",
            title = "Invalid user",
            detail = "Invalid user",
            expected = WallosError.Forbidden("Invalid user")
        ),
        AuthCase(
            situation = "valid key, not admin, admin set_*",
            title = "Forbidden",
            detail = "Only the admin user (user ID 1) can update this.",
            expected = WallosError.Forbidden("Forbidden")
        ),
        AuthCase(
            situation = "valid key, not admin, get_subscriptions.php + all-user-subscription=1",
            title = "Denied. Not admin user",
            detail = "Denied. Not admin user",
            expected = WallosError.Forbidden("Denied. Not admin user")
        )
    )

    @Test
    fun `auth failure titles map exactly as the API doc table says`() {
        authFailures.forEach { case ->
            assertEquals(case.expected, mapError(case.title, case.detail), case.situation)
        }
    }

    @Test
    fun `Unauthorized or Not Found is an ownership check, not an auth failure`() {
        val result = mapError("Unauthorized or Not Found", "Unauthorized or Not Found")

        assertEquals(WallosError.NotFound("Unauthorized or Not Found"), result)
    }

    @Test
    fun `Subscription not found maps to NotFound`() {
        assertEquals(WallosError.NotFound("nope"), mapError("Subscription not found", "nope"))
    }

    @Test
    fun `delete guards map to InUse`() {
        listOf(
            "Category in use",
            "Member in use",
            "Payment method in use",
            "Cannot delete category",
            "Cannot delete member",
            "Cannot delete currency"
        ).forEach { title ->
            assertEquals(WallosError.InUse("detail"), mapError(title, "detail"), title)
        }
    }

    @Test
    fun `validation titles map to Validation`() {
        listOf(
            "Invalid parameter",
            "Invalid cycle",
            "Invalid name",
            "Invalid date",
            "Invalid colors",
            "Invalid currency ID",
            "Invalid provider",
            "Invalid file type",
            "Missing parameter",
            "Validation error",
            "Color validation failed"
        ).forEach { title ->
            assertEquals(WallosError.Validation(title, "detail"), mapError(title, "detail"), title)
        }
    }

    @Test
    fun `an absent title is a server failure`() {
        assertEquals(WallosError.Server("detail"), mapError(null, "detail"))
    }

    @Test
    fun `unrecognised titles fall through to Server`() {
        listOf(
            "Database error",
            "Configuration error",
            "Security Block",
            "Environment override",
            "Managed by environment",
            "A title this client has never seen"
        ).forEach { title ->
            assertEquals(WallosError.Server("detail"), mapError(title, "detail"), title)
        }
    }
}
