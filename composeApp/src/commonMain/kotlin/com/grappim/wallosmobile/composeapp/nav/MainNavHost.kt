package com.grappim.wallosmobile.composeapp.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.grappim.wallosmobile.composeapp.nav.entries.subscriptionsEntry
import com.grappim.wallosmobile.core.navigation.NavigationState
import com.grappim.wallosmobile.core.navigation.Navigator
import com.grappim.wallosmobile.core.navigation.toEntries
import com.grappim.wallosmobile.strings.RString
import com.grappim.wallosmobile.strings.generated.resources.settings_title
import com.grappim.wallosmobile.strings.generated.resources.subscriptions_title
import com.grappim.wallosmobile.uikit.widgets.topappbar.LocalTopBarConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.NavigationIconConfig
import com.grappim.wallosmobile.uikit.widgets.topappbar.TopBarConfig
import com.grappim.wallosmobile.utils.ui.NativeText
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Each feature contributes an `EntryProviderScope<NavKey>` extension in `nav/entries/`
 * (plan §5.3). Settings has no feature module yet, so it still renders [PlaceholderScreen] — 2.6
 * replaces it.
 */
@Composable
fun MainNavHost(navigationState: NavigationState, navigator: Navigator, modifier: Modifier = Modifier) {
    val entryProvider = entryProvider {
        subscriptionsEntry()
        entry<SettingsRoute> { PlaceholderScreen(title = RString.settings_title) }
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

/**
 * Stands in for a real screen until 2.6 writes settings. It declares a top bar so the shell
 * has a menu button — the drawer is otherwise swipe-only.
 */
@Composable
private fun PlaceholderScreen(title: StringResource, modifier: Modifier = Modifier) {
    val topBarController = LocalTopBarConfig.current
    LaunchedEffect(title) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(title),
                navigationIcon = NavigationIconConfig.Menu
            )
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

private const val TRANSITION_DURATION_MS = 150
