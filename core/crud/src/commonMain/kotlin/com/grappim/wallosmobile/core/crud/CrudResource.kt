package com.grappim.wallosmobile.core.crud

/**
 * What every row of `get_categories.php` / `get_household.php` / `get_payment_methods.php`
 * carries in common (`docs/WALLOS_API.md` §3.10) — enough to list it and to know whether
 * deleting it is even worth trying.
 */
interface CrudResource {
    val id: Int
    val name: String
    val inUse: Boolean
}
