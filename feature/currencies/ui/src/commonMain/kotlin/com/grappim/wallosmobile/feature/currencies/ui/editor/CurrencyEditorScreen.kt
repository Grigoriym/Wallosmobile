package com.grappim.wallosmobile.feature.currencies.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.currency_editor_code
import com.grappim.wallosmobile.strings.generated.resources.currency_editor_delete
import com.grappim.wallosmobile.strings.generated.resources.currency_editor_delete_cancel
import com.grappim.wallosmobile.strings.generated.resources.currency_editor_delete_confirm
import com.grappim.wallosmobile.strings.generated.resources.currency_editor_delete_confirm_message
import com.grappim.wallosmobile.strings.generated.resources.currency_editor_delete_confirm_title
import com.grappim.wallosmobile.strings.generated.resources.currency_editor_error_invalid
import com.grappim.wallosmobile.strings.generated.resources.currency_editor_name
import com.grappim.wallosmobile.strings.generated.resources.currency_editor_rate
import com.grappim.wallosmobile.strings.generated.resources.currency_editor_save
import com.grappim.wallosmobile.strings.generated.resources.currency_editor_symbol
import com.grappim.wallosmobile.strings.generated.resources.currency_editor_title
import com.grappim.wallosmobile.strings.generated.resources.currency_editor_title_edit
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.uikit.widgets.network.LocalIsOffline
import com.grappim.wallosmobile.uikit.widgets.topappbar.LocalTopBarConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.NavigationIconConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarActionVectorButton
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarConfig
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.ObserveAsEvents
import com.grappim.wallosmobile.utils.ui.asString
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CurrencyEditorScreen(
    onBackClick: () -> Unit,
    currencyId: Int? = null,
    name: String = "",
    symbol: String = "",
    code: String = "",
    rate: Double = DEFAULT_RATE,
    viewModel: CurrencyEditorViewModel = koinViewModel {
        parametersOf(currencyId, name, symbol, code, rate)
    }
) {
    val topBarController = LocalTopBarConfig.current
    val uiState by viewModel.uiState.collectAsState()
    val deleteContentDescription = stringResource(RString.currency_editor_delete)

    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(
                    if (currencyId != null) RString.currency_editor_title_edit else RString.currency_editor_title
                ),
                navigationIcon = NavigationIconConfig.Back(onBackClick),
                actions = if (currencyId != null) {
                    persistentListOf(
                        TopBarActionVectorButton(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = deleteContentDescription,
                            onClick = uiState.onDeleteClick
                        )
                    )
                } else {
                    persistentListOf()
                }
            )
        )
    }

    ObserveAsEvents(flow = viewModel.saved) { onBackClick() }
    ObserveAsEvents(flow = viewModel.deleted) { onBackClick() }

    CurrencyEditorContent(uiState = uiState)
}

@Composable
private fun CurrencyEditorContent(uiState: CurrencyEditorUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(FIELD_SPACING)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.name,
            onValueChange = uiState.onNameChange,
            label = { Text(stringResource(RString.currency_editor_name)) },
            singleLine = true
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.symbol,
            onValueChange = uiState.onSymbolChange,
            label = { Text(stringResource(RString.currency_editor_symbol)) },
            singleLine = true
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.code,
            onValueChange = uiState.onCodeChange,
            label = { Text(stringResource(RString.currency_editor_code)) },
            singleLine = true
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.rate,
            onValueChange = uiState.onRateChange,
            label = { Text(stringResource(RString.currency_editor_rate)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        if (uiState.error.isNotEmpty()) {
            Text(
                text = uiState.error.asString(),
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
                enabled = !LocalIsOffline.current
            ) {
                Text(stringResource(RString.currency_editor_save))
            }
        }
    }

    if (uiState.isDeleteDialogOpen) {
        DeleteConfirmDialog(uiState = uiState)
    }
}

/**
 * Opening the dialog is not itself a write and stays enabled offline; the confirm button inside it
 * is the actual write (`CLAUDE.md`'s offline rule), same split as `CategoryEditorScreen`.
 */
@Composable
private fun DeleteConfirmDialog(uiState: CurrencyEditorUiState, modifier: Modifier = Modifier) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = uiState.onDeleteDialogDismiss,
        title = { Text(stringResource(RString.currency_editor_delete_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
                Text(stringResource(RString.currency_editor_delete_confirm_message))
                if (uiState.deleteError.isNotEmpty()) {
                    Text(text = uiState.deleteError.asString(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            if (uiState.isDeleting) {
                CircularProgressIndicator(modifier = Modifier.padding(DIALOG_PROGRESS_PADDING))
            } else {
                TextButton(onClick = uiState.onDeleteConfirm, enabled = !LocalIsOffline.current) {
                    Text(stringResource(RString.currency_editor_delete_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = uiState.onDeleteDialogDismiss) {
                Text(stringResource(RString.currency_editor_delete_cancel))
            }
        }
    )
}

private val SCREEN_PADDING = 16.dp
private val FIELD_SPACING = 16.dp
private val ROW_SPACING = 2.dp
private val DIALOG_PROGRESS_PADDING = 8.dp
private const val DEFAULT_RATE = 1.0

@PreviewWallosDarkLight
@Composable
private fun CurrencyEditorContentPreview() = WallosMobilePreviewTheme {
    CurrencyEditorContent(
        uiState = CurrencyEditorUiState(name = "US Dollar", symbol = "$", code = "USD", rate = "1.0")
    )
}

@PreviewWallosDarkLight
@Composable
private fun CurrencyEditorContentSavingPreview() = WallosMobilePreviewTheme {
    CurrencyEditorContent(
        uiState = CurrencyEditorUiState(
            name = "US Dollar",
            symbol = "$",
            code = "USD",
            rate = "1.0",
            isSaving = true
        )
    )
}

@PreviewWallosDarkLight
@Composable
private fun CurrencyEditorContentErrorPreview() = WallosMobilePreviewTheme {
    CurrencyEditorContent(
        uiState = CurrencyEditorUiState(error = NativeText.Resource(RString.currency_editor_error_invalid))
    )
}

@PreviewWallosDarkLight
@Composable
private fun CurrencyEditorContentDeleteConfirmPreview() = WallosMobilePreviewTheme {
    CurrencyEditorContent(
        uiState = CurrencyEditorUiState(name = "US Dollar", symbol = "$", code = "USD", isDeleteDialogOpen = true)
    )
}

@PreviewWallosDarkLight
@Composable
private fun CurrencyEditorContentDeleteFailedPreview() = WallosMobilePreviewTheme {
    CurrencyEditorContent(
        uiState = CurrencyEditorUiState(
            name = "US Dollar",
            symbol = "$",
            code = "USD",
            isDeleteDialogOpen = true,
            deleteError = NativeText.Simple("That item is still in use elsewhere.")
        )
    )
}
