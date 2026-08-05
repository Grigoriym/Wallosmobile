@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.grappim.wallosmobile.feature.subscriptions.ui.list.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grappim.wallosmobile.feature.subscriptions.ui.list.SubscriptionFilter
import com.grappim.wallosmobile.feature.subscriptions.ui.list.SubscriptionSort
import com.grappim.wallosmobile.feature.subscriptions.ui.list.SubscriptionStatusFilter
import com.grappim.wallosmobile.feature.subscriptions.ui.list.SubscriptionsFilterUiState
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_filter_category
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_filter_clear
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_filter_payer
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_filter_payment_method
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_filter_status
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_filter_status_active
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_filter_status_all
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_filter_status_inactive
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_filter_title
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_sort_category
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_sort_id
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_sort_inactive
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_sort_name
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_sort_next_payment
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_sort_payer
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_sort_payment_method
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_sort_price
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_sort_price_mixed
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_sort_title
import com.grappim.wallosmobile.uikit.WallosMobilePreviewTheme
import com.grappim.wallosmobile.uikit.utils.PreviewWallosDarkLight
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Filter and sort, both client-side over the cache (3.6). Every option here is built from the rows
 * already on screen, so the sheet needs no catalog endpoint and works with no network at all —
 * which is the whole reason 2.3 kept the two sides apart.
 *
 * The sheet is the wrapper only; [FilterSheetContent] is what a preview can render, since a
 * `ModalBottomSheet` puts its content in a window of its own.
 */
@Composable
internal fun SubscriptionsFilterSheet(uiState: SubscriptionsFilterUiState, modifier: Modifier = Modifier) {
    ModalBottomSheet(onDismissRequest = uiState.onDismiss, modifier = modifier) {
        FilterSheetContent(uiState = uiState)
    }
}

@Composable
private fun FilterSheetContent(uiState: SubscriptionsFilterUiState, modifier: Modifier = Modifier) {
    val filter = uiState.filter

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = SHEET_PADDING, end = SHEET_PADDING, bottom = SHEET_PADDING),
        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(RString.subscriptions_filter_title),
                style = MaterialTheme.typography.titleMedium
            )

            TextButton(onClick = uiState.onClear, enabled = filter.isActive) {
                Text(stringResource(RString.subscriptions_filter_clear))
            }
        }

        SortSection(
            sort = uiState.sort,
            spansCurrencies = uiState.spansCurrencies,
            onSortChange = uiState.onSortChange
        )

        StatusSection(
            status = filter.status,
            onStatusChange = { uiState.onFilterChange(filter.copy(status = it)) }
        )

        OptionSection(
            title = RString.subscriptions_filter_category,
            options = uiState.categories,
            selected = filter.categories,
            onToggle = { uiState.onFilterChange(filter.copy(categories = filter.categories.toggle(it))) }
        )

        OptionSection(
            title = RString.subscriptions_filter_payer,
            options = uiState.payers,
            selected = filter.payers,
            onToggle = { uiState.onFilterChange(filter.copy(payers = filter.payers.toggle(it))) }
        )

        OptionSection(
            title = RString.subscriptions_filter_payment_method,
            options = uiState.paymentMethods,
            selected = filter.paymentMethods,
            onToggle = { uiState.onFilterChange(filter.copy(paymentMethods = filter.paymentMethods.toggle(it))) }
        )
    }
}

/**
 * Single-select, and always one selected: there is no unsorted list, only a default order.
 *
 * [SubscriptionSort.PRICE] is the one option that can stop meaning anything (5.3): with the drawn
 * rows in more than one currency it orders raw amounts as though they shared a unit, so it is taken
 * off and the reason goes under the chips rather than being left to be inferred from a greyed one.
 * The note is written to read true either way, because a sort chosen while the rows *were*
 * comparable stays selected when a widened filter brings a second currency back in — that order is
 * as incomparable as the one the chip now refuses, and saying so is the honest half of it.
 */
