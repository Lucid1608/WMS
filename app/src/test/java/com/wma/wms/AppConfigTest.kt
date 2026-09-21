package com.wma.wms

import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigTest {

    @Test
    fun `start url embeds the mobile role access token`() {
        // NetSuite's Suitelet fails hard ("Invalid page parameter") without ns-at present;
        // it is a persistent access token for this mobile role, not a per-session cookie.
        assertTrue(AppConfig.START_URL.contains("ns-at="))
    }

    @Test
    fun `start url uses https`() {
        assertTrue(AppConfig.START_URL.startsWith("https://"))
    }

    @Test
    fun `start url targets the expected suitelet`() {
        assertTrue(AppConfig.START_URL.contains("script=896"))
        assertTrue(AppConfig.START_URL.contains("compid=5150631"))
    }
}
