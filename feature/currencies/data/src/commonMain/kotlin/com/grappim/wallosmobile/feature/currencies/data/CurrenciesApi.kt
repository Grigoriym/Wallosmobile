package com.grappim.wallosmobile.feature.currencies.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.api.post
import com.grappim.wallosmobile.core.crud.CrudEndpoint
import com.grappim.wallosmobile.core.crud.WallosCrudApi
import com.grappim.wallosmobile.feature.currencies.dto.CurrenciesResponse
import com.grappim.wallosmobile.feature.currencies.dto.CurrencyDTO
import org.koin.core.annotation.Single

/**
 * `get_currencies.php` / `set_currencies.php` (`docs/WALLOS_API.md` §3.10). Not a
 * `CrudApi<CurrencyDTO>` like categories/household/payment methods: [getAll] needs the envelope's
 * top-level `main_currency` alongside the list (9.1's milestone preamble), which
 * `WallosCrudApi.getAll()`'s generic form drops. Add/edit/delete stay the shared implementation,
 * composed rather than reimplemented.
 */
interface CurrenciesApi {

    suspend fun getAll(): CurrenciesPayload

    /** @return the id the server assigned the new currency. */
    suspend fun add(fields: FormParams): Int

    suspend fun edit(id: Int, fields: FormParams)

    /** @throws com.grappim.wallosmobile.core.domain.WallosError.InUse if the row is referenced elsewhere. */
    suspend fun delete(id: Int)
}

data class CurrenciesPayload(val currencies: List<CurrencyDTO>, val mainCurrencyId: Int?)

@Single(binds = [CurrenciesApi::class])
internal class CurrenciesApiImpl(private val apiClient: WallosApiClient) : CurrenciesApi {

    private val crud = WallosCrudApi(apiClient, CURRENCIES_ENDPOINT, CurrencyDTO.serializer())

    override suspend fun getAll(): CurrenciesPayload =
        apiClient.post<CurrenciesResponse>(CURRENCIES_ENDPOINT.getPath).let { response ->
            CurrenciesPayload(currencies = response.currencies, mainCurrencyId = response.mainCurrency)
        }

    override suspend fun add(fields: FormParams): Int = crud.add(fields)

    override suspend fun edit(id: Int, fields: FormParams) = crud.edit(id, fields)

    override suspend fun delete(id: Int) = crud.delete(id)
}

private val CURRENCIES_ENDPOINT = CrudEndpoint(
    getPath = "api/currencies/get_currencies.php",
    setPath = "api/currencies/set_currencies.php",
    listKey = "currencies",
    idParam = "currencyId"
)
