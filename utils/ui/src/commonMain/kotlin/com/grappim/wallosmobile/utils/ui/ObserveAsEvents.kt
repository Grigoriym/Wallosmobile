package com.grappim.wallosmobile.utils.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * How a screen consumes a one-off event `Channel` — navigation, a snackbar, a success signal.
 * Collection is tied to the lifecycle, so an event sent while the screen is in the background is
 * delivered when it comes back rather than acted on off-screen.
 */
@Composable
fun <T> ObserveAsEvents(flow: Flow<T>, isImmediate: Boolean = true, onEvent: (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(flow, lifecycleOwner.lifecycle) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (isImmediate) {
                withContext(Dispatchers.Main.immediate) {
                    flow.collect(onEvent)
                }
            } else {
                flow.collect(onEvent)
            }
        }
    }
}
