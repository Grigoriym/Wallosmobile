package com.grappim.wallosmobile.feature.paymentmethods.dto

import com.grappim.wallosmobile.core.crud.CrudResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A row of `get_payment_methods.php` (`docs/WALLOS_API.md` §3.10). [icon] is already relative to
 * the web root (e.g. `images/uploads/icons/paypal.png`) — unlike a subscription's bare `logo`
 * filename, its display URL is `{base}/{icon}` directly, no prefix to add client-side (§4).
 * [order] is on the wire but not in the domain model, same as 7.2's `CategoryDTO`.
 */
@Serializable
data class PaymentMethodDTO(
    override val id: Int,
    override val name: String = "",
    val icon: String = "",
    val enabled: Int = 1,
    val order: Int = 0,
    @SerialName("in_use") override val inUse: Boolean = false
) : CrudResource
