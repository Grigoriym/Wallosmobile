package com.grappim.wallosmobile.utils.formatter.datetime

import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import org.koin.core.annotation.Single

/**
 * The `YYYY-MM-DD` strings every Wallos date field is made of — `next_payment`, `start_date`,
 * `cancellation_date` (API doc §3.1).
 */
@Single
class DateFormatter {

    /**
     * `null` rather than a throw, for the two things the wire actually does: an **unset** date is
     * `""`, not `null` (API doc §3.1, confirmed against the live instance in 2.1), and a value an
     * older instance wrote in some other shape must not sink the whole list it arrived in. Both
     * read as "no date" and the screen leaves the row out.
     */
    fun parseIsoDate(text: String): LocalDate? = if (text.isBlank()) {
        null
    } else {
        // kotlinx-datetime's DateTimeFormatException is an IllegalArgumentException, so this
        // catches a malformed date without a bare `catch (Exception)` swallowing cancellation.
        try {
            LocalDate.parse(text, LocalDate.Formats.ISO)
        } catch (e: IllegalArgumentException) {
            logcat(priority = LogPriority.WARN, throwable = e) { "Unparseable date: $text" }
            null
        }
    }

    /** The shape `FormParams.date()` writes, for anything that needs the string on its own. */
    fun formatIsoDate(date: LocalDate): String = date.format(LocalDate.Formats.ISO)
}
