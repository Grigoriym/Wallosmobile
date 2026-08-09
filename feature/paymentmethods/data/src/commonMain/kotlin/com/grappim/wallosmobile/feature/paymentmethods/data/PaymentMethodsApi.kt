package com.grappim.wallosmobile.feature.paymentmethods.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.api.MultipartFile
import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.crud.CrudApi
import com.grappim.wallosmobile.core.crud.CrudEndpoint
import com.grappim.wallosmobile.core.crud.WallosCrudApi
import com.grappim.wallosmobile.feature.paymentmethods.dto.PaymentMethodDTO
import org.koin.core.annotation.Single

/**
 * `get_payment_methods.php` / `set_payment_methods.php` (`docs/WALLOS_API.md` §3.10). [addWithIcon]/
 * [editWithIcon] are this resource's own addition (9.5) — categories and household have no file
 * upload, so they stay off [CrudApi] itself.
 */
interface PaymentMethodsApi : CrudApi<PaymentMethodDTO> {

    suspend fun addWithIcon(fields: FormParams, icon: MultipartFile): Int

    suspend fun editWithIcon(id: Int, fields: FormParams, icon: MultipartFile)
}

/**
 * Composition, not interface delegation (9.1's `CurrenciesApi` precedent): [addWithIcon]/
 * [editWithIcon] call [WallosCrudApi.addWithFile]/[WallosCrudApi.editWithFile], which aren't part
 * of [CrudApi] and so aren't reachable through a `by` delegate.
 */
@Single(binds = [PaymentMethodsApi::class])
internal class PaymentMethodsApiImpl(apiClient: WallosApiClient) : PaymentMethodsApi {

    private val crud = WallosCrudApi(apiClient, PAYMENT_METHODS_ENDPOINT, PaymentMethodDTO.serializer())

    override suspend fun getAll(): List<PaymentMethodDTO> = crud.getAll()

    override suspend fun add(fields: FormParams): Int = crud.add(fields)

    override suspend fun edit(id: Int, fields: FormParams) = crud.edit(id, fields)

    override suspend fun delete(id: Int) = crud.delete(id)

    override suspend fun addWithIcon(fields: FormParams, icon: MultipartFile): Int = crud.addWithFile(fields, icon)

    override suspend fun editWithIcon(id: Int, fields: FormParams, icon: MultipartFile) =
        crud.editWithFile(id, fields, icon)
}

private val PAYMENT_METHODS_ENDPOINT = CrudEndpoint(
    getPath = "api/payment_methods/get_payment_methods.php",
    setPath = "api/payment_methods/set_payment_methods.php",
    listKey = "payment_methods",
    idParam = "paymentId"
)
