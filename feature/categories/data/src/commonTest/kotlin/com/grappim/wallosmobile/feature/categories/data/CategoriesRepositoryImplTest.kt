package com.grappim.wallosmobile.feature.categories.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.categories.dto.CategoryDTO
import com.grappim.wallosmobile.feature.categories.mapper.CategoryMapper
import com.grappim.wallosmobile.feature.subscriptions.mapper.HtmlUnescaper
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CategoriesRepositoryImplTest {

    @Test
    fun `getCategories maps every row through the mapper`() = runTest {
        val api = FakeCategoriesApi(all = listOf(CategoryDTO(id = 1, name = "1&amp;1 Telekom")))

        val result = repository(api).getCategories().getOrThrow()

        assertEquals(listOf("1&1 Telekom"), result.map { it.name })
    }

    @Test
    fun `a failed getCategories surfaces the WallosError`() = runTest {
        val api = FakeCategoriesApi(allFailure = WallosError.Server("502 Bad Gateway"))

        assertFailsWith<WallosError.Server> { repository(api).getCategories().getOrThrow() }
    }

    @Test
    fun `addCategory sends the name and returns the new id`() = runTest {
        val api = FakeCategoriesApi(addedId = 9)

        val result = repository(api).addCategory("Streaming").getOrThrow()

        assertEquals(9, result)
        assertEquals("Streaming", api.addedFields?.asMap()?.get("name"))
    }

    @Test
    fun `editCategory sends the id and the new name`() = runTest {
        val api = FakeCategoriesApi()

        repository(api).editCategory(7, "Renamed").getOrThrow()

        val (id, fields) = api.editCalls.single()
        assertEquals(7, id)
        assertEquals("Renamed", fields.asMap()["name"])
    }

    @Test
    fun `deleteCategory forwards the id`() = runTest {
        val api = FakeCategoriesApi()

        repository(api).deleteCategory(4).getOrThrow()

        assertEquals(listOf(4), api.deleteCalls)
    }

    @Test
    fun `a delete on a referenced category surfaces as InUse`() = runTest {
        val api = FakeCategoriesApi(deleteFailure = WallosError.InUse("Category in use"))

        assertFailsWith<WallosError.InUse> { repository(api).deleteCategory(4).getOrThrow() }
    }

    private fun repository(api: CategoriesApi = FakeCategoriesApi()): CategoriesRepositoryImpl =
        CategoriesRepositoryImpl(
            api = api,
            mapper = CategoryMapper(HtmlUnescaper()),
            dispatcher = UnconfinedTestDispatcher()
        )

    private class FakeCategoriesApi(
        private val all: List<CategoryDTO>? = null,
        private val addedId: Int = 0,
        private val allFailure: Throwable? = null,
        private val addFailure: Throwable? = null,
        private val editFailure: Throwable? = null,
        private val deleteFailure: Throwable? = null
    ) : CategoriesApi {

        var addedFields: FormParams? = null
            private set
        val editCalls = mutableListOf<Pair<Int, FormParams>>()
        val deleteCalls = mutableListOf<Int>()

        override suspend fun getAll(): List<CategoryDTO> {
            allFailure?.let { throw it }
            return all ?: error("categories not set")
        }

        override suspend fun add(fields: FormParams): Int {
            addFailure?.let { throw it }
            addedFields = fields
            return addedId
        }

        override suspend fun edit(id: Int, fields: FormParams) {
            editFailure?.let { throw it }
            editCalls += id to fields
        }

        override suspend fun delete(id: Int) {
            deleteFailure?.let { throw it }
            deleteCalls += id
        }
    }
}
