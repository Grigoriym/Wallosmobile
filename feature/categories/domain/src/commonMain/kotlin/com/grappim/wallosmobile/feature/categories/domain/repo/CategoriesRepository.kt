package com.grappim.wallosmobile.feature.categories.domain.repo

import com.grappim.wallosmobile.feature.categories.domain.model.Category

/**
 * Categories, straight off `get_categories.php` / `set_categories.php` (`docs/WALLOS_API.md`
 * §3.10) — reference data, so unlike [com.grappim.wallosmobile.feature.subscriptions.domain.repo]
 * there is no cache to read instead: every call is a round trip.
 *
 * A [Result] failure is always a [com.grappim.wallosmobile.core.domain.WallosError], and a
 * [deleteCategory] on a category still referenced by a subscription is
 * [com.grappim.wallosmobile.core.domain.WallosError.InUse].
 */
interface CategoriesRepository {

    suspend fun getCategories(): Result<List<Category>>

    /** @return the id the server assigned the new category. */
    suspend fun addCategory(name: String): Result<Int>

    suspend fun editCategory(id: Int, name: String): Result<Unit>

    suspend fun deleteCategory(id: Int): Result<Unit>
}
