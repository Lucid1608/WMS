package com.example.wmai

/**
 * Decides whether a WebView load failure should surface the full-screen error/retry view.
 * Subresource failures (a missing icon, a blocked tracking pixel, etc.) are ignored so a
 * single broken subresource doesn't hide a page that otherwise loaded fine.
 */
object WebViewErrorPolicy {

    fun shouldShowError(isForMainFrame: Boolean): Boolean = isForMainFrame
}
