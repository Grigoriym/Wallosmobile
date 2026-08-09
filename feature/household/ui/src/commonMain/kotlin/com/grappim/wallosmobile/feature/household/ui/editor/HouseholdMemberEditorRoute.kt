package com.grappim.wallosmobile.feature.household.ui.editor

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Pushed from the list's FAB (`memberId` `null`) or from tapping a row (`memberId` set) — one form
 * either way, mirroring `CategoryEditorRoute`. There is no separate detail screen: a household
 * member is two fields, so edit and delete both live here.
 *
 * [name]/[email] ride along on the edit path so the fields are pre-filled without a round trip —
 * the list already holds them (no cache, this milestone's preamble), and `HouseholdRepository` has
 * no single-row fetch to redo that work with.
 *
 * Registered in the shell's `navKeySerializersModule` — a route missing from there survives every
 * gate and only breaks back-stack restore after process death.
 */
@Serializable
data class HouseholdMemberEditorRoute(val memberId: Int? = null, val name: String = "", val email: String = "") :
    NavKey
