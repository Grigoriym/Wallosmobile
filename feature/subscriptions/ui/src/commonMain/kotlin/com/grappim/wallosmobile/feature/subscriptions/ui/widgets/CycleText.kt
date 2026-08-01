package com.grappim.wallosmobile.feature.subscriptions.ui.widgets

import androidx.compose.runtime.Composable
import com.grappim.wallosmobile.feature.subscriptions.domain.model.BillingCycle
import com.grappim.wallosmobile.strings.RPlurals
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.subscription_cycle_days
import com.grappim.wallosmobile.strings.generated.resources.subscription_cycle_months
import com.grappim.wallosmobile.strings.generated.resources.subscription_cycle_one_time
import com.grappim.wallosmobile.strings.generated.resources.subscription_cycle_weeks
import com.grappim.wallosmobile.strings.generated.resources.subscription_cycle_years
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * "every 6 months" is a plural, so it can only be built where `pluralStringResource` can be
 * called — which is why 2.2 could not put it in `utils:formatter:datetime` and both screens carry
 * the [BillingCycle] rather than a string. `null` for an unknown cycle: the caller drops the line
 * instead of guessing a unit.
 */
@Composable
internal fun cycleText(cycle: BillingCycle?, frequency: Int): String? = when (cycle) {
    null -> null
    BillingCycle.ONE_TIME -> stringResource(RString.subscription_cycle_one_time)
    BillingCycle.DAYS -> pluralStringResource(RPlurals.subscription_cycle_days, frequency, frequency)
    BillingCycle.WEEKS -> pluralStringResource(RPlurals.subscription_cycle_weeks, frequency, frequency)
    BillingCycle.MONTHS -> pluralStringResource(RPlurals.subscription_cycle_months, frequency, frequency)
    BillingCycle.YEARS -> pluralStringResource(RPlurals.subscription_cycle_years, frequency, frequency)
}
