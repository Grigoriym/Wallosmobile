package com.grappim.wallosmobile.feature.settings.ui.customheaders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.wallosmobile.core.storage.customheaders.CustomHeader
import com.grappim.wallosmobile.core.storage.customheaders.CustomHeaderError
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.custom_headers_add
import com.grappim.wallosmobile.strings.generated.resources.custom_headers_description
import com.grappim.wallosmobile.strings.generated.resources.custom_headers_error_duplicate_name
import com.grappim.wallosmobile.strings.generated.resources.custom_headers_error_invalid_name
import com.grappim.wallosmobile.strings.generated.resources.custom_headers_error_invalid_value
import com.grappim.wallosmobile.strings.generated.resources.custom_headers_error_missing_name
import com.grappim.wallosmobile.strings.generated.resources.custom_headers_error_reserved_name
import com.grappim.wallosmobile.strings.generated.resources.custom_headers_name_label
import com.grappim.wallosmobile.strings.generated.resources.custom_headers_name_placeholder
import com.grappim.wallosmobile.strings.generated.resources.custom_headers_remove
import com.grappim.wallosmobile.strings.generated.resources.custom_headers_save
import com.grappim.wallosmobile.strings.generated.resources.custom_headers_value_hint
import com.grappim.wallosmobile.strings.generated.resources.custom_headers_value_label
import com.grappim.wallosmobile.strings.generated.resources.settings_custom_headers
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.uikit.widgets.network.LocalIsOffline
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CustomHeadersScreen(
    viewModel: CustomHeadersViewModel = koinViewModel<CustomHeadersViewModel>(),
    onBackClick: () -> Unit
) {
    val topBarController = LocalTopBarConfig.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.settings_custom_headers),
                navigationIcon = NavigationIconConfig.Back(onBackClick = onBackClick)
            )
        )
    }

    CustomHeadersContent(uiState = uiState)
}

/**
 * The row fields match the login screen's custom-headers section (`feature:setup:ui`); a feature
 * `ui` module can't depend on another, and `uikit` has neither `:strings` nor `core:storage`.
 */
@Composable
private fun CustomHeadersContent(uiState: CustomHeadersUiState, modifier: Modifier = Modifier) {
    val enabled = !uiState.isSaving

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING)
    ) {
        Text(
            text = stringResource(RString.custom_headers_description),
            style = MaterialTheme.typography.bodyMedium
        )

        uiState.rows.forEachIndexed { index, row ->
            CustomHeaderRowFields(
                row = row,
                enabled = enabled,
                onNameChange = { uiState.onNameChange(index, it) },
                onValueChange = { uiState.onValueChange(index, it) },
                onRemove = { uiState.onRemove(index) }
            )
        }

        TextButton(onClick = uiState.onAdd, enabled = enabled) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Text(stringResource(RString.custom_headers_add))
        }

        if (uiState.isSaving) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = uiState.onSaveClick,
                enabled = !LocalIsOffline.current && uiState.isSaveEnabled
            ) {
                Text(stringResource(RString.custom_headers_save))
            }
        }
    }
}

@Composable
private fun CustomHeaderRowFields(
    row: CustomHeaderRow,
    enabled: Boolean,
    onNameChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    val error = row.error
    val isValueError = error == CustomHeaderError.InvalidValue

    Row(verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(FIELD_SPACING)
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = row.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(RString.custom_headers_name_label)) },
                placeholder = { Text(stringResource(RString.custom_headers_name_placeholder)) },
                isError = error != null && !isValueError,
                supportingText = error?.takeUnless { isValueError }?.let { { Text(stringResource(it.message())) } },
                singleLine = true,
                enabled = enabled
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = row.value,
                onValueChange = onValueChange,
                label = { Text(stringResource(RString.custom_headers_value_label)) },
                isError = isValueError,
                supportingText = {
                    Text(
                        stringResource(
                            error?.takeIf { isValueError }?.message() ?: RString.custom_headers_value_hint
                        )
                    )
                },
                singleLine = true,
                enabled = enabled
            )
        }
        IconButton(onClick = onRemove, enabled = enabled) {
            Icon(imageVector = Icons.Filled.Delete, contentDescription = stringResource(RString.custom_headers_remove))
        }
    }
}

private fun CustomHeaderError.message(): StringResource = when (this) {
    CustomHeaderError.MissingName -> RString.custom_headers_error_missing_name
    CustomHeaderError.InvalidName -> RString.custom_headers_error_invalid_name
    CustomHeaderError.ReservedName -> RString.custom_headers_error_reserved_name
    CustomHeaderError.DuplicateName -> RString.custom_headers_error_duplicate_name
    CustomHeaderError.InvalidValue -> RString.custom_headers_error_invalid_value
}

private val SCREEN_PADDING = 16.dp
private val ROW_SPACING = 8.dp
private val FIELD_SPACING = 4.dp

@PreviewWallosDarkLight
@Composable
private fun CustomHeadersContentEmptyPreview() = WallosMobilePreviewTheme {
    CustomHeadersContent(uiState = CustomHeadersUiState())
}

@PreviewWallosDarkLight
@Composable
private fun CustomHeadersContentPreview() = WallosMobilePreviewTheme {
    CustomHeadersContent(
        uiState = CustomHeadersUiState(
            rows = persistentListOf(
                CustomHeaderRow(name = "Authorization", value = "Basic cGFuZ29saW46c2VjcmV0"),
                CustomHeaderRow(name = "Host", value = "wallos.lan", error = CustomHeaderError.ReservedName)
            ),
            saved = persistentListOf(CustomHeader(name = "Authorization", value = "Basic cGFuZ29saW46c2VjcmV0"))
        )
    )
}
