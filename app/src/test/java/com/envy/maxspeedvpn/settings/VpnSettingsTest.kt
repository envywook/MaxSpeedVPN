package com.envy.maxspeedvpn.settings

import com.envy.maxspeedvpn.routing.RoutingMode

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.assertFailsWith

class VpnSettingsTest {
    @Test
    fun `accepts normal mtu dns and ipv6 preference`() {
        assertEquals(
            VpnSettings(mtu = 1400, dnsServer = "9.9.9.9", ipv6Enabled = false),
            VpnSettings.validate("1400", "9.9.9.9", false),
        )
    }

    @Test
    fun `enables server checks at launch by default and preserves opt out`() {
        assertEquals(true, VpnSettings().pingOnLaunchEnabled)
        assertEquals(
            false,
            VpnSettings.validate("1500", "1.1.1.1", true, pingOnLaunchEnabled = false).pingOnLaunchEnabled,
        )
    }

    @Test
    fun `rejects mtu outside supported range`() {
        assertFailsWith<IllegalArgumentException> { VpnSettings.validate("575", "1.1.1.1", true) }
        assertFailsWith<IllegalArgumentException> { VpnSettings.validate("9001", "1.1.1.1", true) }
    }

    @Test
    fun `drops hidden custom rules outside custom mode`() {
        val settings = VpnSettings.validate("1500", "1.1.1.1", true, routingMode = RoutingMode.BYPASS_LAN, routingRules = "private.example")
        assertEquals("", settings.routingRules)
    }

    @Test
    fun `rejects blank or malformed dns host`() {
        assertFailsWith<IllegalArgumentException> { VpnSettings.validate("1500", "", true) }
        assertFailsWith<IllegalArgumentException> { VpnSettings.validate("1500", "not a host!", true) }
    }
}
