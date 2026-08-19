package com.envy.maxspeedvpn.server

import com.envy.maxspeedvpn.subscription.ServerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerListPlannerTest {
    private val alpha = ServerProfile("alpha", "work", "Alpha", "vless", "alpha.example", 443, "{}")
    private val beta = ServerProfile("beta", "personal", "Beta", "trojan", "beta.example", 443, "{}")
    private val gamma = ServerProfile("gamma", "work", "Gamma", "vmess", "gamma.example", 443, "{}")

    @Test
    fun `favorites are pinned and grouped by subscription`() {
        val groups = ServerListPlanner.plan(
            servers = listOf(alpha, beta, gamma),
            subscriptionNames = mapOf("work" to "Работа", "personal" to "Личное"),
            favoriteIds = setOf("gamma"),
            query = "",
            sort = ServerSort.NAME,
            latencyMillis = emptyMap(),
        )

        assertEquals(listOf("Работа", "Личное"), groups.map { it.name })
        assertEquals(listOf("gamma", "alpha"), groups.first().servers.map { it.server.id })
        assertTrue(groups.first().servers.first().favorite)
    }

    @Test
    fun `search matches name endpoint and protocol case insensitively`() {
        fun ids(query: String) = ServerListPlanner.plan(
            servers = listOf(alpha, beta, gamma),
            subscriptionNames = emptyMap(),
            favoriteIds = emptySet(),
            query = query,
            sort = ServerSort.NAME,
            latencyMillis = emptyMap(),
        ).flatMap { it.servers }.map { it.server.id }

        assertEquals(listOf("beta"), ids("TROJAN"))
        assertEquals(listOf("alpha"), ids("alpha.example"))
        assertEquals(listOf("gamma"), ids("amm"))
    }

    @Test
    fun `latency sort puts reachable fastest first and unavailable last`() {
        val groups = ServerListPlanner.plan(
            servers = listOf(alpha, beta, gamma),
            subscriptionNames = emptyMap(),
            favoriteIds = emptySet(),
            query = "",
            sort = ServerSort.LATENCY,
            latencyMillis = mapOf("alpha" to 90L, "beta" to null, "gamma" to 25L),
        )

        assertEquals(listOf("gamma", "alpha", "beta"), groups.single().servers.map { it.server.id })
    }

    @Test
    fun `favorites codec ignores blanks and removes duplicates`() {
        assertEquals(setOf("one", "two"), ServerFavoritesCodec.decode("one\ntwo\none\n"))
        assertEquals("one\ntwo", ServerFavoritesCodec.encode(setOf("two", "one")))
    }
}
