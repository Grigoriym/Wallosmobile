package com.grappim.wallosmobile.feature.profile.dto

import kotlinx.serialization.Serializable

/**
 * `get_user.php` (API doc §3.9) nests the row under `user`, the same shape
 * `get_subscription.php`'s own `SubscriptionResponse` uses — not a top-level envelope like
 * `get_monthly_cost.php`/`get_period_budget.php`.
 */
@Serializable
data class UserResponse(val user: UserDTO)
