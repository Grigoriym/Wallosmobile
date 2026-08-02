package com.grappim.wallosmobile.composeapp.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.grappim.wallosmobile.composeapp.nav.entries.settingsEntry
import com.grappim.wallosmobile.composeapp.nav.entries.subscriptionsEntry
import com.grappim.wallosmobile.core.navigation.NavigationState
import com.grappim.wallosmobile.core.navigation.Navigator
import com.grappim.wallosmobile.core.navigation.toEntries

/**
 * Each feature contributes an `EntryProviderScope<NavKey>` extension in `nav/entries/`
 * (plan §5.3), which is why this file names no route and no screen of its own.
 */
@Composable
fun MainNavHost(navigationState: NavigationState, navigator: Navigator, modifier: Modifier = Modifier) {
    val entryProvider = entryProvider {
        subscriptionsEntry(navigator)
        settingsEntry()
    }

    NavDisplay(
        modifier = modifier,
        transitionSpec = {
            fadeIn(animationSpec = tween(TRANSITION_DURATION_MS)) togetherWith
                fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))
        },
        popTransitionSpec = {
            fadeIn(animationSpec = tween(TRANSITION_DURATION_MS)) togetherWith
                fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))
        },
        onBack = { navigator.goBack() },
        entries = navigationState.toEntries(entryProvider)
    )
}

private const val TRANSITION_DURATION_MS = 150
