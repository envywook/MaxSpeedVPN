package com.envy.dualcorevpn.core

import org.json.JSONObject

/** Selects a core from the actual profile format, not a UI preference. */
object EngineSelector {
    fun select(config: String, compatibleDefault: EngineKind = EngineKind.XRAY): EngineKind {
        val root = JSONObject(config)
        val nativeOutbound = root.optJSONObject("outbound")
        if (root.optString("maxspeedvpn_format") == "sing-box" || root.optString("lust_format") == "sing-box") {
            return when (nativeOutbound?.optString("type")?.lowercase()) {
                "mieru", "naive" -> EngineKind.SING_BOX
                else -> EngineKind.SING_BOX
            }
        }
        val proxy = root.optJSONArray("outbounds")
            ?.let { outbounds ->
                (0 until outbounds.length()).asSequence().mapNotNull(outbounds::optJSONObject)
                    .firstOrNull { it.optString("protocol") !in setOf("freedom", "blackhole", "dns") }
            }
            ?: return compatibleDefault
        if (proxy.optString("protocol").lowercase() == "vless") {
            val stream = proxy.optJSONObject("streamSettings")
            val network = stream?.optString("network")?.lowercase()
            val security = stream?.optString("security")?.lowercase()
            if (security == "reality" || (network == "xhttp" && security == "tls")) return EngineKind.XRAY
        }
        return compatibleDefault
    }
}
