package com.grappim.wallosmobile.feature.dashboard.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MonthlyBudgetTest {

    @Test
    fun `absent, not zeroed, when the budget is 0`() {
        assertNull(MonthlyBudget.from(budget = 0.0, monthlyCost = 42.0))
    }

    @Test
    fun `absent when the budget is negative`() {
        assertNull(MonthlyBudget.from(budget = -10.0, monthlyCost = 42.0))
    }

    @Test
    fun `under budget clamps remaining and leaves overBudget at 0`() {
        val result = MonthlyBudget.from(budget = 100.0, monthlyCost = 42.0)

        assertEquals(
            MonthlyBudget(amount = 100.0, used = 42.0, remaining = 58.0, overBudget = 0.0, isOverBudget = false),
            result
        )
    }

    @Test
    fun `over budget clamps remaining to 0 and reports the excess`() {
        val result = MonthlyBudget.from(budget = 100.0, monthlyCost = 150.0)

        assertTrue(result!!.isOverBudget)
        assertEquals(0.0, result.remaining)
        assertEquals(50.0, result.overBudget)
        assertEquals(100.0, result.used)
    }

    @Test
    fun `used caps at 100 rather than exceeding it`() {
        val result = MonthlyBudget.from(budget = 100.0, monthlyCost = 300.0)

        assertEquals(100.0, result!!.used)
        assertFalse(result.used > 100.0)
    }
}
