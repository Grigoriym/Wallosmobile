package com.grappim.wallosmobile.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.profile_budget
import com.grappim.wallosmobile.strings.generated.resources.profile_period_budget
import com.grappim.wallosmobile.strings.generated.resources.profile_save
import com.grappim.wallosmobile.strings.generated.resources.profile_title
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_retry
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.uikit.widgets.network.LocalIsOffline
import com.grappim.wallosmobile.uikit.widgets.topappbar.LocalTopBarConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.NavigationIconConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarConfig
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.asString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = koinViewModel<ProfileViewModel>(), onBackClick: () -> Unit) {
    val topBarController = LocalTopBarConfig.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.profile_title),
                navigationIcon = NavigationIconConfig.Back(onBackClick = onBackClick)
            )
        )
    }

    // No cache behind this screen, same reasoning as `CategoriesScreen`/`CurrenciesScreen`: every
    // fresh composition — the first open and a return trip after process death disposes and
    // restarts this Nav3 entry — is the refresh.
    LaunchedEffect(Unit) { uiState.onRetryClick() }

    ProfileContent(uiState = uiState)
}

@Composable
private fun ProfileContent(uiState: ProfileUiState, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        ProfileForm(uiState = uiState, modifier = Modifier.fillMaxSize())

        when {
            uiState.isLoading -> LoadingState()
            uiState.isFailed -> ErrorState(uiState = uiState)
        }
    }
}

@Composable
private fun ProfileForm(uiState: ProfileUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(FIELD_SPACING)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.budget,
            onValueChange = uiState.onBudgetChange,
            label = { Text(stringResource(RString.profile_budget)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.periodBudget,
            onValueChange = uiState.onPeriodBudgetChange,
            label = { Text(stringResource(RString.profile_period_budget)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        if (uiState.saveError.isNotEmpty()) {
            Text(
                text = uiState.saveError.asString(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (uiState.isSaving) {
            CircularProgressIndicator(modifier = Modifier.padding(top = FIELD_SPACING))
        } else {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = uiState.onSaveClick,
                enabled = !LocalIsOffline.current && !uiState.isFailed
            ) {
                Text(stringResource(RString.profile_save))
            }
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
private fun ErrorState(uiState: ProfileUiState, modifier: Modifier = Modifier) {
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
private val FIELD_SPACING = 16.dp

@PreviewWallosDarkLight
@Composable
private fun ProfileContentPreview() = WallosMobilePreviewTheme {
    ProfileContent(uiState = ProfileUiState(budget = "50.0", periodBudget = "50.0"))
}

@PreviewWallosDarkLight
@Composable
private fun ProfileContentLoadingPreview() = WallosMobilePreviewTheme {
    ProfileContent(uiState = ProfileUiState(isLoading = true))
}

@PreviewWallosDarkLight
@Composable
private fun ProfileContentErrorPreview() = WallosMobilePreviewTheme {
    ProfileContent(
        uiState = ProfileUiState(
            error = NativeText.Simple("Couldn't reach that server. Check the URL and your connection.")
        )
    )
}

@PreviewWallosDarkLight
@Composable
private fun ProfileContentSavingPreview() = WallosMobilePreviewTheme {
    ProfileContent(uiState = ProfileUiState(budget = "50.0", periodBudget = "50.0", isSaving = true))
}
