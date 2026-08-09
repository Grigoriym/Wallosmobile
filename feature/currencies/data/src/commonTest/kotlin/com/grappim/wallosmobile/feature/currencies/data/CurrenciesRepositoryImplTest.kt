package com.grappim.wallosmobile.feature.currencies.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.currencies.dto.CurrencyDTO
import com.grappim.wallosmobile.feature.currencies.mapper.CurrencyMapper
import com.grappim.wallosmobile.feature.subscriptions.mapper.HtmlUnescaper
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CurrenciesRepositoryImplTest {

    @Test
    fun `getCurrencies maps every row and marks the main currency`() = runTest {
        val api = FakeCurrenciesApi(
            payload = CurrenciesPayload(
                currencies = listOf(
                    CurrencyDTO(id = 1, name = "1&amp;1 Bank", rate = "1.0000"),
                    CurrencyDTO(id = 2, name = "US Dollar", rate = "1.1000")
                ),
                mainCurrencyId = 1
            )
        )

        val result = repository(api).getCurrencies().getOrThrow()

        assertEquals(listOf("1&1 Bank", "US Dollar"), result.map { it.name })
        assertEquals(listOf(1.0, 1.1), result.map { it.rate })
        assertTrue(result.first().isMain)
        assertEquals(false, result[1].isMain)
    }

    @Test
    fun `a failed getCurrencies surfaces the WallosError`() = runTest {
        val api = FakeCurrenciesApi(allFailure = WallosError.Server("502 Bad Gateway"))

        assertFailsWith<WallosError.Server> { repository(api).getCurrencies().getOrThrow() }
    }

    @Test
    fun `addCurrency sends every field and returns the new id`() = runTest {
        val api = FakeCurrenciesApi(addedId = 9)

        val result = repository(api).addCurrency("Euro", "€", "EUR", 1.0).getOrThrow()

        assertEquals(9, result)
        val fields = api.addedFields?.asMap()
        assertEquals("Euro", fields?.get("name"))
        assertEquals("€", fields?.get("symbol"))
        assertEquals("EUR", fields?.get("code"))
        assertEquals("1.0", fields?.get("rate"))
    }

    @Test
    fun `editCurrency sends the id and every field`() = runTest {
        val api = FakeCurrenciesApi()

        repository(api).editCurrency(7, "Euro", "€", "EUR", 1.17).getOrThrow()

        val (id, fields) = api.editCalls.single()
        assertEquals(7, id)
        assertEquals("1.17", fields.asMap()["rate"])
    }

    @Test
    fun `deleteCurrency forwards the id`() = runTest {
        val api = FakeCurrenciesApi()

        repository(api).deleteCurrency(4).getOrThrow()

        assertEquals(listOf(4), api.deleteCalls)
    }

    @Test
    fun `a delete on the main currency surfaces as InUse`() = runTest {
        val api = FakeCurrenciesApi(deleteFailure = WallosError.InUse("Currency in use"))

        assertFailsWith<WallosError.InUse> { repository(api).deleteCurrency(1).getOrThrow() }
    }

    private fun repository(api: CurrenciesApi = FakeCurrenciesApi()): CurrenciesRepositoryImpl =
        CurrenciesRepositoryImpl(
            api = api,
            mapper = CurrencyMapper(HtmlUnescaper()),
            dispatcher = UnconfinedTestDispatcher()
        )

    private class FakeCurrenciesApi(
        private val payload: CurrenciesPayload? = null,
        private val addedId: Int = 0,
        private val allFailure: Throwable? = null,
        private val addFailure: Throwable? = null,
        private val editFailure: Throwable? = null,
        private val deleteFailure: Throwable? = null
    ) : CurrenciesApi {

        var addedFields: FormParams? = null
            private set
        val editCalls = mutableListOf<Pair<Int, FormParams>>()
        val deleteCalls = mutableListOf<Int>()

        override suspend fun getAll(): CurrenciesPayload {
            allFailure?.let { throw it }
            return payload ?: error("currencies not set")
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
