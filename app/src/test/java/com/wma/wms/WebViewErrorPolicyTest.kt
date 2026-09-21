package com.wma.wms

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewErrorPolicyTest {

    @Test
    fun `shows error when main frame fails to load`() {
        assertTrue(
            WebViewErrorPolicy.shouldShowError(
                isForMainFrame = true,
                errorDescription = "net::ERR_INTERNET_DISCONNECTED"
            )
        )
    }

    @Test
    fun `ignores subresource failures`() {
        assertFalse(
            WebViewErrorPolicy.shouldShowError(
                isForMainFrame = false,
                errorDescription = "net::ERR_INTERNET_DISCONNECTED"
            )
        )
    }

    @Test
    fun `ignores aborted main frame navigations`() {
        assertFalse(
            WebViewErrorPolicy.shouldShowError(isForMainFrame = true, errorDescription = "net::ERR_ABORTED")
        )
    }

    @Test
    fun `shows error when description is missing`() {
        assertTrue(WebViewErrorPolicy.shouldShowError(isForMainFrame = true, errorDescription = null))
    }
}
