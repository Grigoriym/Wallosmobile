package com.grappim.wallosmobile.feature.subscriptions.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A row of `subscriptions` as `get_subscriptions.php` / `get_subscription.php` return it —
 * `SELECT *` plus three resolved names (API doc §3.1).
 *
 * Two traps the field list encodes:
 * - the endpoints' own docblocks show `cancelation_date` with one `l`; only
 *   [cancellationDate] exists in the schema.
 * - dates come back as `""` rather than `null` when unset (verified against the live
 *   instance: `cancellation_date` is empty on every active row, `start_date` on older ones),
 *   so a consumer has to treat blank as absent — see `Subscription`.
 *
 * Only the columns that have been `NOT NULL` since the table was created are required; every
 * later one carries a default. The schema grows by incremental migration, so a column added
 * after an older instance was installed simply isn't in its response, and a missing field must
 * not turn a readable subscription into a `Malformed`.
 */
@Serializable
data class SubscriptionDTO(
    val id: Int,
    val name: String,
    val logo: String = "",
    val price: Double,
    @SerialName("currency_id") val currencyId: Int,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("next_payment") val nextPayment: String = "",
    val cycle: Int,
    val frequency: Int,
    @SerialName("auto_renew") val autoRenew: Int = 1,
    val notes: String = "",
    @SerialName("payment_method_id") val paymentMethodId: Int? = null,
    @SerialName("payer_user_id") val payerUserId: Int? = null,
    @SerialName("category_id") val categoryId: Int? = null,
    val notify: Int = 0,
    @SerialName("notify_days_before") val notifyDaysBefore: Int? = null,
    val url: String = "",
    val inactive: Int = 0,
    @SerialName("cancellation_date") val cancellationDate: String? = null,
    @SerialName("replacement_subscription_id") val replacementSubscriptionId: Int? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("payer_user_name") val payerUserName: String? = null,
    @SerialName("payment_method_name") val paymentMethodName: String? = null
)
