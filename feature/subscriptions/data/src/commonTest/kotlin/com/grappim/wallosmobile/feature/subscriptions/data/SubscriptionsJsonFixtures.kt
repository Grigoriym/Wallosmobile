package com.grappim.wallosmobile.feature.subscriptions.data

/**
 * Bodies recorded off the live instance (`docs/local-info.txt`), trimmed to two rows. Kotlin
 * constants rather than files: `commonTest` has no portable way to read a resource.
 */
internal object SubscriptionsJsonFixtures {

    val SUBSCRIPTIONS = """
        {"success":true,"title":"subscriptions","subscriptions":[
        {"id":4,"name":"Fiton","logo":"1732556915-Fiton.png","price":31.99,"currency_id":1,
         "next_payment":"2026-01-31","cycle":4,"frequency":1,"notes":"","payment_method_id":4,
         "payer_user_id":1,"category_id":6,"notify":0,"url":"","inactive":1,
         "notify_days_before":null,"user_id":1,"cancellation_date":"",
         "replacement_subscription_id":null,"start_date":"2024-01-31","auto_renew":1,
         "logo_text_color":null,"logo_variant":null,"category_name":"Health & Wellbeing",
         "payer_user_name":"gregorz","payment_method_name":"Direct Debit"},
        {"id":26,"name":"1&amp;1 Telekom","logo":"","price":18,"currency_id":2,
         "next_payment":"2026-02-12","cycle":3,"frequency":1,"notes":"","payment_method_id":4,
         "payer_user_id":1,"category_id":4,"notify":0,"url":"","inactive":0,
         "notify_days_before":null,"user_id":1,"cancellation_date":"",
         "replacement_subscription_id":null,"start_date":"","auto_renew":1,
         "logo_text_color":null,"logo_variant":null,"category_name":"Utilities",
         "payer_user_name":"gregorz","payment_method_name":"Direct Debit"}],"notes":[]}
    """.trimIndent()

    /** What a user with no subscriptions gets: `success: true` and an empty array. */
    const val NO_SUBSCRIPTIONS = """{"success":true,"title":"subscriptions","subscriptions":[],"notes":[]}"""

    val SUBSCRIPTION = """
        {"success":true,"title":"subscription","subscription":
        {"id":4,"name":"Fiton","logo":"1732556915-Fiton.png","price":31.99,"currency_id":1,
         "next_payment":"2026-01-31","cycle":4,"frequency":1,"notes":"","payment_method_id":4,
         "payer_user_id":1,"category_id":6,"notify":0,"url":"","inactive":1,
         "notify_days_before":null,"user_id":1,"cancellation_date":"",
         "replacement_subscription_id":null,"start_date":"2024-01-31","auto_renew":1,
         "logo_text_color":null,"logo_variant":null,"category_name":"Health & Wellbeing",
         "payer_user_name":"gregorz","payment_method_name":"Direct Debit"},"notes":[]}
    """.trimIndent()

    val SUBSCRIPTION_NOT_FOUND = """
        {"success":false,"title":"Subscription not found",
         "message":"Subscription not found or does not belong to you."}
    """.trimIndent()

    val CURRENCIES = """
        {"success":true,"title":"currencies","main_currency":1,"currencies":[
        {"id":1,"name":"Euro","symbol":"€","code":"EUR","rate":"1","in_use":true},
        {"id":2,"name":"US Dollar","symbol":"${'$'}","code":"USD","rate":"1","in_use":false}]}
    """.trimIndent()

    /**
     * Recorded off the live instance, trimmed of the theme keys — the point is the shape of the one
     * field that is read: an **int**, inside a nested `settings` object.
     */
    val SETTINGS_CONVERT_ON = """
        {"success":true,"title":"settings","settings":{
        "dark_theme":1,"convert_currency":1,"show_original_price":1},"notes":[]}
    """.trimIndent()

    val SETTINGS_CONVERT_OFF = """
        {"success":true,"title":"settings","settings":{
        "dark_theme":1,"convert_currency":0,"show_original_price":1},"notes":[]}
    """.trimIndent()

    /** An instance predating the column, which `ignoreUnknownKeys` cannot help with. */
    val SETTINGS_WITHOUT_CONVERT = """
        {"success":true,"title":"settings","settings":{"dark_theme":1},"notes":[]}
    """.trimIndent()
}
