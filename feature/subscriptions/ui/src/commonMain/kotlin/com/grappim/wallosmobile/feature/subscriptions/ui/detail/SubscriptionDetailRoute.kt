package com.grappim.wallosmobile.feature.subscriptions.ui.detail

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Pushed onto the Subscriptions sub-stack, so it is the first route in the app that carries data.
 * The id is all it carries: with no cache (plan §7.2) the screen re-reads the row rather than
 * trusting a snapshot the list took at some earlier point.
 *
 * Registered in the shell's `navKeySerializersModule` — a route missing from there survives every
 * gate and only breaks back-stack restore after process death.
 */
@Serializable
data class SubscriptionDetailRoute(val subscriptionId: Int) : NavKey
