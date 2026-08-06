package com.grappim.wallosmobile.feature.categories.dto

import com.grappim.wallosmobile.core.crud.CrudResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A row of `get_categories.php` (`docs/WALLOS_API.md` §3.10). [order] is on the wire but not in
 * the domain model — nothing in Phase 3 reads it (7.2's step note).
 */
@Serializable
data class CategoryDTO(
    override val id: Int,
    override val name: String = "",
    val order: Int = 0,
    @SerialName("in_use") override val inUse: Boolean = false
) : CrudResource
