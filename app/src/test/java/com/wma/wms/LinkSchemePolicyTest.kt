package com.wma.wms

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkSchemePolicyTest {

    @Test
    fun `http is a web scheme`() {
        assertTrue(LinkSchemePolicy.isWebScheme("http"))
    }

    @Test
    fun `https is a web scheme`() {
        assertTrue(LinkSchemePolicy.isWebScheme("https"))
    }

    @Test
    fun `scheme comparison is case insensitive`() {
        assertTrue(LinkSchemePolicy.isWebScheme("HTTPS"))
    }

    @Test
    fun `tel is not a web scheme`() {
        assertFalse(LinkSchemePolicy.isWebScheme("tel"))
    }

    @Test
    fun `mailto is not a web scheme`() {
        assertFalse(LinkSchemePolicy.isWebScheme("mailto"))
    }

    @Test
    fun `null scheme is not a web scheme`() {
        assertFalse(LinkSchemePolicy.isWebScheme(null))
    }
}
