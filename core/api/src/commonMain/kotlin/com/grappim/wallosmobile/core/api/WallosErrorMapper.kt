package com.grappim.wallosmobile.core.api

import com.grappim.wallosmobile.core.domain.WallosError

/** Key is bad, revoked or absent — clearing the stored key is the right response. */
private val AUTH_TITLES = setOf("Invalid API key", "Unauthorized", "Missing API key")

/**
 * Key is fine, the user just isn't the admin (`user.id === 1`). Clearing the key here would log
 * a perfectly valid user out.
 */
private val PERMISSION_TITLES = setOf("Invalid user", "Forbidden", "Denied. Not admin user")

/**
 * `Unauthorized or Not Found` belongs here despite its name — it is the per-row ownership check
 * on categories, currencies, household members and payment methods, not an auth failure.
 */
private val NOT_FOUND_TITLES = setOf("Subscription not found", "Unauthorized or Not Found")

/**
 * Maps the envelope's `title` onto a [WallosError]. Wallos has no error codes: the title *is*
 * the code, it is English, and it is inconsistent per endpoint — see `docs/WALLOS_API.md` §5.3
 * and §5.4 for the full catalogue.
 *
 * This lives in `core:api` rather than next to [WallosError] so that `core:domain` stays free of
 * API knowledge.
 */
internal fun mapError(title: String?, detail: String): WallosError = when {
    title == null -> WallosError.Server(detail)

    title in AUTH_TITLES -> WallosError.Unauthenticated(title)

    title in PERMISSION_TITLES -> WallosError.Forbidden(title)

    title in NOT_FOUND_TITLES -> WallosError.NotFound(detail)

    title.endsWith(" in use") -> WallosError.InUse(detail)

    title.startsWith("Cannot delete") -> WallosError.InUse(detail)

    title.startsWith("Invalid") ||
        title.startsWith("Missing") ||
        title.startsWith("Validation") ||
        title.startsWith("Color") -> WallosError.Validation(title, detail)

    else -> WallosError.Server(detail)
}
