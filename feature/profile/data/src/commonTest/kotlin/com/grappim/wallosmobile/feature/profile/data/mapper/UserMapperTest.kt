package com.grappim.wallosmobile.feature.profile.data.mapper

import com.grappim.wallosmobile.feature.profile.dto.UserDTO
import kotlin.test.Test
import kotlin.test.assertEquals

class UserMapperTest {

    private val mapper = UserMapper()

    @Test
    fun `maps every field the domain model keeps`() {
        val dto = UserDTO(id = 1, budget = 150.5, periodBudget = 25.0, mainCurrency = 1)

        val result = mapper.toDomain(dto)

        assertEquals(1, result.id)
        assertEquals(150.5, result.budget)
        assertEquals(25.0, result.periodBudget)
        assertEquals(1, result.mainCurrencyId)
    }
}
