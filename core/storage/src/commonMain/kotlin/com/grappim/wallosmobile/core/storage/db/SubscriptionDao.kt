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

    /**
     * The detail screen's row. A `Flow` rather than 3.3's one-shot read because the row it shows
     * is the same row a list refresh rewrites underneath it (3.4) — nothing else in the app knows
     * to tell the screen. `null` while the cache has never seen this id.
     */
    @Query("SELECT * FROM $SUBSCRIPTION_TABLE WHERE id = :id")
    fun observeById(id: Int): Flow<SubscriptionEntity?>

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

    /** A single deleted row (7.5) — the write already told the server, this just catches the cache up. */
    @Query("DELETE FROM $SUBSCRIPTION_TABLE WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subscriptions: List<SubscriptionEntity>)
}
