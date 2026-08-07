package com.grappim.wallosmobile.feature.subscriptions.ui.editor

import androidx.compose.runtime.Composable
import com.grappim.wallosmobile.feature.subscriptions.domain.model.LogoFile

/**
 * The one platform seam this milestone needs (checklist 7.9): reading an on-device image's bytes
 * has no portable API, unlike everything else in this feature, which reaches the server through
 * `core:api` alone. `actual` lives in `androidMain` — the first this module has needed, per
 * `CLAUDE.md`'s "no `androidMain` in feature modules, use `expect`/`actual`" rule.
 *
 * @return a function that launches the picker. The result reaches [onPick] later, off the
 * composition that called this — a launcher's callback, not a return value.
 */
@Composable
expect fun rememberLogoPickerLauncher(onPick: (LogoFile) -> Unit): () -> Unit
