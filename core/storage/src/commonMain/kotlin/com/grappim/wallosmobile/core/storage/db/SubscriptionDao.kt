package com.grappim.wallosmobile.core.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    /**
     * The list, oldest id first — the same order `get_subscriptions.php` returns without a `sort`
     * parameter (2.4). Sorting is client-side and belongs to 3.6, not here.
     */
    @Query("SELECT * FROM $SUBSCRIPTION_TABLE ORDER BY id")
    fun observeAll(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM $SUBSCRIPTION_TABLE WHERE id = :id")
    suspend fun getById(id: Int): SubscriptionEntity?

    /**
     * The cache is a snapshot of the whole list, never a merge: the API sends every subscription
     * in one response, so a row missing from a fresh fetch has been deleted on the server and
     * must not survive as a stale local row.
     */
    @Transaction
    suspend fun replaceAll(subscriptions: List<SubscriptionEntity>) {
        deleteAll()
        insertAll(subscriptions)
    }

    @Query("DELETE FROM $SUBSCRIPTION_TABLE")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subscriptions: List<SubscriptionEntity>)
}
