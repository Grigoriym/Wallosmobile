package com.grappim.wallosmobile.feature.profile.domain.model

/** `get_user.php`'s row (`docs/WALLOS_API.md` §3.9), trimmed to what M10's dashboard card needs. */
data class User(val id: Int, val budget: Double, val periodBudget: Double, val mainCurrencyId: Int)
