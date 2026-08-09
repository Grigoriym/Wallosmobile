package com.grappim.wallosmobile.composeapp.nav.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.grappim.wallosmobile.core.navigation.Navigator
import com.grappim.wallosmobile.feature.categories.ui.editor.CategoryEditorRoute
import com.grappim.wallosmobile.feature.categories.ui.editor.CategoryEditorScreen
import com.grappim.wallosmobile.feature.categories.ui.list.CategoriesRoute
import com.grappim.wallosmobile.feature.categories.ui.list.CategoriesScreen

/**
 * One route pair, not three (9.2's own note) — the list and the add/edit/delete form.
 */
fun EntryProviderScope<NavKey>.categoriesEntry(navigator: Navigator) {
    entry<CategoriesRoute> {
        CategoriesScreen(
            onCategoryClick = { id, name -> navigator.navigate(CategoryEditorRoute(categoryId = id, name = name)) }
        )
    }
    entry<CategoryEditorRoute> { route ->
        CategoryEditorScreen(
            onBackClick = { navigator.goBack() },
            categoryId = route.categoryId,
            name = route.name
        )
    }
}
