package com.grappim.wallosmobile.feature.subscriptions.mapper

import com.grappim.wallosmobile.core.storage.db.SubscriptionEntity
import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.model.Subscription
import com.grappim.wallosmobile.utils.formatter.datetime.DateFormatter
import org.koin.core.annotation.Single

/**
 * The cached row both ways (3.4). It sits here rather than in `core:storage` because the entity is
 * SQLite primitives only — no `TypeConverter` — so that module can stay below `feature:` in the
 * dependency graph (3.3): turning a raw `cycleCode` into a [BillingCycle] and an ISO string into a
 * `LocalDate` is this feature's business, not the database's.
 *
 * Lossless in the direction that matters: what [toEntity] writes, [toDomain] reads back unchanged.
 * The one asymmetry is a cycle code the app doesn't know — `null` in, `null` out, and the raw code
 * is not preserved, because [Subscription] has nowhere to keep it either.
 */
@Single
class SubscriptionEntityMapper(private val dateFormatter: DateFormatter) {

    fun toEntity(subscription: Subscription): SubscriptionEntity = SubscriptionEntity(
        id = subscription.id,
        name = subscription.name,
        logo = subscription.logo,
        price = subscription.price,
        currencyId = subscription.currencyId,
        // Denormalised on purpose (plan §4.7): the cached list is then one query with no join.
        currencySymbol = subscription.currencySymbol,
        cycleCode = subscription.cycle?.code,
        frequency = subscription.frequency,
        nextPayment = subscription.nextPayment?.let(dateFormatter::formatIsoDate),
        startDate = subscription.startDate?.let(dateFormatter::formatIsoDate),
        isActive = subscription.isActive,
        notes = subscription.notes,
        url = subscription.url,
        categoryName = subscription.categoryName,
        paymentMethodName = subscription.paymentMethodName,
        payerName = subscription.payerName,
        categoryId = subscription.categoryId,
        paymentMethodId = subscription.paymentMethodId,
        payerUserId = subscription.payerUserId,
        autoRenew = subscription.autoRenew,
        notify = subscription.notify,
        notifyDaysBefore = subscription.notifyDaysBefore
    )

    fun toDomain(entity: SubscriptionEntity): Subscription = Subscription(
        id = entity.id,
        // Already unescaped by `SubscriptionMapper` on the way in — the cache stores what the
        // screen renders, so nothing here launders anything a second time.
        name = entity.name,
        logo = entity.logo,
        price = entity.price,
        currencyId = entity.currencyId,
        currencySymbol = entity.currencySymbol,
        cycle = entity.cycleCode?.let(BillingCycle::fromCode),
        frequency = entity.frequency,
        nextPayment = entity.nextPayment?.let(dateFormatter::parseIsoDate),
        startDate = entity.startDate?.let(dateFormatter::parseIsoDate),
        isActive = entity.isActive,
        notes = entity.notes,
        url = entity.url,
        categoryName = entity.categoryName,
        paymentMethodName = entity.paymentMethodName,
        payerName = entity.payerName,
        categoryId = entity.categoryId,
        paymentMethodId = entity.paymentMethodId,
        payerUserId = entity.payerUserId,
        autoRenew = entity.autoRenew,
        notify = entity.notify,
        notifyDaysBefore = entity.notifyDaysBefore
    )
}
