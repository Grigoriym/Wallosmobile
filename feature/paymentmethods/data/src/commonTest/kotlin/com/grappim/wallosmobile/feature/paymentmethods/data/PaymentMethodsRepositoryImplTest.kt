package com.grappim.wallosmobile.feature.paymentmethods.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.api.MultipartFile
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.paymentmethods.domain.model.IconFile
import com.grappim.wallosmobile.feature.paymentmethods.dto.PaymentMethodDTO
import com.grappim.wallosmobile.feature.paymentmethods.mapper.PaymentMethodMapper
import com.grappim.wallosmobile.feature.subscriptions.mapper.HtmlUnescaper
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaymentMethodsRepositoryImplTest {

    @Test
    fun `getPaymentMethods maps every row through the mapper`() = runTest {
        val api = FakePaymentMethodsApi(
            all = listOf(PaymentMethodDTO(id = 1, name = "PayPal &amp; Co", enabled = 1))
        )

        val result = repository(api).getPaymentMethods().getOrThrow()

        assertEquals(listOf("PayPal & Co"), result.map { it.name })
    }

    @Test
    fun `a failed getPaymentMethods surfaces the WallosError`() = runTest {
        val api = FakePaymentMethodsApi(allFailure = WallosError.Server("502 Bad Gateway"))

        assertFailsWith<WallosError.Server> { repository(api).getPaymentMethods().getOrThrow() }
    }

    @Test
    fun `addPaymentMethod sends the name and enabled flag and returns the new id`() = runTest {
        val api = FakePaymentMethodsApi(addedId = 32)

        val result = repository(api).addPaymentMethod("PayPal", enabled = true).getOrThrow()

        assertEquals(32, result)
        assertEquals("PayPal", api.addedFields?.asMap()?.get("name"))
        assertEquals("1", api.addedFields?.asMap()?.get("enabled"))
        assertNull(api.addedFields?.asMap()?.get("icon_url"))
    }

    @Test
    fun `addPaymentMethod forwards an icon_url when given one`() = runTest {
        val api = FakePaymentMethodsApi(addedId = 32)

        repository(api).addPaymentMethod("PayPal", enabled = true, iconUrl = "https://example.com/paypal.png")
            .getOrThrow()

        assertEquals("https://example.com/paypal.png", api.addedFields?.asMap()?.get("icon_url"))
    }

    @Test
    fun `editPaymentMethod sends the id, name and enabled flag`() = runTest {
        val api = FakePaymentMethodsApi()

        repository(api).editPaymentMethod(7, "Renamed", enabled = false).getOrThrow()

        val (id, fields) = api.editCalls.single()
        assertEquals(7, id)
        assertEquals("Renamed", fields.asMap()["name"])
        assertEquals("0", fields.asMap()["enabled"])
    }

    @Test
    fun `addPaymentMethod with an icon file switches to the multipart call`() = runTest {
        val api = FakePaymentMethodsApi(addedId = 32)

        repository(api).addPaymentMethod("PayPal", enabled = true, iconFile = iconFile()).getOrThrow()

        assertNull(api.addedFields)
        assertEquals("PayPal", api.addedIconFields?.asMap()?.get("name"))
        val sentIcon = assertNotNull(api.addedIcon)
        assertEquals("paymenticon", sentIcon.fieldName)
        assertEquals("icon.png", sentIcon.fileName)
        assertContentEquals(byteArrayOf(1, 2, 3), sentIcon.bytes)
    }

    @Test
    fun `editPaymentMethod with an icon file switches to the multipart call too`() = runTest {
        val api = FakePaymentMethodsApi()

        repository(api).editPaymentMethod(7, "Renamed", enabled = false, iconFile = iconFile()).getOrThrow()

        assertTrue(api.editCalls.isEmpty())
        val (id, fields) = api.editIconCalls.single()
        assertEquals(7, id)
        assertEquals("Renamed", fields.asMap()["name"])
    }

    private fun iconFile() = IconFile(bytes = byteArrayOf(1, 2, 3), fileName = "icon.png", mimeType = "image/png")

    @Test
    fun `deletePaymentMethod forwards the id`() = runTest {
        val api = FakePaymentMethodsApi()

        repository(api).deletePaymentMethod(4).getOrThrow()

        assertEquals(listOf(4), api.deleteCalls)
    }

    @Test
    fun `a delete on a referenced payment method surfaces as InUse`() = runTest {
        val api = FakePaymentMethodsApi(deleteFailure = WallosError.InUse("Payment method in use"))

        assertFailsWith<WallosError.InUse> { repository(api).deletePaymentMethod(4).getOrThrow() }
    }

    private fun repository(api: PaymentMethodsApi = FakePaymentMethodsApi()): PaymentMethodsRepositoryImpl =
        PaymentMethodsRepositoryImpl(
            api = api,
            mapper = PaymentMethodMapper(HtmlUnescaper()),
            dispatcher = UnconfinedTestDispatcher()
        )

    private class FakePaymentMethodsApi(
        private val all: List<PaymentMethodDTO>? = null,
        private val addedId: Int = 0,
        private val allFailure: Throwable? = null,
        private val addFailure: Throwable? = null,
        private val editFailure: Throwable? = null,
        private val deleteFailure: Throwable? = null
    ) : PaymentMethodsApi {

        var addedFields: FormParams? = null
            private set
        var addedIconFields: FormParams? = null
            private set
        var addedIcon: MultipartFile? = null
            private set
        val editCalls = mutableListOf<Pair<Int, FormParams>>()
        val editIconCalls = mutableListOf<Pair<Int, FormParams>>()
        val deleteCalls = mutableListOf<Int>()

        override suspend fun getAll(): List<PaymentMethodDTO> {
            allFailure?.let { throw it }
            return all ?: error("payment methods not set")
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

        override suspend fun addWithIcon(fields: FormParams, icon: MultipartFile): Int {
            addFailure?.let { throw it }
            addedIconFields = fields
            addedIcon = icon
            return addedId
        }

        override suspend fun editWithIcon(id: Int, fields: FormParams, icon: MultipartFile) {
            editFailure?.let { throw it }
            editIconCalls += id to fields
        }

        override suspend fun delete(id: Int) {
            deleteFailure?.let { throw it }
            deleteCalls += id
        }
    }
}
