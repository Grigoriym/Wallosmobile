package com.grappim.wallosmobile.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.grappim.wallosmobile.core.storage.db.CurrencyDao
import com.grappim.wallosmobile.core.storage.db.PriceConversionDao
import com.grappim.wallosmobile.core.storage.db.SubscriptionDao
import com.grappim.wallosmobile.core.storage.db.WallosDB
import okio.Path.Companion.toPath
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

private const val STORAGE_FILE_NAME = "wallos_storage"
private const val DATABASE_NAME = "wallos.db"

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

    /**
     * `BundledSQLiteDriver` ships its own SQLite rather than using the one on the device, so
     * every install behaves the same regardless of API level.
     *
     * `fallbackToDestructiveMigration` because pre-v1 there is nothing to migrate *from*, and the
     * contents are a server snapshot: dropping them costs one refresh. The day the app ships,
     * that stays true for this database — the API key is the thing that needs care, not the cache.
     */
    @Single
    fun provideWallosDB(context: Context): WallosDB = Room
        .databaseBuilder<WallosDB>(
            context = context.applicationContext,
            name = context.applicationContext.getDatabasePath(DATABASE_NAME).absolutePath
        )
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    @Single
    fun provideSubscriptionDao(db: WallosDB): SubscriptionDao = db.subscriptionDao()

    @Single
    fun provideCurrencyDao(db: WallosDB): CurrencyDao = db.currencyDao()

    @Single
    fun providePriceConversionDao(db: WallosDB): PriceConversionDao = db.priceConversionDao()
}
