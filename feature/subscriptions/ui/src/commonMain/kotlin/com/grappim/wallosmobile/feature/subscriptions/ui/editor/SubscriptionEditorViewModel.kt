package com.grappim.wallosmobile.feature.subscriptions.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.feature.categories.domain.repo.CategoriesRepository
import com.grappim.wallosmobile.feature.household.domain.repo.HouseholdRepository
import com.grappim.wallosmobile.feature.paymentmethods.domain.repo.PaymentMethodsRepository
import com.grappim.wallosmobile.feature.subscriptions.domain.model.AddSubscriptionParams
import com.grappim.wallosmobile.feature.subscriptions.domain.model.WritableBillingCycle
import com.grappim.wallosmobile.feature.subscriptions.domain.repo.SubscriptionsRepository
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscription_editor_error_invalid
import com.grappim.wallosmobile.utils.ui.NativeText
import com.grappim.wallosmobile.utils.ui.getErrorMessage
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/**
 * Add-only until 7.7 gives this an id to pre-fill from — see [SubscriptionEditorRoute]. Exactly
 * five constructor dependencies: the four reference-data repositories a pick list needs plus
 * [savedStateHandle], the ceiling this step's own checklist entry calls out. A sixth is a signal
 * to split, the way 3.4 split `SubscriptionsCache`, rather than to widen detekt's
 * `allowedConstructorParameters`.
 *
 * Categories, household members and payment methods have no cache to read (7.2–7.4), so each is a
 * one-shot round trip in [init] rather than an `observe*` — unlike currencies, which reuse
 * [SubscriptionsRepository.observeCurrencies] and so stay live for the life of the screen.
 */
