package com.envy.dualcorevpn.subscription

import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun doesNotAttachDeviceHeadersToUnsupportedSchemes() {
        assertEquals(emptyMap(), subscriptionRequestHeaders("ftp", identity))
    }
}
