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
import com.grappim.wallosmobile.feature.subscriptions.domain.model.EditSubscriptionParams
import com.grappim.wallosmobile.feature.subscriptions.domain.model.LogoFile
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
import kotlinx.coroutines.flow.first
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
 * Add- or edit- depending on [subscriptionId] (`null` from the FAB, set from the detail screen's
 * edit action, 7.7) — see [SubscriptionEditorRoute]. Exactly six constructor dependencies: the
 * four reference-data repositories a pick list needs plus [subscriptionId] and
 * [savedStateHandle], the ceiling this step's own checklist entry calls out. A seventh is a signal
 * to split, the way 3.4 split `SubscriptionsCache`, rather than to widen detekt's
 * `allowedConstructorParameters`.
 *
 * Categories, household members and payment methods have no cache to read (7.2–7.4), so each is a
 * one-shot round trip in [init] rather than an `observe*` — unlike currencies, which reuse
 * [SubscriptionsRepository.observeCurrencies] and so stay live for the life of the screen.
 */
@KoinViewModel
class SubscriptionEditorViewModel(
    @InjectedParam private val subscriptionId: Int?,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val householdRepository: HouseholdRepository,
    private val paymentMethodsRepository: PaymentMethodsRepository,
    @InjectedParam private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Read before [persistForm] can write the first entry under [KEY_FORM] and hide it. */
    private val hadStoredForm = savedStateHandle.get<String>(KEY_FORM) != null

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

        // No extra round trip (7.7): the row is already in the cache, put there by whichever
        // screen led here. A saved form always wins — it is either mid-edit or a restored add.
        if (subscriptionId != null && !hadStoredForm) {
            loadForEdit(subscriptionId)
        }
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

    private fun onLogoUrlChange(value: String) {
        _uiState.update { it.copy(logoUrl = value) }
    }

    /**
     * Not persisted (see [SavedFormState]) — a process death between picking and saving loses the
     * bytes, same as it loses everything else [restoreForm] cannot represent. The picked file
     * replaces whatever was picked before rather than merging with it; the server-side upload is
     * always a single file per request.
     */
    private fun onLogoFilePick(file: LogoFile) {
        _uiState.update { it.copy(logoFile = file) }
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
     *
     * [subscriptionId] picks `add` or `edit`; the fields sent are otherwise identical, since the
     * form always carries a real value for every one of them — an edit is a full resubmission, not
     * a diff, so nothing relies on `EditSubscriptionParams`' "omitted fields keep their current
     * value" behaviour (`WALLOS_API.md` §3.4).
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

            val result = subscriptionId?.let { id ->
                subscriptionsRepository.editSubscription(
                    id,
                    EditSubscriptionParams(
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
                        inactive = state.inactive,
                        logoUrl = state.logoUrl.trim().ifBlank { null },
                        logoFile = state.logoFile
                    )
                )
            } ?: subscriptionsRepository.addSubscription(
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
                    inactive = state.inactive,
                    logoUrl = state.logoUrl.trim().ifBlank { null },
                    logoFile = state.logoFile
                )
            ).map { }

            result.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                _saved.send(Unit)
            }.onFailure { throwable ->
                val action = if (subscriptionId != null) "Editing" else "Adding"
                logcat(priority = LogPriority.WARN, throwable = throwable) { "$action subscription failed" }
                _uiState.update { it.copy(isSaving = false, error = getErrorMessage(throwable)) }
            }
        }
    }

    /**
     * Pre-fills every field from the row the cache already holds — 5.2's `SavedFormState` shape
     * has no room for [WritableBillingCycle] or ids beside their picker text, so this writes
     * straight into [SubscriptionEditorUiState] rather than through [restoreForm]'s decode path.
     * An unmatched [WritableBillingCycle] (a `ONE_TIME` row, or a cycle this build doesn't know)
     * falls back to [WritableBillingCycle.MONTHS], the same default [restoreForm] uses for a
     * stored code it can't place — the server rejects `cycle=5` on write regardless, so the editor
     * cannot represent a one-time subscription's own cycle back to it.
     */
    private fun loadForEdit(id: Int) {
        viewModelScope.launch {
            val subscription = subscriptionsRepository.observeSubscription(id).first() ?: return@launch
            val cycle = subscription.cycle?.let { cycle ->
                WritableBillingCycle.entries.firstOrNull { it.code == cycle.code }
            } ?: WritableBillingCycle.MONTHS

            _uiState.update {
                it.copy(
                    name = subscription.name,
                    price = subscription.price.toString(),
                    currency = it.currency.copy(selectedId = subscription.currencyId),
                    cycle = cycle,
                    frequency = subscription.frequency.toString(),
                    nextPayment = subscription.nextPayment,
                    startDate = subscription.startDate,
                    category = it.category.copy(selectedId = subscription.categoryId),
                    payer = it.payer.copy(selectedId = subscription.payerUserId),
                    paymentMethod = it.paymentMethod.copy(selectedId = subscription.paymentMethodId),
                    notes = subscription.notes,
                    url = subscription.url,
                    notify = subscription.notify,
                    notifyDaysBefore = subscription.notifyDaysBefore?.toString().orEmpty(),
                    autoRenew = subscription.autoRenew,
                    inactive = !subscription.isActive
                )
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
                    _uiState.update {
                        it.copy(category = it.category.copy(options = options.toPersistentList(), isLoading = false))
                    }
                }
                .onFailure {
                    logcat(priority = LogPriority.WARN, throwable = it) { "Loading categories failed" }
                    _uiState.update { it.copy(category = it.category.copy(isLoading = false)) }
                }
        }
    }

    private fun loadPayers() {
        viewModelScope.launch {
            householdRepository.getMembers()
                .onSuccess { members ->
                    val options = members.map { PickerOption(id = it.id, label = it.name) }
                    _uiState.update {
                        it.copy(payer = it.payer.copy(options = options.toPersistentList(), isLoading = false))
                    }
                }
                .onFailure {
                    logcat(priority = LogPriority.WARN, throwable = it) { "Loading household members failed" }
                    _uiState.update { it.copy(payer = it.payer.copy(isLoading = false)) }
                }
        }
    }

    private fun loadPaymentMethods() {
        viewModelScope.launch {
            paymentMethodsRepository.getPaymentMethods()
                .onSuccess { methods ->
                    val options = methods.map { PickerOption(id = it.id, label = it.name) }
                    _uiState.update {
                        it.copy(
                            paymentMethod = it.paymentMethod.copy(
                                options = options.toPersistentList(),
                                isLoading = false
                            )
                        )
                    }
                }
                .onFailure {
                    logcat(priority = LogPriority.WARN, throwable = it) { "Loading payment methods failed" }
                    _uiState.update { it.copy(paymentMethod = it.paymentMethod.copy(isLoading = false)) }
                }
        }
    }

    /**
     * The one place the form is written down, for the same reason [SubscriptionsViewModel]'s
     * `persistCriteria` is: a save driven off the state itself cannot disagree with what is on
     * screen, where a write next to each of the seventeen setters above could miss one.
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
        category = EditorPickerUiState(selectedId = categoryId, isLoading = true, onSelect = ::onCategorySelect),
        payer = EditorPickerUiState(selectedId = payerId, isLoading = true, onSelect = ::onPayerSelect),
        paymentMethod = EditorPickerUiState(
            selectedId = paymentMethodId,
            isLoading = true,
            onSelect = ::onPaymentMethodSelect
        ),
        notes = notes,
        onNotesChange = ::onNotesChange,
        url = url,
        onUrlChange = ::onUrlChange,
        logoUrl = logoUrl,
        onLogoUrlChange = ::onLogoUrlChange,
        onLogoFilePick = ::onLogoFilePick,
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
        logoUrl = logoUrl,
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
    val logoUrl: String = "",
    val notify: Boolean = false,
    val notifyDaysBefore: String = "",
    val autoRenew: Boolean = true,
    val inactive: Boolean = false
)
