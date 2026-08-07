package com.grappim.wallosmobile.feature.subscriptions.ui.editor

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Pushed from the FAB on the subscriptions list (7.6, [subscriptionId] `null`) or from the edit
 * action on the detail screen (7.7, [subscriptionId] set) — the same form either way, pre-filled
 * from the cache when an id is given.
 *
 * Registered in the shell's `navKeySerializersModule` — a route missing from there survives every
 * gate and only breaks back-stack restore after process death.
 */
@Serializable
data class SubscriptionEditorRoute(val subscriptionId: Int? = null) : NavKey
