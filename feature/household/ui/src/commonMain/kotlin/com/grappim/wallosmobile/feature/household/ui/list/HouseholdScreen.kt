package com.grappim.wallosmobile.feature.household.ui.list

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
import com.grappim.wallosmobile.strings.generated.resources.household_empty
import com.grappim.wallosmobile.strings.generated.resources.household_title
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
fun HouseholdScreen(
    onMemberClick: (id: Int, name: String, email: String) -> Unit,
    viewModel: HouseholdViewModel = koinViewModel()
) {
    val topBarController = LocalTopBarConfig.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.household_title),
                navigationIcon = NavigationIconConfig.Menu
            )
        )
    }

    // No cache behind this list (`HouseholdViewModel`'s own note): every fresh composition of this
    // screen — the first open and a return trip from the editor after an add/edit/delete — is the
    // refresh, since a covered Nav3 entry is disposed and this restarts on the way back to it.
    LaunchedEffect(Unit) { uiState.onRetryClick() }

    HouseholdContent(uiState = uiState, onMemberClick = onMemberClick)
}

@Composable
private fun HouseholdContent(
    uiState: HouseholdUiState,
    onMemberClick: (id: Int, name: String, email: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        HouseholdList(uiState = uiState, onMemberClick = onMemberClick, modifier = Modifier.fillMaxSize())

        when {
            uiState.isLoading -> LoadingState()
            uiState.isFailed -> ErrorState(uiState = uiState)
            uiState.isEmpty -> EmptyState()
        }
    }
}

@Composable
private fun HouseholdList(
    uiState: HouseholdUiState,
    onMemberClick: (id: Int, name: String, email: String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(SCREEN_PADDING)) {
        items(items = uiState.items, key = { it.id }) { item ->
            HouseholdMemberRow(item = item, onClick = { onMemberClick(item.id, item.name, item.email) })
        }
    }
}

@Composable
private fun HouseholdMemberRow(item: HouseholdMemberUiItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = ROW_PADDING)
    ) {
        Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
        if (item.email.isNotEmpty()) {
            Text(
                text = item.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
            text = stringResource(RString.household_empty),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorState(uiState: HouseholdUiState, modifier: Modifier = Modifier) {
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
    HouseholdMemberUiItem(id = 1, name = "Gregory", email = "gregory@example.com"),
    HouseholdMemberUiItem(id = 2, name = "Sam", email = "")
)

@PreviewWallosDarkLight
@Composable
private fun HouseholdContentPreview() = WallosMobilePreviewTheme {
    HouseholdContent(uiState = HouseholdUiState(items = previewItems), onMemberClick = { _, _, _ -> })
}

@PreviewWallosDarkLight
@Composable
private fun HouseholdContentLoadingPreview() = WallosMobilePreviewTheme {
    HouseholdContent(uiState = HouseholdUiState(isLoading = true), onMemberClick = { _, _, _ -> })
}

@PreviewWallosDarkLight
@Composable
private fun HouseholdContentEmptyPreview() = WallosMobilePreviewTheme {
    HouseholdContent(uiState = HouseholdUiState(), onMemberClick = { _, _, _ -> })
}

@PreviewWallosDarkLight
@Composable
private fun HouseholdContentErrorPreview() = WallosMobilePreviewTheme {
    HouseholdContent(
        uiState = HouseholdUiState(
            error = NativeText.Simple("Couldn't reach that server. Check the URL and your connection.")
        ),
        onMemberClick = { _, _, _ -> }
    )
}
