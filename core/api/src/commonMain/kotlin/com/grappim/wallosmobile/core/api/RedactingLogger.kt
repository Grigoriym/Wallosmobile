package com.grappim.wallosmobile.core.api

import com.grappim.wallosmobile.core.logger.logcat
import io.ktor.client.plugins.logging.Logger

/**
 * Wallos carries its credential in the **form body**, not a header, so Ktor's own header
 * sanitizer can't help: at `LogLevel.BODY` the API key — and, on the login bridge, the user's
 * password — would be written to logcat verbatim on every request. Plan §4.1.
 */
internal class RedactingLogger(private val tag: String) : Logger {
    override fun log(message: String) {
        logcat(tag = tag) { redactCredentials(message) }
    }
}

/**
 * Both spellings of the key parameter are accepted by the server, so both have to be redacted
 * even though we only ever send `api_key`.
 */
private val CREDENTIAL_PARAMS = Regex("""((?:api_key|apiKey|password)=)[^&\s]*""")

internal fun redactCredentials(message: String): String = message.replace(CREDENTIAL_PARAMS, "$1REDACTED")
