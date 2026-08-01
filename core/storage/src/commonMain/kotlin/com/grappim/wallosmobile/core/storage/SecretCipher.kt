package com.grappim.wallosmobile.core.storage

/**
 * Encrypts the API key before it reaches DataStore, which stores plaintext on disk.
 *
 * A platform seam rather than an `expect`/`actual`: the Android implementation talks to the
 * Android Keystore, which does not exist in a host test, so every storage test would have to be
 * an instrumented one. As an interface it can be faked, and [ApiKeyStorageImpl] stays in
 * `commonMain`.
 */
interface SecretCipher {

    /** Throws if the platform key store is unavailable — a genuine failure the caller surfaces. */
    fun encrypt(value: String): String

    /**
     * `null` when [value] cannot be read back: the key was invalidated, or the ciphertext came
     * from a device backup restored onto another device. That is an expected state, not an
     * error — it means "no key", and the user re-onboards.
     */
    fun decrypt(value: String): String?
}
