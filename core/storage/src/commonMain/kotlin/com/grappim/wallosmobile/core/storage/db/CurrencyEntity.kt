package com.grappim.wallosmobile.core.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

const val CURRENCY_TABLE = "currencies"

/**
 * One cached currency, mirroring the domain `Currency`. Cached so that refreshing the
 * subscription list doesn't have to re-read `get_currencies.php` to resolve a price symbol —
 * 2.3's second round trip per call.
 */
@Entity(tableName = CURRENCY_TABLE)
data class CurrencyEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val name: String,
    val symbol: String,
    val code: String
)
