package com.grappim.wallosmobile.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.grappim.kit.storage.SecretCipher
import com.grappim.wallosmobile.core.storage.db.CurrencyDao
import com.grappim.wallosmobile.core.storage.db.PriceConversionDao
import com.grappim.wallosmobile.core.storage.db.SubscriptionDao
import com.grappim.wallosmobile.core.storage.subscriptions.SubscriptionCriteriaStorageImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [ApiKeyStorage::class])
internal class ApiKeyStorageImpl(
    private val dataStore: DataStore<Preferences>,
    private val secretCipher: SecretCipher,
    private val subscriptionDao: SubscriptionDao,
    private val currencyDao: CurrencyDao,
    private val priceConversionDao: PriceConversionDao
) : ApiKeyStorage {

    private val storedKey: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_API_KEY]?.let(secretCipher::decrypt)
    }

    override val isConnected: Flow<Boolean> = storedKey
        .map { !it.isNullOrBlank() }
        .distinctUntilChanged()

    override suspend fun getKey(): String? = storedKey.first()

    override suspend fun setKey(key: String) {
        val encrypted = secretCipher.encrypt(key)
        dataStore.edit { prefs ->
            prefs[KEY_API_KEY] = encrypted
        }
    }

    /**
     * The cache goes with the key. It is emptied *before* the key, so there is no window in which
     * a screen observing the tables can read one account's rows with no credential behind them.
     * The subscriptions list's saved filter names that account's payers and categories, so it goes
     * in the same edit as the key.
     */
    override suspend fun clear() {
        subscriptionDao.deleteAll()
        currencyDao.deleteAll()
        priceConversionDao.deleteAll()
        dataStore.edit { prefs ->
            prefs.remove(KEY_API_KEY)
            prefs.remove(SubscriptionCriteriaStorageImpl.KEY_CRITERIA)
        }
    }

    private companion object {
        private val KEY_API_KEY = stringPreferencesKey("api_key")
    }
}
