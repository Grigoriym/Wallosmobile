package com.grappim.wallosmobile.feature.household.dto

import com.grappim.wallosmobile.core.crud.CrudResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row of `get_household.php` (`docs/WALLOS_API.md` §3.10). */
@Serializable
data class HouseholdMemberDTO(
    override val id: Int,
    override val name: String = "",
    val email: String = "",
    @SerialName("in_use") override val inUse: Boolean = false
) : CrudResource
