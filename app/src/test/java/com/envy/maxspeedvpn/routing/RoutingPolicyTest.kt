package com.envy.maxspeedvpn.routing

import com.envy.maxspeedvpn.core.SingBoxConfigConverter
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertFailsWith

class RoutingPolicyTest {
    private val xray = """{"inbounds":[{"tag":"socks-in","protocol":"socks","settings":{"udp":true}}],"outbounds":[{"tag":"proxy","protocol":"freedom","settings":{}}]}"""

    @Test
    fun `all traffic leaves xray config without routing rules`() {
        val root = JSONObject(XrayRouting.apply(xray, RoutingPolicy(RoutingMode.ALL)))
        assertFalse(root.has("routing"))
    }

    @Test
    fun `local bypass adds private networks to xray direct route`() {
        val root = JSONObject(XrayRouting.apply(xray, RoutingPolicy(RoutingMode.BYPASS_LAN)))
        val rule = root.getJSONObject("routing").getJSONArray("rules").getJSONObject(0)
        assertEquals("direct", rule.getString("outboundTag"))
        assertTrue(rule.getJSONArray("ip").toString().contains("192.168.0.0/16"))
        assertTrue(rule.getJSONArray("ip").toString().contains("fc00::/7"))
        assertEquals("freedom", root.getJSONArray("outbounds").getJSONObject(1).getString("protocol"))
    }

    @Test
    fun `custom bypass separates domains and cidr for both engines`() {
        val policy = RoutingPolicy.parse(RoutingMode.CUSTOM, "example.com\n*.internal.test\n10.20.0.0/16\n192.0.2.4\nfd00::/8")
        val xrayRoot = JSONObject(XrayRouting.apply(xray, policy))
        val xrayRule = xrayRoot.getJSONObject("routing").getJSONArray("rules").getJSONObject(0)
        assertTrue(xrayRule.getJSONArray("domain").toString().contains("domain:example.com"))
        assertTrue(xrayRule.getJSONArray("ip").toString().contains("10.20.0.0/16"))

        val singRoot = JSONObject(SingBoxConfigConverter.convert(xray, policy))
        val route = singRoot.getJSONObject("route")
        val rule = route.getJSONArray("rules").getJSONObject(0)
        assertEquals("direct", rule.getString("outbound"))
        assertTrue(rule.getJSONArray("domain_suffix").toString().contains("example.com"))
        assertTrue(rule.getJSONArray("ip_cidr").toString().contains("192.0.2.4/32"))
        assertTrue(rule.getJSONArray("ip_cidr").toString().contains("fd00:0:0:0:0:0:0:0/8"))
        assertEquals("direct", singRoot.getJSONArray("outbounds").getJSONObject(1).getString("type"))
    }

    @Test
    fun `rejects malformed custom entries`() {
        assertFailsWith<IllegalArgumentException> { RoutingPolicy.parse(RoutingMode.CUSTOM, "https://example.com/path") }
        assertFailsWith<IllegalArgumentException> { RoutingPolicy.parse(RoutingMode.CUSTOM, "10.0.0.0/99") }
        assertFailsWith<IllegalArgumentException> { RoutingPolicy.parse(RoutingMode.CUSTOM, "a".repeat(4097)) }
    }
}
