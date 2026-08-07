package com.grappim.wallosmobile.feature.subscriptions.domain.model

/**
 * A logo image picked on-device, held in memory until [AddSubscriptionParams]/
 * [EditSubscriptionParams] upload it as `set_subscriptions.php`'s `logo` multipart field
 * (`WALLOS_API.md` §3.4, §4). Not a `data class`: [bytes] would give it a reference-equality
 * `equals`/`hashCode` pair that looks structural and isn't, and nothing here needs either.
 */
class LogoFile(val bytes: ByteArray, val fileName: String, val mimeType: String)
