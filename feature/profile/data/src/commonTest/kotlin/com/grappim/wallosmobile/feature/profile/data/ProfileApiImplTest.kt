package com.grappim.wallosmobile.feature.profile.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.api.WallosEnvelopeParser
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
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileApiImplTest {

    @Test
    fun `unwraps the user envelope, nested under user`() = runTest {
        val result = api(USER_BODY).getUser()

        assertEquals(1, result.id)
        assertEquals("gregorz", result.username)
        assertEquals("gregorz@example.com", result.email)
        assertEquals(150.5, result.budget)
        assertEquals(25.0, result.periodBudget)
        assertEquals(1, result.mainCurrency)
        assertEquals("monthly", result.budgetPeriodType)
        assertEquals("2026-07-18", result.budgetPeriodAnchorDate)
        assertEquals(0, result.totpEnabled)
    }

    @Test
    fun `setBudget hits the documented path and sends all four fields`() = runTest {
        var captured: HttpRequestData? = null

        api(OK_BODY) { captured = it }.setBudget(
            FormParams()
                .put("monthly_budget", "150.0")
                .put("period_budget", "25.0")
                .put("budget_period_type", "weekly")
                .put("budget_period_anchor_date", "2026-07-18")
        )

        val request = checkNotNull(captured)
        val body = request.body as FormDataContent
        assertEquals("/api/users/set_budget.php", request.url.encodedPath)
        assertEquals("150.0", body.formData["monthly_budget"])
        assertEquals("25.0", body.formData["period_budget"])
        assertEquals("weekly", body.formData["budget_period_type"])
        assertEquals("2026-07-18", body.formData["budget_period_anchor_date"])
    }

    private fun api(
        responseBody: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        onRequest: (HttpRequestData) -> Unit = {}
    ): ProfileApi {
        val engine = MockEngine { request ->
            onRequest(request)
            respond(content = responseBody, status = status)
        }
        return ProfileApiImpl(
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
        const val USER_BODY = """
            {"success":true,"title":"user","user":{"id":1,"username":"gregorz",
             "email":"gregorz@example.com","password":"********","main_currency":1,
             "avatar":"images/avatars/0.svg","language":"en","budget":150.5,"period_budget":25.0,
             "budget_period_type":"monthly","budget_period_anchor_date":"2026-07-18",
             "totp_enabled":0,"api_key":"********","firstname":"","lastname":"","oidc_sub":null},
             "notes":[]}
        """
        const val OK_BODY = """{"success":true,"title":"Updated","message":"Budget settings updated successfully."}"""
    }
}
