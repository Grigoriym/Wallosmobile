package com.grappim.wallosmobile.feature.setup.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Onboarding's only route. It is not a drawer destination and has no top bar — the shell isn't
 * around it at all; 1.11's startup branch shows either this or `AuthenticatedMainScreen`.
 */
@Serializable
data object LoginRoute : NavKey
