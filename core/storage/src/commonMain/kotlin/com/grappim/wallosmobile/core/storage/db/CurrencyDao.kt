package com.grappim.wallosmobile.core.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {

    @Query("SELECT * FROM $CURRENCY_TABLE ORDER BY id")
    suspend fun getAll(): List<CurrencyEntity>

    /** The same rows a screen can keep reading: re-emits whenever a refresh replaces the table. */
    @Query("SELECT * FROM $CURRENCY_TABLE ORDER BY id")
    fun observeAll(): Flow<List<CurrencyEntity>>

    /** A snapshot of the instance's whole currency list, for the same reason as the subscriptions. */
    @Transaction
    suspend fun replaceAll(currencies: List<CurrencyEntity>) {
        deleteAll()
        insertAll(currencies)
    }

    @Query("DELETE FROM $CURRENCY_TABLE")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(currencies: List<CurrencyEntity>)
}
