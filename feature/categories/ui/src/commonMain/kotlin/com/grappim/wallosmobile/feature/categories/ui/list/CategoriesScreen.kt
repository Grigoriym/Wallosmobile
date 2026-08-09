package com.grappim.wallosmobile.feature.categories.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.categories_empty
import com.grappim.wallosmobile.strings.generated.resources.categories_title
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_retry
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.uikit.widgets.topappbar.LocalTopBarConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.NavigationIconConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarConfig
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.asString
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CategoriesScreen(
    onCategoryClick: (id: Int, name: String) -> Unit,
    viewModel: CategoriesViewModel = koinViewModel()
) {
    val topBarController = LocalTopBarConfig.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.categories_title),
                navigationIcon = NavigationIconConfig.Menu
            )
        )
    }

    // No cache behind this list (`CategoriesViewModel`'s own note): every fresh composition of this
    // screen — the first open and a return trip from the editor after an add/edit/delete — is the
    // refresh, since a covered Nav3 entry is disposed and this restarts on the way back to it.
    LaunchedEffect(Unit) { uiState.onRetryClick() }

    CategoriesContent(uiState = uiState, onCategoryClick = onCategoryClick)
}

@Composable
private fun CategoriesContent(
    uiState: CategoriesUiState,
    onCategoryClick: (id: Int, name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        CategoriesList(uiState = uiState, onCategoryClick = onCategoryClick, modifier = Modifier.fillMaxSize())

        when {
            uiState.isLoading -> LoadingState()
            uiState.isFailed -> ErrorState(uiState = uiState)
            uiState.isEmpty -> EmptyState()
        }
    }
}

@Composable
private fun CategoriesList(
    uiState: CategoriesUiState,
    onCategoryClick: (id: Int, name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(SCREEN_PADDING)) {
        items(items = uiState.items, key = { it.id }) { item ->
            CategoryRow(item = item, onClick = { onCategoryClick(item.id, item.name) })
        }
    }
}

@Composable
private fun CategoryRow(item: CategoryUiItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = item.name,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = ROW_PADDING),
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(SCREEN_PADDING), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(RString.categories_empty),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorState(uiState: CategoriesUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(SCREEN_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = uiState.error.asString(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Button(onClick = uiState.onRetryClick) {
            Text(stringResource(RString.subscriptions_retry))
        }
    }
}

private val SCREEN_PADDING = 16.dp
private val ROW_PADDING = 12.dp

private val previewItems = persistentListOf(
    CategoryUiItem(id = 1, name = "Entertainment"),
    CategoryUiItem(id = 2, name = "Utilities"),
    CategoryUiItem(id = 3, name = "Sport & Fitness")
)

@PreviewWallosDarkLight
@Composable
private fun CategoriesContentPreview() = WallosMobilePreviewTheme {
    CategoriesContent(uiState = CategoriesUiState(items = previewItems), onCategoryClick = { _, _ -> })
}

@PreviewWallosDarkLight
@Composable
private fun CategoriesContentLoadingPreview() = WallosMobilePreviewTheme {
    CategoriesContent(uiState = CategoriesUiState(isLoading = true), onCategoryClick = { _, _ -> })
}

@PreviewWallosDarkLight
@Composable
private fun CategoriesContentEmptyPreview() = WallosMobilePreviewTheme {
    CategoriesContent(uiState = CategoriesUiState(), onCategoryClick = { _, _ -> })
}

@PreviewWallosDarkLight
@Composable
private fun CategoriesContentErrorPreview() = WallosMobilePreviewTheme {
    CategoriesContent(
        uiState = CategoriesUiState(
            error = NativeText.Simple("Couldn't reach that server. Check the URL and your connection.")
        ),
        onCategoryClick = { _, _ -> }
    )
}
