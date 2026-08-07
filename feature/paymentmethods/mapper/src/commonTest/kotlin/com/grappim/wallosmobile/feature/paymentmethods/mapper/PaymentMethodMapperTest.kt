package com.grappim.wallosmobile.feature.paymentmethods.mapper

import com.grappim.wallosmobile.feature.paymentmethods.dto.PaymentMethodDTO
import com.grappim.wallosmobile.feature.subscriptions.mapper.HtmlUnescaper
import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentMethodMapperTest {

    private val mapper = PaymentMethodMapper(HtmlUnescaper())

    @Test
    fun `maps a row and drops order`() {
        val result = mapper.toDomain(
            PaymentMethodDTO(
                id = 1,
                name = "PayPal",
                icon = "images/uploads/icons/paypal.png",
                enabled = 1,
                order = 3,
                inUse = true
            )
        )

        assertEquals(1, result.id)
        assertEquals("PayPal", result.name)
        assertEquals("images/uploads/icons/paypal.png", result.icon)
        assertEquals(true, result.enabled)
        assertEquals(true, result.inUse)
    }

    @Test
    fun `maps a disabled method`() {
        val result = mapper.toDomain(PaymentMethodDTO(id = 2, name = "Cash", enabled = 0))

        assertEquals(false, result.enabled)
    }

    @Test
    fun `unescapes a user-entered name`() {
        val result = mapper.toDomain(PaymentMethodDTO(id = 9, name = "Mom &amp; Dad's Card"))

        assertEquals("Mom & Dad's Card", result.name)
    }
}
