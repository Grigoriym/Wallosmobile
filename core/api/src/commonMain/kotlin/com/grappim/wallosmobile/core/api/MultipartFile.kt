package com.grappim.wallosmobile.core.api

/**
 * One file part for [WallosApiClient.postMultipart] — `set_subscriptions.php`'s `logo` field is
 * the first caller (`WALLOS_API.md` §3.4, §4). Not a `data class`: [bytes] would give it a
 * reference-equality `equals`/`hashCode` pair that looks structural and isn't, and nothing here
 * needs either.
 */
class MultipartFile(val fieldName: String, val fileName: String, val mimeType: String, val bytes: ByteArray)
