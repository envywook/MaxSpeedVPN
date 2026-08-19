package com.envy.maxspeedvpn.backup

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

data class MaxSpeedVpnBackup(
    val subscriptionsJson: String,
    val serversJson: String,
    val selectedServerId: String?,
    val favoriteServerIds: String,
    val vpnSettings: Map<String, String>,
)

object MaxSpeedVpnBackupCodec {
    private const val SCHEMA_VERSION = 1
    private const val MAX_ENTRIES = 50_000

    fun encode(backup: MaxSpeedVpnBackup): String = JSONObject().apply {
        put("schemaVersion", SCHEMA_VERSION)
        put("subscriptions", backup.subscriptionsJson)
        put("servers", backup.serversJson)
        backup.selectedServerId?.let { put("selectedServerId", it) }
        put("favoriteServerIds", backup.favoriteServerIds)
        put("vpnSettings", JSONObject(backup.vpnSettings))
    }.toString(2)

    fun decode(value: String): MaxSpeedVpnBackup {
        val root = JSONObject(value)
        require(root.optInt("schemaVersion", -1) == SCHEMA_VERSION) { "Неподдерживаемая версия резервной копии" }
        val subscriptionsJson = root.getString("subscriptions")
        val serversJson = root.getString("servers")
        val subscriptionIds = validateSubscriptions(JSONArray(subscriptionsJson))
        val serverIds = validateServers(JSONArray(serversJson), subscriptionIds)
        val selected = root.optString("selectedServerId").takeIf(String::isNotBlank)
        require(selected == null || selected in serverIds) { "Выбранный сервер отсутствует в резервной копии" }
        val favorites = root.optString("favoriteServerIds")
        require(parseFavorites(favorites).all(serverIds::contains)) { "Избранный сервер отсутствует в резервной копии" }
        val settings = root.optJSONObject("vpnSettings") ?: JSONObject()
        return MaxSpeedVpnBackup(
            subscriptionsJson = subscriptionsJson,
            serversJson = serversJson,
            selectedServerId = selected,
            favoriteServerIds = favorites,
            vpnSettings = settings.keys().asSequence().associateWith { settings.get(it).toString() },
        )
    }

    private fun validateSubscriptions(array: JSONArray): Set<String> {
        require(array.length() <= MAX_ENTRIES) { "Слишком много подписок" }
        return buildSet {
            repeat(array.length()) { index ->
                val item = array.optJSONObject(index) ?: throw IllegalArgumentException("Некорректная подписка")
                val id = requiredString(item, "id")
                require(add(id)) { "Повторяющийся ID подписки" }
                requiredString(item, "name")
                val url = requiredString(item, "url")
                require(runCatching {
                    URI(url).let { it.scheme in setOf("http", "https") && !it.host.isNullOrBlank() }
                }.getOrDefault(false)) { "Некорректный URL подписки" }
                require(item.has("updatedAt") && item.getLong("updatedAt") >= 0L) { "Некорректная дата подписки" }
                if (item.has("usage") && !item.isNull("usage")) {
                    val usage = item.optJSONObject("usage") ?: throw IllegalArgumentException("Некорректные метаданные подписки")
                    listOf("upload", "download", "total", "expire").forEach { key ->
                        if (usage.has(key) && !usage.isNull(key)) require(usage.getLong(key) >= 0L) { "Некорректные метаданные подписки" }
                    }
                }
            }
        }
    }

    private fun validateServers(array: JSONArray, subscriptionIds: Set<String>): Set<String> {
        require(array.length() <= MAX_ENTRIES) { "Слишком много серверов" }
        return buildSet {
            repeat(array.length()) { index ->
                val item = array.optJSONObject(index) ?: throw IllegalArgumentException("Некорректный сервер")
                val id = requiredString(item, "id")
                require(add(id)) { "Повторяющийся ID сервера" }
                val subscriptionId = requiredString(item, "subscriptionId")
                require(subscriptionId in subscriptionIds) { "Сервер ссылается на отсутствующую подписку" }
                requiredString(item, "name")
                requiredString(item, "protocol")
                requiredString(item, "address")
                require(item.getInt("port") in 1..65535) { "Некорректный порт сервера" }
                val config = requiredString(item, "config")
                require(runCatching { JSONObject(config) }.isSuccess) { "Некорректная конфигурация сервера" }
            }
        }
    }

    private fun requiredString(item: JSONObject, key: String): String = item.getString(key).also {
        require(it.isNotBlank()) { "Пустое обязательное поле: $key" }
    }

    private fun parseFavorites(value: String): Set<String> = value.lineSequence().map(String::trim).filter(String::isNotEmpty).toSet()
}
