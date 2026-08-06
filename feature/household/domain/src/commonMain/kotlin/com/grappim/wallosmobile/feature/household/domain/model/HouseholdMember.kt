package com.grappim.wallosmobile.feature.household.domain.model

/** A household member the instance knows about. [email] is optional server-side. */
data class HouseholdMember(val id: Int, val name: String, val email: String, val inUse: Boolean)
