package com.grappim.wallosmobile.feature.household.mapper

import com.grappim.wallosmobile.feature.household.domain.model.HouseholdMember
import com.grappim.wallosmobile.feature.household.dto.HouseholdMemberDTO
import com.grappim.wallosmobile.feature.subscriptions.mapper.HtmlUnescaper
import org.koin.core.annotation.Single

/** A `household` row into the model. */
@Single
class HouseholdMemberMapper(private val htmlUnescaper: HtmlUnescaper) {

    fun toDomain(dto: HouseholdMemberDTO): HouseholdMember = HouseholdMember(
        id = dto.id,
        name = htmlUnescaper.unescape(dto.name),
        email = htmlUnescaper.unescape(dto.email),
        inUse = dto.inUse
    )
}
