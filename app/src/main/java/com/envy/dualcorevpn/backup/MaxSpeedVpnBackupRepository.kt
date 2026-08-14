package com.envy.dualcorevpn.backup

import android.content.Context
import com.envy.dualcorevpn.core.EngineKind
import com.envy.dualcorevpn.routing.RoutingMode
import com.envy.dualcorevpn.settings.VpnSettings

class MaxSpeedVpnBackupRepository(context: Context) {
    private val subscriptions = context.getSharedPreferences("subscriptions", Context.MODE_PRIVATE)
    private val settings = context.getSharedPreferences("vpn_settings", Context.MODE_PRIVATE)

    fun export(): String = MaxSpeedVpnBackupCodec.encode(snapshot())

    fun restore(value: String) {
        val decoded = MaxSpeedVpnBackupCodec.decode(value)
        val restoredSettings = validateSettings(decoded.vpnSettings)
        val before = snapshot()
        val beforeSettings = validateSettings(before.vpnSettings)
        try {
            check(writeSubscriptions(decoded)) { "Не удалось восстановить подписки" }
            check(writeSettings(restoredSettings)) { "Не удалось восстановить настройки" }
        } catch (error: Throwable) {
            val subscriptionsRolledBack = writeSubscriptions(before)
            val settingsRolledBack = writeSettings(beforeSettings)
            if (!subscriptionsRolledBack || !settingsRolledBack) {
                throw IllegalStateException("Восстановление прервано, откат состояния выполнен не полностью", error)
            }
            throw error
        }
    }

    private fun snapshot(): MaxSpeedVpnBackup = MaxSpeedVpnBackup(
        subscriptionsJson = subscriptions.getString(KEY_SUBSCRIPTIONS, "[]") ?: "[]",
        serversJson = subscriptions.getString(KEY_SERVERS, "[]") ?: "[]",
        selectedServerId = subscriptions.getString(KEY_SELECTED, null),
        favoriteServerIds = subscriptions.getString(KEY_FAVORITES, "") ?: "",
        vpnSettings = mapOf(
            KEY_MTU to settings.getInt(KEY_MTU, VpnSettings.DEFAULT_MTU).toString(),
            KEY_DNS to (settings.getString(KEY_DNS, VpnSettings.DEFAULT_DNS) ?: VpnSettings.DEFAULT_DNS),
            KEY_IPV6 to settings.getBoolean(KEY_IPV6, true).toString(),
            KEY_ENGINE to (settings.getString(KEY_ENGINE, EngineKind.XRAY.name) ?: EngineKind.XRAY.name),
            KEY_ROUTING_MODE to (settings.getString(KEY_ROUTING_MODE, RoutingMode.ALL.name) ?: RoutingMode.ALL.name),
            KEY_ROUTING_RULES to (settings.getString(KEY_ROUTING_RULES, "") ?: ""),
        ),
    )

    private fun writeSubscriptions(backup: MaxSpeedVpnBackup): Boolean = subscriptions.edit().clear()
        .putString(KEY_SUBSCRIPTIONS, backup.subscriptionsJson)
        .putString(KEY_SERVERS, backup.serversJson)
        .putString(KEY_SELECTED, backup.selectedServerId)
        .putString(KEY_FAVORITES, backup.favoriteServerIds)
        .commit()

    private fun validateSettings(values: Map<String, String>): VpnSettings {
        require(values.keys.all(SUPPORTED_SETTINGS::contains)) { "Резервная копия содержит неизвестные настройки" }
        val ipv6 = values[KEY_IPV6]?.toBooleanStrictOrNull()
            ?: if (KEY_IPV6 in values) throw IllegalArgumentException("Некорректное значение IPv6") else true
        val engine = values[KEY_ENGINE]?.let {
            runCatching { EngineKind.valueOf(it) }.getOrElse { throw IllegalArgumentException("Некорректное ядро VPN") }
        } ?: EngineKind.XRAY
        val routingMode = values[KEY_ROUTING_MODE]?.let {
            runCatching { RoutingMode.valueOf(it) }.getOrElse { throw IllegalArgumentException("Некорректный режим маршрутизации") }
        } ?: RoutingMode.ALL
        return VpnSettings.validate(
            mtu = values[KEY_MTU] ?: VpnSettings.DEFAULT_MTU.toString(),
            dnsServer = values[KEY_DNS] ?: VpnSettings.DEFAULT_DNS,
            ipv6Enabled = ipv6,
            engine = engine,
            routingMode = routingMode,
            routingRules = values[KEY_ROUTING_RULES] ?: "",
        )
    }

    private fun writeSettings(value: VpnSettings): Boolean = settings.edit().clear()
        .putInt(KEY_MTU, value.mtu)
        .putString(KEY_DNS, value.dnsServer)
        .putBoolean(KEY_IPV6, value.ipv6Enabled)
        .putString(KEY_ENGINE, value.engine.name)
        .putString(KEY_ROUTING_MODE, value.routingMode.name)
        .putString(KEY_ROUTING_RULES, value.routingRules)
        .commit()

    private companion object {
        const val KEY_SUBSCRIPTIONS = "subscriptions"
        const val KEY_SERVERS = "servers"
        const val KEY_SELECTED = "selected_server"
        const val KEY_FAVORITES = "favorite_servers"
        const val KEY_MTU = "mtu"
        const val KEY_DNS = "dns_server"
        const val KEY_IPV6 = "ipv6_enabled"
        const val KEY_ENGINE = "engine"
        const val KEY_ROUTING_MODE = "routing_mode"
        const val KEY_ROUTING_RULES = "routing_rules"
        val SUPPORTED_SETTINGS = setOf(KEY_MTU, KEY_DNS, KEY_IPV6, KEY_ENGINE, KEY_ROUTING_MODE, KEY_ROUTING_RULES)
    }
}
