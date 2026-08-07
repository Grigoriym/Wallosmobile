package com.grappim.wallosmobile.feature.paymentmethods.domain.model

/**
 * A payment method the instance knows about. `order` stays off the model, same as 7.2's
 * `Category` — nothing in Phase 3 reads it.
 */
data class PaymentMethod(val id: Int, val name: String, val icon: String, val enabled: Boolean, val inUse: Boolean)
