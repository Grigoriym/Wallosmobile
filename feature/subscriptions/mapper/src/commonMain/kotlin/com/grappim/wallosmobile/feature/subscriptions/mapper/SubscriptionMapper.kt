package com.grappim.wallosmobile.feature.subscriptions.mapper

import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.feature.subscriptions.dto.SubscriptionDTO
import com.grappim.wallosmobile.utils.formatter.datetime.DateFormatter
import org.koin.core.annotation.Single

/**
 * A `subscriptions` row into the model the two v1 screens render (plan §7.1).
 *
 * Total by construction: an unknown cycle code and an unparseable or blank date each become
 * `null` rather than a throw, so one odd row can't sink the list it arrived in.
 */
@Single
class SubscriptionMapper(private val dateFormatter: DateFormatter, private val htmlUnescaper: HtmlUnescaper) {

    /**
     * @param currencySymbol looked up from [SubscriptionDTO.currencyId] by the repository, which
     * owns the join — this class has no currency list to consult.
     */
    fun toDomain(dto: SubscriptionDTO, currencySymbol: String): Subscription = Subscription(
        id = dto.id,
        name = htmlUnescaper.unescape(dto.name),
        logo = dto.logo,
        price = dto.price,
        currencyId = dto.currencyId,
        currencySymbol = currencySymbol,
        cycle = BillingCycle.fromCode(dto.cycle),
        frequency = dto.frequency,
        nextPayment = dateFormatter.parseIsoDate(dto.nextPayment),
        // `""` on rows created before `start_date` became mandatory, and `parseIsoDate` reads
        // blank as absent — so `orEmpty()` collapses both nulls into the one the model wants.
        startDate = dateFormatter.parseIsoDate(dto.startDate.orEmpty()),
        isActive = dto.inactive == INACTIVE_FALSE,
        notes = htmlUnescaper.unescape(dto.notes),
        // Not unescaped: a URL is a URL, and the server stores it raw.
        url = dto.url,
        // Resolved server-side, and never null in practice — an unmatched id comes back as
        // "No category" / "Unknown member" / "Unknown payment method".
        categoryName = htmlUnescaper.unescape(dto.categoryName.orEmpty()),
        paymentMethodName = htmlUnescaper.unescape(dto.paymentMethodName.orEmpty()),
        payerName = htmlUnescaper.unescape(dto.payerUserName.orEmpty())
    )

    private companion object {
        const val INACTIVE_FALSE = 0
    }
}
