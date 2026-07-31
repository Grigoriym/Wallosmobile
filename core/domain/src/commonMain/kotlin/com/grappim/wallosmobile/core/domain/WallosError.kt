package com.grappim.wallosmobile.core.domain

/**
 * Every Wallos failure a client can act on. Wallos answers `200 OK` to almost everything, so
 * these come from the JSON envelope's `title` rather than from the HTTP status — see
 * `docs/WALLOS_API.md` §5.
 */
sealed class WallosError : Exception() {

    /** Key is bad/revoked — clear stored credentials, return to setup. */
    data class Unauthenticated(val title: String) : WallosError()

    /** Key is fine, this user just can't do it — keep the key. */
    data class Forbidden(val title: String) : WallosError()

    /**
     * Missing row, or a row owned by someone else. `Unauthorized or Not Found` lands here
     * despite its name: it is a per-row ownership check, not an auth failure, and clearing
     * the key on it logs the user out at random.
     */
    data class NotFound(val detail: String) : WallosError()

    data class Validation(val title: String, val detail: String) : WallosError()

    /** A delete guard: the row is referenced elsewhere, or is a default row. */
    data class InUse(val detail: String) : WallosError()

    /** `Database error` and friends — log [detail], show something generic. */
    data class Server(val detail: String) : WallosError()

    /** Endpoint absent: feature unsupported on this Wallos version. */
    data object UnsupportedEndpoint : WallosError()

    /** Body wasn't JSON, or PHP diagnostics were mixed in. */
    data class Malformed(val body: String) : WallosError()
}
