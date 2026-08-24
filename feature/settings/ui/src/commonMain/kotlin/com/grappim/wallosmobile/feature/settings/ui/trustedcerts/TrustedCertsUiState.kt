package com.grappim.wallosmobile.feature.settings.ui.trustedcerts

import com.grappim.wallosmobile.core.domain.PendingCertTrust
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * [certs] is whatever storage last emitted, never a local list — same reasoning as
 * `StartDestinationUiState.selected`: a revoke goes to DataStore and comes back through the same
 * flow, so the list can't disagree with what's actually stored. [certPendingRevoke] non-null is
 * what opens the confirm dialog; there is no separate `isRevokeDialogOpen` to keep in sync with it.
 */
data class TrustedCertsUiState(
    val certs: ImmutableList<PendingCertTrust> = persistentListOf(),
    val certPendingRevoke: PendingCertTrust? = null,
    val onRevokeClick: (PendingCertTrust) -> Unit = {},
    val onRevokeConfirm: () -> Unit = {},
    val onRevokeDialogDismiss: () -> Unit = {}
)
