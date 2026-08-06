package com.grappim.wallosmobile.core.crud

/**
 * What tells [WallosCrudApi] apart from one resource to the next (`docs/WALLOS_API.md` §3.10):
 * the two paths, the key the list is nested under in `get_*.php`'s envelope, and the id
 * parameter's resource-specific alias — the same key `set_*.php` reads on `edit`/`delete` and
 * returns the new row under on a successful `add`.
 */
data class CrudEndpoint(val getPath: String, val setPath: String, val listKey: String, val idParam: String)
