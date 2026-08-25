package com.envy.maxspeedvpn.subscription

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubscriptionRequestHeadersTest {
    private val identity = SubscriptionDeviceIdentity.Headers(
        hwid = "UE42LJXu4DbiCaBv",
        deviceOs = "Android",
        osVersion = "14",
        deviceModel = "Pixel 8",
    )

    @Test
    fun includesHwidForHttpSubscriptionRequests() {
        assertEquals(
            identity.hwid,
            subscriptionRequestHeaders("http", identity).getValue("X-Hwid"),
        )
    }

    @Test
    fun generatedHwidUsesOnlyCharactersAcceptedByRemnawave() {
        val urlSafeBase64 = "abc_DEF-0123456789"

        assertEquals("abc=DEF-0123456789", remnawaveCompatibleHwid(urlSafeBase64))
        assertTrue(isRemnawaveCompatibleHwid(remnawaveCompatibleHwid(urlSafeBase64)))
    }

    @Test
    fun previouslyPersistedHwidIsRejectedWhenRemnawaveWouldIgnoreIt() {
        assertEquals(false, isRemnawaveCompatibleHwid("abc_DEF-0123456789"))
    }

    @Test
    fun doesNotAttachDeviceHeadersToUnsupportedSchemes() {
        assertEquals(emptyMap(), subscriptionRequestHeaders("ftp", identity))
    }
}
