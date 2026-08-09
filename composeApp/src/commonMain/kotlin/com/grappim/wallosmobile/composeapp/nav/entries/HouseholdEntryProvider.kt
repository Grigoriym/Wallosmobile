package com.grappim.wallosmobile.composeapp.nav.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.grappim.wallosmobile.core.navigation.Navigator
import com.grappim.wallosmobile.feature.household.ui.editor.HouseholdMemberEditorRoute
import com.grappim.wallosmobile.feature.household.ui.editor.HouseholdMemberEditorScreen
import com.grappim.wallosmobile.feature.household.ui.list.HouseholdRoute
import com.grappim.wallosmobile.feature.household.ui.list.HouseholdScreen

/**
 * One route pair, not three (9.2's own note, mirrored by 9.3) — the list and the add/edit/delete
 * form.
 */
fun EntryProviderScope<NavKey>.householdEntry(navigator: Navigator) {
    entry<HouseholdRoute> {
        HouseholdScreen(
            onMemberClick = { id, name, email ->
                navigator.navigate(HouseholdMemberEditorRoute(memberId = id, name = name, email = email))
            }
        )
    }
    entry<HouseholdMemberEditorRoute> { route ->
        HouseholdMemberEditorScreen(
            onBackClick = { navigator.goBack() },
            memberId = route.memberId,
            name = route.name,
            email = route.email
        )
    }
}
