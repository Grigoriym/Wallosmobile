package com.grappim.wallosmobile.feature.paymentmethods.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.feature.paymentmethods.domain.model.IconFile
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.payment_method_editor_delete
import com.grappim.wallosmobile.strings.generated.resources.payment_method_editor_delete_cancel
import com.grappim.wallosmobile.strings.generated.resources.payment_method_editor_delete_confirm
import com.grappim.wallosmobile.strings.generated.resources.payment_method_editor_delete_confirm_message
import com.grappim.wallosmobile.strings.generated.resources.payment_method_editor_delete_confirm_title
import com.grappim.wallosmobile.strings.generated.resources.payment_method_editor_enabled
import com.grappim.wallosmobile.strings.generated.resources.payment_method_editor_error_invalid
import com.grappim.wallosmobile.strings.generated.resources.payment_method_editor_icon_file_pick
import com.grappim.wallosmobile.strings.generated.resources.payment_method_editor_icon_file_picked
import com.grappim.wallosmobile.strings.generated.resources.payment_method_editor_icon_url
import com.grappim.wallosmobile.strings.generated.resources.payment_method_editor_name
import com.grappim.wallosmobile.strings.generated.resources.payment_method_editor_save
import com.grappim.wallosmobile.strings.generated.resources.payment_method_editor_title
import com.grappim.wallosmobile.strings.generated.resources.payment_method_editor_title_edit
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
fun PaymentMethodEditorScreen(
    onBackClick: () -> Unit,
    paymentMethodId: Int? = null,
    name: String = "",
    enabled: Boolean = true,
    viewModel: PaymentMethodEditorViewModel = koinViewModel { parametersOf(paymentMethodId, name, enabled) }
) {
    val topBarController = LocalTopBarConfig.current
    val uiState by viewModel.uiState.collectAsState()
    val deleteContentDescription = stringResource(RString.payment_method_editor_delete)

    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(
                    if (paymentMethodId != null) {
                        RString.payment_method_editor_title_edit
                    } else {
                        RString.payment_method_editor_title
                    }
                ),
                navigationIcon = NavigationIconConfig.Back(onBackClick),
                actions = if (paymentMethodId != null) {
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

    val pickIconFile = rememberIconFilePickerLauncher(onPick = uiState.onIconFilePick)

    PaymentMethodEditorContent(uiState = uiState, onPickIconFileClick = pickIconFile)
}

@Composable
private fun PaymentMethodEditorContent(
    uiState: PaymentMethodEditorUiState,
    modifier: Modifier = Modifier,
    onPickIconFileClick: () -> Unit = {}
) {
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
            label = { Text(stringResource(RString.payment_method_editor_name)) },
            singleLine = true
        )

        SwitchRow(
            label = stringResource(RString.payment_method_editor_enabled),
            checked = uiState.enabled,
            onCheckedChange = uiState.onEnabledChange
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.iconUrl,
            onValueChange = uiState.onIconUrlChange,
            label = { Text(stringResource(RString.payment_method_editor_icon_url)) },
            singleLine = true
        )

        IconFilePicker(iconFile = uiState.iconFile, onPickIconFileClick = onPickIconFileClick)

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
                Text(stringResource(RString.payment_method_editor_save))
            }
        }
    }

    if (uiState.isDeleteDialogOpen) {
        DeleteConfirmDialog(uiState = uiState)
    }
}

/** The multipart alternative to [PaymentMethodEditorUiState.iconUrl] (9.5) — device gallery, not a URL. */
@Composable
private fun IconFilePicker(iconFile: IconFile?, onPickIconFileClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        TextButton(onClick = onPickIconFileClick) {
            Text(stringResource(RString.payment_method_editor_icon_file_pick))
        }

        if (iconFile != null) {
            Text(
                text = stringResource(RString.payment_method_editor_icon_file_picked, iconFile.fileName),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * Opening the dialog is not itself a write and stays enabled offline; the confirm button inside it
 * is the actual write (`CLAUDE.md`'s offline rule), same split as `HouseholdMemberEditorScreen`.
 */
@Composable
private fun DeleteConfirmDialog(uiState: PaymentMethodEditorUiState, modifier: Modifier = Modifier) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = uiState.onDeleteDialogDismiss,
        title = { Text(stringResource(RString.payment_method_editor_delete_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
                Text(stringResource(RString.payment_method_editor_delete_confirm_message))
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
                    Text(stringResource(RString.payment_method_editor_delete_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = uiState.onDeleteDialogDismiss) {
                Text(stringResource(RString.payment_method_editor_delete_cancel))
            }
        }
    )
}

private val SCREEN_PADDING = 16.dp
private val FIELD_SPACING = 16.dp
private val ROW_SPACING = 2.dp
private val DIALOG_PROGRESS_PADDING = 8.dp

@PreviewWallosDarkLight
@Composable
private fun PaymentMethodEditorContentPreview() = WallosMobilePreviewTheme {
    PaymentMethodEditorContent(uiState = PaymentMethodEditorUiState(name = "PayPal", enabled = true))
}

@PreviewWallosDarkLight
@Composable
private fun PaymentMethodEditorContentSavingPreview() = WallosMobilePreviewTheme {
    PaymentMethodEditorContent(uiState = PaymentMethodEditorUiState(name = "PayPal", isSaving = true))
}

@PreviewWallosDarkLight
@Composable
private fun PaymentMethodEditorContentErrorPreview() = WallosMobilePreviewTheme {
    PaymentMethodEditorContent(
        uiState = PaymentMethodEditorUiState(error = NativeText.Resource(RString.payment_method_editor_error_invalid))
    )
}

@PreviewWallosDarkLight
@Composable
private fun PaymentMethodEditorContentDeleteConfirmPreview() = WallosMobilePreviewTheme {
    PaymentMethodEditorContent(
        uiState = PaymentMethodEditorUiState(name = "PayPal", isDeleteDialogOpen = true)
    )
}

@PreviewWallosDarkLight
@Composable
private fun PaymentMethodEditorContentDeleteFailedPreview() = WallosMobilePreviewTheme {
    PaymentMethodEditorContent(
        uiState = PaymentMethodEditorUiState(
            name = "PayPal",
            isDeleteDialogOpen = true,
            deleteError = NativeText.Simple("That item is still in use elsewhere.")
        )
    )
}
