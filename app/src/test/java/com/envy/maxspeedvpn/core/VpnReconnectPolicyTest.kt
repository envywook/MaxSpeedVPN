package com.envy.maxspeedvpn.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnReconnectPolicyTest {
    @Test
    fun `restarts an active session when another server is selected`() {
        assertTrue(shouldRestartForSelection(VpnSessionState.Connected(EngineKind.XRAY, 1L), "old", "new"))
        assertTrue(shouldRestartForSelection(VpnSessionState.Connecting(EngineKind.XRAY), "old", "new"))
    }

    @Test
    fun `does not start vpn merely by changing selection while inactive`() {
        assertFalse(shouldRestartForSelection(VpnSessionState.Disconnected, "old", "new"))
        assertFalse(shouldRestartForSelection(VpnSessionState.Error(EngineKind.XRAY, "failed"), "old", "new"))
    }

    @Test
    fun `does not restart when selection did not change`() {
        assertFalse(shouldRestartForSelection(VpnSessionState.Connected(EngineKind.XRAY, 1L), "same", "same"))
    }

    @Test
    fun `active session helper allows settings changes to restart only live vpn`() {
        assertTrue(hasActiveVpnSession(VpnSessionState.Connected(EngineKind.SING_BOX, 1L)))
        assertFalse(hasActiveVpnSession(VpnSessionState.Disconnected))
    }
}
