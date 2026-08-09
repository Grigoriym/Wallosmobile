package com.grappim.wallosmobile.feature.currencies.domain.repo

import com.grappim.wallosmobile.feature.currencies.domain.model.Currency

/**
 * Currencies, straight off `get_currencies.php` / `set_currencies.php` (`docs/WALLOS_API.md`
 * §3.10) — reference data, so there is no cache to read instead: every call is a round trip.
 *
 * A [Result] failure is always a [com.grappim.wallosmobile.core.domain.WallosError], and a
 * [deleteCurrency] on the main currency or one still referenced by a subscription is
 * [com.grappim.wallosmobile.core.domain.WallosError.InUse].
 */
interface CurrenciesRepository {

    suspend fun getCurrencies(): Result<List<Currency>>

    /** @return the id the server assigned the new currency. */
    suspend fun addCurrency(name: String, symbol: String, code: String, rate: Double): Result<Int>

    suspend fun editCurrency(id: Int, name: String, symbol: String, code: String, rate: Double): Result<Unit>

    suspend fun deleteCurrency(id: Int): Result<Unit>
}
