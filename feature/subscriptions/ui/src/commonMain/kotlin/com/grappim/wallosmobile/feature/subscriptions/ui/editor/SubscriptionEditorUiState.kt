package com.grappim.wallosmobile.feature.subscriptions.ui.editor

import com.grappim.wallosmobile.feature.subscriptions.domain.model.WritableBillingCycle
import com.grappim.wallosmobile.utils.ui.NativeText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate

/** One entry in a picker: a currency, category, household member or payment method. */
data class PickerOption(val id: Int, val label: String)

/**
 * One picker field, reused for currency/category/payer/payment method (7.6) rather than four
 * near-identical id+options+callback triples on [SubscriptionEditorUiState] itself. Currency is
 * the one of the four the server refuses to add without — nothing here enforces that, since it is
 * a fact about the *write*, not about how a picker renders; [SubscriptionEditorViewModel.onSaveClick]
 * is where a missing currency turns into [SubscriptionEditorUiState.error].
 */
data class EditorPickerUiState(
    val selectedId: Int? = null,
    val options: ImmutableList<PickerOption> = persistentListOf(),
    val onSelect: (Int?) -> Unit = {}
)

/**
 * The add/edit form (7.6, add-only until 7.7 gives it an id to pre-fill from). Every typed field
 * is already a `String`, even where the domain wants a `Double`/`Int`/`LocalDate` — parsing only
 * happens once, in [SubscriptionEditorViewModel.onSaveClick], so a field mid-edit ("12." on the
 * way to "12.5") never has to round-trip through a domain type that can't represent it.
 * [nextPayment]/[startDate] are the exception: both are picked from a date picker rather than
 * typed, so there is no intermediate text to carry and a value here is already a valid date or
 * absent.
 */
data class SubscriptionEditorUiState(
    val name: String = "",
    val onNameChange: (String) -> Unit = {},
    val price: String = "",
    val onPriceChange: (String) -> Unit = {},
    val currency: EditorPickerUiState = EditorPickerUiState(),
    val cycle: WritableBillingCycle = WritableBillingCycle.MONTHS,
    val onCycleChange: (WritableBillingCycle) -> Unit = {},
    val frequency: String = "1",
    val onFrequencyChange: (String) -> Unit = {},
    val nextPayment: LocalDate? = null,
    val onNextPaymentChange: (LocalDate) -> Unit = {},
    val startDate: LocalDate? = null,
    val onStartDateChange: (LocalDate?) -> Unit = {},
    val category: EditorPickerUiState = EditorPickerUiState(),
    val payer: EditorPickerUiState = EditorPickerUiState(),
    val paymentMethod: EditorPickerUiState = EditorPickerUiState(),
    val notes: String = "",
    val onNotesChange: (String) -> Unit = {},
    val url: String = "",
    val onUrlChange: (String) -> Unit = {},
    val notify: Boolean = false,
    val onNotifyChange: (Boolean) -> Unit = {},
    val notifyDaysBefore: String = "",
    val onNotifyDaysBeforeChange: (String) -> Unit = {},
    val autoRenew: Boolean = true,
    val onAutoRenewChange: (Boolean) -> Unit = {},
    val inactive: Boolean = false,
    val onInactiveChange: (Boolean) -> Unit = {},
    val isSaving: Boolean = false,
    val error: NativeText = NativeText.Empty,
    val onSaveClick: () -> Unit = {}
)
