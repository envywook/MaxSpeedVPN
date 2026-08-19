package com.envy.maxspeedvpn.server

import com.envy.maxspeedvpn.subscription.ServerProfile

enum class ServerSort { NAME, LATENCY }

data class PlannedServer(
    val server: ServerProfile,
    val favorite: Boolean,
    val latencyMillis: Long?,
)

data class ServerGroup(
    val id: String,
    val name: String,
    val servers: List<PlannedServer>,
)

object ServerListPlanner {
    fun plan(
        servers: List<ServerProfile>,
        subscriptionNames: Map<String, String>,
        favoriteIds: Set<String>,
        query: String,
        sort: ServerSort,
        latencyMillis: Map<String, Long?>,
    ): List<ServerGroup> {
        val needle = query.trim().lowercase()
        val filtered = servers.filter { server ->
            needle.isEmpty() || listOf(server.name, server.address, server.protocol)
                .any { it.lowercase().contains(needle) }
        }
        val grouped = linkedMapOf<String, MutableList<ServerProfile>>()
        filtered.forEach { server ->
            val groupId = server.subscriptionId.takeIf(subscriptionNames::containsKey) ?: UNGROUPED_ID
            grouped.getOrPut(groupId) { mutableListOf() }.add(server)
        }
        return grouped.map { (groupId, values) ->
            val planned = values.map { server ->
                PlannedServer(server, server.id in favoriteIds, latencyMillis[server.id])
            }.sortedWith(
                compareByDescending<PlannedServer> { it.favorite }
                    .thenComparator { left, right -> compare(left, right, sort) },
            )
            ServerGroup(
                id = groupId,
                name = subscriptionNames[groupId] ?: "Серверы",
                servers = planned,
            )
        }
    }

    private fun compare(left: PlannedServer, right: PlannedServer, sort: ServerSort): Int = when (sort) {
        ServerSort.NAME -> compareValuesBy(left, right) { it.server.name.lowercase() }
        ServerSort.LATENCY -> compareValuesBy(left, right, { it.latencyMillis ?: Long.MAX_VALUE }, { it.server.name.lowercase() })
    }

    private const val UNGROUPED_ID = "__all__"
}

object ServerFavoritesCodec {
    fun decode(value: String?): Set<String> = value.orEmpty().lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toSet()

    fun encode(values: Set<String>): String = values.filter(String::isNotBlank).sorted().joinToString("\n")
}
