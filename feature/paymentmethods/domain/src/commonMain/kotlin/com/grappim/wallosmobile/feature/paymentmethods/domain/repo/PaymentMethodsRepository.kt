package com.grappim.wallosmobile.feature.paymentmethods.domain.repo

import com.grappim.wallosmobile.feature.paymentmethods.domain.model.PaymentMethod

/**
 * Payment methods, straight off `get_payment_methods.php` / `set_payment_methods.php`
 * (`docs/WALLOS_API.md` §3.10) — reference data, so unlike
 * [com.grappim.wallosmobile.feature.subscriptions.domain.repo] there is no cache to read instead:
 * every call is a round trip.
 *
 * A [Result] failure is always a [com.grappim.wallosmobile.core.domain.WallosError], and a
 * [deletePaymentMethod] on a method still referenced by a subscription is
 * [com.grappim.wallosmobile.core.domain.WallosError.InUse].
 */
interface PaymentMethodsRepository {

    suspend fun getPaymentMethods(): Result<List<PaymentMethod>>

    /**
     * @param iconUrl a source URL the server fetches server-side (same shape as subscriptions'
     * `logo_url`, 7.8) — `null` leaves the icon unset on an add, or untouched on an edit. The
     * multipart `paymenticon` upload is out of scope: no picker calls it before Phase 5.
     * @return the id the server assigned the new payment method.
     */
    suspend fun addPaymentMethod(name: String, enabled: Boolean, iconUrl: String? = null): Result<Int>

    suspend fun editPaymentMethod(id: Int, name: String, enabled: Boolean, iconUrl: String? = null): Result<Unit>

    suspend fun deletePaymentMethod(id: Int): Result<Unit>
}
