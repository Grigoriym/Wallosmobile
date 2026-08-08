package com.grappim.wallosmobile.feature.dashboard.data

import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.api.WallosEnvelopeParser
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.core.storage.ApiKeyStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DashboardApiImplTest {

    @Test
    fun `unwraps the monthly cost envelope`() = runTest {
        val result = api(MONTHLY_COST_BODY).getMonthlyCost(month = 3, year = 2025)

        assertEquals("March 2025", result.title)
        assertEquals("1,234.56", result.monthlyCost)
        assertEquals("€", result.currencySymbol)
    }

    @Test
    fun `hits the documented paths with month and year`() = runTest {
        var body: FormDataContent? = null

        api(MONTHLY_COST_BODY) { body = it.body as FormDataContent }.getMonthlyCost(month = 3, year = 2025)

        assertEquals("3", body?.formData?.get("month"))
        assertEquals("2025", body?.formData?.get("year"))
    }

    @Test
    fun `unwraps the period budget envelope`() = runTest {
        val result = api(PERIOD_BUDGET_BODY).getPeriodBudget(referenceDate = null)

        assertEquals("Aug 1 - Aug 31", result.periodLabel)
        assertEquals(100.0, result.periodBudget)
        assertEquals(40.0, result.amountRemainingThisPeriod)
        assertEquals(0.0, result.amountOverBudget)
        assertEquals(false, result.isOverBudget)
        assertEquals("€", result.currencySymbol)
        assertEquals("2025-08-01", result.periodStart)
        assertEquals("2025-08-31", result.periodEnd)
    }

    @Test
    fun `sends no reference_date when null, and the formatted date when set`() = runTest {
        var noDateBody: FormDataContent? = null
        var withDateBody: FormDataContent? = null

        api(PERIOD_BUDGET_BODY) { noDateBody = it.body as FormDataContent }.getPeriodBudget(referenceDate = null)
        api(PERIOD_BUDGET_BODY) { withDateBody = it.body as FormDataContent }
            .getPeriodBudget(referenceDate = LocalDate(2025, 8, 15))

        assertNull(noDateBody?.formData?.get("reference_date"))
        assertEquals("2025-08-15", withDateBody?.formData?.get("reference_date"))
    }

    @Test
    fun `a 404 on get_period_budget surfaces as UnsupportedEndpoint`() = runTest {
        assertFailsWith<WallosError.UnsupportedEndpoint> {
            api(responseBody = "", status = HttpStatusCode.NotFound).getPeriodBudget(referenceDate = null)
        }
    }

    private fun api(
        responseBody: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        onRequest: (HttpRequestData) -> Unit = {}
    ): DashboardApi {
        val engine = MockEngine { request ->
            onRequest(request)
            respond(content = responseBody, status = status)
        }
        return DashboardApiImpl(
            WallosApiClient(
                httpClient = HttpClient(engine),
                apiKeyStorage = FakeApiKeyStorage,
                envelopeParser = WallosEnvelopeParser()
            )
        )
    }

    private object FakeApiKeyStorage : ApiKeyStorage {
        override val isConnected: Flow<Boolean> = flowOf(true)

        override suspend fun getKey(): String = "s3cr3tk3y"

        override suspend fun setKey(key: String) = error("not used by this test")

        override suspend fun clear() = error("not used by this test")
    }

    private companion object {
        const val MONTHLY_COST_BODY = """
            {"success":true,"title":"March 2025","monthly_cost":"1,234.56",
             "localized_monthly_cost":"€1,234.56","currency_code":"EUR","currency_symbol":"€","notes":[]}
        """
        const val PERIOD_BUDGET_BODY = """
            {"success":true,"title":"period_budget","period_budget":100.0,
             "amount_needed_this_period":60.0,"amount_needed_full_period":60.0,
             "amount_remaining_this_period":40.0,"amount_over_budget":0.0,"is_over_budget":false,
             "budget_period_type":"monthly","budget_period_anchor_date":"2025-08-01",
             "period_start":"2025-08-01","period_end":"2025-08-31","period_label":"Aug 1 - Aug 31",
             "currency_code":"EUR","currency_symbol":"€","reference_date":"2025-08-15","notes":[]}
        """
    }
}
