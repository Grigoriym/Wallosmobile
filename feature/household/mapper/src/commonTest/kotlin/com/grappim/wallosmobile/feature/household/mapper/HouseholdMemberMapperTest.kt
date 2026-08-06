package com.grappim.wallosmobile.feature.household.mapper

import com.grappim.wallosmobile.feature.household.dto.HouseholdMemberDTO
import com.grappim.wallosmobile.feature.subscriptions.mapper.HtmlUnescaper
import kotlin.test.Test
import kotlin.test.assertEquals

class HouseholdMemberMapperTest {

    private val mapper = HouseholdMemberMapper(HtmlUnescaper())

    @Test
    fun `maps a row`() {
        val result = mapper.toDomain(
            HouseholdMemberDTO(id = 1, name = "John Doe", email = "john@example.com", inUse = true)
        )

        assertEquals(1, result.id)
        assertEquals("John Doe", result.name)
        assertEquals("john@example.com", result.email)
        assertEquals(true, result.inUse)
    }

    @Test
    fun `unescapes a user-entered name`() {
        val result = mapper.toDomain(HouseholdMemberDTO(id = 9, name = "Mom &amp; Dad"))

        assertEquals("Mom & Dad", result.name)
    }
}
