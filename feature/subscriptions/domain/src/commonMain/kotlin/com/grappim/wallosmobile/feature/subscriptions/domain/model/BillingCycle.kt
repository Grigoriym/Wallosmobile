package com.grappim.wallosmobile.feature.subscriptions.domain.model

/**
 * How often a subscription renews, paired with a `frequency` multiplier: `MONTHS` + `6` is
 * "every 6 months" (API doc §3.1).
 *
 * [code] is the wire value; [fromCode] is the only way in, because the server sends an `Int`.
 */
enum class BillingCycle(val code: Int) {

    DAYS(1),
    WEEKS(2),
    MONTHS(3),
    YEARS(4),

    /**
     * Readable but **not writable**: `set_subscriptions.php` rejects `cycle=5` even though the
     * web UI and the database both support it (API doc §3.4). An editor must not offer it,
     * which is what [isWritable] is for; read paths still have to render it.
     */
    ONE_TIME(5);

    val isWritable: Boolean get() = this != ONE_TIME

    companion object {

        /**
         * `null` for a code this app doesn't know — the cycle list is hard-coded in Wallos, so
         * that only happens on an instance newer than this build. Callers drop the cycle text
         * rather than guessing a wrong one.
         */
        fun fromCode(code: Int): BillingCycle? = entries.firstOrNull { it.code == code }
    }
}
