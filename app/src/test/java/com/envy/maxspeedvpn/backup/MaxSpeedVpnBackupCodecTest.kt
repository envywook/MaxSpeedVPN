package com.envy.maxspeedvpn.backup

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.assertFailsWith

class MaxSpeedVpnBackupCodecTest {
    private val subscriptions = """[{"id":"sub","name":"Main","url":"https://example.test/sub","updatedAt":1}]"""
    private val servers = """[{"id":"server","subscriptionId":"sub","name":"Server","protocol":"vless","address":"edge.example","port":443,"config":"{}"}]"""
    private val backup = MaxSpeedVpnBackup(
        subscriptionsJson = subscriptions,
        serversJson = servers,
        selectedServerId = "server",
        favoriteServerIds = "server",
        vpnSettings = mapOf("engine" to "SING_BOX", "mtu" to "1500"),
    )

    @Test
    fun `round trips complete versioned backup`() {
        assertEquals(backup, MaxSpeedVpnBackupCodec.decode(MaxSpeedVpnBackupCodec.encode(backup)))
    }

    @Test
    fun `rejects unknown schema before reading state`() {
        assertFailsWith<IllegalArgumentException> {
            MaxSpeedVpnBackupCodec.decode("""{"schemaVersion":2,"subscriptions":"[]","servers":"[]"}""")
        }
    }

    @Test
    fun `rejects malformed embedded snapshots`() {
        assertFailsWith<Exception> {
            MaxSpeedVpnBackupCodec.decode("""{"schemaVersion":1,"subscriptions":"not-json","servers":"[]"}""")
        }
        assertFailsWith<Exception> { decodeWith("[1]", "[]") }
        assertFailsWith<Exception> { decodeWith("[{\"id\":\"sub\"}]", "[]") }
        assertFailsWith<Exception> {
            decodeWith("[{\"id\":\"sub\",\"name\":\"Main\",\"url\":\"https://example.test/sub\",\"updatedAt\":1,\"usage\":\"broken\"}]", "[]")
        }
    }

    @Test
    fun `accepts persisted subscription usage keys`() {
        val withUsage = "[{\"id\":\"sub\",\"name\":\"Main\",\"url\":\"https://example.test/sub\",\"updatedAt\":1,\"usage\":{\"upload\":1,\"download\":2,\"total\":3,\"expire\":4}}]"
        assertEquals(withUsage, MaxSpeedVpnBackupCodec.decode(MaxSpeedVpnBackupCodec.encode(backup.copy(subscriptionsJson = withUsage))).subscriptionsJson)
    }

    @Test
    fun `round trips locally imported profiles without a subscription record`() {
        val localServers = servers.replace("\"subscriptionId\":\"sub\"", "\"subscriptionId\":\"local-manual-import\"")
        val localBackup = backup.copy(subscriptionsJson = "[]", serversJson = localServers)

        assertEquals(localBackup, MaxSpeedVpnBackupCodec.decode(MaxSpeedVpnBackupCodec.encode(localBackup)))
    }

    @Test
    fun `round trips all persisted VPN settings`() {
        val completeSettings = mapOf(
            "mtu" to "1400",
            "dns_server" to "8.8.8.8",
            "ipv6_enabled" to "false",
            "engine" to "SING_BOX",
            "routing_mode" to "CUSTOM",
            "routing_rules" to "domain:example.com",
            "smart_connect" to "true",
            "ping_on_launch" to "false",
            "split_tunnel_mode" to "ONLY_SELECTED",
            "split_tunnel_packages" to "com.example.one\ncom.example.two",
        )

        assertEquals(
            completeSettings,
            MaxSpeedVpnBackupCodec.decode(MaxSpeedVpnBackupCodec.encode(backup.copy(vpnSettings = completeSettings))).vpnSettings,
        )
    }

    @Test
    fun `rejects duplicate and broken references`() {
        val duplicateSubscriptions = "[$subscriptions".replace("[[", "[").removeSuffix("]") + "," + subscriptions.removePrefix("[")
        assertFailsWith<Exception> { decodeWith(duplicateSubscriptions, servers) }
        assertFailsWith<Exception> {
            decodeWith(subscriptions, servers.replace("\"subscriptionId\":\"sub\"", "\"subscriptionId\":\"missing\""))
        }
        assertFailsWith<Exception> { decodeWith(subscriptions, servers.replace("\"config\":\"{}\"", "\"config\":\"broken\"")) }
        assertFailsWith<Exception> { decodeWith(subscriptions, servers, selected = "missing") }
        assertFailsWith<Exception> { decodeWith(subscriptions, servers, favorite = "missing") }
    }

    private fun decodeWith(
        subscriptions: String,
        servers: String,
        selected: String? = null,
        favorite: String = "",
    ) = MaxSpeedVpnBackupCodec.decode(JSONObject().apply {
        put("schemaVersion", 1)
        put("subscriptions", subscriptions)
        put("servers", servers)
        selected?.let { put("selectedServerId", it) }
        put("favoriteServerIds", favorite)
        put("vpnSettings", JSONObject())
    }.toString())
}