@KoinViewModel
class SubscriptionEditorViewModel(
    private val subscriptionsRepository: SubscriptionsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val householdRepository: HouseholdRepository,
    private val paymentMethodsRepository: PaymentMethodsRepository,
    @InjectedParam private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(restoreForm().toUiState())
    val uiState: StateFlow<SubscriptionEditorUiState> = _uiState.asStateFlow()

    /** One-off, per plan: a successful save is a signal the screen acts on, never UI state. */
    private val _saved = Channel<Unit>()
    val saved = _saved.receiveAsFlow()

    init {
        persistForm()
        observeCurrencies()
        loadCategories()
        loadPayers()
        loadPaymentMethods()
    }

    private fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    private fun onPriceChange(value: String) {
        _uiState.update { it.copy(price = value) }
    }

    private fun onCurrencySelect(id: Int?) {
        _uiState.update { it.copy(currency = it.currency.copy(selectedId = id)) }
    }

    private fun onCycleChange(cycle: WritableBillingCycle) {
        _uiState.update { it.copy(cycle = cycle) }
    }

    private fun onFrequencyChange(value: String) {
        _uiState.update { it.copy(frequency = value) }
    }

    private fun onNextPaymentChange(date: LocalDate) {
        _uiState.update { it.copy(nextPayment = date) }
    }

    private fun onStartDateChange(date: LocalDate?) {
        _uiState.update { it.copy(startDate = date) }
    }

    private fun onCategorySelect(id: Int?) {
        _uiState.update { it.copy(category = it.category.copy(selectedId = id)) }
    }

    private fun onPayerSelect(id: Int?) {
        _uiState.update { it.copy(payer = it.payer.copy(selectedId = id)) }
    }

    private fun onPaymentMethodSelect(id: Int?) {
        _uiState.update { it.copy(paymentMethod = it.paymentMethod.copy(selectedId = id)) }
    }

    private fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    private fun onUrlChange(value: String) {
        _uiState.update { it.copy(url = value) }
    }

    private fun onNotifyChange(value: Boolean) {
        _uiState.update { it.copy(notify = value) }
    }

    private fun onNotifyDaysBeforeChange(value: String) {
        _uiState.update { it.copy(notifyDaysBefore = value) }
    }

    private fun onAutoRenewChange(value: Boolean) {
        _uiState.update { it.copy(autoRenew = value) }
    }

    private fun onInactiveChange(value: Boolean) {
        _uiState.update { it.copy(inactive = value) }
    }

    /**
     * The one place every typed field gets parsed (the state class note explains why nothing
     * parses earlier). A blank required field never reaches [AddSubscriptionParams] — it becomes
     * [SubscriptionEditorUiState.error] instead, so the server never sees a request that names no
     * currency or no date.
     */
    private fun onSaveClick() {
        val state = _uiState.value
        val name = state.name.trim()
        val price = state.price.toDoubleOrNull()
        val frequency = state.frequency.toIntOrNull()?.takeIf { it > 0 }
        val currencyId = state.currency.selectedId
        val nextPayment = state.nextPayment

        if (name.isBlank() || price == null || frequency == null || currencyId == null || nextPayment == null) {
            _uiState.update { it.copy(error = NativeText.Resource(RString.subscription_editor_error_invalid)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = NativeText.Empty) }

            subscriptionsRepository.addSubscription(
                AddSubscriptionParams(
                    name = name,
                    price = price,
                    currencyId = currencyId,
                    cycle = state.cycle,
                    frequency = frequency,
                    nextPayment = nextPayment,
                    startDate = state.startDate,
                    autoRenew = state.autoRenew,
                    paymentMethodId = state.paymentMethod.selectedId,
                    payerUserId = state.payer.selectedId,
                    categoryId = state.category.selectedId,
                    notes = state.notes.trim().ifBlank { null },
                    url = state.url.trim().ifBlank { null },
                    notify = state.notify,
                    notifyDaysBefore = if (state.notify) state.notifyDaysBefore.toIntOrNull() else null,
                    inactive = state.inactive
                )
            ).onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                _saved.send(Unit)
            }.onFailure { throwable ->
                logcat(priority = LogPriority.WARN, throwable = throwable) { "Adding subscription failed" }
                _uiState.update { it.copy(isSaving = false, error = getErrorMessage(throwable)) }
            }
        }
    }

    /** Live for the life of the screen, unlike the three below — the cache 3.11 already built. */
    private fun observeCurrencies() {
        subscriptionsRepository.observeCurrencies()
            .onEach { currencies ->
                val options = currencies.map { PickerOption(id = it.id, label = "${it.code} (${it.symbol})") }
                _uiState.update { it.copy(currency = it.currency.copy(options = options.toPersistentList())) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoriesRepository.getCategories()
                .onSuccess { categories ->
                    val options = categories.map { PickerOption(id = it.id, label = it.name) }
                    _uiState.update { it.copy(category = it.category.copy(options = options.toPersistentList())) }
                }
                .onFailure { logcat(priority = LogPriority.WARN, throwable = it) { "Loading categories failed" } }
        }
    }

    private fun loadPayers() {
        viewModelScope.launch {
            householdRepository.getMembers()
                .onSuccess { members ->
                    val options = members.map { PickerOption(id = it.id, label = it.name) }
                    _uiState.update { it.copy(payer = it.payer.copy(options = options.toPersistentList())) }
                }
                .onFailure {
                    logcat(priority = LogPriority.WARN, throwable = it) { "Loading household members failed" }
                }
        }
    }

    private fun loadPaymentMethods() {
        viewModelScope.launch {
            paymentMethodsRepository.getPaymentMethods()
                .onSuccess { methods ->
                    val options = methods.map { PickerOption(id = it.id, label = it.name) }
                    _uiState.update {
                        it.copy(paymentMethod = it.paymentMethod.copy(options = options.toPersistentList()))
                    }
                }
                .onFailure { logcat(priority = LogPriority.WARN, throwable = it) { "Loading payment methods failed" } }
        }
    }

    /**
     * The one place the form is written down, for the same reason [SubscriptionsViewModel]'s
     * `persistCriteria` is: a save driven off the state itself cannot disagree with what is on
     * screen, where a write next to each of the sixteen setters above could miss one.
     */
    private fun persistForm() {
        _uiState.onEach { savedStateHandle[KEY_FORM] = Json.encodeToString(it.toSaved()) }
            .launchIn(viewModelScope)
    }

    private fun restoreForm(): SavedFormState {
        val stored = savedStateHandle.get<String>(KEY_FORM) ?: return SavedFormState()
        return try {
            Json.decodeFromString<SavedFormState>(stored)
        } catch (e: IllegalArgumentException) {
            // Pre-v1 that state is disposable (5.2's precedent) — discarding it beats crashing on it.
            logcat(priority = LogPriority.WARN, throwable = e) { "Stored form was unreadable" }
            SavedFormState()
        }
    }

    private fun SavedFormState.toUiState() = SubscriptionEditorUiState(
        name = name,
        onNameChange = ::onNameChange,
        price = price,
        onPriceChange = ::onPriceChange,
        currency = EditorPickerUiState(selectedId = currencyId, onSelect = ::onCurrencySelect),
        cycle = WritableBillingCycle.entries.firstOrNull { it.code == cycleCode } ?: WritableBillingCycle.MONTHS,
        onCycleChange = ::onCycleChange,
        frequency = frequency,
        onFrequencyChange = ::onFrequencyChange,
        nextPayment = nextPayment.toIsoDateOrNull(),
        onNextPaymentChange = ::onNextPaymentChange,
        startDate = startDate.toIsoDateOrNull(),
        onStartDateChange = ::onStartDateChange,
        category = EditorPickerUiState(selectedId = categoryId, onSelect = ::onCategorySelect),
        payer = EditorPickerUiState(selectedId = payerId, onSelect = ::onPayerSelect),
        paymentMethod = EditorPickerUiState(selectedId = paymentMethodId, onSelect = ::onPaymentMethodSelect),
        notes = notes,
        onNotesChange = ::onNotesChange,
        url = url,
        onUrlChange = ::onUrlChange,
        notify = notify,
        onNotifyChange = ::onNotifyChange,
        notifyDaysBefore = notifyDaysBefore,
        onNotifyDaysBeforeChange = ::onNotifyDaysBeforeChange,
        autoRenew = autoRenew,
        onAutoRenewChange = ::onAutoRenewChange,
        inactive = inactive,
        onInactiveChange = ::onInactiveChange,
        onSaveClick = ::onSaveClick
    )

    private fun SubscriptionEditorUiState.toSaved() = SavedFormState(
        name = name,
        price = price,
        currencyId = currency.selectedId,
        cycleCode = cycle.code,
        frequency = frequency,
        nextPayment = nextPayment?.toIsoString().orEmpty(),
        startDate = startDate?.toIsoString().orEmpty(),
        categoryId = category.selectedId,
        payerId = payer.selectedId,
        paymentMethodId = paymentMethod.selectedId,
        notes = notes,
        url = url,
        notify = notify,
        notifyDaysBefore = notifyDaysBefore,
        autoRenew = autoRenew,
        inactive = inactive
    )

    /** A stored date this build cannot parse reads as unset, the same answer 2.1 gives the wire. */
    private fun String.toIsoDateOrNull(): LocalDate? = if (isBlank()) {
        null
    } else {
        try {
            LocalDate.parse(this, LocalDate.Formats.ISO)
        } catch (e: IllegalArgumentException) {
            logcat(priority = LogPriority.WARN, throwable = e) { "Stored date was unreadable: $this" }
            null
        }
    }

    private fun LocalDate.toIsoString(): String = format(LocalDate.Formats.ISO)

    private companion object {
        const val KEY_FORM = "subscription_editor_form"
    }
}

/**
 * 5.2's shape again: one JSON string under one key, since a [SavedStateHandle] value has to be
 * Bundle-safe on Android and this is also the only form a host test can read back. Plain `String`s
 * and `Int?`s throughout rather than [WritableBillingCycle] or a picked `LocalDate` — neither has a
 * kotlinx serializer available in this module (the domain enum's own module carries no
 * serialization plugin), so [SubscriptionEditorViewModel.toUiState] and
 * [SubscriptionEditorViewModel.toSaved] are what translate between the two shapes.
 */
@Serializable
private data class SavedFormState(
    val name: String = "",
    val price: String = "",
    val currencyId: Int? = null,
    val cycleCode: Int = WritableBillingCycle.MONTHS.code,
    val frequency: String = "1",
    val nextPayment: String = "",
    val startDate: String = "",
    val categoryId: Int? = null,
    val payerId: Int? = null,
    val paymentMethodId: Int? = null,
    val notes: String = "",
    val url: String = "",
    val notify: Boolean = false,
    val notifyDaysBefore: String = "",
    val autoRenew: Boolean = true,
    val inactive: Boolean = false
)
