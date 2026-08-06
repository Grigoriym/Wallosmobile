package com.grappim.wallosmobile.feature.categories.data

import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.crud.CrudApi
import com.grappim.wallosmobile.core.crud.CrudEndpoint
import com.grappim.wallosmobile.core.crud.WallosCrudApi
import com.grappim.wallosmobile.feature.categories.dto.CategoryDTO
import org.koin.core.annotation.Single

/** `get_categories.php` / `set_categories.php` (`docs/WALLOS_API.md` §3.10), nothing of its own. */
interface CategoriesApi : CrudApi<CategoryDTO>

@Single(binds = [CategoriesApi::class])
internal class CategoriesApiImpl(apiClient: WallosApiClient) :
    CategoriesApi,
    CrudApi<CategoryDTO> by WallosCrudApi(apiClient, CATEGORIES_ENDPOINT, CategoryDTO.serializer())

private val CATEGORIES_ENDPOINT = CrudEndpoint(
    getPath = "api/categories/get_categories.php",
    setPath = "api/categories/set_categories.php",
    listKey = "categories",
    idParam = "categoryId"
)
