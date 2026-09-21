package com.example.wmai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewErrorPolicyTest {

    @Test
    fun `shows error when main frame fails to load`() {
        assertTrue(WebViewErrorPolicy.shouldShowError(isForMainFrame = true))
    }

    @Test
    fun `ignores subresource failures`() {
        assertFalse(WebViewErrorPolicy.shouldShowError(isForMainFrame = false))
    }
}
