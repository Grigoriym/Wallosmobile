package com.grappim.wallosmobile.utils.formatter.datetime

import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
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

    /**
     * `2026-03-05` → `5 Mar 2026`, for the dates a screen shows rather than sends (2.4).
     *
     * The month names are **hard-coded English**, for the same reason `MoneyFormatter` hard-codes
     * `1,234.56`: locale-aware date formatting doesn't exist in `commonMain` and reaching the
     * platform one needs `expect`/`actual`, which also puts it beyond a host test. This stays
     * honest — one format, tested — until the app is translated.
     */
    fun formatDisplayDate(date: LocalDate): String = date.format(displayFormat)

    private companion object {
        val displayFormat = LocalDate.Format {
            day(Padding.NONE)
            char(' ')
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            year()
        }
    }
}
