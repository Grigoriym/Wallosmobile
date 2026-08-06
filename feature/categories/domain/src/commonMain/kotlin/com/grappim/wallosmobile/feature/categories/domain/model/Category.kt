package com.grappim.wallosmobile.feature.categories.domain.model

/**
 * A category the instance knows about. `order` stays off the model — nothing in Phase 3 reads it
 * (7.2's step note); a list screen that needs it is Phase 5's business.
 */
data class Category(val id: Int, val name: String, val inUse: Boolean)
