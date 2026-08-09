package com.grappim.wallosmobile.feature.paymentmethods.domain.model

/**
 * An icon image picked on-device, held in memory until [com.grappim.wallosmobile.feature.
 * paymentmethods.domain.repo.PaymentMethodsRepository] uploads it as `set_payment_methods.php`'s
 * `paymenticon` multipart field (`WALLOS_API.md` §3.10, §4). Not a `data class`, mirroring
 * `feature:subscriptions`' `LogoFile`: [bytes] would give it a reference-equality `equals`/
 * `hashCode` pair that looks structural and isn't, and nothing here needs either.
 */
class IconFile(val bytes: ByteArray, val fileName: String, val mimeType: String)
