package com.grappim.wallosmobile.feature.paymentmethods.ui.editor

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Pushed from the list's FAB (`paymentMethodId` `null`) or from tapping a row (`paymentMethodId`
 * set) — one form either way, mirroring `HouseholdMemberEditorRoute`. There is no separate detail
 * screen: edit and delete both live here.
 *
 * [name]/[enabled] ride along on the edit path so the fields are pre-filled without a round trip —
 * the list already holds them (no cache, this milestone's preamble), and
 * `PaymentMethodsRepository` has no single-row fetch to redo that work with. `iconUrl` does *not*
 * ride along: it names a *source* URL the server fetches from, which the list never gets back (the
 * server hands back a resolved `icon` path instead) — leaving it blank on edit is exactly right,
 * since a blank `iconUrl` on a save means "leave the icon untouched" (`PaymentMethodsRepository`'s
 * own doc comment).
 *
 * Registered in the shell's `navKeySerializersModule` — a route missing from there survives every
 * gate and only breaks back-stack restore after process death.
 */
@Serializable
data class PaymentMethodEditorRoute(
    val paymentMethodId: Int? = null,
    val name: String = "",
    val enabled: Boolean = true
) : NavKey
