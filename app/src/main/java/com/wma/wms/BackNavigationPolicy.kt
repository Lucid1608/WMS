package com.wma.wms

/**
 * Decides what the Android back button should do based on whether the WebView
 * has in-page navigation history to go back to.
 */
object BackNavigationPolicy {

    enum class Action { NAVIGATE_WEBVIEW_BACK, EXIT_ACTIVITY }

    fun resolve(webViewCanGoBack: Boolean): Action =
        if (webViewCanGoBack) Action.NAVIGATE_WEBVIEW_BACK else Action.EXIT_ACTIVITY
}
