package com.grappim.wallosmobile.core.api

import com.grappim.wallosmobile.core.domain.WallosError
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Serializable
private data class SubscriptionsEnvelope(
    val success: Boolean,
    val title: String? = null,
    val subscriptions: List<SubscriptionRow> = emptyList()
)

@Serializable
private data class SubscriptionRow(val id: Int, val name: String)

class WallosEnvelopeParserTest {

    private val sut = WallosEnvelopeParser()

    private fun parse(body: String, statusCode: Int = HTTP_OK): SubscriptionsEnvelope =
        sut.parse(statusCode, body, SubscriptionsEnvelope.serializer())

    @Test
    fun `decodes a successful envelope`() {
        val body = """
            {"success":true,"title":"subscriptions",
            "subscriptions":[{"id":1,"name":"Netflix"}],"notes":[]}
        """.trimIndent()

        val result = parse(body)

        assertEquals("subscriptions", result.title)
        assertEquals(listOf(SubscriptionRow(id = 1, name = "Netflix")), result.subscriptions)
    }

    @Test
    fun `ignores fields the client does not model`() {
        val body = """{"success":true,"title":"subscriptions","notes":[],"future_field":42}"""

        assertEquals(true, parse(body).success)
    }

    @Test
    fun `strips the PHP display_errors prefix and parses the JSON that follows`() {
        val body = "<br />\n<b>Warning</b>:  Trying to access array offset on null in " +
            "/var/www/html/api/subscriptions/get_subscriptions.php on line 84<br />\n" +
            """{"success":true,"title":"subscriptions","subscriptions":[{"id":7,"name":"Plex"}]}"""

        val result = parse(body)

        assertEquals(listOf(SubscriptionRow(id = 7, name = "Plex")), result.subscriptions)
    }

    @Test
    fun `a diagnostics prefix in front of an error envelope still maps the error`() {
        val body = "<br /><b>Warning</b>: something<br />" +
            """{"success":false,"title":"Invalid API key"}"""

        val error = assertFailsWith<WallosError.Unauthenticated> { parse(body) }

        assertEquals("Invalid API key", error.title)
    }

    @Test
    fun `404 means the endpoint is absent on this Wallos version`() {
        assertFailsWith<WallosError.UnsupportedEndpoint> {
            parse(body = "<html>404 Not Found</html>", statusCode = HTTP_NOT_FOUND)
        }
    }

    @Test
    fun `5xx is a server failure and keeps the body as detail`() {
        val body = "<html>500 Internal Server Error</html>"

        val error = assertFailsWith<WallosError.Server> {
            parse(body = body, statusCode = HTTP_INTERNAL_ERROR)
        }

        assertEquals(body, error.detail)
    }

    @Test
    fun `5xx with an empty body falls back to the status code`() {
        val error = assertFailsWith<WallosError.Server> {
            parse(body = "", statusCode = HTTP_BAD_GATEWAY)
        }

        assertEquals("HTTP 502", error.detail)
    }

    @Test
    fun `a body with no JSON at all is malformed`() {
        val body = "Connection to the database failed."

        val error = assertFailsWith<WallosError.Malformed> { parse(body) }

        assertEquals(body, error.body)
    }

    @Test
    fun `truncated JSON is malformed and keeps the whole body for logging`() {
        val body = """{"success":true,"subscriptions":["""

        val error = assertFailsWith<WallosError.Malformed> { parse(body) }

        assertEquals(body, error.body)
    }

    @Test
    fun `a payload that does not match the model is malformed`() {
        val body = """{"success":true,"subscriptions":[{"id":1}]}"""

        val error = assertFailsWith<WallosError.Malformed> { parse(body) }

        assertEquals(body, error.body)
    }

    @Test
    fun `success false is an error even though the status is 200`() {
        val error = assertFailsWith<WallosError.Validation> {
            parse("""{"success":false,"title":"Invalid date","message":"next_payment is invalid."}""")
        }

        assertEquals("Invalid date", error.title)
        assertEquals("next_payment is invalid.", error.detail)
    }

    @Test
    fun `detail comes from notes when there is no message`() {
        val body = """
            {"success":false,"title":"Invalid parameter",
            "notes":["Invalid reference_date."]}
        """.trimIndent()

        val error = assertFailsWith<WallosError.Validation> { parse(body) }

        assertEquals("Invalid reference_date.", error.detail)
    }

    @Test
    fun `detail falls back to the title when neither message nor notes carry text`() {
        val error = assertFailsWith<WallosError.NotFound> {
            parse("""{"success":false,"title":"Subscription not found","notes":[]}""")
        }

        assertEquals("Subscription not found", error.detail)
    }

    @Test
    fun `notes as a string does not break detail resolution`() {
        // get_user.php returns `notes` as an empty string rather than an array.
        val error = assertFailsWith<WallosError.Unauthenticated> {
            parse("""{"success":false,"title":"Invalid API key","notes":""}""")
        }

        assertEquals("Invalid API key", error.title)
    }

    @Test
    fun `a missing success field is treated as failure`() {
        val error = assertFailsWith<WallosError.Server> {
            parse("""{"title":null,"message":"who knows"}""")
        }

        assertEquals("who knows", error.detail)
    }

    @Test
    fun `the reified overload resolves the serializer at the call site`() {
        val result: SubscriptionsEnvelope =
            sut.parse(HTTP_OK, """{"success":true,"subscriptions":[{"id":3,"name":"Sonarr"}]}""")

        assertEquals(listOf(SubscriptionRow(id = 3, name = "Sonarr")), result.subscriptions)
    }

    private companion object {
        const val HTTP_OK = 200
        const val HTTP_NOT_FOUND = 404
        const val HTTP_INTERNAL_ERROR = 500
        const val HTTP_BAD_GATEWAY = 502
    }
}
