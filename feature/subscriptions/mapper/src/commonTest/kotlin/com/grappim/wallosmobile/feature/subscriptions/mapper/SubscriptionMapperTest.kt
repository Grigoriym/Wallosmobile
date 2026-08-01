package com.grappim.wallosmobile.feature.subscriptions.mapper

import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.dto.SubscriptionDTO
import com.grappim.wallosmobile.utils.formatter.datetime.DateFormatter
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubscriptionMapperTest {

    private val mapper = SubscriptionMapper(DateFormatter(), HtmlUnescaper())

    @Test
    fun `maps a full row`() {
        val result = mapper.toDomain(subscriptionDTO(), currencySymbol = "€")

        assertEquals(4, result.id)
        assertEquals("Fiton", result.name)
        assertEquals("1732556915-Fiton.png", result.logo)
        assertEquals(31.99, result.price)
        assertEquals(1, result.currencyId)
        assertEquals("€", result.currencySymbol)
        assertEquals(BillingCycle.YEARS, result.cycle)
        assertEquals(1, result.frequency)
        assertEquals(LocalDate(2026, 1, 31), result.nextPayment)
        assertEquals(LocalDate(2024, 1, 31), result.startDate)
        assertEquals("Health & Wellbeing", result.categoryName)
        assertEquals("Direct Debit", result.paymentMethodName)
        assertEquals("gregorz", result.payerName)
    }

    @Test
    fun `unescapes the name, the notes and the resolved names`() {
        val result = mapper.toDomain(
            subscriptionDTO(
                name = "1&amp;1 Telekom",
                notes = "cancel &lt;before&gt; renewal",
                categoryName = "Phone &amp; Internet",
                paymentMethodName = "PayPal &amp; friends",
                payerUserName = "Bob &amp; Alice"
            ),
            currencySymbol = "€"
        )

        assertEquals("1&1 Telekom", result.name)
        assertEquals("cancel <before> renewal", result.notes)
        assertEquals("Phone & Internet", result.categoryName)
        assertEquals("PayPal & friends", result.paymentMethodName)
        assertEquals("Bob & Alice", result.payerName)
    }

    @Test
    fun `reads a blank date as absent, because that is what an unset one is on the wire`() {
        val result = mapper.toDomain(
            subscriptionDTO(startDate = "", nextPayment = ""),
            currencySymbol = "€"
        )

        assertNull(result.startDate)
        assertNull(result.nextPayment)
    }

    @Test
    fun `reads a null start date as absent too`() {
        assertNull(mapper.toDomain(subscriptionDTO(startDate = null), currencySymbol = "€").startDate)
    }

    @Test
    fun `drops an unparseable date instead of failing the whole row`() {
        val result = mapper.toDomain(subscriptionDTO(nextPayment = "31/01/2026"), currencySymbol = "€")

        assertNull(result.nextPayment)
        assertEquals("Fiton", result.name)
    }

    @Test
    fun `leaves the cycle null for a code this build does not know`() {
        assertNull(mapper.toDomain(subscriptionDTO(cycle = 99), currencySymbol = "€").cycle)
    }

    @Test
    fun `maps the one-time cycle, which is readable even though it is not writable`() {
        assertEquals(
            BillingCycle.ONE_TIME,
            mapper.toDomain(subscriptionDTO(cycle = 5), currencySymbol = "€").cycle
        )
    }

    @Test
    fun `inverts the inactive flag into isActive`() {
        assertTrue(mapper.toDomain(subscriptionDTO(inactive = 0), currencySymbol = "€").isActive)
        assertFalse(mapper.toDomain(subscriptionDTO(inactive = 1), currencySymbol = "€").isActive)
    }

    @Test
    fun `defaults the resolved names to blank when the instance sent none`() {
        val result = mapper.toDomain(
            subscriptionDTO(categoryName = null, paymentMethodName = null, payerUserName = null),
            currencySymbol = "€"
        )

        assertEquals("", result.categoryName)
        assertEquals("", result.paymentMethodName)
        assertEquals("", result.payerName)
    }

    @Test
    fun `carries a blank symbol through, for a currency the instance no longer has`() {
        assertEquals("", mapper.toDomain(subscriptionDTO(), currencySymbol = "").currencySymbol)
    }

    /** Shaped after a real row off the live instance (2.1). */
    private fun subscriptionDTO(
        name: String = "Fiton",
        price: Double = 31.99,
        currencyId: Int = 1,
        cycle: Int = 4,
        nextPayment: String = "2026-01-31",
        startDate: String? = "2024-01-31",
        inactive: Int = 0,
        notes: String = "",
        categoryName: String? = "Health & Wellbeing",
        paymentMethodName: String? = "Direct Debit",
        payerUserName: String? = "gregorz"
    ) = SubscriptionDTO(
        id = 4,
        name = name,
        logo = "1732556915-Fiton.png",
        price = price,
        currencyId = currencyId,
        startDate = startDate,
        nextPayment = nextPayment,
        cycle = cycle,
        frequency = 1,
        notes = notes,
        url = "https://fitonapp.com",
        inactive = inactive,
        categoryName = categoryName,
        payerUserName = payerUserName,
        paymentMethodName = paymentMethodName
    )
}
