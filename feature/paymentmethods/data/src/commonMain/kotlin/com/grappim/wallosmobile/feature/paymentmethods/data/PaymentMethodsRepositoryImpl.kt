package com.grappim.wallosmobile.feature.paymentmethods.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.asynckmp.IoDispatcher
import com.grappim.wallosmobile.core.domain.resultOf
import com.grappim.wallosmobile.feature.paymentmethods.domain.model.PaymentMethod
import com.grappim.wallosmobile.feature.paymentmethods.domain.repo.PaymentMethodsRepository
import com.grappim.wallosmobile.feature.paymentmethods.mapper.PaymentMethodMapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

/**
 * No cache behind this one (unlike `SubscriptionsRepository`, 3.4) — payment methods are
 * reference data with no offline requirement in this milestone, so every call is a round trip to
 * [PaymentMethodsApi].
 */
@Single(binds = [PaymentMethodsRepository::class])
internal class PaymentMethodsRepositoryImpl(
    private val api: PaymentMethodsApi,
    private val mapper: PaymentMethodMapper,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : PaymentMethodsRepository {

    override suspend fun getPaymentMethods(): Result<List<PaymentMethod>> = resultOf {
        withContext(dispatcher) { api.getAll().map(mapper::toDomain) }
    }

    override suspend fun addPaymentMethod(name: String, enabled: Boolean, iconUrl: String?): Result<Int> = resultOf {
        withContext(dispatcher) { api.add(fields(name, enabled, iconUrl)) }
    }

    override suspend fun editPaymentMethod(id: Int, name: String, enabled: Boolean, iconUrl: String?): Result<Unit> =
        resultOf {
            withContext(dispatcher) { api.edit(id, fields(name, enabled, iconUrl)) }
        }

    override suspend fun deletePaymentMethod(id: Int): Result<Unit> = resultOf {
        withContext(dispatcher) { api.delete(id) }
    }

    private fun fields(name: String, enabled: Boolean, iconUrl: String?): FormParams {
        val params = FormParams().put(PARAM_NAME, name).flag(PARAM_ENABLED, enabled)
        return if (iconUrl != null) params.put(PARAM_ICON_URL, iconUrl) else params
    }

    private companion object {
        const val PARAM_NAME = "name"
        const val PARAM_ENABLED = "enabled"
        const val PARAM_ICON_URL = "icon_url"
    }
}
