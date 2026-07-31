package com.grappim.wallosmobile.core.api

import kotlin.test.Test
import kotlin.test.assertEquals

class RedactingLoggerTest {

    @Test
    fun `redacts the api key out of a form body`() {
        assertEquals(
            "BODY START\nsort=name&api_key=REDACTED&convert_currency=true",
            redactCredentials("BODY START\nsort=name&api_key=s3cr3tk3y&convert_currency=true")
        )
    }

    @Test
    fun `redacts the key when it is the last parameter, with no ampersand to stop at`() {
        assertEquals("action=add&api_key=REDACTED", redactCredentials("action=add&api_key=s3cr3tk3y"))
    }

    @Test
    fun `redacts the alternate apiKey spelling the server also accepts`() {
        assertEquals("apiKey=REDACTED&x=1", redactCredentials("apiKey=s3cr3tk3y&x=1"))
    }

    @Test
    fun `redacts the password the login bridge posts`() {
        assertEquals(
            "username=demo&password=REDACTED",
            redactCredentials("username=demo&password=hunter2")
        )
    }

    @Test
    fun `stops at the end of the line so it cannot eat the rest of a log entry`() {
        assertEquals(
            "api_key=REDACTED\nHEADERS\n-> Accept: */*",
            redactCredentials("api_key=s3cr3tk3y\nHEADERS\n-> Accept: */*")
        )
    }

    @Test
    fun `leaves a body carrying no credentials alone`() {
        assertEquals("sort=name&sort_order=asc", redactCredentials("sort=name&sort_order=asc"))
    }
}
