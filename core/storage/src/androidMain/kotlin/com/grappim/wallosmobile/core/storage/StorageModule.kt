package com.grappim.wallosmobile.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import okio.Path.Companion.toPath
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

private const val STORAGE_FILE_NAME = "wallos_storage"

/**
 * Lives in `androidMain`, unlike every other module's DI class, because the DataStore file path
 * needs a `Context`. Android is the only target, so this is also the only `@ComponentScan` of
 * `core.storage` — the `commonMain` implementations are compiled into this same compilation and
 * are picked up by it. A second target would split this the way TaigaMobileNova does, with an
 * `expect class PlatformStorageModule`.
 */
@Module
@Configuration
@ComponentScan("com.grappim.wallosmobile.core.storage")
class StorageModule {

    /**
     * One store for both the URL and the key: `ApiKeyStorage.clear()` removes its own key rather
     * than clearing the file, so sharing it costs nothing and keeps disconnect from wiping the
     * server the user just typed in.
     */
    @Single
    fun provideDataStore(context: Context): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = { context.preferencesDataStoreFile(STORAGE_FILE_NAME).absolutePath.toPath() }
    )
}
