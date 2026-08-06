package com.grappim.wallosmobile.feature.categories.mapper

import com.grappim.wallosmobile.feature.categories.dto.CategoryDTO
import com.grappim.wallosmobile.feature.subscriptions.mapper.HtmlUnescaper
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryMapperTest {

    private val mapper = CategoryMapper(HtmlUnescaper())

    @Test
    fun `maps a row and drops order`() {
        val result = mapper.toDomain(CategoryDTO(id = 1, name = "Streaming", order = 3, inUse = true))

        assertEquals(1, result.id)
        assertEquals("Streaming", result.name)
        assertEquals(true, result.inUse)
    }

    @Test
    fun `unescapes a user-entered name`() {
        val result = mapper.toDomain(CategoryDTO(id = 9, name = "Food &amp; Drink"))

        assertEquals("Food & Drink", result.name)
    }
}
