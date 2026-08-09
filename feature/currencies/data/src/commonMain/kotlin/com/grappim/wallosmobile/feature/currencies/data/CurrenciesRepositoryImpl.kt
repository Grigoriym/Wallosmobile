package com.grappim.wallosmobile.feature.currencies.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.asynckmp.IoDispatcher
import com.grappim.wallosmobile.core.domain.resultOf
import com.grappim.wallosmobile.feature.currencies.domain.model.Currency
import com.grappim.wallosmobile.feature.currencies.domain.repo.CurrenciesRepository
import com.grappim.wallosmobile.feature.currencies.mapper.CurrencyMapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

/**
 * No cache behind this one, same as `CategoriesRepositoryImpl` (9.1's precedent, 7.2's own note)
 * — currencies are reference data with no offline requirement in this milestone, so every call is
 * a round trip to [CurrenciesApi].
 */
@Single(binds = [CurrenciesRepository::class])
internal class CurrenciesRepositoryImpl(
    private val api: CurrenciesApi,
    private val mapper: CurrencyMapper,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : CurrenciesRepository {

    override suspend fun getCurrencies(): Result<List<Currency>> = resultOf {
        withContext(dispatcher) {
            val payload = api.getAll()
            payload.currencies.map { mapper.toDomain(it, payload.mainCurrencyId) }
        }
    }

    override suspend fun addCurrency(name: String, symbol: String, code: String, rate: Double): Result<Int> = resultOf {
        withContext(dispatcher) { api.add(currencyFields(name, symbol, code, rate)) }
    }

    override suspend fun editCurrency(id: Int, name: String, symbol: String, code: String, rate: Double): Result<Unit> =
        resultOf {
            withContext(dispatcher) { api.edit(id, currencyFields(name, symbol, code, rate)) }
        }

    override suspend fun deleteCurrency(id: Int): Result<Unit> = resultOf {
        withContext(dispatcher) { api.delete(id) }
    }

    private fun currencyFields(name: String, symbol: String, code: String, rate: Double): FormParams = FormParams()
        .put(PARAM_NAME, name)
        .put(PARAM_SYMBOL, symbol)
        .put(PARAM_CODE, code)
        .put(PARAM_RATE, rate.toString())

    private companion object {
        const val PARAM_NAME = "name"
        const val PARAM_SYMBOL = "symbol"
        const val PARAM_CODE = "code"
        const val PARAM_RATE = "rate"
    }
}
