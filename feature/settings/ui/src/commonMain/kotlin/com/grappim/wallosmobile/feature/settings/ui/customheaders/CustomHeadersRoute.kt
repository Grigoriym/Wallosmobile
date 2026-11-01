package com.grappim.wallosmobile.feature.settings.ui.customheaders

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Lives with its screen (plan §5.3). Not a drawer destination — same shape as
 * [com.grappim.wallosmobile.feature.settings.ui.trustedcerts.TrustedCertsRoute].
 */
@Serializable
data object CustomHeadersRoute : NavKey
