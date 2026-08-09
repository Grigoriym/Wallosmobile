package com.grappim.wallosmobile.feature.paymentmethods.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.api.MultipartFile
import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.api.WallosEnvelopeParser
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.core.storage.ApiKeyStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PaymentMethodsApiImplTest {

    @Test
    fun `unwraps the payment methods list`() = runTest {
        val result = api(GET_ALL_BODY).getAll()

        assertEquals(2, result.size)
        assertEquals("PayPal", result.first().name)
        assertEquals("images/uploads/icons/paypal.png", result.first().icon)
        assertEquals(1, result.first().enabled)
        assertEquals(false, result.first().inUse)
        assertEquals(true, result[1].inUse)
    }

    @Test
    fun `reads an empty list as an empty list`() = runTest {
        assertTrue(api(EMPTY_BODY).getAll().isEmpty())
    }

    @Test
    fun `hits the documented paths`() = runTest {
        var getPath: String? = null
        var setPath: String? = null

        api(GET_ALL_BODY) { getPath = it.url.encodedPath }.getAll()
        api(ADD_BODY) { setPath = it.url.encodedPath }.add(FormParams().put("name", "New"))

        assertEquals("/api/payment_methods/get_payment_methods.php", getPath)
        assertEquals("/api/payment_methods/set_payment_methods.php", setPath)
    }

    @Test
    fun `add returns the id under the paymentId alias`() = runTest {
        val id = api(ADD_BODY).add(FormParams().put("name", "New"))

        assertEquals(32, id)
    }

    @Test
    fun `edit sends the id under the paymentId alias`() = runTest {
        var body: FormDataContent? = null

        api(OK_BODY) { body = it.body as FormDataContent }
            .edit(7, FormParams().put("name", "Renamed").put("enabled", "0"))

        assertEquals("edit", body?.formData?.get("action"))
        assertEquals("7", body?.formData?.get("paymentId"))
        assertEquals("Renamed", body?.formData?.get("name"))
        assertEquals("0", body?.formData?.get("enabled"))
    }

    @Test
    fun `delete sends the id under the paymentId alias`() = runTest {
        var body: FormDataContent? = null

        api(OK_BODY) { body = it.body as FormDataContent }.delete(7)

        assertEquals("delete", body?.formData?.get("action"))
        assertEquals("7", body?.formData?.get("paymentId"))
    }

    @Test
    fun `surfaces the in-use delete failure as a typed WallosError`() = runTest {
        assertFailsWith<WallosError.InUse> { api(IN_USE_BODY).delete(7) }
    }

    // --- 9.5: multipart icon upload -----------------------------------------------------------

    @OptIn(InternalAPI::class)
    @Test
    fun `addWithIcon sends a multipart body carrying the paymenticon field`() = runTest {
        var body: MultiPartFormDataContent? = null

        val id = api(ADD_BODY) { body = it.body as MultiPartFormDataContent }
            .addWithIcon(FormParams().put("name", "New"), iconFile())

        assertEquals(32, id)
        val parts = body?.parts.orEmpty()
        assertEquals("add", parts.formValue("action"))
        assertEquals("New", parts.formValue("name"))
        val filePart = parts.filterIsInstance<PartData.BinaryItem>().single()
        assertEquals("paymenticon", filePart.name)
    }

    @OptIn(InternalAPI::class)
    @Test
    fun `editWithIcon sends the id under the paymentId alias, still multipart`() = runTest {
        var body: MultiPartFormDataContent? = null

        api(OK_BODY) { body = it.body as MultiPartFormDataContent }
            .editWithIcon(7, FormParams().put("name", "Renamed"), iconFile())

        val parts = body?.parts.orEmpty()
        assertEquals("edit", parts.formValue("action"))
        assertEquals("7", parts.formValue("paymentId"))
        assertEquals(1, parts.filterIsInstance<PartData.BinaryItem>().size)
    }

    private fun iconFile() = MultipartFile(
        fieldName = "paymenticon",
        fileName = "icon.png",
        mimeType = "image/png",
        bytes = byteArrayOf(1, 2, 3)
    )

    private fun List<PartData>.formValue(name: String): String? =
        filterIsInstance<PartData.FormItem>().firstOrNull { it.name == name }?.value

    private fun api(responseBody: String, onRequest: (HttpRequestData) -> Unit = {}): PaymentMethodsApi {
        val engine = MockEngine { request ->
            onRequest(request)
            respond(content = responseBody, status = HttpStatusCode.OK)
        }
        return PaymentMethodsApiImpl(
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
        const val GET_ALL_BODY = """
            {"success":true,"title":"payment_methods","payment_methods":[
                {"id":1,"name":"PayPal","icon":"images/uploads/icons/paypal.png","enabled":1,"order":1,"in_use":false},
                {"id":2,"name":"Cash","icon":"images/uploads/icons/cash.png","enabled":1,"order":2,"in_use":true}
            ]}
        """
        const val EMPTY_BODY = """{"success":true,"title":"payment_methods","payment_methods":[]}"""
        const val ADD_BODY = """{"success":true,"title":"Payment method added","paymentId":32}"""
        const val OK_BODY = """{"success":true,"title":"Payment method updated"}"""
        const val IN_USE_BODY = """{"success":false,"title":"Payment method in use","message":"in use"}"""
    }
}
