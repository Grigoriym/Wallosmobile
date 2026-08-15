@file:OptIn(ExperimentalMaterial3Api::class)

package com.grappim.wallosmobile.feature.subscriptions.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.feature.subscriptions.domain.model.LogoFile
import com.grappim.wallosmobile.feature.subscriptions.domain.model.WritableBillingCycle
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_auto_renew
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_category
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_currency
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_cycle
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_cycle_days
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_cycle_months
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_cycle_weeks
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_cycle_years
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_date_cancel
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_date_confirm
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_frequency
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_inactive
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_logo_file_pick
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_logo_file_picked
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_logo_url
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_name
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_next_payment
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_notes
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_notify
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_notify_days_before
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_payer
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_payment_method
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_picker_none
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_price
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_save
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_start_date
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_title
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_title_edit
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_url
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import com.grappim.wallosmobile.uikit.widgets.network.LocalIsOffline
import com.grappim.wallosmobile.uikit.widgets.topappbar.LocalTopBarConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.NavigationIconConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarConfig
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.ObserveAsEvents
import com.grappim.wallosmobile.utils.ui.asString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Instant

@Composable
fun SubscriptionEditorScreen(
    onBackClick: () -> Unit,
    subscriptionId: Int? = null,
    viewModel: SubscriptionEditorViewModel = koinViewModel { parametersOf(subscriptionId) }
) {
    val topBarController = LocalTopBarConfig.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(
                    if (subscriptionId != null) {
                        RString.subscription_editor_title_edit
                    } else {
                        RString.subscription_editor_title
                    }
                ),
                navigationIcon = NavigationIconConfig.Back(onBackClick)
            )
        )
    }

    ObserveAsEvents(flow = viewModel.saved) { onBackClick() }

    val pickLogoFile = rememberLogoPickerLauncher(onPick = uiState.onLogoFilePick)

    SubscriptionEditorContent(uiState = uiState, onPickLogoFileClick = pickLogoFile)
}

