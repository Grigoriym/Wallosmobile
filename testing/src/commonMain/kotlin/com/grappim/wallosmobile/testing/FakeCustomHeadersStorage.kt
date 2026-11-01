package com.grappim.wallosmobile.testing

import com.grappim.wallosmobile.core.storage.customheaders.CustomHeader
import com.grappim.wallosmobile.core.storage.customheaders.CustomHeadersStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeCustomHeadersStorage(initial: List<CustomHeader> = emptyList()) : CustomHeadersStorage {

    private val state = MutableStateFlow(initial)

    /** Every list passed to [setHeaders], in order. */
    val setHeadersCalledWith = mutableListOf<List<CustomHeader>>()

    override val headers: Flow<List<CustomHeader>> = state.asStateFlow()

    override val currentHeaders: List<CustomHeader>
        get() = state.value

    override suspend fun setHeaders(headers: List<CustomHeader>) {
        setHeadersCalledWith += headers
        state.value = headers
    }
}
