package com.grappim.wallosmobile.composeapp.nav.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.grappim.wallosmobile.core.navigation.Navigator
import com.grappim.wallosmobile.feature.currencies.ui.editor.CurrencyEditorRoute
import com.grappim.wallosmobile.feature.currencies.ui.editor.CurrencyEditorScreen
import com.grappim.wallosmobile.feature.currencies.ui.list.CurrenciesRoute
import com.grappim.wallosmobile.feature.currencies.ui.list.CurrenciesScreen

/**
 * One route pair, not three (9.2's own note, mirrored by 9.6) — the list and the add/edit/delete
 * form.
 */
fun EntryProviderScope<NavKey>.currenciesEntry(navigator: Navigator) {
    entry<CurrenciesRoute> {
        CurrenciesScreen(
            onCurrencyClick = { id, name, symbol, code, rate ->
                navigator.navigate(
                    CurrencyEditorRoute(currencyId = id, name = name, symbol = symbol, code = code, rate = rate)
                )
            }
        )
    }
    entry<CurrencyEditorRoute> { route ->
        CurrencyEditorScreen(
            onBackClick = { navigator.goBack() },
            currencyId = route.currencyId,
            name = route.name,
            symbol = route.symbol,
            code = route.code,
            rate = route.rate
        )
    }
}