@Composable
private fun SubscriptionEditorContent(
    uiState: SubscriptionEditorUiState,
    modifier: Modifier = Modifier,
    onPickLogoFileClick: () -> Unit = {}
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
            label = { Text(stringResource(RString.subscription_editor_name)) },
            singleLine = true
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.price,
            onValueChange = uiState.onPriceChange,
            label = { Text(stringResource(RString.subscription_editor_price)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        PickerField(label = RString.subscription_editor_currency, state = uiState.currency)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FIELD_SPACING)) {
            OutlinedTextField(
                modifier = Modifier.weight(FREQUENCY_WEIGHT),
                value = uiState.frequency,
                onValueChange = uiState.onFrequencyChange,
                label = { Text(stringResource(RString.subscription_editor_frequency)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            CyclePickerField(
                modifier = Modifier.weight(CYCLE_WEIGHT),
                cycle = uiState.cycle,
                onCycleChange = uiState.onCycleChange
            )
        }

        DateField(
            label = RString.subscription_editor_next_payment,
            date = uiState.nextPayment,
            onDateChange = { date -> date?.let(uiState.onNextPaymentChange) }
        )

        DateField(
            label = RString.subscription_editor_start_date,
            date = uiState.startDate,
            onDateChange = uiState.onStartDateChange,
            clearable = true
        )

        PickerField(label = RString.subscription_editor_category, state = uiState.category, clearable = true)

        PickerField(label = RString.subscription_editor_payer, state = uiState.payer, clearable = true)

        PickerField(
            label = RString.subscription_editor_payment_method,
            state = uiState.paymentMethod,
            clearable = true
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.notes,
            onValueChange = uiState.onNotesChange,
            label = { Text(stringResource(RString.subscription_editor_notes)) }
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.url,
            onValueChange = uiState.onUrlChange,
            label = { Text(stringResource(RString.subscription_editor_url)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.logoUrl,
            onValueChange = uiState.onLogoUrlChange,
            label = { Text(stringResource(RString.subscription_editor_logo_url)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )

        LogoFilePicker(logoFile = uiState.logoFile, onPickLogoFileClick = onPickLogoFileClick)

        SwitchRow(
            label = RString.subscription_editor_notify,
            checked = uiState.notify,
            onCheckedChange = uiState.onNotifyChange
        )

        if (uiState.notify) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.notifyDaysBefore,
                onValueChange = uiState.onNotifyDaysBeforeChange,
                label = { Text(stringResource(RString.subscription_editor_notify_days_before)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        SwitchRow(
            label = RString.subscription_editor_auto_renew,
            checked = uiState.autoRenew,
            onCheckedChange = uiState.onAutoRenewChange
        )

        SwitchRow(
            label = RString.subscription_editor_inactive,
            checked = uiState.inactive,
            onCheckedChange = uiState.onInactiveChange
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
                Text(stringResource(RString.subscription_editor_save))
            }
        }
    }
}

/** The multipart alternative to [SubscriptionEditorUiState.logoUrl] (7.9) — device gallery, not a URL. */
@Composable
private fun LogoFilePicker(logoFile: LogoFile?, onPickLogoFileClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        TextButton(onClick = onPickLogoFileClick) {
            Text(stringResource(RString.subscription_editor_logo_file_pick))
        }

        if (logoFile != null) {
            Text(
                text = stringResource(RString.subscription_editor_logo_file_picked, logoFile.fileName),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SwitchRow(
    label: StringResource,
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
        Text(text = stringResource(label), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * Currency/category/payer/payment method all share this — a read-only field that opens a menu
 * rather than the keyboard. Takes the whole [EditorPickerUiState] rather than its `selectedId` and
 * `options` as separate parameters, which is what keeps this under detekt's five-parameter limit
 * alongside [label], [modifier] and [clearable]. [clearable] is what a picker with no value at all
 * needs (category, payer, payment method): currency is the one field the server refuses to add
 * without (`AddSubscriptionParams`), so its call site leaves this `false`.
 */
@Composable
private fun PickerField(
    label: StringResource,
    state: EditorPickerUiState,
    modifier: Modifier = Modifier,
    clearable: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = isExpanded, onExpandedChange = { isExpanded = it }, modifier = modifier) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            value = state.options.labelOf(state.selectedId),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(label)) },
            trailingIcon = {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(PICKER_SPINNER_SIZE), strokeWidth = 2.dp)
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                }
            }
        )

        ExposedDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            if (clearable) {
                DropdownMenuItem(
                    text = { Text(stringResource(RString.subscription_editor_picker_none)) },
                    onClick = {
                        state.onSelect(null)
                        isExpanded = false
                    }
                )
            }

            state.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        state.onSelect(option.id)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CyclePickerField(
    cycle: WritableBillingCycle,
    onCycleChange: (WritableBillingCycle) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = WritableBillingCycle.entries.map { PickerOption(id = it.code, label = stringResource(it.label)) }
    val state = EditorPickerUiState(
        selectedId = cycle.code,
        options = options.toImmutableList(),
        onSelect = { id -> onCycleChange(WritableBillingCycle.entries.first { it.code == id }) }
    )

    PickerField(modifier = modifier, label = RString.subscription_editor_cycle, state = state)
}

@Composable
private fun DateField(
    label: StringResource,
    date: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    clearable: Boolean = false
) {
    var isOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Box {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = date?.let { it.format(LocalDate.Formats.ISO) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(label)) }
            )

            // A `readOnly` field still routes taps into text selection rather than into a click
            // listener of its own, so this transparent layer sitting over it is what actually
            // opens the dialog — the same reason `login_password_show` reaches for a `TextButton`
            // instead of fighting a `TextField`'s own gesture handling.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isOpen = true }
            )
        }

        if (clearable && date != null) {
            TextButton(onClick = { onDateChange(null) }) {
                Text(stringResource(RString.subscription_editor_picker_none))
            }
        }
    }

    if (isOpen) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date?.toEpochMillisUtc())
        DatePickerDialog(
            onDismissRequest = { isOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis -> onDateChange(millis.toUtcLocalDate()) }
                        isOpen = false
                    }
                ) {
                    Text(stringResource(RString.subscription_editor_date_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { isOpen = false }) {
                    Text(stringResource(RString.subscription_editor_date_cancel))
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

private fun LocalDate.toEpochMillisUtc(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun Long.toUtcLocalDate(): LocalDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

private fun ImmutableList<PickerOption>.labelOf(id: Int?): String =
    id?.let { selected -> firstOrNull { it.id == selected }?.label }.orEmpty()

private val WritableBillingCycle.label: StringResource
    get() = when (this) {
        WritableBillingCycle.DAYS -> RString.subscription_editor_cycle_days
        WritableBillingCycle.WEEKS -> RString.subscription_editor_cycle_weeks
        WritableBillingCycle.MONTHS -> RString.subscription_editor_cycle_months
        WritableBillingCycle.YEARS -> RString.subscription_editor_cycle_years
    }

private val SCREEN_PADDING = 16.dp
private val FIELD_SPACING = 16.dp
private val PICKER_SPINNER_SIZE = 24.dp
private const val FREQUENCY_WEIGHT = 1f
private const val CYCLE_WEIGHT = 2f

private val previewUiState = SubscriptionEditorUiState(
    name = "Disney+",
    price = "8.99",
    currency = EditorPickerUiState(
        selectedId = 1,
        options = persistentListOf(PickerOption(1, "EUR (€)"), PickerOption(2, "USD ($)"))
    ),
    frequency = "1",
    category = EditorPickerUiState(options = persistentListOf(PickerOption(1, "Entertainment"))),
    payer = EditorPickerUiState(options = persistentListOf(PickerOption(1, "gregorz"))),
    paymentMethod = EditorPickerUiState(options = persistentListOf(PickerOption(1, "Direct Debit")))
)

@PreviewWallosDarkLight
@Composable
private fun SubscriptionEditorContentPreview() = WallosMobilePreviewTheme {
    SubscriptionEditorContent(uiState = previewUiState)
}

@PreviewWallosDarkLight
@Composable
private fun SubscriptionEditorContentSavingPreview() = WallosMobilePreviewTheme {
    SubscriptionEditorContent(uiState = previewUiState.copy(isSaving = true))
}

@PreviewWallosDarkLight
@Composable
private fun SubscriptionEditorContentPickersLoadingPreview() = WallosMobilePreviewTheme {
    SubscriptionEditorContent(
        uiState = previewUiState.copy(
            category = EditorPickerUiState(isLoading = true),
            payer = EditorPickerUiState(isLoading = true),
            paymentMethod = EditorPickerUiState(isLoading = true)
        )
    )
}

@PreviewWallosDarkLight
@Composable
private fun SubscriptionEditorContentErrorPreview() = WallosMobilePreviewTheme {
    SubscriptionEditorContent(
        uiState = previewUiState.copy(
            error = NativeText.Simple("Fill in the name, price, currency and next payment date.")
        )
    )
}
