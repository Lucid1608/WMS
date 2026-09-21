package com.example.wmai

/**
 * Decides whether a link's URI scheme is one the WebView can navigate itself (`http`/`https`)
 * or one that must be handed off to the system (`tel:`, `mailto:`, `geo:`, etc.). WebView has
 * no built-in handler for non-web schemes: left alone, tapping such a link fails to load and
 * surfaces the app's generic error screen instead of, say, opening the dialer.
 */
object LinkSchemePolicy {

    fun isWebScheme(scheme: String?): Boolean =
        scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)
}
