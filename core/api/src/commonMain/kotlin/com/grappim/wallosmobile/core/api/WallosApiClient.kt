package com.grappim.wallosmobile.core.api

import com.grappim.wallosmobile.core.storage.ApiKeyStorage
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.serializer
import org.koin.core.annotation.Single

/**
 * The only way a feature API talks to Wallos. It owns the two things every call needs and no
 * call should repeat: the API key in the form body, and the envelope parse.
 *
 * Everything is a form POST — one shape for the whole API, and the key never reaches server
 * access logs, browser history or referrers. Read endpoints accept POST too (plan §4.1).
 */
@Single
class WallosApiClient(
    private val httpClient: HttpClient,
    private val apiKeyStorage: ApiKeyStorage,
    private val envelopeParser: WallosEnvelopeParser
) {

    /**
     * @param path **relative** to the instance root — `api/status/version.php`, no leading
     * slash. A leading slash discards any subpath the user's install lives under.
     * @throws com.grappim.wallosmobile.core.domain.WallosError for every failure, HTTP or API.
     */
    suspend fun <T> post(path: String, params: FormParams, deserializer: DeserializationStrategy<T>): T {
        val response = httpClient.submitForm(
            url = path,
            formParameters = params.withApiKey(apiKeyStorage.getKey()).build()
        )
        return envelopeParser.parse(response.status.value, response.bodyAsText(), deserializer)
    }

    /**
     * [post], plus one [file] part — `multipart/form-data` (API doc §4.2) rather than the
     * urlencoded body every other call sends, since a file can't ride inside that.
     */
    suspend fun <T> postMultipart(
        path: String,
        params: FormParams,
        file: MultipartFile,
        deserializer: DeserializationStrategy<T>
    ): T {
        val response = httpClient.submitFormWithBinaryData(
            url = path,
            formData = formData {
                params.withApiKey(apiKeyStorage.getKey()).asMap().forEach { (key, value) -> append(key, value) }
                append(
                    file.fieldName,
                    file.bytes,
                    Headers.build {
                        append(HttpHeaders.ContentType, file.mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=\"${file.fileName}\"")
                    }
                )
            }
        )
        return envelopeParser.parse(response.status.value, response.bodyAsText(), deserializer)
    }
}

/**
 * [WallosApiClient.post] with the serializer resolved from the call site. It can't be a member:
 * a public `inline` function may not read the class's private state — the same wall
 * [WallosEnvelopeParser] hit (plan §4.1, §4.2).
 */
suspend inline fun <reified T> WallosApiClient.post(path: String, params: FormParams = FormParams()): T =
    post(path, params, serializer())

/** [WallosApiClient.postMultipart] with the serializer resolved from the call site. */
suspend inline fun <reified T> WallosApiClient.postMultipart(path: String, params: FormParams, file: MultipartFile): T =
    postMultipart(path, params, file, serializer())
