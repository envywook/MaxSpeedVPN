package com.envy.dualcorevpn.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerDisplayNameTest {
    @Test
    fun `removes meaningless right arrows and route flags from server label`() {
        assertEquals("США", serverDisplayName("🇺🇸🇷🇺➙🇦🇹➙США"))
    }

    @Test
    fun `keeps a plain server label unchanged`() {
        assertEquals("Frankfurt", serverDisplayName("Frankfurt"))
    }
}
