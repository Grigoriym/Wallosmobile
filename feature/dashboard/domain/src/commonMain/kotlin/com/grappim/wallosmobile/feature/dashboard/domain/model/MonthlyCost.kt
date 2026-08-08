package com.grappim.wallosmobile.feature.dashboard.domain.model

/** This month's spend (`docs/WALLOS_API.md` §3.5), trimmed to what the dashboard card renders. */
data class MonthlyCost(val title: String, val amount: Double, val currencySymbol: String)
