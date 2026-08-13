package com.grappim.wallosmobile.core.api

import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import org.koin.core.annotation.Single

/**
 * Turns a raw Wallos response body into either a decoded envelope or a [WallosError].
 *
 * Wallos answers `200 OK` to everything — auth failures included — and PHP's `display_errors`
 * default prepends warning HTML to otherwise valid JSON, so nothing may decode straight from the
 * byte stream. This class is deliberately Ktor-free: it takes a status code and a string, which
 * makes it a pure unit-test target. See `docs/WALLOS_API.md` §5 and plan §4.2.
 */
@Single
class WallosEnvelopeParser {

    /**
     * Decodes [body] as [T], or throws the [WallosError] the response describes.
     *
     * @param statusCode the HTTP status. Only `404` and `5xx` mean anything — every API-level
     * failure arrives as `200`.
     */
    fun <T> parse(statusCode: Int, body: String, deserializer: DeserializationStrategy<T>): T {
        if (statusCode == HTTP_NOT_FOUND) throw WallosError.UnsupportedEndpoint
        if (statusCode >= HTTP_SERVER_ERROR) {
            throw WallosError.Server(body.ifBlank { "HTTP $statusCode" })
        }

        val envelope = decodeEnvelope(body)

        if (envelope.boolean(KEY_SUCCESS) != true) {
            val title = envelope.string(KEY_TITLE)
            // The human-readable detail sits in a different field per endpoint: `message` on
            // writes, `notes[0]` on get_monthly_cost / get_period_budget, nothing on most reads.
            val detail = envelope.string(KEY_MESSAGE) ?: envelope.firstNote() ?: title.orEmpty()
            throw mapError(title, detail)
        }

        return try {
            json.decodeFromJsonElement(deserializer, envelope)
        } catch (e: IllegalArgumentException) {
            logShapeMismatch(e, "response did not match the expected shape")
            throw WallosError.Malformed(body)
        }
    }

    private fun decodeEnvelope(body: String): JsonObject {
        val start = body.indexOf(JSON_START)
        if (start < 0) {
            logcat(priority = LogPriority.ERROR) { "response body carried no JSON at all: $body" }
            throw WallosError.Malformed(body)
        }
        if (start > 0) {
            // PHP wrote a warning or notice into the response before the JSON. The JSON that
            // follows is still valid, so parse it — but the prefix is a real server-side bug.
            logcat(priority = LogPriority.WARN) {
                "server emitted diagnostics before the JSON: ${body.take(start)}"
            }
        }

        return try {
            json.parseToJsonElement(body.substring(start)).jsonObject
        } catch (e: IllegalArgumentException) {
            logShapeMismatch(e, "response body was not a JSON object")
            throw WallosError.Malformed(body)
        }
    }

    /**
     * The real [e] (message, stack) is logged at [LogPriority.WARN] for local Logcat only —
     * `CrashlyticsTree` only forwards `ERROR`. A `kotlinx.serialization` decode failure embeds the
     * full offending JSON body in [e]'s own message, so what actually goes to [LogPriority.ERROR]
     * (and therefore Crashlytics) is a scrubbed exception with a fixed message instead.
     */
    private fun logShapeMismatch(e: IllegalArgumentException, message: String) {
        logcat(priority = LogPriority.WARN, throwable = e) { message }
        val scrubbed = IllegalArgumentException("Envelope shape mismatch: ${e::class.simpleName}")
        logcat(priority = LogPriority.ERROR, throwable = scrubbed) { message }
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.boolean(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull

    /**
     * `notes` is a JSON array on every endpoint checked, `get_user.php` included (corrected
     * 2026-08-08 — a live `curl` disproved the doc's older claim that it comes back as an empty
     * string there; `docs/WALLOS_API.md` §1 has the evidence). The safe cast stays regardless: it
     * costs nothing and keeps this parser from breaking if a future endpoint ever does send
     * something else.
     */
    private fun JsonObject.firstNote(): String? =
        ((this[KEY_NOTES] as? JsonArray)?.firstOrNull() as? JsonPrimitive)?.contentOrNull

    private companion object {
        /**
         * Mandatory, not a nicety: responses are raw DB rows and self-hosted instances sit at
         * different migration levels.
         */
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        const val JSON_START = '{'
        const val HTTP_NOT_FOUND = 404
        const val HTTP_SERVER_ERROR = 500

        const val KEY_SUCCESS = "success"
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"
        const val KEY_NOTES = "notes"
    }
}

/** [WallosEnvelopeParser.parse] with the serializer resolved from the call site. */
inline fun <reified T> WallosEnvelopeParser.parse(statusCode: Int, body: String): T =
    parse(statusCode, body, serializer())
