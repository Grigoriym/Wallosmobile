package com.grappim.wallosmobile.core.crud

import com.grappim.wallosmobile.core.api.FormParams

/**
 * The four `set_*.php` operations plus their `get_*.php` counterpart, shared byte-for-byte by
 * categories, household members and payment methods (`docs/WALLOS_API.md` §3.10). [WallosCrudApi]
 * is the one implementation; a feature's own API interface extends this rather than repeating it.
 */
interface CrudApi<T : CrudResource> {

    suspend fun getAll(): List<T>

    /** @return the id the server assigned the new row. */
    suspend fun add(fields: FormParams): Int

    suspend fun edit(id: Int, fields: FormParams)

    /** @throws com.grappim.wallosmobile.core.domain.WallosError.InUse if the row is referenced elsewhere. */
    suspend fun delete(id: Int)
}
