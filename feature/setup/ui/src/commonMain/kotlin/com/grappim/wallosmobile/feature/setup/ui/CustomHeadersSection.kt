package com.grappim.wallosmobile.feature.setup.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.grappim.wallosmobile.strings.generated.resources.custom_headers_value_hint
import com.grappim.wallosmobile.strings.generated.resources.custom_headers_value_label
import com.grappim.wallosmobile.strings.generated.resources.login_custom_headers_toggle
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Collapsed by default: most instances have no proxy asking for anything, and the section is the
 * one part of the screen that only those users need (plan §4.5b).
 */
@Composable
internal fun CustomHeadersSection(uiState: LoginUiState, modifier: Modifier = Modifier) {
    val enabled = !uiState.isLoading

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING)
    ) {
        TextButton(
            onClick = { uiState.onCustomHeadersExpandedChange(!uiState.isCustomHeadersExpanded) },
            enabled = enabled
        ) {
            Text(stringResource(RString.login_custom_headers_toggle))
            Icon(
                imageVector = if (uiState.isCustomHeadersExpanded) {
                    Icons.Filled.KeyboardArrowUp
                } else {
                    Icons.Filled.KeyboardArrowDown
                },
                contentDescription = null
            )
        }

        if (uiState.isCustomHeadersExpanded) {
            Text(
                text = stringResource(RString.custom_headers_description),
                style = MaterialTheme.typography.bodySmall
            )

            uiState.customHeaders.forEachIndexed { index, row ->
                CustomHeaderRowFields(
                    row = row,
                    enabled = enabled,
                    onNameChange = { uiState.onCustomHeaderNameChange(index, it) },
                    onValueChange = { uiState.onCustomHeaderValueChange(index, it) },
                    onRemove = { uiState.onCustomHeaderRemove(index) }
                )
            }

            TextButton(onClick = uiState.onCustomHeaderAdd, enabled = enabled) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Text(stringResource(RString.custom_headers_add))
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

private val ROW_SPACING = 8.dp
private val FIELD_SPACING = 4.dp

@PreviewWallosDarkLight
@Composable
private fun CustomHeadersSectionCollapsedPreview() = WallosMobilePreviewTheme {
    CustomHeadersSection(uiState = LoginUiState())
}

@PreviewWallosDarkLight
@Composable
private fun CustomHeadersSectionExpandedPreview() = WallosMobilePreviewTheme {
    CustomHeadersSection(
        uiState = LoginUiState(
            isCustomHeadersExpanded = true,
            customHeaders = persistentListOf(
                CustomHeaderRow(name = "Authorization", value = "Basic cGFuZ29saW46c2VjcmV0"),
                CustomHeaderRow(name = "Host", value = "wallos.lan", error = CustomHeaderError.ReservedName)
            )
        )
    )
}
