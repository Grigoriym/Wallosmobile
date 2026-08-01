package com.grappim.wallosmobile.feature.subscriptions.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The `cycle` column's five values, as API doc §3.1 lists them. */
class BillingCycleTest {

    @Test
    fun `maps every documented cycle code`() {
        val cases = listOf(
            1 to BillingCycle.DAYS,
            2 to BillingCycle.WEEKS,
            3 to BillingCycle.MONTHS,
            4 to BillingCycle.YEARS,
            5 to BillingCycle.ONE_TIME
        )

        cases.forEach { (code, expected) ->
            assertEquals(expected, BillingCycle.fromCode(code), "cycle=$code")
        }
    }

    /** Codes outside the table belong to an instance newer than this build, not to a default. */
    @Test
    fun `returns null for an unknown code`() {
        listOf(0, 6, -1).forEach { code ->
            assertNull(BillingCycle.fromCode(code), "cycle=$code")
        }
    }

    @Test
    fun `round-trips a cycle through its wire code`() {
        BillingCycle.entries.forEach { cycle ->
            assertEquals(cycle, BillingCycle.fromCode(cycle.code))
        }
    }

    /** `set_subscriptions.php` rejects `cycle=5`, so the editor must not offer One-time. */
    @Test
    fun `marks one-time as the only unwritable cycle`() {
        assertFalse(BillingCycle.ONE_TIME.isWritable)
        BillingCycle.entries.minus(BillingCycle.ONE_TIME).forEach { cycle ->
            assertTrue(cycle.isWritable, "$cycle")
        }
    }
}
