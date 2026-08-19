package com.envy.maxspeedvpn.subscription

data class SubscriptionUsage(
    val uploadBytes: Long? = null,
    val downloadBytes: Long? = null,
    val totalBytes: Long? = null,
    val expiresAtEpochSeconds: Long? = null,
) {
    val usedBytes: Long?
        get() = when {
            uploadBytes == null && downloadBytes == null -> null
            else -> runCatching { Math.addExact(uploadBytes ?: 0L, downloadBytes ?: 0L) }.getOrNull()
        }

    /** Providers conventionally report total=0 for an unlimited plan. */
    val quotaBytes: Long?
        get() = totalBytes?.takeIf { it > 0L }
}

object SubscriptionUsageParser {
    fun parse(header: String?): SubscriptionUsage? {
        val values = header.orEmpty().split(';').mapNotNull { part ->
            val pair = part.trim().split('=', limit = 2)
            val key = pair.getOrNull(0)?.trim()?.lowercase().orEmpty()
            val value = pair.getOrNull(1)?.trim()?.toLongOrNull()?.takeIf { it >= 0L }
            if (key.isBlank() || value == null) null else key to value
        }.toMap()
        if (values.isEmpty()) return null
        return SubscriptionUsage(
            uploadBytes = values["upload"],
            downloadBytes = values["download"],
            totalBytes = values["total"],
            expiresAtEpochSeconds = values["expire"]?.takeIf { it > 0L },
        )
    }
}
