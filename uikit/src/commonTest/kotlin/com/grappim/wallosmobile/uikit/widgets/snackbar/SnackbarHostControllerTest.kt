package com.grappim.wallosmobile.uikit.widgets.snackbar

import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SnackbarHostControllerTest {

    @Test
    fun `show enqueues the message on the host state and reports the action tap`() = runTest {
        val controller = SnackbarHostController()

        val result = async { controller.show(message = "Update ready", actionLabel = "Restart") }
        runCurrent()

        val data = assertNotNull(controller.hostState.currentSnackbarData)
        assertEquals("Update ready", data.visuals.message)
        assertEquals("Restart", data.visuals.actionLabel)

        data.performAction()

        assertEquals(SnackbarResult.ActionPerformed, result.await())
    }
}
