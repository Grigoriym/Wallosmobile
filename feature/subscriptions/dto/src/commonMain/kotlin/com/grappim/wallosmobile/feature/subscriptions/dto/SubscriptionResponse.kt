package com.grappim.wallosmobile.feature.subscriptions.dto

import kotlinx.serialization.Serializable

/**
 * `get_subscription.php` (API doc §3.3) — the singular endpoint, which nests the row under
 * `subscription` rather than returning it at the top level.
 *
 * A row that isn't the caller's own never reaches here: the server answers
 * `title: "Subscription not found"` with `success: false`, which the parser turns into a
 * `WallosError` before decoding.
 */
@Serializable
data class SubscriptionResponse(val subscription: SubscriptionDTO)
