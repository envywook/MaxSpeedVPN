package com.envy.maxspeedvpn.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.envy.maxspeedvpn.core.EngineKind
import com.envy.maxspeedvpn.routing.RoutingMode
import com.envy.maxspeedvpn.settings.SplitTunnelMode
import com.envy.maxspeedvpn.settings.VpnSettings
import com.envy.maxspeedvpn.settings.VpnSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaxSpeedVpnBackupRepositoryInstrumentedTest {
    @Test
    fun exportAndRestorePreservesEveryVpnSetting() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("subscriptions", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("vpn_settings", Context.MODE_PRIVATE).edit().clear().commit()
        val expected = VpnSettings.validate(
            mtu = "1400",
            dnsServer = "8.8.8.8",
            ipv6Enabled = false,
            engine = EngineKind.SING_BOX,
            routingMode = RoutingMode.CUSTOM,
            routingRules = "example.com",
            smartConnectEnabled = true,
            pingOnLaunchEnabled = false,
            splitTunnelMode = SplitTunnelMode.ONLY_SELECTED,
            splitTunnelPackages = setOf("com.example.one", "com.example.two"),
        )
        val settings = VpnSettingsRepository(context)
        val backup = MaxSpeedVpnBackupRepository(context)

        settings.save(expected)
        val exported = backup.export()
        settings.save(VpnSettings())
        backup.restore(exported)

        assertEquals(expected, settings.load())
    }
}
