package com.wma.wms

/**
 * Decides whether a WebView load failure should surface the full-screen error/retry view.
 * Subresource failures (a missing icon, a blocked tracking pixel, etc.) are ignored so a
 * single broken subresource doesn't hide a page that otherwise loaded fine. A main-frame
 * failure described as `net::ERR_ABORTED` is also ignored: Chromium reports that code when a
 * navigation is intentionally cancelled — most commonly because it was handed off to the
 * download handler or superseded by a newer navigation (e.g. the user tapped Retry) — not
 * because the page actually failed to load.
 */
object WebViewErrorPolicy {

    fun shouldShowError(isForMainFrame: Boolean, errorDescription: String?): Boolean =
        isForMainFrame && errorDescription?.contains("ERR_ABORTED", ignoreCase = true) != true
}