@Composable
private fun SortSection(
    sort: SubscriptionSort,
    spansCurrencies: Boolean,
    onSortChange: (SubscriptionSort) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(TITLE_SPACING)) {
        Section(title = RString.subscriptions_sort_title) {
            SubscriptionSort.entries.forEach { entry ->
                FilterChip(
                    selected = entry == sort,
                    onClick = { onSortChange(entry) },
                    label = { Text(stringResource(entry.label)) },
                    enabled = !spansCurrencies || entry != SubscriptionSort.PRICE
                )
            }
        }

        if (spansCurrencies) {
            Text(
                text = stringResource(RString.subscriptions_sort_price_mixed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusSection(
    status: SubscriptionStatusFilter,
    onStatusChange: (SubscriptionStatusFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Section(title = RString.subscriptions_filter_status, modifier = modifier) {
        SubscriptionStatusFilter.entries.forEach { entry ->
            FilterChip(
                selected = entry == status,
                onClick = { onStatusChange(entry) },
                label = { Text(stringResource(entry.label)) }
            )
        }
    }
}

/**
 * Multi-select: an empty selection means every value, so unpicking the last chip widens back out.
 * A dimension with no options at all is left out rather than shown empty — before the first fetch
 * there are none, and an instance with one household member has no useful "Paid by" section.
 */
@Composable
private fun OptionSection(
    title: StringResource,
    options: ImmutableList<String>,
    selected: ImmutableSet<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (options.isNotEmpty()) {
        Section(title = title, modifier = modifier) {
            options.forEach { option ->
                FilterChip(
                    selected = option in selected,
                    onClick = { onToggle(option) },
                    label = { Text(option) }
                )
            }
        }
    }
}

@Composable
private fun Section(title: StringResource, modifier: Modifier = Modifier, chips: @Composable FlowRowScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TITLE_SPACING)
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING), content = chips)
    }
}

private val SubscriptionSort.label: StringResource
    get() = when (this) {
        SubscriptionSort.NEXT_PAYMENT -> RString.subscriptions_sort_next_payment
        SubscriptionSort.NAME -> RString.subscriptions_sort_name
        SubscriptionSort.PRICE -> RString.subscriptions_sort_price
        SubscriptionSort.ID -> RString.subscriptions_sort_id
        SubscriptionSort.PAYER -> RString.subscriptions_sort_payer
        SubscriptionSort.CATEGORY -> RString.subscriptions_sort_category
        SubscriptionSort.PAYMENT_METHOD -> RString.subscriptions_sort_payment_method
        SubscriptionSort.INACTIVE -> RString.subscriptions_sort_inactive
    }

private val SubscriptionStatusFilter.label: StringResource
    get() = when (this) {
        SubscriptionStatusFilter.ALL -> RString.subscriptions_filter_status_all
        SubscriptionStatusFilter.ACTIVE -> RString.subscriptions_filter_status_active
        SubscriptionStatusFilter.INACTIVE -> RString.subscriptions_filter_status_inactive
    }

private fun ImmutableSet<String>.toggle(value: String): ImmutableSet<String> = if (value in this) {
    minus(value).toPersistentSet()
} else {
    plus(value).toPersistentSet()
}

private val SHEET_PADDING = 16.dp
private val SECTION_SPACING = 16.dp
private val TITLE_SPACING = 4.dp
private val CHIP_SPACING = 8.dp

private val previewUiState = SubscriptionsFilterUiState(
    payers = persistentListOf("gregorz"),
    categories = persistentListOf("Entertainment", "Internet & Phone", "Utilities"),
    paymentMethods = persistentListOf("Direct Debit", "PayPal")
)

@PreviewWallosDarkLight
@Composable
private fun FilterSheetContentPreview() = WallosMobilePreviewTheme {
    FilterSheetContent(uiState = previewUiState)
}

@PreviewWallosDarkLight
@Composable
private fun FilterSheetContentFilteredPreview() = WallosMobilePreviewTheme {
    FilterSheetContent(
        uiState = previewUiState.copy(
            sort = SubscriptionSort.PRICE,
            filter = SubscriptionFilter(
                categories = persistentSetOf("Entertainment"),
                status = SubscriptionStatusFilter.ACTIVE
            )
        )
    )
}

/** 5.3: the same sheet over rows in two currencies — Price refused, and the reason under it. */
@PreviewWallosDarkLight
@Composable
private fun FilterSheetContentMixedCurrencyPreview() = WallosMobilePreviewTheme {
    FilterSheetContent(uiState = previewUiState.copy(spansCurrencies = true))
}
