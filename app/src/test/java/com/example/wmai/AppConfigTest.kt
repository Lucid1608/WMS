package com.example.wmai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigTest {

    @Test
    fun `start url does not embed a session token`() {
        assertFalse(AppConfig.START_URL.contains("ns-at"))
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
