package com.grappim.wallosmobile.uikit.widgets.network

import androidx.compose.runtime.compositionLocalOf

/**
 * Offline state for every screen inside the shell, which is the one place that provides it.
 *
 * `CLAUDE.md`'s rule is "offline → disable write actions", and a composition local is what keeps
 * that from becoming a field on every UI state class. It errors rather than defaulting to `false`
 * so an unprovided tree fails loudly instead of quietly claiming to be online —
 * `WallosMobilePreviewTheme` provides it for previews.
 *
 * `compositionLocalOf`, not `staticCompositionLocalOf`: the value changes while the app runs, and
 * a static local would invalidate the whole shell on each flip instead of only its readers.
 */
val LocalIsOffline = compositionLocalOf<Boolean> {
    error("LocalIsOffline not provided")
}
