package com.grappim.wallosmobile.feature.paymentmethods.ui.editor

import androidx.compose.runtime.Composable
import com.grappim.wallosmobile.feature.paymentmethods.domain.model.IconFile

/**
 * The one platform seam this module needs (checklist 9.5, mirroring 7.9's `LogoPicker`): reading
 * an on-device image's bytes has no portable API. `actual` lives in `androidMain` — the first
 * this module has needed, per `CLAUDE.md`'s "no `androidMain` in feature modules, use
 * `expect`/`actual`" rule.
 *
 * @return a function that launches the picker. The result reaches [onPick] later, off the
 * composition that called this — a launcher's callback, not a return value.
 */
@Composable
expect fun rememberIconFilePickerLauncher(onPick: (IconFile) -> Unit): () -> Unit
