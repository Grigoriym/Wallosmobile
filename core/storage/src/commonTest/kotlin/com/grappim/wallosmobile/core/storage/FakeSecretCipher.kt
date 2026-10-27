package com.grappim.wallosmobile.core.storage

import com.grappim.kit.storage.SecretCipher

/**
 * Reversible stand-in for the Keystore: `encrypt` is observable in the stored value, and anything
 * it did not produce fails to decrypt — the restored-backup case.
 */
internal class FakeSecretCipher : SecretCipher {

    override fun encrypt(value: String): String = PREFIX + value

    override fun decrypt(value: String): String? = value.removePrefix(PREFIX).takeIf { value.startsWith(PREFIX) }

    companion object {
        const val PREFIX = "enc:"
    }
}
