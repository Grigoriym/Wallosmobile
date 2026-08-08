package com.grappim.wallosmobile.feature.profile.data

import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.api.WallosEnvelopeParser
import com.grappim.wallosmobile.core.storage.ApiKeyStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
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
        assertEquals(150.5, result.budget)
        assertEquals(25.0, result.periodBudget)
        assertEquals(1, result.mainCurrency)
    }

    private fun api(responseBody: String, status: HttpStatusCode = HttpStatusCode.OK): ProfileApi {
        val engine = MockEngine { respond(content = responseBody, status = status) }
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
    }
}
