package com.grappim.wallosmobile.core.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

const val PRICE_CONVERSION_TABLE = "price_conversion"

/**
 * How the cached prices were fetched (3.11) — a single row, replaced by every list refresh.
 *
 * It exists because the response cannot be read back for this: `get_subscriptions.php` overwrites
 * `price` when it converts and leaves `currency_id` naming the **source** currency, so a converted
 * row and an unconverted one are byte-identical on the wire. Whether the price in the
 * [SUBSCRIPTION_TABLE] beside this is in its own currency or in the instance's main one is only
 * knowable from the three facts here, and a cold offline start has no request to re-derive them
 * from.
 *
 * One row rather than a column on every subscription: this is a property of the *fetch*, and the
 * two tables are written inside the same refresh.
 *
 * @param mainCurrencyId the instance's `main_currency`, `null` on a version whose
 *   `get_currencies.php` doesn't send one — conversion is then off regardless of [isEnabled],
 *   since there is no target to convert into.
 * @param hasRates whether the instance's exchange rates have ever been fetched. Wallos gates
 *   conversion on a `last_exchange_update` row that no endpoint exposes, so this is read off the
 *   rate table instead: every rate is exactly `1` until the first update writes both.
 */
@Entity(tableName = PRICE_CONVERSION_TABLE)
data class PriceConversionEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int = SINGLETON_ID,
    val isEnabled: Boolean,
    val mainCurrencyId: Int?,
    val hasRates: Boolean
) {

    companion object {
        /** The only id this table ever holds — `REPLACE` on it is the update. */
        const val SINGLETON_ID = 0
    }
}
