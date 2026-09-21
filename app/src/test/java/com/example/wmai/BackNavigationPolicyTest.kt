package com.example.wmai

import org.junit.Assert.assertEquals
import org.junit.Test

class BackNavigationPolicyTest {

    @Test
    fun `navigates webview back when webview has history`() {
        val action = BackNavigationPolicy.resolve(webViewCanGoBack = true)
        assertEquals(BackNavigationPolicy.Action.NAVIGATE_WEBVIEW_BACK, action)
    }

    @Test
    fun `exits activity when webview has no history`() {
        val action = BackNavigationPolicy.resolve(webViewCanGoBack = false)
        assertEquals(BackNavigationPolicy.Action.EXIT_ACTIVITY, action)
    }
}
