package com.grappim.wallosmobile.core.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import org.koin.core.annotation.Single
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES/GCM with a key that never leaves the Android Keystore, so the API key is not sitting in
 * cleartext inside DataStore's file. Stored form is `base64(iv || ciphertext)`.
 */
@Single(binds = [SecretCipher::class])
internal class KeystoreSecretCipher : SecretCipher {

    override fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.encodeToByteArray())
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    @Suppress("ReturnCount")
    override fun decrypt(value: String): String? {
        val bytes = try {
            Base64.decode(value, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            logcat(priority = LogPriority.WARN, throwable = e) { "Stored key is not valid base64" }
            return null
        }
        if (bytes.size <= IV_LENGTH_BYTES) {
            logcat(priority = LogPriority.WARN) { "Stored key is too short to hold an IV" }
            return null
        }
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(TAG_LENGTH_BITS, bytes, 0, IV_LENGTH_BYTES)
            )
            cipher.doFinal(bytes, IV_LENGTH_BYTES, bytes.size - IV_LENGTH_BYTES).decodeToString()
        } catch (e: GeneralSecurityException) {
            // The Keystore key is gone or no longer matches this ciphertext — a restored backup,
            // or a reinstall. Treated as "no key stored"; the user re-onboards.
            logcat(priority = LogPriority.WARN, throwable = e) { "Stored key could not be decrypted" }
            null
        } catch (e: IOException) {
            logcat(priority = LogPriority.WARN, throwable = e) { "Keystore is unreadable" }
            null
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec
                .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "wallos_api_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH_BYTES = 12
        private const val TAG_LENGTH_BITS = 128
    }
}
