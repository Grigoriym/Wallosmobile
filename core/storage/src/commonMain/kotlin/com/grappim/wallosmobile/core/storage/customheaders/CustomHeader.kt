package com.grappim.wallosmobile.core.storage.customheaders

import kotlinx.serialization.Serializable

/** One user-defined request header for an authenticating reverse proxy in front of Wallos (plan §4.5b). */
@Serializable
data class CustomHeader(val name: String, val value: String)
