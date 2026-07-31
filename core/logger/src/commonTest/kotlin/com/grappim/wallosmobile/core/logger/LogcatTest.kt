package com.grappim.wallosmobile.core.logger

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LogcatTest {

    private val logger = RecordingLogger()

    @BeforeTest
    fun setUp() {
        WallosLogger.uninstall()
    }

    @AfterTest
    fun tearDown() {
        WallosLogger.uninstall()
    }

    @Test
    fun `no logger installed does not evaluate the message`() {
        var evaluated = false

        logcat { "message".also { evaluated = true } }

        assertFalse(WallosLogger.isInstalled)
        assertFalse(evaluated)
    }

    @Test
    fun `installed logger receives priority throwable and message`() {
        WallosLogger.install(logger)
        val throwable = IllegalStateException("boom")

        logcat(priority = LogPriority.ERROR, tag = "Tag", throwable = throwable) { "failed" }

        assertTrue(WallosLogger.isInstalled)
        val entry = logger.entries.single()
        assertEquals(LogPriority.ERROR, entry.priority)
        assertEquals("Tag", entry.tag)
        assertSame(throwable, entry.throwable)
        assertEquals("failed", entry.message)
    }

    // Called through a top-level helper on purpose: inside a class body `logcat { … }` always
    // resolves to the `Any.logcat` extension, so the receiverless overload is only reachable
    // from top-level code.
    @Test
    fun `top-level logcat leaves the tag null`() {
        WallosLogger.install(logger)

        logWithoutReceiver { "no tag" }

        assertNull(logger.entries.single().tag)
    }

    @Test
    fun `receiver logcat defaults the tag to the receiver class name`() {
        WallosLogger.install(logger)

        logcat { "tagged by receiver" }

        assertEquals("LogcatTest", logger.entries.single().tag)
    }

    @Test
    fun `receiver logcat keeps an explicit tag`() {
        WallosLogger.install(logger)

        logcat(tag = "Explicit") { "tagged by hand" }

        assertEquals("Explicit", logger.entries.single().tag)
    }

    @Test
    fun `uninstall reverts to the no-op logger`() {
        WallosLogger.install(logger)
        WallosLogger.uninstall()

        logcat { "dropped" }

        assertFalse(WallosLogger.isInstalled)
        assertTrue(logger.entries.isEmpty())
    }

    private class RecordingLogger : WallosLogger {
        val entries = mutableListOf<Entry>()

        override fun log(priority: LogPriority, tag: String?, throwable: Throwable?, message: () -> String) {
            entries += Entry(priority, tag, throwable, message())
        }

        data class Entry(val priority: LogPriority, val tag: String?, val throwable: Throwable?, val message: String)
    }
}

private fun logWithoutReceiver(message: () -> String) = logcat(message = message)
