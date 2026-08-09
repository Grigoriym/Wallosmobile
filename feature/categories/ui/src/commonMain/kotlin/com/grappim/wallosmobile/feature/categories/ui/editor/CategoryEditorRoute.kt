package com.grappim.wallosmobile.feature.categories.ui.editor

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Pushed from the list's FAB (`categoryId` `null`) or from tapping a row (`categoryId` set) — one
 * form either way, per this milestone's preamble ("the same `SubscriptionEditorRoute(id: Int?)`
 * shape 7.6/7.7 already established"). Unlike that one, there is no separate detail screen: a
 * category is one field, so edit and delete both live here.
 *
 * [name] rides along on the edit path so the field is pre-filled without a round trip — the list
 * already holds it (no cache, this milestone's preamble), and `CategoriesRepository` has no
 * single-row fetch to redo that work with.
 *
 * Registered in the shell's `navKeySerializersModule` — a route missing from there survives every
 * gate and only breaks back-stack restore after process death.
 */
@Serializable
data class CategoryEditorRoute(val categoryId: Int? = null, val name: String = "") : NavKey
