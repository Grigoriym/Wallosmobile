package com.grappim.wallosmobile.feature.paymentmethods.mapper

import com.grappim.wallosmobile.feature.paymentmethods.domain.model.PaymentMethod
import com.grappim.wallosmobile.feature.paymentmethods.dto.PaymentMethodDTO
import com.grappim.wallosmobile.feature.subscriptions.mapper.HtmlUnescaper
import org.koin.core.annotation.Single

/** A `payment_methods` row into the model. `order` is dropped — nothing in Phase 3 reads it. */
@Single
class PaymentMethodMapper(private val htmlUnescaper: HtmlUnescaper) {

    fun toDomain(dto: PaymentMethodDTO): PaymentMethod = PaymentMethod(
        id = dto.id,
        name = htmlUnescaper.unescape(dto.name),
        icon = dto.icon,
        enabled = dto.enabled == ENABLED,
        inUse = dto.inUse
    )

    private companion object {
        const val ENABLED = 1
    }
}
