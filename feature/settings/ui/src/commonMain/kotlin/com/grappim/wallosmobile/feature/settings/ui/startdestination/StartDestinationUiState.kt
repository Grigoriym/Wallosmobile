package com.grappim.wallosmobile.feature.settings.ui.startdestination

import com.grappim.wallosmobile.core.storage.startdestination.StartDestination

/**
 * [selected] is whatever storage last emitted, never a local selection — same reasoning as
 * `InterfaceUiState.selectedMode`: the write goes to DataStore and comes back through the same
 * flow, so the radio group can't disagree with what actually gets read on the next cold start.
 */
data class StartDestinationUiState(
    val selected: StartDestination = StartDestination.default(),
    val onSelect: (StartDestination) -> Unit = {}
)
