package com.grappim.wallosmobile.feature.settings.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.about_build
import com.grappim.wallosmobile.strings.generated.resources.about_build_debug
import com.grappim.wallosmobile.strings.generated.resources.about_build_release
import com.grappim.wallosmobile.strings.generated.resources.about_privacy_policy
import com.grappim.wallosmobile.strings.generated.resources.about_project
import com.grappim.wallosmobile.strings.generated.resources.about_project_url
import com.grappim.wallosmobile.strings.generated.resources.about_report_issue
import com.grappim.wallosmobile.strings.generated.resources.about_report_issue_url
import com.grappim.wallosmobile.strings.generated.resources.about_version
import com.grappim.wallosmobile.strings.generated.resources.about_version_value
import com.grappim.wallosmobile.strings.generated.resources.settings_about
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.uikit.widgets.topappbar.LocalTopBarConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.NavigationIconConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarConfig
import com.grappim.wallosmobile.utils.ui.NativeText
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AboutScreen(onBackClick: () -> Unit, viewModel: AboutViewModel = koinViewModel<AboutViewModel>()) {
    val topBarController = LocalTopBarConfig.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.settings_about),
                navigationIcon = NavigationIconConfig.Back(onBackClick = onBackClick)
            )
        )
    }

    AboutContent(uiState = uiState)
}

@Composable
private fun AboutContent(uiState: AboutUiState, modifier: Modifier = Modifier) {
    // Opening a link is the platform's job and belongs to no ViewModel, so this is the one action
    // here and it is wired locally rather than through the UI state.
    val uriHandler = LocalUriHandler.current
    val projectUrl = stringResource(RString.about_project_url)
    val privacyPolicyUrl = stringResource(uiState.privacyPolicyLink)
    val reportIssueUrl = stringResource(RString.about_report_issue_url)

    // A fixed set of facts, so a `Column` rather than a `LazyColumn`.
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(ITEM_SPACING)
    ) {
        AboutField(
            title = RString.about_version,
            value = stringResource(RString.about_version_value, uiState.versionName, uiState.versionCode)
        )

        AboutField(
            title = RString.about_build,
            value = stringResource(if (uiState.isDebug) RString.about_build_debug else RString.about_build_release)
        )

        Button(
            onClick = { uriHandler.openUri(projectUrl) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(RString.about_project))
        }

        Button(
            onClick = { uriHandler.openUri(privacyPolicyUrl) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(RString.about_privacy_policy))
        }

        Button(
            onClick = { uriHandler.openUri(reportIssueUrl) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(RString.about_report_issue))
        }
    }
}

@Composable
private fun AboutField(title: StringResource, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val SCREEN_PADDING = 16.dp
private val ITEM_SPACING = 16.dp

@PreviewWallosDarkLight
@Composable
private fun AboutContentPreview() = WallosMobilePreviewTheme {
    AboutContent(uiState = AboutUiState(versionName = "0.1.0", versionCode = 1, isDebug = true))
}
