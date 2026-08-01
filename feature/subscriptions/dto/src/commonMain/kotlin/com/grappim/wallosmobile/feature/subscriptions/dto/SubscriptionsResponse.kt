package com.grappim.wallosmobile.feature.subscriptions.dto

import kotlinx.serialization.Serializable

/**
 * `get_subscriptions.php` (API doc §3.2). `WallosEnvelopeParser` decodes the whole envelope, so
 * the response type is the envelope — `success`, `title` and `notes` are the parser's business
 * and don't appear here.
 *
 * [subscriptions] has no default on purpose: the server sends `[]` for a user with none (verified
 * against the live instance), so an *absent* key means a response this app doesn't understand, and
 * a `Malformed` error is a truer answer than an empty-state screen.
 */
@Serializable
data class SubscriptionsResponse(val subscriptions: List<SubscriptionDTO>)
