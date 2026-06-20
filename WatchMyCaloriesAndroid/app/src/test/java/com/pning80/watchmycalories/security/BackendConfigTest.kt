package com.pning80.watchmycalories.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

class BackendConfigTest {

    @Test
    fun testBaseURLIsValidHTTPS() {
        val url = BackendConfig.baseURL
        assertTrue("Backend URL should use HTTPS", url.startsWith("https://"))
        assertFalse("Backend URL should not have a trailing slash", url.endsWith("/"))
        
        try {
            URL(url)
        } catch (e: Exception) {
            org.junit.Assert.fail("Backend URL should be a valid URL: ${e.message}")
        }
    }

    @Test
    fun testDevLegacyKeyIsNullOrNonBlank() {
        // Sourced from Backend/.env.dev via BuildConfig.APP_BACKEND_API_KEY (the
        // single source of truth). May be null when unconfigured (CI / fresh
        // clone); when present it must be a non-blank value, never "".
        val devKey = BackendConfig.devLegacyKey
        if (devKey != null) {
            assertTrue("dev legacy key should be non-blank when present", devKey.isNotBlank())
        }
    }
}
