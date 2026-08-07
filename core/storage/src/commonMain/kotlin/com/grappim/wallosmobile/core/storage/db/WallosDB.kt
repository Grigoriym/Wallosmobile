package com.grappim.wallosmobile.core.storage.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * The offline cache. Holds only what the read screens render, and only ever as a snapshot of the
 * server — nothing here is a source of truth, so there is no dirty-write state to reconcile.
 *
 * `version = 3` with no `autoMigrations`: pre-v1 there are no installs to migrate, and the
 * builder drops the tables on a schema change (see `StorageModule`). The exported schema JSON is
 * committed all the same — it is what the first real migration will be written against.
 */
@Database(
    entities = [
        SubscriptionEntity::class,
        CurrencyEntity::class,
        PriceConversionEntity::class
    ],
    version = 3,
    exportSchema = true
)
@ConstructedBy(WallosDBConstructor::class)
abstract class WallosDB : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun priceConversionDao(): PriceConversionDao
}

// The Room compiler generates the `actual`.
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object WallosDBConstructor : RoomDatabaseConstructor<WallosDB> {
    override fun initialize(): WallosDB
}
