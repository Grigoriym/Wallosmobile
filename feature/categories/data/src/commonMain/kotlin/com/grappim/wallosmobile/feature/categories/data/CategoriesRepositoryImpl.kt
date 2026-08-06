package com.grappim.wallosmobile.feature.categories.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.asynckmp.IoDispatcher
import com.grappim.wallosmobile.core.domain.resultOf
import com.grappim.wallosmobile.feature.categories.domain.model.Category
import com.grappim.wallosmobile.feature.categories.domain.repo.CategoriesRepository
import com.grappim.wallosmobile.feature.categories.mapper.CategoryMapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

/**
 * No cache behind this one (unlike `SubscriptionsRepository`, 3.4) — categories are reference
 * data with no offline requirement in this milestone, so every call is a round trip to
 * [CategoriesApi].
 */
@Single(binds = [CategoriesRepository::class])
internal class CategoriesRepositoryImpl(
    private val api: CategoriesApi,
    private val mapper: CategoryMapper,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : CategoriesRepository {

    override suspend fun getCategories(): Result<List<Category>> = resultOf {
        withContext(dispatcher) { api.getAll().map(mapper::toDomain) }
    }

    override suspend fun addCategory(name: String): Result<Int> = resultOf {
        withContext(dispatcher) { api.add(FormParams().put(PARAM_NAME, name)) }
    }

    override suspend fun editCategory(id: Int, name: String): Result<Unit> = resultOf {
        withContext(dispatcher) { api.edit(id, FormParams().put(PARAM_NAME, name)) }
    }

    override suspend fun deleteCategory(id: Int): Result<Unit> = resultOf {
        withContext(dispatcher) { api.delete(id) }
    }

    private companion object {
        const val PARAM_NAME = "name"
    }
}
