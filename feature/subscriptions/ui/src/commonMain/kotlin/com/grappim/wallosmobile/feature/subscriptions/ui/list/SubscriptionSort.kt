package com.grappim.wallosmobile.feature.subscriptions.ui.list

/**
 * The orders `get_subscriptions.php` offers (API doc §3.2), applied client-side over the cache —
 * v1 sends `api_key` and nothing else, and 2.3 asked Phase 2b to keep it that way.
 *
 * Each name is the server's `sort` value, except where the server sorts by an id this app never
 * receives: [PAYER], [CATEGORY] and [PAYMENT_METHOD] are `payer_user_id` / `category_id` /
 * `payment_method_id` on the wire, and only the *resolved* names reach the cache (2.1), so those
 * three sort alphabetically instead. `alphanumeric` is missing because §3.2 says it is an alias
 * for `name`.
 *
 * The direction belongs to the field, not to the user — see [SubscriptionSorter].
 */
enum class SubscriptionSort {
    NEXT_PAYMENT,
    NAME,
    PRICE,

    /** `id`, descending. Ids are handed out in insertion order, so this reads as newest first. */
    ID,
    PAYER,
    CATEGORY,
    PAYMENT_METHOD,

    /** `inactive`, ascending — the flag is 0 for a live subscription, so active rows come first. */
    INACTIVE
}
