package com.grappim.wallosmobile.core.crud

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.api.MultipartFile
import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.api.post
import com.grappim.wallosmobile.core.api.postMultipart
import com.grappim.wallosmobile.core.domain.WallosError
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * The one implementation of [CrudApi], parameterized per resource by [endpoint] and
 * [itemDeserializer]. A feature's data module composes it by delegation —
 * `class CategoriesApiImpl(apiClient: WallosApiClient) : CategoriesApi,
 * CrudApi<CategoryDTO> by WallosCrudApi(apiClient, CATEGORIES_ENDPOINT, CategoryDTO.serializer())`
 * — rather than reimplementing the four calls per feature.
 *
 * `success: false` on any of the four calls surfaces as a [WallosError] straight out of
 * [WallosApiClient.post] — including the "`<Resource> in use`" delete failure, which
 * `WallosErrorMapper` already maps to [WallosError.InUse] for every endpoint, not just this one.
 */
class WallosCrudApi<T : CrudResource>(
    private val apiClient: WallosApiClient,
    private val endpoint: CrudEndpoint,
    private val itemDeserializer: DeserializationStrategy<T>
) : CrudApi<T> {

    override suspend fun getAll(): List<T> {
        val envelope = apiClient.post<JsonObject>(endpoint.getPath)
        val items = envelope[endpoint.listKey] as? JsonArray ?: throw WallosError.Malformed(envelope.toString())
        return items.map { json.decodeFromJsonElement(itemDeserializer, it) }
    }

    override suspend fun add(fields: FormParams): Int {
        val envelope = apiClient.post<JsonObject>(endpoint.setPath, fields.withAction(ACTION_ADD))
        return (envelope[endpoint.idParam] as? JsonPrimitive)?.intOrNull
            ?: throw WallosError.Malformed(envelope.toString())
    }

    override suspend fun edit(id: Int, fields: FormParams) {
        apiClient.post<JsonObject>(
            endpoint.setPath,
            fields.withAction(ACTION_EDIT).put(endpoint.idParam, id.toString())
        )
    }

    /**
     * [add], with [file] riding alongside as a `multipart/form-data` part — the shape
     * `set_payment_methods.php`'s `paymenticon` needs (`WALLOS_API.md` §3.10, §4). Not part of
     * [CrudApi] itself: categories and household never take a file, so the generic interface stays
     * as it was.
     */
    suspend fun addWithFile(fields: FormParams, file: MultipartFile): Int {
        val envelope = apiClient.postMultipart<JsonObject>(endpoint.setPath, fields.withAction(ACTION_ADD), file)
        return (envelope[endpoint.idParam] as? JsonPrimitive)?.intOrNull
            ?: throw WallosError.Malformed(envelope.toString())
    }

    /** [edit], with [file] riding alongside — see [addWithFile]. */
    suspend fun editWithFile(id: Int, fields: FormParams, file: MultipartFile) {
        apiClient.postMultipart<JsonObject>(
            endpoint.setPath,
            fields.withAction(ACTION_EDIT).put(endpoint.idParam, id.toString()),
            file
        )
    }

    override suspend fun delete(id: Int) {
        apiClient.post<JsonObject>(
            endpoint.setPath,
            FormParams().withAction(ACTION_DELETE).put(endpoint.idParam, id.toString())
        )
    }

    private fun FormParams.withAction(action: String): FormParams = put(ACTION_KEY, action)

    private companion object {
        val json = Json { ignoreUnknownKeys = true }

        const val ACTION_KEY = "action"
        const val ACTION_ADD = "add"
        const val ACTION_EDIT = "edit"
        const val ACTION_DELETE = "delete"
    }
}
