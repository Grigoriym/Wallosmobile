package com.grappim.wallosmobile.feature.currencies.ui.editor

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Pushed from the list's FAB (`currencyId` `null`) or from tapping a row (`currencyId` set) — one
 * form either way, mirroring `CategoryEditorRoute`/`HouseholdMemberEditorRoute`. There is no
 * separate detail screen: edit and delete both live here.
 *
 * [name]/[symbol]/[code]/[rate] ride along on the edit path so the fields are pre-filled without a
 * round trip — the list already holds them (no cache, this milestone's preamble), and
 * `CurrenciesRepository` has no single-row fetch to redo that work with. [rate] defaults to `1.0`
 * on the add path, per this step's own field list.
 *
 * Registered in the shell's `navKeySerializersModule` — a route missing from there survives every
 * gate and only breaks back-stack restore after process death.
 */
@Serializable
data class CurrencyEditorRoute(
    val currencyId: Int? = null,
    val name: String = "",
    val symbol: String = "",
    val code: String = "",
    val rate: Double = 1.0
) : NavKey
