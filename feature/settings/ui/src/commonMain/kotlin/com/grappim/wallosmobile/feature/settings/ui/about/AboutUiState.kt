package com.grappim.wallosmobile.feature.settings.ui.about

import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.privacy_policy_url
import org.jetbrains.compose.resources.StringResource

/**
 * Build-time facts, read once — nothing here can change while the screen is open, so there is no
 * callback and no loading state. The fields are raw rather than a rendered line: the formatting is
 * a string resource, which a platform impl has no way to reach. [privacyPolicyLink] is picked by
 * `crashReporter.isAvailable` (16.4) — a build/flavor fact too, same reasoning.
 */
data class AboutUiState(
    val versionName: String = "",
    val versionCode: Int = 0,
    val isDebug: Boolean = false,
    val privacyPolicyLink: StringResource = RString.privacy_policy_url
)
