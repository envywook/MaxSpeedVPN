package com.envy.maxspeedvpn.routing

import org.json.JSONArray
import org.json.JSONObject
import java.net.IDN
import java.net.Inet6Address
import java.net.InetAddress

enum class RoutingMode {
    ALL,
    BYPASS_LAN,
    CUSTOM,
}

data class RoutingPolicy(
    val mode: RoutingMode = RoutingMode.ALL,
    val domains: List<String> = emptyList(),
    val ipCidrs: List<String> = emptyList(),
) {
    companion object {
        fun parse(mode: RoutingMode, value: String): RoutingPolicy {
            if (mode != RoutingMode.CUSTOM) return RoutingPolicy(mode)
            require(value.length <= 4096) { "Список исключений слишком большой" }
            val domains = linkedSetOf<String>()
            val cidrs = linkedSetOf<String>()
            value.lineSequence().map(String::trim).filter(String::isNotEmpty).forEach { entry ->
                when {
                    entry.contains("://") -> throw IllegalArgumentException("Укажи домен без http:// или пути: $entry")
                    entry.contains(':') -> cidrs += normalizeIpv6Cidr(entry)
                    entry.contains('/') || entry.matches(Regex("[0-9.]+")) -> cidrs += normalizeIpv4Cidr(entry)
                    else -> domains += normalizeDomain(entry)
                }
            }
            require(domains.isNotEmpty() || cidrs.isNotEmpty()) { "Добавь хотя бы один домен или IP/CIDR" }
            return RoutingPolicy(mode, domains.toList(), cidrs.toList())
        }

        private fun normalizeDomain(value: String): String {
            val raw = value.removePrefix("*.").removePrefix(".").lowercase()
            require(raw.length in 1..253 && !raw.contains('/') && !raw.contains(':')) { "Некорректный домен: $value" }
            val ascii = runCatching { IDN.toASCII(raw) }.getOrElse { throw IllegalArgumentException("Некорректный домен: $value") }
            require(ascii.split('.').all { label -> label.isNotEmpty() && label.length <= 63 && label.first() != '-' && label.last() != '-' && label.all { it.isLetterOrDigit() || it == '-' } }) {
                "Некорректный домен: $value"
            }
            return ascii
        }

        private fun normalizeIpv6Cidr(value: String): String {
            require('%' !in value) { "Некорректный IP/CIDR: $value" }
            val parts = value.split('/', limit = 2)
            val address = runCatching { InetAddress.getByName(parts[0]) }.getOrNull()
            require(address is Inet6Address) { "Некорректный IP/CIDR: $value" }
            val prefix = if (parts.size == 2) parts[1].toIntOrNull() else 128
            require(prefix != null && prefix in 0..128) { "Некорректный IP/CIDR: $value" }
            return requireNotNull(address.hostAddress) + "/$prefix"
        }

        private fun normalizeIpv4Cidr(value: String): String {
            val parts = value.split('/', limit = 2)
            val octets = parts[0].split('.').map { it.toIntOrNull() ?: throw IllegalArgumentException("Некорректный IP/CIDR: $value") }
            require(octets.size == 4 && octets.all { it in 0..255 }) { "Некорректный IP/CIDR: $value" }
            val prefix = if (parts.size == 2) parts[1].toIntOrNull() else 32
            require(prefix != null && prefix in 0..32) { "Некорректный IP/CIDR: $value" }
            return octets.joinToString(".") + "/$prefix"
        }
    }
}

object XrayRouting {
    private val PRIVATE_NETWORKS = listOf(
        "10.0.0.0/8",
        "100.64.0.0/10",
        "127.0.0.0/8",
        "169.254.0.0/16",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "::1/128",
        "fc00::/7",
        "fe80::/10",
    )

    fun apply(config: String, policy: RoutingPolicy): String {
        if (policy.mode == RoutingMode.ALL) return config
        val root = JSONObject(config)
        val outbounds = root.optJSONArray("outbounds") ?: JSONArray().also { root.put("outbounds", it) }
        if ((0 until outbounds.length()).none { outbounds.optJSONObject(it)?.optString("tag") == "direct" }) {
            outbounds.put(JSONObject().put("tag", "direct").put("protocol", "freedom").put("settings", JSONObject()))
        }
        val routing = root.optJSONObject("routing") ?: JSONObject().also { root.put("routing", it) }
        val existingRules = routing.optJSONArray("rules") ?: JSONArray()
        val rules = JSONArray().put(buildDirectRule(policy))
        repeat(existingRules.length()) { rules.put(existingRules.get(it)) }
        routing.put("domainStrategy", routing.optString("domainStrategy", "IPIfNonMatch"))
        routing.put("rules", rules)
        return root.toString()
    }

    private fun buildDirectRule(policy: RoutingPolicy): JSONObject = JSONObject().apply {
        put("type", "field")
        put("outboundTag", "direct")
        val domains = JSONArray()
        val ips = JSONArray()
        if (policy.mode == RoutingMode.BYPASS_LAN) PRIVATE_NETWORKS.forEach(ips::put)
        policy.domains.forEach { domains.put("domain:$it") }
        policy.ipCidrs.forEach(ips::put)
        if (domains.length() > 0) put("domain", domains)
        if (ips.length() > 0) put("ip", ips)
    }
}
