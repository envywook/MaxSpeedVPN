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
