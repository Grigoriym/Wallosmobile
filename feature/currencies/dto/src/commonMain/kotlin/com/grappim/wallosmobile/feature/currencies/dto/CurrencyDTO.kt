package com.grappim.wallosmobile.feature.currencies.dto

import com.grappim.wallosmobile.core.crud.CrudResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A row of `get_currencies.php` (`docs/WALLOS_API.md` §3.10). Unlike
 * [com.grappim.wallosmobile.feature.subscriptions.dto.CurrencyDTO]'s trimmed, read-only copy for
 * the price picker, this one restores [rate] and [inUse] — this milestone's whole screen is CRUD
 * on both. [rate] is a **string** on the wire (`"1.1000"`), not a JSON number, confirmed against
 * the live PHP source.
 */
@Serializable
data class CurrencyDTO(
    override val id: Int,
    override val name: String = "",
    val symbol: String = "",
    val code: String = "",
    val rate: String = "1",
    @SerialName("in_use") override val inUse: Boolean = false
) : CrudResource
