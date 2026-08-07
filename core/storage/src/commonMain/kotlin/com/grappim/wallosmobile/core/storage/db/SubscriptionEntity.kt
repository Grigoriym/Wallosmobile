package com.grappim.wallosmobile.core.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

const val SUBSCRIPTION_TABLE = "subscriptions"

/**
 * One cached subscription row.
 *
 * Mirrors the **domain** `Subscription`, not `SubscriptionDTO`: 2.1 kept the domain narrower than
 * the wire on purpose, and caching columns no screen reads would only be a migration liability.
 *
 * Two shape choices this makes deliberately:
 *
 * - **Every column is a SQLite primitive**, so the table needs no `TypeConverter`. `cycleCode` is
 *   the raw wire code rather than a `BillingCycle`, and the two dates are ISO-8601 text — turning
 *   either into its domain type is the entity mapper's job (3.4), and keeping it there is what
 *   lets `core:storage` stay below `feature:` in the dependency graph. TaigaMobileNova's
 *   `core:storage` does depend on a feature's `domain` module; this one does not.
 * - **[currencySymbol] is stored resolved**, denormalised from [CurrencyEntity]. The repository
 *   already resolves it before the row reaches the domain (plan §7.1), so keeping it on the row
 *   makes the cached list a single query with no join. The currency table is still cached, for
 *   the *next* refresh's resolution rather than for this read.
 *
 * @param cycleCode `null` when the instance sent a code this build doesn't know.
 * @param nextPayment ISO-8601, `null` when the server sent `""` or something unparseable.
 * @param categoryId the id [categoryName] was resolved from, `null` when unset (7.7) — cached so
 *   the editor can pre-fill its pickers from the cache alone. [paymentMethodId], [payerUserId],
 *   [autoRenew], [notify] and [notifyDaysBefore] exist on the row for the same reason.
 */
@Entity(tableName = SUBSCRIPTION_TABLE)
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val name: String,
    val logo: String,
    val price: Double,
    val currencyId: Int,
    val currencySymbol: String,
    val cycleCode: Int?,
    val frequency: Int,
    val nextPayment: String?,
    val startDate: String?,
    val isActive: Boolean,
    val notes: String,
    val url: String,
    val categoryName: String,
    val paymentMethodName: String,
    val payerName: String,
    val categoryId: Int?,
    val paymentMethodId: Int?,
    val payerUserId: Int?,
    val autoRenew: Boolean,
    val notify: Boolean,
    val notifyDaysBefore: Int?
)
