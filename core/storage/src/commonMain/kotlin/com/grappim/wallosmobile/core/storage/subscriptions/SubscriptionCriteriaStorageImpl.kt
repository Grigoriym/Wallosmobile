package com.grappim.wallosmobile.core.storage.subscriptions

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

/** Shares the module's one DataStore file, hence the `distinctUntilChanged`. */
@Single(binds = [SubscriptionCriteriaStorage::class])
internal class SubscriptionCriteriaStorageImpl(private val dataStore: DataStore<Preferences>) :
    SubscriptionCriteriaStorage {

    override val criteria: Flow<String?> = dataStore.data
        .map { prefs -> prefs[KEY_CRITERIA] }
        .distinctUntilChanged()

    override suspend fun setCriteria(value: String) {
        dataStore.edit { prefs ->
            prefs[KEY_CRITERIA] = value
        }
    }

    internal companion object {
        /** Also removed by `ApiKeyStorageImpl.clear()`, in the same edit as the key. */
        val KEY_CRITERIA = stringPreferencesKey("subscriptions_criteria")
    }
}
