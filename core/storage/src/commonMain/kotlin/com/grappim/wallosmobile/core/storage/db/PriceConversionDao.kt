package com.grappim.wallosmobile.core.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * `LIMIT 1` rather than a `WHERE id =`: [PriceConversionEntity.SINGLETON_ID] is the only id the
 * table ever holds, so the row is either there or the instance has never been refreshed.
 */
@Dao
interface PriceConversionDao {

    @Query("SELECT * FROM $PRICE_CONVERSION_TABLE LIMIT 1")
    fun observe(): Flow<PriceConversionEntity?>

    @Query("SELECT * FROM $PRICE_CONVERSION_TABLE LIMIT 1")
    suspend fun get(): PriceConversionEntity?

    /** An insert with `REPLACE` on a fixed primary key, so it upserts the one row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(conversion: PriceConversionEntity)

    @Query("DELETE FROM $PRICE_CONVERSION_TABLE")
    suspend fun deleteAll()
}
