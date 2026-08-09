package com.grappim.wallosmobile.composeapp.nav.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.grappim.wallosmobile.core.navigation.Navigator
import com.grappim.wallosmobile.feature.paymentmethods.ui.editor.PaymentMethodEditorRoute
import com.grappim.wallosmobile.feature.paymentmethods.ui.editor.PaymentMethodEditorScreen
import com.grappim.wallosmobile.feature.paymentmethods.ui.list.PaymentMethodsRoute
import com.grappim.wallosmobile.feature.paymentmethods.ui.list.PaymentMethodsScreen

/**
 * One route pair, not three (9.2's own note, mirrored by 9.4) — the list and the add/edit/delete
 * form. `iconUrl` does not ride along on the edit path — `PaymentMethodEditorRoute`'s own doc
 * comment covers why.
 */
fun EntryProviderScope<NavKey>.paymentMethodsEntry(navigator: Navigator) {
    entry<PaymentMethodsRoute> {
        PaymentMethodsScreen(
            onPaymentMethodClick = { id, name, enabled ->
                navigator.navigate(PaymentMethodEditorRoute(paymentMethodId = id, name = name, enabled = enabled))
            }
        )
    }
    entry<PaymentMethodEditorRoute> { route ->
        PaymentMethodEditorScreen(
            onBackClick = { navigator.goBack() },
            paymentMethodId = route.paymentMethodId,
            name = route.name,
            enabled = route.enabled
        )
    }
}
