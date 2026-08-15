package com.envy.dualcorevpn.core

import org.junit.Assert.assertEquals
import org.junit.Test

class EngineSelectorTest {
    @Test
    fun `routes native sing-box-only protocols to sing-box`() {
        assertEquals(EngineKind.SING_BOX, EngineSelector.select("""{"maxspeedvpn_format":"sing-box","outbound":{"type":"mieru"}}"""))
        assertEquals(EngineKind.SING_BOX, EngineSelector.select("""{"maxspeedvpn_format":"sing-box","outbound":{"type":"naive"}}"""))
    }

    @Test
    fun `routes vless xhttp tls and reality to xray`() {
        val xhttpTls = """{"outbounds":[{"protocol":"vless","streamSettings":{"network":"xhttp","security":"tls"}}]}"""
        val reality = """{"outbounds":[{"protocol":"vless","streamSettings":{"network":"tcp","security":"reality"}}]}"""
        assertEquals(EngineKind.XRAY, EngineSelector.select(xhttpTls))
        assertEquals(EngineKind.XRAY, EngineSelector.select(reality))
    }

    @Test
    fun `routes ordinary xray profiles to xray without requiring a settings choice`() {
        val config = """{"outbounds":[{"protocol":"vless","streamSettings":{"network":"ws","security":"tls"}}]}"""
        assertEquals(EngineKind.XRAY, EngineSelector.select(config))
    }
}
