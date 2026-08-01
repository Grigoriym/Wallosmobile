package com.grappim.wallosmobile.feature.setup.dto

import kotlinx.serialization.Serializable

/**
 * `api/status/version.php` — the cheapest endpoint that still requires a valid key, which is what
 * makes it the setup probe (API doc §6.10).
 *
 * [version] is nullable because nothing here reads it yet: setup only cares that the envelope came
 * back with `success: true`, and a missing field must not turn a working instance into a
 * `Malformed`. Version *gating* (API doc §6.6) is a later phase and will want it non-null.
 */
@Serializable
data class VersionDTO(val version: String? = null)
