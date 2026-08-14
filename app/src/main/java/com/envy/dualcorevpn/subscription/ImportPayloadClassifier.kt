package com.envy.dualcorevpn.subscription

import java.util.UUID
import org.json.JSONObject

sealed interface ImportPayload {
    data class Subscription(val request: SubscriptionImportRequest) : ImportPayload
    data class MieruProfile(val request: MieruImportRequest) : ImportPayload
    data class Profiles(val profiles: List<ServerProfile>) : ImportPayload
}

object ImportPayloadClassifier {
    const val MAX_LENGTH = 4_096
    const val MAX_FILE_LENGTH = 1_000_000
    private const val LOCAL_SUBSCRIPTION_ID = "local-manual-import"

    fun classify(
        raw: CharSequence?,
        requireHttpsSubscription: Boolean = false,
        maxLength: Int = MAX_LENGTH,
    ): ImportPayload {
        require(maxLength in MAX_LENGTH..MAX_FILE_LENGTH) { "Invalid import size limit" }
        val value = raw?.toString()?.trim().orEmpty()
        require(value.isNotEmpty() && value.length <= maxLength) { "Invalid import payload" }
        if (value.startsWith("{") || value.startsWith("[")) {
            return ImportPayload.Profiles(parseSingBoxDocument(value))
        }
        if (value.contains('\n') || value.contains('\r')) {
            val report = SubscriptionParser.parseReport(LOCAL_SUBSCRIPTION_ID, value)
            require(report.profiles.isNotEmpty() && report.unsupportedCount == 0 && report.invalidCount == 0) {
                "Unsupported or invalid server URI"
            }
            return ImportPayload.Profiles(report.profiles)
        }
        SubscriptionDeepLink.parse(value)?.let { request ->
            require(!requireHttpsSubscription || request.url.startsWith("https://", ignoreCase = true)) {
                "Insecure subscription URL"
            }
            return ImportPayload.Subscription(request)
        }
        if (value.startsWith("https://", ignoreCase = true) || value.startsWith("http://", ignoreCase = true)) {
            require(!requireHttpsSubscription || value.startsWith("https://", ignoreCase = true)) {
                "Insecure subscription URL"
            }
            return ImportPayload.Subscription(SubscriptionImportRequest(value))
        }
        if (value.startsWith("mieru://", ignoreCase = true) || value.startsWith("meiru://", ignoreCase = true)) {
            return ImportPayload.MieruProfile(MieruDeepLink.parse(value))
        }
        val report = SubscriptionParser.parseReport(LOCAL_SUBSCRIPTION_ID, value)
        require(report.profiles.isNotEmpty() && report.unsupportedCount == 0 && report.invalidCount == 0) {
            "Unsupported or invalid server URI"
        }
        return ImportPayload.Profiles(report.profiles)
    }

    private fun parseSingBoxDocument(value: String): List<ServerProfile> {
        val root = JSONObject(value)
        val outbounds = root.optJSONArray("outbounds") ?: error("Sing-box document has no outbounds")
        val profiles = buildList {
            for (index in 0 until outbounds.length()) {
                val outbound = outbounds.optJSONObject(index) ?: continue
                val type = outbound.optString("type")
                if (type !in setOf("naive", "mieru")) continue
                val address = outbound.optString("server").takeIf(String::isNotBlank)
                    ?: error("$type outbound has no server")
                val port = when {
                    outbound.has("server_port") -> outbound.optInt("server_port").also {
                        require(it in 1..65535) { "$type outbound has invalid port" }
                    }
                    type == "mieru" -> mieruDisplayPort(outbound)
                    else -> error("$type outbound has invalid port")
                }
                val username = outbound.optString("username")
                val password = outbound.optString("password")
                require(username.isNotBlank() && password.isNotBlank()) { "$type outbound has no credentials" }
                val tls = outbound.optJSONObject("tls")
                if (type == "naive") require(tls?.optBoolean("enabled") == true && !tls.optBoolean("insecure")) {
                    "Naive outbound requires verified TLS"
                }
                if (type == "mieru") {
                    require(outbound.optString("transport").uppercase() in setOf("TCP", "UDP")) {
                        "Mieru outbound has invalid transport"
                    }
                }
                val name = outbound.optString("tag").takeIf(String::isNotBlank) ?: "$address:$port"
                val config = JSONObject()
                    .put("maxspeedvpn_format", "sing-box")
                    .put("outbound", JSONObject(outbound.toString()).put("tag", "proxy"))
                    .toString()
                val id = UUID.nameUUIDFromBytes("$LOCAL_SUBSCRIPTION_ID:$type:$address:$port:$name".toByteArray()).toString()
                add(ServerProfile(id, LOCAL_SUBSCRIPTION_ID, name, type, address, port, config))
            }
        }
        require(profiles.isNotEmpty()) { "Sing-box document has no supported proxy outbounds" }
        return profiles.distinctBy { "${it.protocol}:${it.address}:${it.port}:${it.name}" }
    }

    private fun mieruDisplayPort(outbound: JSONObject): Int {
        val ports = outbound.optJSONArray("server_ports") ?: error("mieru outbound has invalid port")
        require(ports.length() > 0) { "mieru outbound has invalid port" }
        val first = ports.optString(0)
        val bounds = first.split('-', limit = 2).map { it.toIntOrNull() ?: error("mieru outbound has invalid port") }
        require(bounds.size in 1..2 && bounds.all { it in 1..65535 }) { "mieru outbound has invalid port" }
        require(bounds.size == 1 || bounds[0] <= bounds[1]) { "mieru outbound has invalid port" }
        for (index in 1 until ports.length()) {
            val range = ports.optString(index)
            val values = range.split('-', limit = 2).map { it.toIntOrNull() ?: error("mieru outbound has invalid port") }
            require(values.size in 1..2 && values.all { it in 1..65535 } && (values.size == 1 || values[0] <= values[1])) {
                "mieru outbound has invalid port"
            }
        }
        return bounds[0]
    }
}
