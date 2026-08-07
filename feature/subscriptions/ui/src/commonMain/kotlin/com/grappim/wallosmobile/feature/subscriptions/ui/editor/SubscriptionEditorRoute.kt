package com.grappim.wallosmobile.feature.subscriptions.ui.editor

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Pushed from the FAB on the subscriptions list (7.6). Carries nothing yet — every field starts
 * blank and the form is add-only until 7.7 gives this route an id to pre-fill from.
 *
 * Registered in the shell's `navKeySerializersModule` — a route missing from there survives every
 * gate and only breaks back-stack restore after process death.
 */
@Serializable
data object SubscriptionEditorRoute : NavKey
