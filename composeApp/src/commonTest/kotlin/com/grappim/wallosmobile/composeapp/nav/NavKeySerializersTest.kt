package com.grappim.wallosmobile.composeapp.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The two ways a new drawer destination breaks the shell, both of them quiet: an unregistered
 * route only fails when the back stack is restored after process death, and a destination missing
 * from [DRAWER_NAV_ITEMS] only fails once that section is opened.
 */
class NavKeySerializersTest {

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `every drawer destination route has a serializer registered`() {
        DrawerDestination.entries.forEach { destination ->
            assertNotNull(
                navKeySerializersModule.getPolymorphic(NavKey::class, destination.route),
                "${destination.name} is missing from navKeySerializersModule"
            )
        }
    }

    @Test
    fun `every drawer destination has a sub stack`() {
        DrawerDestination.entries.forEach { destination ->
            assertTrue(
                destination.route in DRAWER_NAV_ITEMS,
                "${destination.name} is missing from DRAWER_NAV_ITEMS"
            )
        }
    }

    @Test
    fun `the start destination is a drawer destination`() {
        assertTrue(START_DESTINATION in DRAWER_NAV_ITEMS)
    }
}
