package com.grappim.wallosmobile.feature.categories.mapper

import com.grappim.wallosmobile.feature.categories.domain.model.Category
import com.grappim.wallosmobile.feature.categories.dto.CategoryDTO
import com.grappim.wallosmobile.feature.subscriptions.mapper.HtmlUnescaper
import org.koin.core.annotation.Single

/** A `categories` row into the model. `order` is dropped — nothing in Phase 3 reads it. */
@Single
class CategoryMapper(private val htmlUnescaper: HtmlUnescaper) {

    fun toDomain(dto: CategoryDTO): Category = Category(
        id = dto.id,
        name = htmlUnescaper.unescape(dto.name),
        inUse = dto.inUse
    )
}
