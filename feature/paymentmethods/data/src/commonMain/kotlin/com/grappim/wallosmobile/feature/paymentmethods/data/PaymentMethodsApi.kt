package com.grappim.wallosmobile.feature.paymentmethods.data

import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.crud.CrudApi
import com.grappim.wallosmobile.core.crud.CrudEndpoint
import com.grappim.wallosmobile.core.crud.WallosCrudApi
import com.grappim.wallosmobile.feature.paymentmethods.dto.PaymentMethodDTO
import org.koin.core.annotation.Single

/** `get_payment_methods.php` / `set_payment_methods.php` (`docs/WALLOS_API.md` §3.10), nothing of its own. */
interface PaymentMethodsApi : CrudApi<PaymentMethodDTO>

@Single(binds = [PaymentMethodsApi::class])
internal class PaymentMethodsApiImpl(apiClient: WallosApiClient) :
    PaymentMethodsApi,
    CrudApi<PaymentMethodDTO> by WallosCrudApi(apiClient, PAYMENT_METHODS_ENDPOINT, PaymentMethodDTO.serializer())

private val PAYMENT_METHODS_ENDPOINT = CrudEndpoint(
    getPath = "api/payment_methods/get_payment_methods.php",
    setPath = "api/payment_methods/set_payment_methods.php",
    listKey = "payment_methods",
    idParam = "paymentId"
)
